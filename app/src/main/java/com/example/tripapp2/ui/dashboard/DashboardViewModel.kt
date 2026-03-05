package com.example.tripapp2.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.NavigationCommand
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel dla Dashboard
 *
 * Strategia:
 * - Dane z API ładowane PRZED wejściem na Dashboard
 *   (w SplashActivity / LoginActivity / RegisterActivity)
 * - init: tylko wyświetl dane z cache (bez strzału do API)
 * - refreshFromApi(): ręczny refresh (przycisk na karcie)
 * - observeCacheChanges(): reaguje na zmiany z subskrypcji/mutacji
 */
class DashboardViewModel(
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    private val _dashboardState = MutableLiveData<DashboardState>()
    val dashboardState: LiveData<DashboardState> = _dashboardState

    init {
        // Dane już załadowane z API przed Dashboard — tylko pokaż z cache
        refreshFromCache()
        observeCacheChanges()
    }

    /**
     * Ręczny refresh — strzela do API.
     * Wywoływane przez przycisk refresh na karcie.
     */
    fun refreshFromApi() {
        viewModelScope.launch {
            setLoading(true)
            _dashboardState.value = DashboardState.Loading

            val result = tripRepository.loadInitialData()
            setLoading(false)

            result.onSuccess {
                val trips = tripRepository.getAllTripsFromCache()
                if (trips.isEmpty()) {
                    _dashboardState.value = DashboardState.Empty
                } else {
                    _dashboardState.value = DashboardState.Success(trips)
                }
            }.onFailure { error ->
                // Jeśli API fail ale mamy dane w cache — pokaż cache
                val cachedTrips = tripRepository.getAllTripsFromCache()
                if (cachedTrips.isNotEmpty()) {
                    _dashboardState.value = DashboardState.Success(cachedTrips)
                } else {
                    _dashboardState.value = DashboardState.Error(error.message ?: "Błąd")
                }
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
        refreshFromApi()
    }

    private fun observeCacheChanges() {
        viewModelScope.launch {
            tripRepository.cacheChangeFlow.collectLatest {
                val trips = tripRepository.getAllTripsFromCache()
                if (trips.isEmpty()) {
                    _dashboardState.value = DashboardState.Empty
                } else {
                    _dashboardState.value = DashboardState.Success(trips)
                }
            }
        }
    }
}