package com.example.tripapp2.ui.dashboard.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.R
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import com.example.tripapp2.ui.common.base.NavigationCommand
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

    private val _nameError = MutableLiveData<Int?>()
    val nameError: LiveData<Int?> = _nameError

    private val _currencyError = MutableLiveData<Int?>()
    val currencyError: LiveData<Int?> = _currencyError

    private val _dateError = MutableLiveData<Int?>()
    val dateError: LiveData<Int?> = _dateError

    // Event pokazania date pickera
    private val _showDatePickerEvent = MutableLiveData<Event<Unit>>()
    val showDatePickerEvent: LiveData<Event<Unit>> = _showDatePickerEvent

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
            setLoading(true)

            val result = tripRepository.createTrip(name, desc, dates.first, dates.second, curr)

            result.onSuccess { createTripDto ->
                _tripCreatedEvent.value = Event(createTripDto.success.message ?: "")
                createTripDto.trip?.let { trip ->
                    navigate(NavigationCommand.ToTripDetails(trip.id))
                }
            }

            result.onFailure { error ->
                _tripCreatedEvent.value = Event(error.message ?: "")
            }
            setLoading(false)
        }
    }

    /**
     * Waliduje formularz
     */
    private fun validateForm(): Boolean {
        var isValid = true

        // ✅ ZMIANA: Bez .toString(), przekazujemy resource ID
        if (_tripName.value.isNullOrBlank()) {
            _nameError.value = R.string.error_trip_name_required
            isValid = false
        } else if (_tripName.value!!.length < 3) {
            _nameError.value = R.string.error_trip_name_too_short
            isValid = false
        }

        if (_currency.value.isNullOrBlank()) {
            _currencyError.value = R.string.error_currency_required
            isValid = false
        }

        if (_dateRange.value == null) {
            _dateError.value = R.string.error_date_required
            isValid = false
        } else {
            // Sprawdź czy data końca jest po dacie początku
            val (start, end) = _dateRange.value!!
            if (end < start) {
                _dateError.value = R.string.error_date_range_invalid
                isValid = false
            }
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