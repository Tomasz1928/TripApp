package com.example.tripapp2.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.NavigationCommand
import kotlinx.coroutines.launch

/**
 * ViewModel dla Dashboard
 *
 * Po loadInitialData():
 * 1. Pobiera listę ID tripów
 * 2. Dla każdego ID pobiera pełne detale (równolegle)
 * 3. Cache w repo zawiera pełne TripDto
 * 4. Startuje subskrypcje WebSocket na wszystkie tripy
 * 5. Dashboard wyświetla tripy z cache
 */
class DashboardViewModel(
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    private val _dashboardState = MutableLiveData<DashboardState>()
    val dashboardState: LiveData<DashboardState> = _dashboardState

    init {
        loadTrips()
    }

    /**
     * Ładuje tripy: loadInitialData() pobiera ID + pełne detale + startuje subskrypcje.
     * Po sukcesie bierze pełne TripDto z cache.
     */
    fun loadTrips() {
        viewModelScope.launch {
            setLoading(true)
            _dashboardState.value = DashboardState.Loading

            val result = tripRepository.loadInitialData()
            setLoading(false)

            result.onSuccess {
                // Po loadInitialData cache ma pełne TripDto
                val trips = tripRepository.getAllTripsFromCache()
                if (trips.isEmpty()) {
                    _dashboardState.value = DashboardState.Empty
                } else {
                    _dashboardState.value = DashboardState.Success(trips)
                }
            }.onFailure { error ->
                _dashboardState.value = DashboardState.Error(error.message ?: "Błąd")
            }
        }
    }

    fun refreshFromCache() {
        val trips = tripRepository.getAllTripsFromCache()
        if (trips.isEmpty()) {
            _dashboardState.value = DashboardState.Empty
        } else {
            _dashboardState.value = DashboardState.Success(trips)
        }
    }

    fun onTripClicked(tripId: String) {
        navigate(NavigationCommand.ToTripDetails(tripId))
    }

    fun onCreateTripClicked() {
        navigate(NavigationCommand.ToCreateTrip)
    }

    fun onJoinTripClicked() {
        navigate(NavigationCommand.ToJoinTrip)
    }

    fun refresh() {
        loadTrips()
    }
}