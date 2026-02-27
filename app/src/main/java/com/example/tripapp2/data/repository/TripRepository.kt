package com.example.tripapp2.data.repository

import android.content.Context
import android.util.Log
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

/**
 * TripRepository — centralny punkt zarządzania danymi i subskrypcjami.
 *
 * Strategia:
 * - Mutations zwracają tylko success/message (brak trip w response)
 * - Po udanej mutacji → refetch tripDetails aby odświeżyć cache
 * - Subskrypcje WebSocket żyją w repozytorium (nie w ViewModelach)
 *   → przetrwają zmianę fragmentów i ekranów
 * - ViewModele obserwują dane przez StateFlow (observeTrip)
 *
 * WAŻNE: Wymaga Context do inicjalizacji (dla Apollo Client).
 * Użyj getInstance(context) zamiast getInstance().
 */
class TripRepository private constructor(context: Context) {

    private val graphQL = GraphQLDataSource.getInstance(context)
    private val sessionManager = SessionManager(context)
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

    /**
     * Emituje zmianę do StateFlow po aktualizacji cache.
     */
    private fun emitTripUpdate(tripId: String) {
        val trip = tripsCache[tripId]
        _tripFlows[tripId]?.value = trip
    }

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
     * Wywoływane po loadInitialData() i po fetchAndCacheTripDetails().
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
        // Jeśli już działa — skip
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
                .collect { delta ->
                    Log.d(TAG, "Received delta for trip $tripId: ${delta.eventType}")
                    applyDelta(delta)
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
    // AUTH
    // ==========================================

    suspend fun login(username: String, password: String): Result<AuthResultDto> {
        val result = graphQL.login(username, password)
        result.onSuccess { auth ->
            if (auth.success && auth.user != null) {
                cachedUserInfo = auth.user
            }
        }
        return result
    }

    suspend fun register(username: String, password: String): Result<AuthResultDto> {
        val result = graphQL.register(username, password)
        result.onSuccess { auth ->
            if (auth.success && auth.user != null) {
                cachedUserInfo = auth.user
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
        }
        return result
    }

    suspend fun checkSession(): Result<SessionDto> {
        val result = graphQL.getSession()
        result.onSuccess { session ->
            if (session.isAuthenticated && session.user != null) {
                cachedUserInfo = session.user
            }
        }
        return result
    }

    suspend fun getCurrentUserInfo(): UserInfoDto {
        cachedUserInfo?.let { return it }

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
     * Ładuje listę tripów i od razu startuje subskrypcje na wszystkie.
     */
    suspend fun loadInitialData(): Result<TripListDto> {
        return try {
            val result = graphQL.getTripList()
            result.onSuccess { tripListDto ->
                val tripIds = tripListDto.trips ?: emptyList()
                Log.d(TAG, "Trip list loaded: ${tripIds.size} trips, fetching full details...")

                // Dla każdego tripa pobierz pełne detale (równolegle)
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

                Log.d(TAG, "All trip details loaded (${tripsCache.size} in cache), starting subscriptions...")
                startSubscriptionsForAllTrips()
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
                // Upewnij się że subskrypcja działa
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
                        // Nowy trip → startuj subskrypcję
                        startSubscriptionForTrip(trip.id)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinTrip(accessCode: String): Result<SuccessDto> {
        return try {
            val result = graphQL.joinTrip(accessCode)

            result.onSuccess { joinTrip ->
                if (joinTrip.success) {
                    // Przeładuj listę i startuj subskrypcje na nowe tripy
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
        direction:String
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
    // SUBSCRIPTIONS (raw Flow — do użytku wewnętrznego)
    // ==========================================

    fun subscribeTripUpdates(tripId: String): Flow<TripDeltaDto> {
        return graphQL.subscribeTripUpdates(tripId.toInt())
    }

    // ==========================================
    // DELTA APPLICATION
    // ==========================================

    /**
     * Aplikuje TripDelta do lokalnego cache i emituje zmianę przez StateFlow.
     */
    fun applyDelta(delta: TripDeltaDto) {
        val tripId = delta.tripId
        val currentTrip = tripsCache[tripId] ?: return

        var updatedTrip = currentTrip

        // Update total expenses
        delta.totalExpenses?.let { updatedTrip = updatedTrip.copy(totalExpenses = it) }

        // Update my cost
        delta.myCost?.let { updatedTrip = updatedTrip.copy(myCost = it) }

        // Update categories
        delta.categories?.let { updatedTrip = updatedTrip.copy(categories = it) }

        // Update settlement
        delta.settlement?.let { updatedTrip = updatedTrip.copy(settlement = it) }

        // Update participants
        delta.participants?.let { newParticipants ->
            val participantMap = updatedTrip.participants.associateBy { it.id }.toMutableMap()
            newParticipants.forEach { participantMap[it.id] = it }
            delta.removedParticipantIds?.forEach { participantMap.remove(it) }
            updatedTrip = updatedTrip.copy(participants = participantMap.values.toList())
        }

        // Update expenses
        delta.expenses?.let { newExpenses ->
            val expenseMap = updatedTrip.expenses.associateBy { it.id }.toMutableMap()
            newExpenses.forEach { expenseMap[it.id] = it }
            delta.removedExpenseIds?.forEach { expenseMap.remove(it) }
            updatedTrip = updatedTrip.copy(expenses = expenseMap.values.toList())
        }

        tripsCache[tripId] = updatedTrip

        // Emituj zmianę do wszystkich obserwujących ViewModeli
        emitTripUpdate(tripId)

        Log.d(TAG, "Delta applied: ${delta.eventType} for trip $tripId")
    }

    // ==========================================
    // CACHE MANAGEMENT
    // ==========================================

    fun clearCache() {
        stopAllSubscriptions()
        tripsCache.clear()
        cachedUserInfo = null
        _tripFlows.clear()
    }
}