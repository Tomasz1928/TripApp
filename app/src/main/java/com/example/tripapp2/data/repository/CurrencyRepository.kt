package com.example.tripapp2.data.repository

import android.util.Log
import com.example.tripapp2.data.network.GraphQLDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * CurrencyRepository — singleton przechowujący listę dostępnych walut z backendu.
 *
 * Strategia:
 * - Waluty pobierane raz z query `availableCurrencies` i cachowane w RAM
 * - getCurrencies() zwraca cache jeśli dostępny, inaczej fallback
 * - ensureLoaded() — suspend, gwarantuje że cache jest załadowany (lazy-load)
 *   Jeśli cache jest pusty → pobiera z API. Jeśli już załadowany → no-op.
 *   Bezpieczny do wielokrotnego wywołania (Mutex zapobiega race conditions).
 * - loadCurrencies() — wywołanie z TripRepository.loadInitialData() (eager-load)
 * - Fallback na minimalną listę gdy API nieosiągalne
 *
 * UŻYCIE W VIEWMODELACH:
 *   viewModelScope.launch {
 *       CurrencyRepository.getInstance().ensureLoaded()
 *       val currencies = CurrencyRepository.getInstance().getCurrencies()
 *   }
 *
 * UŻYCIE SYNCHRONICZNE (gdy wiadomo że cache jest gotowy):
 *   val currencies = CurrencyRepository.getInstance().getCurrencies()
 */
class CurrencyRepository private constructor() {

    private val graphQL = GraphQLDataSource.getInstance()

    /**
     * Cache walut — po załadowaniu z API nie zmienia się do końca sesji.
     */
    @Volatile
    private var cachedCurrencies: List<String>? = null

    /**
     * Mutex chroniący przed wielokrotnym równoczesnym pobieraniem.
     * Jeśli dwa ViewModele wołają ensureLoaded() jednocześnie,
     * tylko pierwszy faktycznie odpali request — drugi czeka i dostaje gotowy cache.
     */
    private val loadMutex = Mutex()

    companion object {
        private const val TAG = "CurrencyRepository"

        /**
         * Minimalna lista walut — używana TYLKO gdy API jest nieosiągalne.
         * Celowo krótka, żeby zmotywować do naprawy połączenia.
         */
        private val FALLBACK_CURRENCIES = listOf(
            "PLN", "EUR", "USD", "GBP", "CHF", "CZK", "NOK", "SEK", "DKK"
        )

        @Volatile
        private var INSTANCE: CurrencyRepository? = null

        fun getInstance(): CurrencyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CurrencyRepository().also { INSTANCE = it }
            }
        }
    }

    // ==========================================
    // PUBLIC API
    // ==========================================

    /**
     * Zwraca listę dostępnych walut (synchronicznie).
     *
     * - Jeśli waluty zostały załadowane z API → zwraca je
     * - Jeśli nie → zwraca fallback
     *
     * UŻYCIE: ViewModele i Fragmenty wołają tę metodę.
     * Dla pewności że cache jest gotowy, najpierw wywołaj ensureLoaded().
     */
    fun getCurrencies(): List<String> {
        return cachedCurrencies ?: FALLBACK_CURRENCIES
    }

    /**
     * Czy waluty zostały pomyślnie załadowane z API?
     */
    fun isLoaded(): Boolean = cachedCurrencies != null

    /**
     * Gwarantuje że cache jest załadowany (lazy-load).
     *
     * - Jeśli cache jest gotowy → natychmiast wraca (no-op)
     * - Jeśli cache jest pusty → pobiera z API i cachuje
     * - Thread-safe dzięki Mutex (tylko jeden request na raz)
     *
     * UŻYCIE: Wołaj na początku ViewModelu lub przed setupCurrencyDropdown()
     */
    suspend fun ensureLoaded() {
        // Fast path — cache już gotowy, bez locka
        if (cachedCurrencies != null) return

        // Slow path — pobierz z API (Mutex gwarantuje że tylko raz)
        loadMutex.withLock {
            // Double-check po uzyskaniu locka
            if (cachedCurrencies != null) return

            loadFromApi()
        }
    }

    /**
     * Pobiera waluty z backendu i cachuje je.
     *
     * Wywoływane z TripRepository.loadInitialData() — razem z ładowaniem tripów.
     * Bezpieczne do wielokrotnego wywołania (nadpisuje cache).
     */
    suspend fun loadCurrencies() {
        loadFromApi()
    }

    /**
     * Czyści cache (np. przy wylogowaniu).
     */
    fun clear() {
        cachedCurrencies = null
        Log.d(TAG, "Currency cache cleared")
    }

    // ==========================================
    // PRIVATE
    // ==========================================

    private suspend fun loadFromApi() {
        try {
            val result = graphQL.getAvailableCurrencies()
            result.onSuccess { currencies ->
                cachedCurrencies = currencies
                Log.d(TAG, "Loaded ${currencies.size} currencies from API")
            }.onFailure { e ->
                Log.w(TAG, "Failed to load currencies from API, using fallback", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading currencies", e)
        }
    }
}