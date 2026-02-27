package com.example.tripapp2.ui.tripdetails

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import com.example.tripapp2.ui.common.base.NavigationCommand
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * ViewModel dla Trip Details
 *
 * Subskrypcje WebSocket żyją w TripRepository (nie tutaj).
 * Ten ViewModel tylko obserwuje StateFlow i odświeża UI.
 */
class TripDetailsViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    private val _tripDetailsState = MutableLiveData<TripDetailsState>()
    val tripDetailsState: LiveData<TripDetailsState> = _tripDetailsState

    private val _copyCodeEvent = MutableLiveData<Event<CopyAccessCodeEvent>>()
    val copyCodeEvent: LiveData<Event<CopyAccessCodeEvent>> = _copyCodeEvent

    init {
        loadTripDetails()
        observeTripUpdates()
    }

    /**
     * Initial load z cache/API
     */
    fun loadTripDetails() {
        viewModelScope.launch {
            _tripDetailsState.value = TripDetailsState.Loading

            val result = execute(showLoading = false) {
                tripRepository.getTripDetails(tripId)
            }

            result.onSuccess { trip ->
                if (trip != null) {
                    _tripDetailsState.value = TripDetailsState.Success(trip.toDetailsUiModel())
                }
            }.onFailure { error ->
                _tripDetailsState.value = TripDetailsState.Error(
                    error.message ?: "Nie udało się załadować szczegółów wycieczki"
                )
            }
        }
    }

    /**
     * Obserwuje StateFlow — repo aplikuje delty, ten VM odświeża UI.
     */
    private fun observeTripUpdates() {
        viewModelScope.launch {
            tripRepository.observeTrip(tripId)
                .filterNotNull()
                .collect { trip ->
                    Log.d(TAG, "Trip $tripId updated via StateFlow")
                    _tripDetailsState.value = TripDetailsState.Success(trip.toDetailsUiModel())
                }
        }
    }

    fun copyAccessCode(code: String) {
        _copyCodeEvent.value = Event(
            CopyAccessCodeEvent(code = code, message = "Skopiowano kod dostępu")
        )
    }

    fun onBackClicked() {
        navigate(NavigationCommand.Back)
    }

    fun onExpensesClicked() {}

    companion object {
        private const val TAG = "TripDetailsVM"
    }
}