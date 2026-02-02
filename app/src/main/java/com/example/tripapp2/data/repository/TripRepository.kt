package com.example.tripapp2.data.repository

import com.example.tripapp2.data.model.*
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


    suspend fun loadInitialData():Result<TripListDto>{

        return try {
            if(!isInitialDataLoaded){
                isInitialDataLoaded = true
                val response = fetchInitData()
                saveInitDataToCache(response)
                Result.success(response)
            }
            else{
                val cache = getAllTripsFromCache()
                Result.success(TripListDto(cache))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchInitData(): TripListDto {
        delay(300) // Symulacja network delay
        return MockData.getTripList()
    }



    private fun saveInitDataToCache(initialData:TripListDto) {
        initialDataCache.add(initialData)
        initialData.trips?.forEach{
            trip -> tripsCache[trip.id] = trip
        }
    }

    private fun saveCreateNewTripToCache(createTripData:TripDto){
        tripsCache[createTripData.id] = createTripData
    }

    private fun updateTripInCache(trip: TripDto) {
        tripsCache[trip.id] = trip
    }

    suspend fun getTripDetails(tripId: String): TripDto? {
        return tripsCache[tripId]
    }

    suspend fun getDataFromCache(): List<TripListDto> {
        return initialDataCache
    }

    fun getAllTripsFromCache(): List<TripDto> {
        return tripsCache.values.toList()
    }

    suspend fun getCurrentUserInfo(): UserInfoDto {
        return MockData.getUsrInfo()
    }


    fun createTrip(
        title: String,
        description: String,
        dateStart: Long,
        dateEnd: Long,
        currency: String
    ): Result<CreateTripDto> {
        return try {
            val newTrip = MockData.createTripMock(title, dateStart, dateEnd, description, currency)

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

    fun joinTrip(accessCode: String): Result<JoinTripDto> {
        return try {
            val joinTrip = MockData.joinTripMock(accessCode)

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

    // ==================== EXPENSE OPERATIONS ====================

    fun addExpense(tripId: String, expense: ExpenseDto): Result<AddExpenseResponseDto> {
        return try {
            val result = MockData.addExpense(
                tripId = tripId,
                name = expense.name,
                description = expense.description ?: "",
                amount = expense.amount,
                currency = expense.currency,
                date = expense.date,
                categoryId = expense.categoryId,
                payerId = expense.payerId,
                payerNickname = expense.payerNickname,
                sharedWith = expense.sharedWith
            )

            if (result != null && result.success.success) {
                updateTripInCache(result.updatedTrip)
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to add expense - trip not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun deleteExpense(tripId: String, expenseId: String): Result<DeleteExpenseResponseDto> {
        return try {
            val result = MockData.deleteExpense(tripId, expenseId)

            if (result != null && result.success.success) {
                updateTripInCache(result.updatedTrip)
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to delete expense"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateExpense(
        tripId: String,
        expenseId: String,
        name: String? = null,
        description: String? = null,
        amount: Float? = null,
        currency: String? = null,
        date: Long? = null,
        categoryId: String? = null,
        sharedWith: List<ShareDto>? = null
    ): Result<UpdateExpenseResponseDto> {
        return try {
            val result = MockData.updateExpense(
                tripId = tripId,
                expenseId = expenseId,
                name = name,
                description = description,
                amount = amount,
                currency = currency,
                date = date,
                categoryId = categoryId,
                sharedWith = sharedWith
            )

            if (result != null && result.success.success) {
                updateTripInCache(result.updatedTrip)
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to update expense"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PARTICIPANT OPERATIONS ====================

    fun addParticipant(
        tripId: String,
        nickname: String,
        isPlaceholder: Boolean = true
    ): Result<AddParticipantResponseDto> {
        return try {
            val result = MockData.addParticipant(tripId, nickname, isPlaceholder)

            if (result != null && result.success.success) {
                updateTripInCache(result.updatedTrip)
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to add participant"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== SETTLEMENT OPERATIONS ====================

    suspend fun markSettlementAsPaid(
        tripId: String,
        fromUserId: String,
        toUserId: String,
        amount: Float,
        currency: String
    ): Result<Boolean> {
        return try {
            delay(800) // Symulacja API call

            // TODO: Zaimplementować w MockData gdy będzie potrzebne
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== UTILITY ====================

    fun refreshTripFromMockData(tripId: String) {
        MockData.getTripById(tripId)?.let { trip ->
            updateTripInCache(trip)
        }
    }

    fun clearCache() {
        tripsCache.clear()
        initialDataCache.clear()
        isInitialDataLoaded = false
    }
}