package com.example.tripapp2.data.repository

import android.content.Context
import android.util.Log
import com.example.tripapp2.data.model.*
import com.example.tripapp2.data.network.GraphQLDataSource
import com.example.tripapp2.data.network.SessionManager
import com.example.tripapp2.ui.tripdetails.settlements.SettleByCostsItem
import com.example.tripapp2.ui.tripdetails.settlements.SettleByCostsRequest
import kotlinx.coroutines.flow.Flow

/**
 * TripRepository — refactored to use GraphQLDataSource instead of MockData.
 *
 * Strategia:
 * - Mutations zwracają tylko success/message (brak trip w response)
 * - Po udanej mutacji → refetch tripDetails aby odświeżyć cache
 * - Subscription (tripUpdates) może być użyty do real-time updates
 *
 * WAŻNE: Wymaga Context do inicjalizacji (dla Apollo Client).
 * Użyj getInstance(context) zamiast getInstance().
 */
class TripRepository private constructor(context: Context) {

    private val graphQL = GraphQLDataSource.getInstance(context)
    private val sessionManager = SessionManager(context)
    private val tripsCache = mutableMapOf<String, TripDto>()
    private var cachedUserInfo: UserInfoDto? = null

    companion object {
        private const val TAG = "TripRepository"

        @Volatile
        private var INSTANCE: TripRepository? = null

        /**
         * NOWA SYGNATURA — wymaga Context.
         * W ViewModelach: TripRepository.getInstance(application)
         */
        fun getInstance(context: Context): TripRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TripRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Kompatybilność wsteczna — używaj tylko gdy INSTANCE jest już zainicjalizowany.
         * @throws IllegalStateException jeśli nie zainicjalizowany
         */
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
            cachedUserInfo = null
            tripsCache.clear()
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

    // ==========================================
    // USER INFO
    // ==========================================

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
    // INITIAL DATA (TRIP LIST)
    // ==========================================

    suspend fun loadInitialData(): Result<TripListDto> {
        return try {
            val result = graphQL.getTripList()
            result.onSuccess { tripListDto ->
                tripListDto.trips?.forEach { trip ->
                    tripsCache[trip.id] = trip
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // TRIP DETAILS
    // ==========================================

    /**
     * Pobiera trip details. Najpierw sprawdza cache, jeśli brak → fetch z API.
     * Dla wymuszenia odświeżenia użyj refreshTripDetails().
     */
    fun getTripDetails(tripId: String): TripDto? {
        return tripsCache[tripId]
    }

    /**
     * Pobiera trip details z API i aktualizuje cache.
     */
    suspend fun refreshTripDetails(tripId: String): Result<TripDto> {
        return try {
            val result = graphQL.getTripDetails(tripId.toInt())
            result.onSuccess { trip ->
                tripsCache[trip.id] = trip
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "RefreshTripDetails error", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch trip details i zwróć (nie tylko z cache).
     * Używany gdy wchodzimy na ekran szczegółów.
     */
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
                        // Refetch trip details aby mieć aktualne dane
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

    suspend fun deleteExpense(tripId: String, expenseId: String): Result<DeleteExpenseDto> {
        return try {
            val result = graphQL.deleteExpense(tripId, expenseId)

            result.fold(
                onSuccess = { successDto ->
                    if (successDto.success) {
                        refreshTripDetails(tripId)
                        val updatedTrip = tripsCache[tripId]
                        Result.success(DeleteExpenseDto(
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
    // PARTICIPANTS
    // ==========================================

    suspend fun addPlaceholder(tripId: String, nickname: String): Result<ParticipantsDto> {
        return try {
            val result = graphQL.addPlaceholder(tripId, nickname)

            result.fold(
                onSuccess = { successDto ->
                    if (successDto.success) {
                        refreshTripDetails(tripId)
                        val updatedTrip = tripsCache[tripId]
                        Result.success(ParticipantsDto(
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

    suspend fun detachUser(tripId: String, participantId: String): Result<ParticipantsDto> {
        return try {
            val result = graphQL.detachUser(tripId, participantId)

            result.fold(
                onSuccess = { successDto ->
                    if (successDto.success) {
                        refreshTripDetails(tripId)
                        val updatedTrip = tripsCache[tripId]
                        Result.success(ParticipantsDto(
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

    suspend fun removePlaceholder(tripId: String, participantId: String): Result<ParticipantsDto> {
        return try {
            val result = graphQL.removePlaceholder(tripId, participantId)

            result.fold(
                onSuccess = { successDto ->
                    if (successDto.success) {
                        refreshTripDetails(tripId)
                        val updatedTrip = tripsCache[tripId]
                        Result.success(ParticipantsDto(
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
    // SETTLEMENTS
    // ==========================================

    suspend fun addPrepayment(
        tripId: String,
        participantId: String,
        amount: Float,
        currency: String,
        direction: String
    ): Result<SettlementResultDto> {
        return try {
            val result = graphQL.addPrepayment(tripId, participantId, amount, currency, direction)

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
        items: List<SettleByCostsItem>
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
    // SUBSCRIPTIONS
    // ==========================================

    /**
     * Subskrybuje real-time updates dla danego tripu.
     * Zwraca Flow<TripDeltaDto> — ViewModel powinien to collectować.
     *
     * Każdy delta aktualizuje cache automatycznie.
     */
    fun subscribeTripUpdates(tripId: String): Flow<TripDeltaDto> {
        return graphQL.subscribeTripUpdates(tripId.toInt())
    }

    /**
     * Aplikuje TripDelta do lokalnego cache.
     * Wywołaj to w ViewModel po odebraniu delta z subscription.
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
    }

    // ==========================================
    // CACHE MANAGEMENT
    // ==========================================

    private fun updateTripInCache(tripData: TripDto) {
        tripsCache[tripData.id] = tripData
    }

    fun clearCache() {
        tripsCache.clear()
        cachedUserInfo = null
    }
}
