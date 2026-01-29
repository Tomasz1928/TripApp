package com.example.tripapp2.ui.tripdetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import com.example.tripapp2.ui.common.base.NavigationCommand
import kotlinx.coroutines.launch

/**
 * ViewModel dla Trip Details
 * Odpowiedzialny za:
 * - Ładowanie szczegółów wycieczki
 * - Obliczanie wydatków użytkownika
 * - Kopiowanie kodu dostępu
 * - Nawigację powrotną
 */
class TripDetailsViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    // Stan ekranu
    private val _tripDetailsState = MutableLiveData<TripDetailsState>()
    val tripDetailsState: LiveData<TripDetailsState> = _tripDetailsState

    // Event kopiowania kodu
    private val _copyCodeEvent = MutableLiveData<Event<CopyAccessCodeEvent>>()
    val copyCodeEvent: LiveData<Event<CopyAccessCodeEvent>> = _copyCodeEvent

    init {
        loadTripDetails()
    }

    /**
     * Ładuje szczegóły wycieczki
     */
    fun loadTripDetails() {
        viewModelScope.launch {
            _tripDetailsState.value = TripDetailsState.Loading

            val result = execute(showLoading = false) {
                tripRepository.getTripDetails(tripId)
            }

            result.onSuccess { trip ->
                if (trip != null) {
                    val uiModel = trip.toDetailsUiModel()
                    _tripDetailsState.value = TripDetailsState.Success(uiModel)
                }
            }
                .onFailure { error ->
                    _tripDetailsState.value = TripDetailsState.Error(
                        error.message ?: "Nie udało się załadować szczegółów wycieczki"
                    )
                }
        }
    }


    /**
     * Kopiuje kod dostępu do schowka
     */
    fun copyAccessCode(code: String) {
        _copyCodeEvent.value = Event(
            CopyAccessCodeEvent(
                code = code,
                message = "Skopiowano kod dostępu"
            )
        )
    }

    /**
     * Powrót do Dashboard
     */
    fun onBackClicked() {
        navigate(NavigationCommand.Back)
    }

    /**
     * Pokazuje modal z rozbiciem wydatków
     */
    fun onExpensesClicked() {
        // Event zostanie obsłużony w Fragment
    }
}