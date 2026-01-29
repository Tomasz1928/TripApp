package com.example.tripapp2.ui.dashboard.join

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

/**
 * ViewModel dla Join Trip
 * Odpowiedzialny za:
 * - Walidację kodu dostępu
 * - Dołączanie do wycieczki
 */
class JoinTripViewModel(
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    // Kod dostępu
    private val _accessCode = MutableLiveData<String>()
    val accessCode: LiveData<String> = _accessCode

    // Błąd walidacji
    private val _accessCodeError = MutableLiveData<String?>()
    val accessCodeError: LiveData<String?> = _accessCodeError

    // Event sukcesu dołączenia
    private val _tripJoinedEvent = MutableLiveData<Event<String>>()
    val tripJoinedEvent: LiveData<Event<String>> = _tripJoinedEvent

    /**
     * Aktualizacja kodu dostępu
     */
    fun onAccessCodeChanged(code: String) {
        _accessCode.value = code
        _accessCodeError.value = null
    }

    /**
     * Dołącza do wycieczki
     */
    fun onJoinTripClicked() {
        val code = _accessCode.value

        if (code.isNullOrBlank()) {
            _accessCodeError.value = "Podaj kod dostępu"
            return
        }

        if (!isValidAccessCode(code)) {
            _accessCodeError.value = "Nieprawidłowy format kodu (oczekiwano: XXXX-XXXX)"
            return
        }

        viewModelScope.launch {
//            setLoading(true)
//
//            val result = tripRepository.joinTrip(code)
//
//            setLoading(false)
//
//            result.onSuccess { trip ->
//                _tripJoinedEvent.value = Event("Dołączono do wycieczki: ${trip.title}")
//                navigate(NavigationCommand.ToDashboard)
//            }.onFailure { error ->
//                showError(error.message ?: "Nie udało się dołączyć do wycieczki")
//            }
        }
    }

    /**
     * Waliduje format kodu dostępu
     */
    private fun isValidAccessCode(code: String): Boolean {
        // Format: XXXX-XXXX (8 znaków + myślnik)
        val regex = Regex("^[A-Z0-9]{4}-[A-Z0-9]{4}$")
        return regex.matches(code.uppercase())
    }
}