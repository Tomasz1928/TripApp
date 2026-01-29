package com.example.tripapp2.ui.dashboard.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

/**
 * ViewModel dla Create Trip
 * Odpowiedzialny za:
 * - Walidację formularza
 * - Tworzenie wycieczki
 * - Obsługę date pickera
 */
class CreateTripViewModel(
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    // Pola formularza
    private val _tripName = MutableLiveData<String>()
    val tripName: LiveData<String> = _tripName

    private val _currency = MutableLiveData<String>()
    val currency: LiveData<String> = _currency

    private val _dateRange = MutableLiveData<Pair<Long, Long>?>()
    val dateRange: LiveData<Pair<Long, Long>?> = _dateRange

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    // Błędy walidacji
    private val _nameError = MutableLiveData<String?>()
    val nameError: LiveData<String?> = _nameError

    private val _currencyError = MutableLiveData<String?>()
    val currencyError: LiveData<String?> = _currencyError

    private val _dateError = MutableLiveData<String?>()
    val dateError: LiveData<String?> = _dateError

    // Event pokazania date pickera
    private val _showDatePickerEvent = MutableLiveData<Event<Unit>>()
    val showDatePickerEvent: LiveData<Event<Unit>> = _showDatePickerEvent

    // Event sukcesu utworzenia wycieczki
    private val _tripCreatedEvent = MutableLiveData<Event<String>>()
    val tripCreatedEvent: LiveData<Event<String>> = _tripCreatedEvent

    /**
     * Aktualizacja pól formularza
     */
    fun onTripNameChanged(name: String) {
        _tripName.value = name
        _nameError.value = null
    }

    fun onCurrencySelected(currency: String) {
        _currency.value = currency
        _currencyError.value = null
    }

    fun onDateRangeSelected(dateRange: Pair<Long, Long>) {
        _dateRange.value = dateRange
        _dateError.value = null
    }

    fun onDescriptionChanged(description: String) {
        _description.value = description
    }

    /**
     * Pokazuje date picker
     */
    fun onDateFieldClicked() {
        _showDatePickerEvent.value = Event(Unit)
    }

    /**
     * Waliduje i tworzy wycieczkę
     */
    fun onCreateTripClicked() {
        if (!validateForm()) {
            return
        }

        val name = _tripName.value ?: return
        val curr = _currency.value ?: return
        val dates = _dateRange.value ?: return
        val desc = _description.value ?: ""

        viewModelScope.launch {
//            setLoading(true)
//
//            val result = tripRepository.createTrip(
//                title = name,
//                description = desc,
//                dateStart = dates.first,
//                dateEnd = dates.second,
//                currency = curr
//            )
//
//            setLoading(false)
//
//            result.onSuccess { trip ->
//                _tripCreatedEvent.value = Event("Wycieczka utworzona pomyślnie!")
//                navigate(NavigationCommand.ToDashboard)
//            }.onFailure { error ->
//                showError(error.message ?: "Nie udało się utworzyć wycieczki")
//            }
        }
    }

    /**
     * Waliduje formularz
     */
    private fun validateForm(): Boolean {
        var isValid = true

        if (_tripName.value.isNullOrBlank()) {
            _nameError.value = "Podaj nazwę wycieczki"
            isValid = false
        }

        if (_currency.value.isNullOrBlank()) {
            _currencyError.value = "Wybierz walutę"
            isValid = false
        }

        if (_dateRange.value == null) {
            _dateError.value = "Wybierz zakres dat"
            isValid = false
        }

        return isValid
    }

    /**
     * Lista dostępnych walut
     */
    fun getCurrencies(): List<String> {
        return listOf(
            "PLN", "EUR", "USD", "GBP", "CHF", "JPY", "CNY", "AUD", "CAD", "NZD",
            "SEK", "NOK", "DKK", "RUB", "INR", "BRL", "MXN", "KRW", "SGD", "HKD"
        )
    }
}