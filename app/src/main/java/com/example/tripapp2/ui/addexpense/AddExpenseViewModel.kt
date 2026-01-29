package com.example.tripapp2.ui.addexpense

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch
import java.util.Calendar

class AddExpenseViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    // Pola formularza
    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    private val _selectedCategory = MutableLiveData<ExpenseCategory?>()
    val selectedCategory: LiveData<ExpenseCategory?> = _selectedCategory

    private val _amount = MutableLiveData<String>()
    val amount: LiveData<String> = _amount

    private val _currency = MutableLiveData<String>()
    val currency: LiveData<String> = _currency

    private val _dateTime = MutableLiveData<Pair<Long, Long>?>() // (date, time) w millis
    val dateTime: LiveData<Pair<Long, Long>?> = _dateTime

    private val _selectedPayer = MutableLiveData<String?>() // participant ID
    val selectedPayer: LiveData<String?> = _selectedPayer

    private val _expenseSplit = MutableLiveData<ExpenseSplit>()
    val expenseSplit: LiveData<ExpenseSplit> = _expenseSplit

    // Błędy walidacji
    private val _titleError = MutableLiveData<String?>()
    val titleError: LiveData<String?> = _titleError

    private val _amountError = MutableLiveData<String?>()
    val amountError: LiveData<String?> = _amountError

    private val _categoryError = MutableLiveData<String?>()
    val categoryError: LiveData<String?> = _categoryError

    private val _dateError = MutableLiveData<String?>()
    val dateError: LiveData<String?> = _dateError

    private val _payerError = MutableLiveData<String?>()
    val payerError: LiveData<String?> = _payerError

    private val _splitError = MutableLiveData<String?>()
    val splitError: LiveData<String?> = _splitError

    // Lista uczestników wycieczki
    private val _participants = MutableLiveData<List<SplitParticipant>>()
    val participants: LiveData<List<SplitParticipant>> = _participants

    // Eventy
    private val _showCategoryPickerEvent = MutableLiveData<Event<Unit>>()
    val showCategoryPickerEvent: LiveData<Event<Unit>> = _showCategoryPickerEvent

    private val _showDatePickerEvent = MutableLiveData<Event<Unit>>()
    val showDatePickerEvent: LiveData<Event<Unit>> = _showDatePickerEvent

    private val _showTimePickerEvent = MutableLiveData<Event<Unit>>()
    val showTimePickerEvent: LiveData<Event<Unit>> = _showTimePickerEvent

    private val _showSplitModalEvent = MutableLiveData<Event<ExpenseSplit>>()
    val showSplitModalEvent: LiveData<Event<ExpenseSplit>> = _showSplitModalEvent

    private val _expenseAddedEvent = MutableLiveData<Event<String>>()
    val expenseAddedEvent: LiveData<Event<String>> = _expenseAddedEvent

    init {
        loadParticipants()
    }

    private fun loadParticipants() {
        viewModelScope.launch {
            val result = tripRepository.getTripDetails(tripId)
            if (result?.participants != null) {
                val splitParticipants = result.participants.map {
                    SplitParticipant(
                        id = it.id,
                        name = it.nickname,
                        isSelected = false,
                        amount = 0f
                    )
                }
                _participants.value = splitParticipants
                _expenseSplit.value = ExpenseSplit(
                    splitType = SplitType.EQUAL,
                    participants = splitParticipants
                )
            }
        }
    }


    // Aktualizacja pól
    fun onTitleChanged(title: String) {
        _title.value = title
        _titleError.value = null
    }

    fun onDescriptionChanged(description: String) {
        _description.value = description
    }

    fun onAmountChanged(amount: String) {
        _amount.value = amount
        _amountError.value = null
    }

    fun onCurrencySelected(currency: String) {
        _currency.value = currency
    }

    fun onCategorySelected(category: ExpenseCategory) {
        _selectedCategory.value = category
        _categoryError.value = null
    }

    fun onDateSelected(dateMillis: Long) {
        val currentTime = _dateTime.value?.second ?: System.currentTimeMillis()
        _dateTime.value = dateMillis to currentTime
        _dateError.value = null
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val currentDate = _dateTime.value?.first ?: System.currentTimeMillis()
        _dateTime.value = currentDate to calendar.timeInMillis
    }

    fun onPayerSelected(payerId: String) {
        _selectedPayer.value = payerId
        _payerError.value = null
    }

    fun onExpenseSplitUpdated(split: ExpenseSplit) {
        _expenseSplit.value = split
        _splitError.value = null
    }

    // Eventy UI
    fun onCategoryFieldClicked() {
        _showCategoryPickerEvent.value = Event(Unit)
    }

    fun onDateFieldClicked() {
        _showDatePickerEvent.value = Event(Unit)
    }

    fun onTimeFieldClicked() {
        _showTimePickerEvent.value = Event(Unit)
    }

    fun onSplitFieldClicked() {
        _expenseSplit.value?.let { split ->
            _showSplitModalEvent.value = Event(split)
        }
    }

    fun onAddExpenseClicked() {
        if (!validateForm()) {
            return
        }

        viewModelScope.launch {
            setLoading(true)
            kotlinx.coroutines.delay(1000) // Mock
            setLoading(false)
            _expenseAddedEvent.value = Event("Wydatek dodany pomyślnie!")
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (_title.value.isNullOrBlank()) {
            _titleError.value = "Podaj tytuł wydatku"
            isValid = false
        } else if (_title.value!!.length > 40) {
            _titleError.value = "Tytuł nie może być dłuższy niż 40 znaków"
            isValid = false
        }

        if (_selectedCategory.value == null) {
            _categoryError.value = "Wybierz kategorię"
            isValid = false
        }

        val amountStr = _amount.value
        if (amountStr.isNullOrBlank()) {
            _amountError.value = "Podaj kwotę"
            isValid = false
        } else {
            val amountFloat = amountStr.toFloatOrNull()
            if (amountFloat == null || amountFloat <= 0) {
                _amountError.value = "Podaj prawidłową kwotę"
                isValid = false
            }
        }

        if (_dateTime.value == null) {
            _dateError.value = "Wybierz datę i godzinę"
            isValid = false
        }

        if (_selectedPayer.value == null) {
            _payerError.value = "Wybierz kto płacił"
            isValid = false
        }

        val split = _expenseSplit.value
        val amountFloat = _amount.value?.toFloatOrNull() ?: 0f
        if (split == null || !split.isValid(amountFloat)) {
            _splitError.value = when {
                split == null -> "Ustaw podział kosztów"
                split.getSelectedParticipants().isEmpty() -> "Wybierz przynajmniej jedną osobę"
                split.splitType == SplitType.MANUAL -> "Suma podziału nie zgadza się z kwotą wydatku"
                else -> "Nieprawidłowy podział"
            }
            isValid = false
        }

        return isValid
    }

    fun getCurrencies(): List<String> {
        return listOf(
            "PLN", "EUR", "USD", "GBP", "CHF", "JPY", "CNY", "AUD", "CAD", "NZD",
            "SEK", "NOK", "DKK", "RUB", "INR", "BRL", "MXN", "KRW", "SGD", "HKD"
        )
    }
}