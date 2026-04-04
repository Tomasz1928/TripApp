package com.example.tripapp2.data.repository

import android.util.Log
import com.example.tripapp2.data.network.GraphQLDataSource

/**
 * CurrencyRepository — singleton przechowujący listę dostępnych walut z backendu.
 *
 * Strategia:
 * - Waluty pobierane raz przy starcie (po zalogowaniu) z query `availableCurrencies`
 * - Cachowane w pamięci RAM (nie potrzebują persystencji — rzadko się zmieniają)
 * - Jeśli pobranie się nie powiedzie → fallback na minimalną listę
 * - Wszystkie ViewModele i Fragmenty korzystają z getCurrencies() zamiast hardcoded list
 *
 * INICJALIZACJA:
 * - Wołane z TripRepository.loadInitialData() lub osobno po loginie
 * - Nie wymaga Context (korzysta z GraphQLDataSource singleton)
 */
class CurrencyRepository private constructor() {

    private val graphQL = GraphQLDataSource.getInstance()

    /**
     * Cache walut — po załadowaniu z API nie zmienia się do końca sesji.
     */
    @Volatile
    private var cachedCurrencies: List<String>? = null

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
     * Zwraca listę dostępnych walut.
     *
     * - Jeśli waluty zostały załadowane z API → zwraca je
     * - Jeśli nie → zwraca fallback
     *
     * UŻYCIE: ViewModele i Fragmenty wołają tę metodę zamiast
     * trzymać własne hardcoded listy.
     */
    fun getCurrencies(): List<String> {
        return cachedCurrencies ?: FALLBACK_CURRENCIES
    }

    /**
     * Czy waluty zostały pomyślnie załadowane z API?
     */
    fun isLoaded(): Boolean = cachedCurrencies != null

    /**
     * Pobiera waluty z backendu i cachuje je.
     *
     * Wywoływane z TripRepository.loadInitialData() — razem z ładowaniem tripów.
     * Bezpieczne do wielokrotnego wywołania (nadpisuje cache).
     */
    suspend fun loadCurrencies() {
        try {
            val result = graphQL.getAvailableCurrencies()
            result.onSuccess { currencies ->
                cachedCurrencies = currencies
                Log.d(TAG, "Loaded ${currencies.size} currencies from API")
            }.onFailure { e ->
                Log.w(TAG, "Failed to load currencies from API, using fallback", e)
                // Nie nadpisujemy cache — jeśli wcześniej się udało, zachowujemy stare dane
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading currencies", e)
        }
    }

    /**
     * Czyści cache (np. przy wylogowaniu).
     */
    fun clear() {
        cachedCurrencies = null
        Log.d(TAG, "Currency cache cleared")
    }
}