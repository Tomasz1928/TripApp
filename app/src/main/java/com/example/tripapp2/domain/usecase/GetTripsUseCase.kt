//package com.example.tripapp2.domain.usecase
//
//import com.example.tripapp2.data.repository.TripRepository
//import com.example.tripapp2.ui.dashboard.TripUiModel
//import com.example.tripapp2.ui.dashboard.toUiModel
//
///**
// * Use Case do pobierania listy wycieczek
// * Transformuje dane z Repository (TripDto) na UI Models (TripUiModel)
// */
//class GetTripsUseCase(private val repository: TripRepository) {
//
//    /**
//     * Pobiera wycieczki i konwertuje na UI modele
//     * @return Lista TripUiModel gotowych do wyświetlenia
//     */
//    suspend operator fun invoke(): Result<List<TripUiModel>> {
//        return try {
//            val result = repository.getTrips()
//
//            result.fold(
//                onSuccess = { trips ->
//                    val uiModels = trips.map { it.toUiModel() }
//                    Result.success(uiModels)
//                },
//                onFailure = { error ->
//                    Result.failure(error)
//                }
//            )
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//}