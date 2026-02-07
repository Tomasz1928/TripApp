package com.example.tripapp2.ui.tripdetails.costs

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.extension.hide
import com.example.tripapp2.ui.common.extension.show
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.example.tripapp2.ui.tripdetails.costs.adapter.ExpenseAdapter
import com.example.tripapp2.ui.tripdetails.modal.ExpenseDetailModalFragment
import com.google.android.material.button.MaterialButton

/**
 * Fragment kosztów wycieczki
 *
 * Zmiany:
 * - Dodany przycisk "Dodaj koszt" w filtrach (zamiast bottom nav)
 * - Przyciski edytuj/usuń dla wydatków w filtrze PAID_BY_ME
 */
class TripCostsFragment : BaseFragment<TripCostsViewModel>(R.layout.fragment_trip_costs) {

    override val viewModel: TripCostsViewModel by viewModels {
        TripCostsViewModelFactory(getTripId())
    }

    private lateinit var expensesContainer: LinearLayout
    private lateinit var scrollExpensesContainer: ScrollView
    private lateinit var adapter: ExpenseAdapter

    // Przyciski filtrów
    private lateinit var filterAll: MaterialButton
    private lateinit var addExpenseButton: MaterialButton
    private lateinit var filterMine: MaterialButton
    private lateinit var filterPaidByMe: MaterialButton
    private lateinit var filterPaidByOthers: MaterialButton

    override fun setupUI() {
        initializeViews()
        setupAdapter()
        setupFilters()
        setupBottomPadding()
        setupBottomNavSelection()
    }

    override fun setupCustomObservers() {
        // Stan kosztów
        viewModel.costsState.observe(viewLifecycleOwner) { state ->
            handleCostsState(state)
        }

        // Event pokazania szczegółów wydatku
        viewModel.showExpenseDetailEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { detail ->
                showExpenseDetailModal(detail)
            }
        }

        // Aktualny filtr (do podświetlania przycisków i aktualizacji adaptera)
        viewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            updateFilterButtons(filter)
            recreateAdapter(filter)
        }
    }

    private fun initializeViews() {
        val view = requireView()
        expensesContainer = view.findViewById(R.id.expensesContainer)
        scrollExpensesContainer = view.findViewById(R.id.scrollExpensesContainer)

        // Filtry
        filterAll = view.findViewById(R.id.filterAll)
        addExpenseButton = view.findViewById(R.id.addExpenseButton)
        filterMine = view.findViewById(R.id.filterMine)
        filterPaidByMe = view.findViewById(R.id.filterPaidByMe)
        filterPaidByOthers = view.findViewById(R.id.filterPaidByOthers)
    }
    private fun recreateAdapter(currentFilter: ExpenseFilter) {
        adapter = ExpenseAdapter(
            onExpenseClick = { expense ->
                viewModel.onExpenseClicked(expense.id)
            },
            onEditExpense = { expense ->
                viewModel.onEditExpenseClicked(expense.id)
            },
            onDeleteExpense = { expense ->
                viewModel.onDeleteExpenseClicked(expense.id)
            },
            currentFilter = currentFilter
        )

        // Odśwież wyświetlanie
        val currentState = viewModel.costsState.value
        if (currentState is TripCostsState.Success) {
            displayExpenses(currentState.expenses)
        }
    }

    private fun setupAdapter() {
        adapter = ExpenseAdapter(
            onExpenseClick = { expense ->
                viewModel.onExpenseClicked(expense.id)
            },
            onEditExpense = { expense ->
                viewModel.onEditExpenseClicked(expense.id)
            },
            onDeleteExpense = { expense ->
                viewModel.onDeleteExpenseClicked(expense.id)
            },
            currentFilter = viewModel.currentFilter.value ?: ExpenseFilter.ALL
        )
    }

    private fun setupFilters() {
        filterAll.setOnClickListener { viewModel.onFilterAllClicked() }
        addExpenseButton.setOnClickListener { navigateToAddExpense() } // NOWY
        filterMine.setOnClickListener { viewModel.onFilterMineClicked() }
        filterPaidByMe.setOnClickListener { viewModel.onFilterPaidByMeClicked() }
        filterPaidByOthers.setOnClickListener { viewModel.onFilterPaidByOthersClicked() }
    }
    private fun navigateToAddExpense() {
        (activity as? DashboardActivity)?.showAddExpenseFromCosts(getTripId())
    }

    private fun setupBottomPadding() {
        val tripBottomNav = (activity as? DashboardActivity)?.tripBottomNav
        tripBottomNav?.viewTreeObserver?.addOnGlobalLayoutListener {
            val navHeight = tripBottomNav.height
            scrollExpensesContainer.setPadding(
                scrollExpensesContainer.paddingLeft,
                scrollExpensesContainer.paddingTop,
                scrollExpensesContainer.paddingRight,
                navHeight
            )
            scrollExpensesContainer.clipToPadding = false
        }
    }

    private fun setupBottomNavSelection() {
        val tripBottomNav = (activity as? DashboardActivity)?.tripBottomNav
        tripBottomNav?.post {
            tripBottomNav.selectedItemId = R.id.menu_costs
        }
    }

    /**
     * Obsługa różnych stanów ekranu
     */
    private fun handleCostsState(state: TripCostsState) {
        when (state) {
            is TripCostsState.Loading -> {
                expensesContainer.hide()
            }
            is TripCostsState.Success -> {
                expensesContainer.show()
                displayExpenses(state.expenses)
            }
            is TripCostsState.Empty -> {
                expensesContainer.show()
                displayEmptyState()
            }
            is TripCostsState.Error -> {
                expensesContainer.show()
                showError(state.message)
            }
        }
    }

    /**
     * Wyświetla wydatki używając adaptera
     */
    private fun displayExpenses(expenses: List<ExpenseDetailUiModel>) {
        expensesContainer.removeAllViews()

        expenses.forEach { expense ->
            val view = adapter.createExpenseView(expensesContainer, expense)
            expensesContainer.addView(view)
        }
    }

    /**
     * Wyświetla pusty stan
     */
    private fun displayEmptyState() {
        expensesContainer.removeAllViews()
        val emptyView = layoutInflater.inflate(
            R.layout.item_empty_state,
            expensesContainer,
            false
        )
        expensesContainer.addView(emptyView)
    }

    /**
     * Pokazuje modal ze szczegółami wydatku
     */
    private fun showExpenseDetailModal(detail: ExpenseDetailUiModel) {
        val modal = ExpenseDetailModalFragment.newInstance(detail)
        modal.show(parentFragmentManager, "expense_detail_modal")
    }

    /**
     * Aktualizuje wygląd przycisków filtrów
     */
    private fun updateFilterButtons(activeFilter: ExpenseFilter) {
        // Reset wszystkich
        listOf(filterAll, filterMine, filterPaidByMe, filterPaidByOthers).forEach {
            it.alpha = 0.6f
        }

        // Podświetl aktywny
        val activeButton = when (activeFilter) {
            ExpenseFilter.ALL -> filterAll
            ExpenseFilter.MINE -> filterMine
            ExpenseFilter.PAID_BY_ME -> filterPaidByMe
            ExpenseFilter.PAID_BY_OTHERS -> filterPaidByOthers
        }
        activeButton.alpha = 1.0f
    }

    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID) ?: "trip_2" // Mock fallback
    }

    companion object {
        private const val ARG_TRIP_ID = "trip_id"

        fun newInstance(tripId: String) = TripCostsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TRIP_ID, tripId)
            }
        }
    }
}

/**
 * Factory dla ViewModel
 */
class TripCostsViewModelFactory(
    private val tripId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripCostsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripCostsViewModel(tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}