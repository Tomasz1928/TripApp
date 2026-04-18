package com.example.tripapp2.data.repository

import android.content.Context
import android.util.Log
import com.example.tripapp2.data.cache.TripCacheManager
import com.example.tripapp2.data.model.*
import com.example.tripapp2.data.network.GraphQLDataSource
import com.example.tripapp2.data.network.SessionManager
import com.example.tripapp2.ui.tripdetails.settlements.SettleByCostsItemInput
import com.example.tripapp2.ui.tripdetails.settlements.SettleByCostsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * TripRepository — centralny punkt zarządzania danymi i subskrypcjami.
 *
 * Strategia:
 * - Mutations zwracają tylko success/message (brak trip w response)
 * - Po udanej mutacji → refetch tripDetails aby odświeżyć cache
 * - Subskrypcje WebSocket żyją w repozytorium (nie w ViewModelach)
 *   → przetrwają zmianę fragmentów i ekranów
 * - ViewModele obserwują dane przez StateFlow (observeTrip)
 * - Cache persystowany w pamięci telefonu (TripCacheManager)
 *
 * WAŻNE: Wymaga Context do inicjalizacji (dla Apollo Client).
 * Użyj getInstance(context) zamiast getInstance().
 *
 * POPRAWKI vs oryginał:
 * - loadInitialData() automatycznie startuje subskrypcje WS po załadowaniu danych
 * - joinTrip() nie musi osobno wołać startSubscriptionsForAllTrips() (bo robi to loadInitialData)
 * - Import SessionManager z pełnym pakietem
 */
class TripRepository private constructor(context: Context) {

    private val graphQL = GraphQLDataSource.getInstance()
    private val sessionManager by lazy { SessionManager.getInstance() }
    private val currencyRepository by lazy { CurrencyRepository.getInstance() }
    private val cacheManager by lazy { TripCacheManager.getInstance() }
    private val tripsCache = mutableMapOf<String, TripDto>()
    private var cachedUserInfo: UserInfoDto? = null

    // ==========================================
    // COROUTINE SCOPE (żyje tak długo jak repo)
    // ==========================================

    /**
     * Scope repozytorium — subskrypcje uruchomione tutaj przetrwają
     * zmianę fragmentów, ViewModeli itd.
     * SupervisorJob: crash jednej subskrypcji nie zabija reszty.
     */
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ==========================================
    // REACTIVE TRIP OBSERVATION (StateFlow)
    // ==========================================

    /**
     * Mapa StateFlow per trip — ViewModele obserwują te flow
     * i automatycznie dostają aktualizacje z subskrypcji.
     */
    private val _tripFlows = mutableMapOf<String, MutableStateFlow<TripDto?>>()

    /**
     * Obserwuj zmiany danego tripa w czasie rzeczywistym.
     * Każdy ViewModel może collectować ten Flow aby reagować na delty.
     */
    fun observeTrip(tripId: String): StateFlow<TripDto?> {
        return _tripFlows.getOrPut(tripId) {
            MutableStateFlow(tripsCache[tripId])
        }.asStateFlow()
    }

    // ==========================================
    // CACHE CHANGE FLOW (for Dashboard)
    // ==========================================

    private val _cacheChangeFlow = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 5
    )
    val cacheChangeFlow: SharedFlow<Unit> = _cacheChangeFlow.asSharedFlow()

    /**
     * Emituje zmianę do StateFlow po aktualizacji cache.
     */
    private fun emitTripUpdate(tripId: String) {
        val trip = tripsCache[tripId]
        _tripFlows[tripId]?.value = trip

        _cacheChangeFlow.tryEmit(Unit)
    }

    private val _notificationFlow = MutableSharedFlow<TripNotificationDto>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val notificationFlow: SharedFlow<TripNotificationDto> = _notificationFlow.asSharedFlow()

    // ==========================================
    // SUBSCRIPTION MANAGEMENT
    // ==========================================

    /**
     * Mapa aktywnych jobów subskrypcji per trip.
     * Klucz = tripId, wartość = Job coroutine.
     */
    private val subscriptionJobs = mutableMapOf<String, Job>()

    /**
     * Startuje subskrypcje WebSocket na WSZYSTKIE tripy z cache.
     * Wywoływane automatycznie na koniec loadInitialData().
     *
     * Logika:
     * - Dla każdego tripa w cache, jeśli subskrypcja jeszcze nie działa → startuj
     * - Retry: do 5 prób z 3s opóźnieniem
     * - Przy trwałym błędzie → loguje, nie crashuje
     */
    fun startSubscriptionsForAllTrips() {
        val tripIds = tripsCache.keys.toList()
        Log.d(TAG, "Starting subscriptions for ${tripIds.size} trips: $tripIds")

        for (tripId in tripIds) {
            startSubscriptionForTrip(tripId)
        }
    }

    /**
     * Startuje subskrypcję dla jednego tripa (jeśli jeszcze nie działa).
     */
    fun startSubscriptionForTrip(tripId: String) {
        val existingJob = subscriptionJobs[tripId]
        if (existingJob != null && existingJob.isActive) {
            Log.d(TAG, "Subscription for trip $tripId already active, skipping")
            return
        }

        Log.d(TAG, "Starting subscription for trip $tripId")

        val job = repoScope.launch {
            graphQL.subscribeTripUpdates(tripId.toInt())
                .retry(retries = 5) { cause ->
                    Log.w(TAG, "Subscription error for trip $tripId, retrying...", cause)
                    delay(3000L)
                    true
                }
                .catch { e ->
                    Log.e(TAG, "Subscription failed permanently for trip $tripId", e)
                }
                .collect { notification ->
                    Log.d(TAG, "Received notification for trip $tripId: ${notification.eventType} by ${notification.actorNickname}")
                    _notificationFlow.emit(notification)
                }
        }

        subscriptionJobs[tripId] = job
    }

    /**
     * Zatrzymuje subskrypcję dla jednego tripa.
     */
    fun stopSubscriptionForTrip(tripId: String) {
        subscriptionJobs[tripId]?.cancel()
        subscriptionJobs.remove(tripId)
        Log.d(TAG, "Stopped subscription for trip $tripId")
    }

    /**
     * Zatrzymuje wszystkie subskrypcje.
     */
    fun stopAllSubscriptions() {
        subscriptionJobs.values.forEach { it.cancel() }
        subscriptionJobs.clear()
        Log.d(TAG, "Stopped all subscriptions")
    }

    companion object {
        private const val TAG = "TripRepository"

        @Volatile
        private var INSTANCE: TripRepository? = null

        fun getInstance(context: Context): TripRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TripRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getInstance(): TripRepository {
            return INSTANCE ?: throw IllegalStateException(
                "TripRepository not initialized. Call getInstance(context) first."
            )
        }
    }

    // ==========================================
    // PERSISTENT CACHE (NOWE)
    // ==========================================

    /**
     * Wczytuje dane z pamięci telefonu do RAM cache.
     * Wywoływane przy starcie aplikacji PRZED loadInitialData().
     */
    fun loadFromPersistentCache(): Boolean {
        val persistedTrips = cacheManager.getAllTrips()
        if (persistedTrips.isNotEmpty()) {
            tripsCache.putAll(persistedTrips)
            persistedTrips.keys.forEach { tripId -> emitTripUpdate(tripId) }
            Log.d(TAG, "Loaded ${persistedTrips.size} trips from persistent cache")
        }

        val persistedUserInfo = cacheManager.getUserInfo()
        if (persistedUserInfo != null) {
            cachedUserInfo = persistedUserInfo
        }

        return persistedTrips.isNotEmpty()
    }

    /** Zapisuje cały RAM cache do pamięci telefonu. */
    private fun persistToStorage() {
        cacheManager.saveAllTrips(tripsCache)
    }

    /** Zapisuje pojedynczy trip do pamięci telefonu. */
    private fun persistTrip(tripId: String) {
        tripsCache[tripId]?.let { cacheManager.saveTrip(it) }
    }

    // ==========================================
    // AUTH
    // ==========================================

    suspend fun login(username: String, password: String): Result<AuthResultDto> {
        val result = graphQL.login(username, password)
        result.onSuccess { auth ->
            if (auth.success && auth.user != null) {
                cachedUserInfo = auth.user
                cacheManager.saveUserInfo(auth.user)
            }
        }
        return result
    }

    suspend fun register(username: String, password: String, email: String): Result<AuthResultDto> {
        val result = graphQL.register(username, password, email)
        result.onSuccess { auth ->
            if (auth.success && auth.user != null) {
                cachedUserInfo = auth.user
                cacheManager.saveUserInfo(auth.user)
            }
        }
        return result
    }

    suspend fun resetPassword(username: String, email: String): Result<AuthResultDto> {
        return graphQL.resetPassword(username, email)
    }

    suspend fun changeEmail(newEmail: String): Result<AuthResultDto> {
        return graphQL.changeEmail(newEmail)
    }

    suspend fun changePassword(newPassword: String, newPasswordConfirm: String): Result<AuthResultDto> {
        val result = graphQL.changePassword(newPassword, newPasswordConfirm)
        result.onSuccess { auth ->
            if (auth.success) {
                // Wyczyść sesję — użytkownik musi się zalogować ponownie
                stopAllSubscriptions()
                cachedUserInfo = null
                tripsCache.clear()
                _tripFlows.clear()
                sessionManager.clearSession()
                cacheManager.clearAll()
            }
        }
        return result
    }

    suspend fun logout(): Result<AuthResultDto> {
        val result = graphQL.logout()
        result.onSuccess {
            stopAllSubscriptions()
            cachedUserInfo = null
            tripsCache.clear()
            _tripFlows.clear()
            sessionManager.clearSession()
            cacheManager.clearAll()
            currencyRepository.clear()
        }
        return result
    }

    suspend fun checkSession(): Result<SessionDto> {
        val result = graphQL.getSession()
        result.onSuccess { session ->
            if (session.isAuthenticated && session.user != null) {
                cachedUserInfo = session.user
                cacheManager.saveUserInfo(session.user)
            }
        }
        return result
    }

    suspend fun getCurrentUserInfo(): UserInfoDto {
        cachedUserInfo?.let { return it }

        cacheManager.getUserInfo()?.let {
            cachedUserInfo = it
            return it
        }

        val sessionResult = graphQL.getSession()
        sessionResult.onSuccess { session ->
            session.user?.let {
                cachedUserInfo = it
                return it
            }
        }

        throw Exception("User not authenticated")
    }

    // ==========================================
    // TRIP LOADING
    // ==========================================

    /**
     * Ładuje listę tripów, pobiera ich detale i startuje subskrypcje WS.
     *
     * POPRAWKA: Na koniec automatycznie woła startSubscriptionsForAllTrips(),
     * dzięki czemu callery (SplashActivity, LoginActivity, RegisterActivity)
     * nie muszą tego robić osobno.
     */
    suspend fun loadInitialData(): Result<TripListDto> {
        return try {
            val result = graphQL.getTripList()
            result.onSuccess { tripListDto ->
                currencyRepository.loadCurrencies()
                val tripIds = tripListDto.trips ?: emptyList()
                Log.d(TAG, "Trip list loaded: ${tripIds.size} trips, fetching full details...")
                coroutineScope {
                    tripIds.map { tripIdDto ->
                        async {
                            try {
                                val detailResult = graphQL.getTripDetails(tripIdDto.id.toInt())
                                detailResult.onSuccess { fullTrip ->
                                    tripsCache[fullTrip.id] = fullTrip
                                    emitTripUpdate(fullTrip.id)
                                    Log.d(TAG, "Full details loaded for trip ${fullTrip.id}: ${fullTrip.title}")
                                }.onFailure { e ->
                                    Log.w(TAG, "Failed to load details for trip ${tripIdDto.id}", e)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Exception loading details for trip ${tripIdDto.id}", e)
                            }
                        }
                    }.forEach { it.await() }
                }
                persistToStorage()

                // POPRAWKA: Automatycznie startuj subskrypcje WS po załadowaniu danych
                // Dzięki temu callery nie muszą tego robić osobno
                startSubscriptionsForAllTrips()
                Log.d(TAG, "loadInitialData completed: ${tripsCache.size} trips cached, subscriptions started")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "loadInitialData error", e)
            Result.failure(e)
        }
    }

    fun getTripDetails(tripId: String): TripDto? {
        return tripsCache[tripId]
    }

    fun getCachedTrip(tripId: String): TripDto? {
        return tripsCache[tripId]
    }

    /**
     * Pobiera trip details z API i aktualizuje cache.
     * Startuje subskrypcję jeśli jeszcze nie działa.
     */
    suspend fun refreshTripDetails(tripId: String): Result<TripDto> {
        return try {
            val result = graphQL.getTripDetails(tripId.toInt())
            result.onSuccess { trip ->
                tripsCache[trip.id] = trip
                emitTripUpdate(trip.id)
                persistTrip(trip.id)
                startSubscriptionForTrip(trip.id)
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "RefreshTripDetails error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAndCacheTripDetails(tripId: String): Result<TripDto> {
        return refreshTripDetails(tripId)
    }

    fun getAllTripsFromCache(): List<TripDto> {
        return tripsCache.values.toList()
    }

    // ==========================================
    // TRIP CRUD
    // ==========================================

    suspend fun createTrip(
        title: String,
        description: String,
        dateStart: Long,
        dateEnd: Long,
        currency: String
    ): Result<CreateTripDto> {
        return try {
            val result = graphQL.createTrip(title, dateStart, dateEnd, description, currency)
            result.onSuccess { createTrip ->
                if (createTrip.success && createTrip.trip != null) {
                    val detailsResult = graphQL.getTripDetails(createTrip.trip)
                    detailsResult.onSuccess { trip ->
                        tripsCache[trip.id] = trip
                        emitTripUpdate(trip.id)
                        persistTrip(trip.id)
                        startSubscriptionForTrip(trip.id)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dołącza do tripu i przeładowuje dane.
     *
     * POPRAWKA: loadInitialData() teraz automatycznie startuje subskrypcje WS,
     * więc po joinTrip nowy trip będzie miał subskrypcję.
     */
    suspend fun joinTrip(accessCode: String): Result<SuccessDto> {
        return try {
            val result = graphQL.joinTrip(accessCode)

            result.onSuccess { joinTrip ->
                if (joinTrip.success) {
                    // Przeładuj listę i pobierz detale — loadInitialData()
                    // automatycznie startuje subskrypcje na WSZYSTKIE tripy (w tym nowy)
                    loadInitialData()
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // EXPENSES
    // ==========================================

    suspend fun addExpense(request: AddExpenseRequest): Result<AddExpenseDto> {
        return try {
            val result = graphQL.addExpense(request)

            result.fold(
                onSuccess = { successDto ->
                    if (successDto.success) {
                        refreshTripDetails(request.tripId)
                        val updatedTrip = tripsCache[request.tripId]
                        Result.success(AddExpenseDto(
                            success = successDto,
                            trip = updatedTrip
                        ))
                    } else {
                        Result.failure(Exception(successDto.message))
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExpense(request: UpdateExpenseRequest): Result<UpdateExpenseDto> {
        return try {
            val result = graphQL.updateExpense(request)

            result.fold(
                onSuccess = { successDto ->
                    if (successDto.success) {
                        refreshTripDetails(request.tripId)
                        val updatedTrip = tripsCache[request.tripId]
                        Result.success(UpdateExpenseDto(
                            success = successDto,
                            trip = updatedTrip
                        ))
                    } else {
                        Result.failure(Exception(successDto.message))
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(tripId: String, expenseId: String): Result<SuccessDto> {
        return try {
            val result = graphQL.deleteExpense(tripId, expenseId)

            result.onSuccess { successDto ->
                if (successDto.success) {
                    refreshTripDetails(tripId)
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // PARTICIPANTS
    // ==========================================

    suspend fun addPlaceholder(tripId: String, nickname: String): Result<SuccessDto> {
        return try {
            val result = graphQL.addPlaceholder(tripId, nickname)

            result.onSuccess { successDto ->
                if (successDto.success) {
                    refreshTripDetails(tripId)
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun detachUser(tripId: String, participantId: String): Result<SuccessDto> {
        return try {
            val result = graphQL.detachUser(tripId, participantId)

            result.onSuccess { successDto ->
                if (successDto.success) {
                    refreshTripDetails(tripId)
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removePlaceholder(tripId: String, participantId: String): Result<SuccessDto> {
        return try {
            val result = graphQL.removePlaceholder(tripId, participantId)

            result.onSuccess { successDto ->
                if (successDto.success) {
                    refreshTripDetails(tripId)
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // PREPAYMENTS
    // ==========================================

    suspend fun addPrepayment(
        tripId: String,
        participantId: String,
        amount: Float,
        currency: String,
        direction: String
    ): Result<SuccessDto> {
        return try {
            val result = graphQL.addPrepayment(tripId, participantId, amount, currency, direction)

            result.onSuccess { successDto ->
                if (successDto.success) {
                    refreshTripDetails(tripId)
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // SETTLEMENTS
    // ==========================================

    suspend fun settleByAmount(
        tripId: String,
        fromUserId: String,
        toUserId: String,
        amount: Float,
        currency: String,
        isMainCurrency: Boolean
    ): Result<SettlementResultDto> {
        return try {
            val result = graphQL.settleByAmount(
                tripId, fromUserId, toUserId, amount, currency, isMainCurrency
            )

            result.fold(
                onSuccess = { successDto ->
                    if (successDto.success) {
                        refreshTripDetails(tripId)
                        val updatedTrip = tripsCache[tripId]
                        Result.success(SettlementResultDto(
                            success = successDto,
                            trip = updatedTrip
                        ))
                    } else {
                        Result.failure(Exception(successDto.message))
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun settleByCosts(
        tripId: String,
        items: List<SettleByCostsItemInput>
    ): Result<SettlementResultDto> {
        return try {
            val result = graphQL.settleByCosts(tripId, items)

            result.fold(
                onSuccess = { successDto ->
                    if (successDto.success) {
                        refreshTripDetails(tripId)
                        val updatedTrip = tripsCache[tripId]
                        Result.success(SettlementResultDto(
                            success = successDto,
                            trip = updatedTrip
                        ))
                    } else {
                        Result.failure(Exception(successDto.message))
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // CACHE MANAGEMENT
    // ==========================================

    fun clearCache() {
        stopAllSubscriptions()
        tripsCache.clear()
        cachedUserInfo = null
        _tripFlows.clear()
        cacheManager.clearAll()
    }
}