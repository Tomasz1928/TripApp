package com.example.tripapp2.ui.tripdetails.costs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

/**
 * ViewModel dla Trip Costs
 * Odpowiedzialny za:
 * - Ładowanie wydatków
 * - Filtrowanie wydatków
 * - Wyszukiwanie wydatków
 * - Wyświetlanie szczegółów wydatku
 */
class TripCostsViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    // Stan ekranu
    private val _costsState = MutableLiveData<TripCostsState>()
    val costsState: LiveData<TripCostsState> = _costsState

    // Aktualny filtr
    private val _currentFilter = MutableLiveData(ExpenseFilter.ALL)
    val currentFilter: LiveData<ExpenseFilter> = _currentFilter

    // Event kliknięcia w wydatek (do pokazania szczegółów)
    private val _showExpenseDetailEvent = MutableLiveData<Event<ExpenseDetailUiModel>>()
    val showExpenseDetailEvent: LiveData<Event<ExpenseDetailUiModel>> = _showExpenseDetailEvent

    // Cache'owane dane
    private var allExpenses: List<ExpenseDetailUiModel> = emptyList()
    private var currentUserId : String = ""

    init {
        loadExpenses()
    }

    /**
     * Ładuje wydatki dla wycieczki
     */
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
                    allExpenses = expenses.map { expense ->
                        expense.toDetailUiModel(currentUserId = currentUserId, trip.currency )
                    }
                    applyFilter(_currentFilter.value ?: ExpenseFilter.ALL)
                }
            }.onFailure { error ->
                _costsState.value = TripCostsState.Error(
                    error.message ?: "Nie udało się załadować wydatków"
                )
            }
        }
    }

    /**
     * Aplikuje filtr do wydatków
     */
    fun applyFilter(filter: ExpenseFilter) {
        _currentFilter.value = filter

        val filteredExpenses = when (filter) {
            ExpenseFilter.ALL -> allExpenses
            ExpenseFilter.MINE -> allExpenses.filter { it.isMine }
            ExpenseFilter.PAID_BY_ME -> allExpenses.filter {
                it.payerId == "Adam" // Mock - w prawdziwej app porównanie ID
            }

            ExpenseFilter.PAID_BY_OTHERS -> allExpenses.filter {
                it.payerId != "ADAM"
            }
        }

        if (filteredExpenses.isEmpty()) {
            _costsState.value = TripCostsState.Empty
        } else {
            _costsState.value = TripCostsState.Success(filteredExpenses, filter)
        }
    }

    /**
     * Wyszukiwanie wydatków
     */
    fun searchExpenses(query: String) {
        if (query.isBlank()) {
            applyFilter(_currentFilter.value ?: ExpenseFilter.ALL)
            return
        }

        val searchResults = allExpenses.filter { expense ->
            expense.name.contains(query, ignoreCase = true) ||
                    expense.payerId.contains(query, ignoreCase = true)
        }

        if (searchResults.isEmpty()) {
            _costsState.value = TripCostsState.Empty
        } else {
            _costsState.value = TripCostsState.Success(
                searchResults,
                _currentFilter.value ?: ExpenseFilter.ALL
            )
        }
    }

    /**
     * Kliknięcie w wydatek - pokazuje szczegóły
     */
    fun onExpenseClicked(expenseId: String) {
        viewModelScope.launch {
            val result = execute(showLoading = false) {
                tripRepository.getTripDetails(tripId)
            }
            result.onSuccess { trip ->
                if (trip == null) {
                    _costsState.value = TripCostsState.Error("Nie znaleziono wycieczki")
                    return@launch
                }

                val expense = trip.expenses.find { it.id ==expenseId }
                    expense?.let {
                        val detailModel = it.toDetailUiModel(currentUserId, trip.currency)
                        _showExpenseDetailEvent.value = Event(detailModel)
                    }
                }
            }
        }

    /**
     * Obsługa kliknięcia w filtr
     */
    fun onFilterAllClicked() = applyFilter(ExpenseFilter.ALL)
    fun onFilterMineClicked() = applyFilter(ExpenseFilter.MINE)
    fun onFilterPaidByMeClicked() = applyFilter(ExpenseFilter.PAID_BY_ME)
    fun onFilterPaidByOthersClicked() = applyFilter(ExpenseFilter.PAID_BY_OTHERS)

    /**
     * Obsługa wyszukiwania
     */
    fun onSearchClicked() {
//        showMessage("Wyszukiwanie będzie zaimplementowane w przyszłości")
    }
}