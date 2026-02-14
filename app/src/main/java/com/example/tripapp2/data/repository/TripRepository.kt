package com.example.tripapp2.data.repository

import com.example.tripapp2.data.model.*
import com.example.tripapp2.data.repository.MockData.createTripMock
import com.example.tripapp2.data.repository.MockData.joinTripMock
import com.example.tripapp2.data.repository.MockData.addExpenseMock
import com.example.tripapp2.data.repository.MockData.updateExpenseMock
import kotlinx.coroutines.delay

class TripRepository private constructor() {

    private val tripsCache = mutableMapOf<String, TripDto>()
    private var isInitialDataLoaded = false
    private val initialDataCache = mutableListOf<TripListDto>()

    companion object {
        @Volatile
        private var INSTANCE: TripRepository? = null

        fun getInstance(): TripRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TripRepository().also { INSTANCE = it }
            }
        }
    }

    // ==========================================
    // INITIAL DATA
    // ==========================================

    suspend fun loadInitialData(): Result<TripListDto> {
        return try {
            val response = fetchInitData()
            saveInitDataToCache(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchInitData(): TripListDto {
        return MockData.getTripList()
    }

    private fun saveInitDataToCache(initialData: TripListDto) {
        initialDataCache.add(initialData)
        initialData.trips?.forEach { trip ->
            tripsCache[trip.id] = trip
        }
    }

    private fun saveCreateNewTripToCache(createTripData: TripDto) {
        tripsCache[createTripData.id] = createTripData
    }

    // ==========================================
    // TRIP DETAILS
    // ==========================================

    suspend fun getTripDetails(tripId: String): TripDto? {
        return tripsCache[tripId]
    }

    suspend fun getFullInitDetails(): List<TripListDto> {
        return initialDataCache
    }

    fun getAllTripsFromCache(): List<TripDto> {
        return tripsCache.values.toList()
    }

    // ==========================================
    // USER INFO
    // ==========================================

    suspend fun getCurrentUserInfo(): UserInfoDto {
        return MockData.getUsrInfo()
    }

    // ==========================================
    // CACHE MANAGEMENT
    // ==========================================

    private fun updateTripInCache(tripData: TripDto) {
        tripsCache[tripData.id] = tripData
    }

    // ==========================================
    // TRIP CRUD
    // ==========================================

    /**
     * Tworzy nową wycieczkę
     */
    fun createTrip(
        title: String,
        description: String,
        dateStart: Long,
        dateEnd: Long,
        currency: String
    ): Result<CreateTripDto> {
        return try {
            val newTrip = createTripMock(title, dateStart, dateEnd, description, currency)

            if (newTrip.success.success) {
                newTrip.trip?.let { saveCreateNewTripToCache(it) }
                Result.success(newTrip)
            } else {
                Result.failure(Exception(newTrip.success.message))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dołącza do wycieczki po kodzie dostępu
     */
    fun joinTrip(accessCode: String): Result<JoinTripDto> {
        return try {
            val joinTrip = joinTripMock(accessCode)

            if (joinTrip.success.success) {
                joinTrip.trip?.let { saveCreateNewTripToCache(it) }
                Result.success(joinTrip)
            } else {
                Result.failure(Exception(joinTrip.success.message))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // EXPENSES
    // ==========================================

    fun addExpense(request: AddExpenseRequest): Result<AddExpenseDto> {
        return try {
            val result = addExpenseMock(request)

            if (result.success.success) {
                result.trip?.let { updateTripInCache(it) }
                Result.success(result)
            } else {
                Result.failure(Exception(result.success.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateExpense(request: UpdateExpenseRequest): Result<UpdateExpenseDto> {
        return try {
            val result = updateExpenseMock(request)

            if (result.success.success) {
                result.trip?.let { updateTripInCache(it) }
                Result.success(result)
            } else {
                Result.failure(Exception(result.success.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteExpense(tripId: String, expenseId: String): Result<DeleteExpenseDto> {
        return try {
            val result = MockData.deleteExpense(tripId, expenseId)

            if (result.success.success) {
                result.trip?.let { updateTripInCache(it) }
                Result.success(result)
            } else {
                Result.failure(Exception(result.success.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // PARTICIPANTS
    // ==========================================

    fun addPlaceholder(tripId: String, nickname: String): Result<ParticipantsDto> {
        return try {
            val result = MockData.addPlaceholder(tripId, nickname)
            if (result.success.success) {
                result.trip?.let { updateTripInCache(it) }
                Result.success(result)
            } else {
                Result.failure(Exception(result.success.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun detachUser(tripId: String, participantId: String): Result<ParticipantsDto> {
        return try {
            val result = MockData.detachUser(tripId, participantId)
            if (result.success.success) {
                result.trip?.let { updateTripInCache(it) }
                Result.success(result)
            } else {
                Result.failure(Exception(result.success.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removePlaceholder(tripId: String, participantId: String): Result<ParticipantsDto> {
        return try {
            val result = MockData.removePlaceholder(tripId, participantId)
            if (result.success.success) {
                result.trip?.let { updateTripInCache(it) }
                Result.success(result)
            } else {
                Result.failure(Exception(result.success.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // SETTLEMENTS
    // ==========================================

     fun addPrepayment(
        tripId: String,
        participantId: String,
        amount: Float,
        currency: String,
        direction: String
    ): Result<SettlementResultDto> {
        return try {
            val result = MockData.addPrepayment(
                tripId = tripId,
                participantId = participantId,
                amount = amount,
                currency = currency,
                direction = direction
            )

            if (result.success.success) {
                result.trip?.let { updateTripInCache(it) }
                Result.success(result)
            } else {
                Result.failure(Exception(result.success.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun markSettlementAsPaid(
        tripId: String,
        fromUserId: String,
        toUserId: String,
        amount: Float,
        currency: String,
        isMainCurrency: Boolean
    ): Result<SettlementResultDto> {
        return try {
            val result = MockData.markSettlementAsPaid(
                tripId = tripId,
                fromUserId = fromUserId,
                toUserId = toUserId,
                amount = amount,
                currency = currency,
                isMainCurrency = isMainCurrency
            )

            if (result.success.success) {
                result.trip?.let { updateTripInCache(it) }
                Result.success(result)
            } else {
                Result.failure(Exception(result.success.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun getSettlementData(tripId: String): Result<SettlementDto?> {
        return try {
            val trip = tripsCache[tripId]
            if (trip != null) {
                Result.success(trip.settlement)
            } else {
                Result.failure(Exception("Trip not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}