package com.example.tripapp2.data.repository

import com.example.tripapp2.data.model.*
import com.example.tripapp2.data.repository.MockData.createTripMock
import com.example.tripapp2.data.repository.MockData.joinTripMock
import com.example.tripapp2.data.repository.MockData.addExpenseMock
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



    private fun saveInitDataToCache(initialData:TripListDto) {
        initialDataCache.add(initialData)
        initialData.trips?.forEach{
            trip -> tripsCache[trip.id] = trip
        }
    }

    private fun saveCreateNewTripToCache(createTripData:TripDto){
        tripsCache[createTripData.id] = createTripData
    }

    suspend fun getTripDetails(tripId: String):TripDto?{
        return tripsCache[tripId]
    }

    suspend fun getFullInitDetails():List<TripListDto>{
    return initialDataCache
    }

    fun getAllTripsFromCache(): List<TripDto> {
        return tripsCache.values.toList()
    }

    suspend fun getCurrentUserInfo():UserInfoDto{
        return MockData.getUsrInfo()
    }

    private fun updateTripInCache(tripData: TripDto) {
        tripsCache[tripData.id] = tripData
    }



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

            if (newTrip.success.success){
                newTrip.trip?.let { saveCreateNewTripToCache(it) }
                Result.success(newTrip)
            }else{
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

            if (joinTrip.success.success){
                joinTrip.trip?.let { saveCreateNewTripToCache(it) }
                Result.success(joinTrip)
            }else{
                Result.failure(Exception(joinTrip.success.message))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


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

    /**
     * Pobiera wydatki dla wycieczki
     */
//    suspend fun getExpensesForTrip(tripId: String): Result<List<ExpenseDto>> {
//        return try {
//            delay(300)
//
//            val trip = getMockTrips().find { it.id == tripId }
//            Result.success(trip?.expenses ?: emptyList())
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    /**
     * Oznacza rozliczenie jako spłacone
     * W przyszłości: POST /api/trips/{tripId}/settlements/{settlementId}/settle
     */
    suspend fun markSettlementAsPaid(
        tripId: String,
        fromUserId: String,
        toUserId: String,
        amount: Float,
        currency: String
    ): Result<Boolean> {
        return try {
            delay(800) // Symulacja API call

            // Mock - zawsze sukces
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}