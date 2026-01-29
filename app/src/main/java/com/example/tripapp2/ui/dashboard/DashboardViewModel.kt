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
 * Odpowiedzialny za:
 * - Ładowanie listy wycieczek
 * - Zarządzanie stanem ekranu (loading/success/empty/error)
 * - Nawigację do szczegółów wycieczki
 * - Nawigację do tworzenia/dołączania do wycieczki
 */
class DashboardViewModel(
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    // Stan ekranu Dashboard
    private val _dashboardState = MutableLiveData<DashboardState>()
    val dashboardState: LiveData<DashboardState> = _dashboardState

    init {
        loadTrips()
    }

    /**
     * Ładuje listę wycieczek
     */
    private fun loadTrips() {
        viewModelScope.launch {
            _dashboardState.value = DashboardState.Loading

            try {
                val tripListDtos = tripRepository.getFullInitDetails()
                val allTrips = tripListDtos.flatMap { it.trips ?: emptyList() }

                if (allTrips.isEmpty()) {
                    _dashboardState.value = DashboardState.Empty
                } else {
                    // Zwróć TripDto, nie TripUiModel
                    _dashboardState.value = DashboardState.Success(allTrips)
                }

            } catch (e: Exception) {
                _dashboardState.value = DashboardState.Error(
                    e.message ?: "Wystąpił błąd podczas ładowania"
                )
            }
        }
    }

    /**
     * Obsługa kliknięcia w kartę wycieczki
     */
    fun onTripClicked(tripId: String) {
        navigate(NavigationCommand.ToTripDetails(tripId))
    }

    /**
     * Nawigacja do tworzenia wycieczki
     */
    fun onCreateTripClicked() {
        navigate(NavigationCommand.ToCreateTrip)
    }

    /**
     * Nawigacja do dołączania do wycieczki
     */
    fun onJoinTripClicked() {
        navigate(NavigationCommand.ToJoinTrip)
    }

    /**
     * Odświeżanie listy (pull-to-refresh)
     */
    fun refresh() {
        loadTrips()
    }
}