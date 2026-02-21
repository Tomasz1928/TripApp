package com.example.tripapp2.ui.tripdetails.costs.editexpense

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.R
import com.example.tripapp2.data.model.ShareRequest
import com.example.tripapp2.data.model.SimpleMoneyValueDto
import com.example.tripapp2.data.model.UpdateExpenseRequest
import com.example.tripapp2.data.model.mainCurrencyAmount
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.tripdetails.costs.addexpense.ExpenseCategories
import com.example.tripapp2.ui.tripdetails.costs.addexpense.SplitParticipant
import com.example.tripapp2.ui.tripdetails.costs.addexpense.SplitType
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import com.example.tripapp2.ui.tripdetails.costs.addexpense.ExpenseCategory
import com.example.tripapp2.ui.tripdetails.costs.addexpense.ExpenseSplit
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel dla edycji wydatku
 *
 * Różnice od AddExpenseViewModel:
 * - Ładuje istniejący wydatek i pre-populuje pola
 * - Wywołuje updateExpense zamiast addExpense
 * - Emituje expenseUpdatedEvent zamiast expenseAddedEvent
 */
class EditExpenseViewModel(
    private val tripId: String,
    private val expenseId: String,
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
    private val _titleError = MutableLiveData<Int?>()
    val titleError: LiveData<Int?> = _titleError

    private val _amountError = MutableLiveData<Int?>()
    val amountError: LiveData<Int?> = _amountError

    private val _categoryError = MutableLiveData<Int?>()
    val categoryError: LiveData<Int?> = _categoryError

    private val _dateError = MutableLiveData<Int?>()
    val dateError: LiveData<Int?> = _dateError

    private val _payerError = MutableLiveData<Int?>()
    val payerError: LiveData<Int?> = _payerError

    private val _splitError = MutableLiveData<Int?>()
    val splitError: LiveData<Int?> = _splitError

    // Lista uczestników wycieczki
    private val _participants = MutableLiveData<List<SplitParticipant>>()
    val participants: LiveData<List<SplitParticipant>> = _participants

    // Flaga czy dane zostały załadowane
    private val _dataLoaded = MutableLiveData<Boolean>(false)
    val dataLoaded: LiveData<Boolean> = _dataLoaded

    // Eventy
    private val _showCategoryPickerEvent = MutableLiveData<Event<Unit>>()
    val showCategoryPickerEvent: LiveData<Event<Unit>> = _showCategoryPickerEvent

    private val _showDatePickerEvent = MutableLiveData<Event<Unit>>()
    val showDatePickerEvent: LiveData<Event<Unit>> = _showDatePickerEvent

    private val _showTimePickerEvent = MutableLiveData<Event<Unit>>()
    val showTimePickerEvent: LiveData<Event<Unit>> = _showTimePickerEvent

    private val _showSplitModalEvent = MutableLiveData<Event<ExpenseSplit>>()
    val showSplitModalEvent: LiveData<Event<ExpenseSplit>> = _showSplitModalEvent

    private val _expenseUpdatedEvent = MutableLiveData<Event<String>>()
    val expenseUpdatedEvent: LiveData<Event<String>> = _expenseUpdatedEvent

    init {
        loadExpenseData()
    }

    /**
     * Ładuje dane wydatku i uczestników
     */
    private fun loadExpenseData() {
        viewModelScope.launch {
            setLoading(true)

            val trip = tripRepository.getTripDetails(tripId)

            if (trip == null) {
                showError("Nie znaleziono wycieczki")
                setLoading(false)
                return@launch
            }

            val expense = trip.expenses.find { it.id == expenseId }

            if (expense == null) {
                showError("Nie znaleziono wydatku")
                setLoading(false)
                return@launch
            }

            // Załaduj uczestników
            val splitParticipants = trip.participants.map { participant ->
                val share = expense.sharedWith.find { it.participantId == participant.id }
                SplitParticipant(
                    id = participant.id,
                    name = participant.nickname,
                    isSelected = share != null,
                    amount = share?.splitValue?.mainCurrencyAmount() ?: 0f
                )
            }
            _participants.value = splitParticipants

            // Pre-populuj pola formularza
            _title.value = expense.name
            _description.value = expense.description ?: ""
            _amount.value = expense.amount.toString()
            _currency.value = expense.currency
            _selectedPayer.value = expense.payerId

            // Kategoria
            val category = ExpenseCategories.getById(expense.categoryId)
            _selectedCategory.value = category

            // Data i czas
            _dateTime.value = expense.date to expense.date

            // Podział kosztów
            _expenseSplit.value = ExpenseSplit(
                splitType = SplitType.MANUAL,
                participants = splitParticipants
            )

            _dataLoaded.value = true
            setLoading(false)
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

    /**
     * Aktualizuje wydatek
     */
    fun onUpdateExpenseClicked() {
        if (!validateForm()) {
            return
        }

        viewModelScope.launch {
            setLoading(true)

            val request = buildUpdateExpenseRequest()
            val result = tripRepository.updateExpense(request)

            setLoading(false)

            result.onSuccess { updateExpenseDto ->
                _expenseUpdatedEvent.value = Event("EXPENSE_UPDATED_SUCCESS_RES_ID:${R.string.edit_expense_success}")
            }.onFailure { error ->
                _expenseUpdatedEvent.value = Event(error.message ?: "Błąd aktualizacji wydatku")
            }
        }
    }

    private fun buildUpdateExpenseRequest(): UpdateExpenseRequest {
        val amount = _amount.value?.toFloatOrNull() ?: 0f
        val currency = _currency.value ?: "PLN"
        val payerId = _selectedPayer.value ?: ""
        val split = _expenseSplit.value

        return UpdateExpenseRequest(
            tripId = tripId,
            expenseId = expenseId,
            name = _title.value ?: "",
            description = _description.value,
            amount = amount,
            currency = currency,
            categoryId = _selectedCategory.value?.id ?: "",
            date = _dateTime.value?.first ?: System.currentTimeMillis(),
            payerId = payerId,
            payerNickname = _participants.value?.find { it.id == payerId }?.name ?: "",
            sharedWith = split?.let { buildSharedWithList(it) } ?: emptyList()
        )
    }

    private fun buildSharedWithList(split: ExpenseSplit): List<ShareRequest> {
        return split.getSelectedParticipants().map { participant ->
            ShareRequest(
                participantId = participant.id,
                participantNickname = participant.name,
                splitValue = listOf(
                    SimpleMoneyValueDto(
                        isMainCurrency = true,
                        currency = _currency.value ?: "",
                        amount = participant.amount
                    )
                )
            )
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (_title.value.isNullOrBlank()) {
            _titleError.value = R.string.error_title_required
            isValid = false
        } else if (_title.value!!.length > 40) {
            _titleError.value = R.string.error_title_too_long
            isValid = false
        }

        if (_selectedCategory.value == null) {
            _categoryError.value = R.string.error_category_required
            isValid = false
        }

        val amountStr = _amount.value
        if (amountStr.isNullOrBlank()) {
            _amountError.value = R.string.error_amount_required
            isValid = false
        } else {
            val amountFloat = amountStr.toFloatOrNull()
            if (amountFloat == null || amountFloat <= 0) {
                _amountError.value = R.string.error_amount_invalid
                isValid = false
            }
        }

        if (_dateTime.value == null) {
            _dateError.value = R.string.error_date_time_required
            isValid = false
        }

        if (_selectedPayer.value == null) {
            _payerError.value = R.string.error_payer_required
            isValid = false
        }

        val split = _expenseSplit.value
        val amountFloat = _amount.value?.toFloatOrNull() ?: 0f

        if (split == null || split.getSelectedParticipants().isEmpty()) {
            _splitError.value = R.string.error_split_required
            isValid = false
        } else if (!split.isValid(amountFloat)) {
            _splitError.value = R.string.error_split_invalid
            isValid = false
        }

        return isValid
    }
}