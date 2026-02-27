package com.example.tripapp2.ui.tripdetails.costs

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.model.TripDto
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * ViewModel dla Trip Costs
 *
 * Subskrypcje żyją w TripRepository.
 * Ten ViewModel obserwuje StateFlow i auto-odświeża listę wydatków.
 */
class TripCostsViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    private val _costsState = MutableLiveData<TripCostsState>()
    val costsState: LiveData<TripCostsState> = _costsState

    private val _currentFilter = MutableLiveData(ExpenseFilter.ALL)
    val currentFilter: LiveData<ExpenseFilter> = _currentFilter

    private val _showExpenseDetailEvent = MutableLiveData<Event<ExpenseDetailUiModel>>()
    val showExpenseDetailEvent: LiveData<Event<ExpenseDetailUiModel>> = _showExpenseDetailEvent

    private var allExpenses: List<ExpenseDetailUiModel> = emptyList()
    private var currentUserId: String = ""

    private val _navigateToEditExpenseEvent = MutableLiveData<Event<Pair<String, String>>>()
    val navigateToEditExpenseEvent: LiveData<Event<Pair<String, String>>> = _navigateToEditExpenseEvent

    private val _showDeleteConfirmationEvent = MutableLiveData<Event<ExpenseDetailUiModel>>()
    val showDeleteConfirmationEvent: LiveData<Event<ExpenseDetailUiModel>> = _showDeleteConfirmationEvent

    private val _expenseDeletedEvent = MutableLiveData<Event<String>>()
    val expenseDeletedEvent: LiveData<Event<String>> = _expenseDeletedEvent

    init {
        loadExpenses()
        observeTripUpdates()
    }

    // ==========================================
    // REAL-TIME OBSERVATION
    // ==========================================

    private fun observeTripUpdates() {
        viewModelScope.launch {
            tripRepository.observeTrip(tripId)
                .filterNotNull()
                .collect { trip ->
                    if (currentUserId.isNotEmpty()) {
                        Log.d(TAG, "Trip updated via StateFlow, refreshing expenses")
                        updateExpensesFromTrip(trip)
                    }
                }
        }
    }

    private fun updateExpensesFromTrip(trip: TripDto) {
        val expenses = trip.expenses
        if (expenses.isEmpty()) {
            allExpenses = emptyList()
            _costsState.value = TripCostsState.Empty
        } else {
            allExpenses = expenses.map { it.toDetailUiModel(currentUserId, trip.currency) }
            applyFilter(_currentFilter.value ?: ExpenseFilter.ALL)
        }
    }

    // ==========================================
    // INITIAL LOAD
    // ==========================================

    fun loadExpenses() {
        viewModelScope.launch {
            _costsState.value = TripCostsState.Loading
            currentUserId = tripRepository.getCurrentUserInfo().id
            val result = execute(showLoading = false) {
                tripRepository.getTripDetails(tripId)
            }

            result.onSuccess { trip ->
                if (trip == null) {
                    _costsState.value = TripCostsState.Error("Nie znaleziono wycieczki")
                    return@launch
                }

                val expenses = trip.expenses
                if (expenses.isEmpty()) {
                    _costsState.value = TripCostsState.Empty
                } else {
                    allExpenses = expenses.map { it.toDetailUiModel(currentUserId, trip.currency) }
                    applyFilter(_currentFilter.value ?: ExpenseFilter.ALL)
                }
            }.onFailure { error ->
                showError(error.message ?: "Nie udało się załadować wydatków")
                _costsState.value = TripCostsState.Empty
            }
        }
    }

    // ==========================================
    // FILTERING & SEARCH
    // ==========================================

    fun applyFilter(filter: ExpenseFilter) {
        _currentFilter.value = filter

        val filteredExpenses = when (filter) {
            ExpenseFilter.ALL -> allExpenses
            ExpenseFilter.MINE -> allExpenses.filter { it.isMine }
            ExpenseFilter.PAID_BY_ME -> allExpenses.filter { it.payerId == currentUserId }
            ExpenseFilter.PAID_BY_OTHERS -> allExpenses.filter { it.payerId != currentUserId }
        }

        if (filteredExpenses.isEmpty()) {
            _costsState.value = TripCostsState.Empty
        } else {
            _costsState.value = TripCostsState.Success(filteredExpenses, filter)
        }
    }

    fun searchExpenses(query: String) {
        if (query.isBlank()) {
            applyFilter(_currentFilter.value ?: ExpenseFilter.ALL)
            return
        }

        val searchResults = allExpenses.filter { expense ->
            expense.name.contains(query, ignoreCase = true) ||
                    expense.payerName.contains(query, ignoreCase = true)
        }

        if (searchResults.isEmpty()) {
            _costsState.value = TripCostsState.Empty
        } else {
            _costsState.value = TripCostsState.Success(
                searchResults, _currentFilter.value ?: ExpenseFilter.ALL
            )
        }
    }

    // ==========================================
    // USER ACTIONS
    // ==========================================

    fun onExpenseClicked(expenseId: String) {
        viewModelScope.launch {
            val result = execute(showLoading = false) {
                tripRepository.getTripDetails(tripId)
            }
            result.onSuccess { trip ->
                if (trip == null) {
                    showError("Nie znaleziono wycieczki")
                    return@launch
                }
                val expense = trip.expenses.find { it.id == expenseId }
                expense?.let {
                    _showExpenseDetailEvent.value = Event(it.toDetailUiModel(currentUserId, trip.currency))
                }
            }
        }
    }

    fun onEditExpenseClicked(expenseId: String) {
        _navigateToEditExpenseEvent.value = Event(tripId to expenseId)
    }

    fun onDeleteExpenseClicked(expenseId: String) {
        val expense = allExpenses.find { it.id == expenseId }
        expense?.let { _showDeleteConfirmationEvent.value = Event(it) }
    }

    fun confirmDeleteExpense(expenseId: String) {
        viewModelScope.launch {
            setLoading(true)
            val result = tripRepository.deleteExpense(tripId, expenseId)
            result.onSuccess {
                _expenseDeletedEvent.value = Event("Wydatek został usunięty")
            }.onFailure { error ->
                showError(error.message ?: "Nie udało się usunąć wydatku")
            }
            setLoading(false)
        }
    }

    fun onFilterAllClicked() = applyFilter(ExpenseFilter.ALL)
    fun onFilterMineClicked() = applyFilter(ExpenseFilter.MINE)
    fun onFilterPaidByMeClicked() = applyFilter(ExpenseFilter.PAID_BY_ME)
    fun onFilterPaidByOthersClicked() = applyFilter(ExpenseFilter.PAID_BY_OTHERS)

    companion object {
        private const val TAG = "TripCostsVM"
    }
}