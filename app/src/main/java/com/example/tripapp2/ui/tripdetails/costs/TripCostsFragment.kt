package com.example.tripapp2.ui.tripdetails.costs

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.baseModals.ConfirmModalFragment
import com.example.tripapp2.ui.common.extension.hide
import com.example.tripapp2.ui.common.extension.show
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.example.tripapp2.ui.tripdetails.costs.adapter.ExpenseAdapter
import com.example.tripapp2.ui.tripdetails.modal.ExpenseDetailModalFragment
import com.google.android.material.button.MaterialButton
import android.view.View
import com.example.tripapp2.ui.common.extension.applyStatusBarInsets

/**
 * Fragment kosztów wycieczki — Propozycja C: Floating Sections
 *
 * Zmiany vs oryginał:
 * - Brak header image (top bar z tytułem + przycisk "Dodaj")
 * - Przycisk "Dodaj koszt" przeniesiony z filtrów do top bar
 * - Filtry jako pill chipy pod top barem
 * - Wydatki w jednej grouped card (divider_horizontal między wierszami)
 * - Logika filtrów, adaptera i stanów — BEZ ZMIAN
 */
class TripCostsFragment : BaseFragment<TripCostsViewModel>(R.layout.fragment_trip_costs) {

    override val viewModel: TripCostsViewModel by viewModels {
        TripCostsViewModelFactory(getTripId())
    }

    private lateinit var expensesContainer: LinearLayout
    private lateinit var scrollExpensesContainer: ScrollView
    private lateinit var adapter: ExpenseAdapter

    // Przycisk dodaj (teraz w top bar)
    private lateinit var addExpenseButton: MaterialButton

    // Przyciski filtrów
    private lateinit var filterAll: MaterialButton
    private lateinit var filterMine: MaterialButton
    private lateinit var filterPaidByMe: MaterialButton
    private lateinit var filterPaidByOthers: MaterialButton

    override fun setupUI() {
        requireView().findViewById<View>(R.id.topBar).applyStatusBarInsets()
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

        // Event potwierdzenia usunięcia
        viewModel.showDeleteConfirmationEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { expense ->
                showDeleteConfirmationDialog(expense)
            }
        }

        // Event sukcesu usunięcia
        viewModel.expenseDeletedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                showMessage(message)
            }
        }

        // Event pokazania szczegółów wydatku
        viewModel.showExpenseDetailEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { detail ->
                showExpenseDetailModal(detail)
            }
        }

        // Aktualny filtr
        viewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            updateFilterButtons(filter)
            recreateAdapter(filter)
        }

        viewModel.navigateToEditExpenseEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { (tripId, expenseId) ->
                navigateToEditExpense(tripId, expenseId)
            }
        }
    }

    private fun initializeViews() {
        val view = requireView()
        expensesContainer = view.findViewById(R.id.expensesContainer)
        scrollExpensesContainer = view.findViewById(R.id.scrollExpensesContainer)

        // Przycisk dodaj — teraz w top bar
        addExpenseButton = view.findViewById(R.id.addExpenseButton)

        // Filtry — ID zachowane
        filterAll = view.findViewById(R.id.filterAll)
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
        addExpenseButton.setOnClickListener { navigateToAddExpense() }
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

    private fun displayExpenses(expenses: List<ExpenseDetailUiModel>) {
        expensesContainer.removeAllViews()
        expenses.forEach { expense ->
            val view = adapter.createExpenseView(expensesContainer, expense)
            expensesContainer.addView(view)
        }
    }

    private fun displayEmptyState() {
        expensesContainer.removeAllViews()
        val emptyView = layoutInflater.inflate(
            R.layout.item_empty_state,
            expensesContainer,
            false
        )
        expensesContainer.addView(emptyView)
    }

    private fun showExpenseDetailModal(detail: ExpenseDetailUiModel) {
        val modal = ExpenseDetailModalFragment.newInstance(detail)
        modal.show(parentFragmentManager, "expense_detail_modal")
    }

    private fun showDeleteConfirmationDialog(expense: ExpenseDetailUiModel) {
        ConfirmModalFragment.newInstance(
            title = getString(R.string.expense_action_delete),
            message = "Czy na pewno chcesz usunąć wydatek \"${expense.name}\"?",
            confirmText = getString(R.string.dialog_button_delete),
            confirmStyle = ConfirmModalFragment.ConfirmStyle.DANGER,
            onConfirm = { viewModel.confirmDeleteExpense(expense.id) }
        ).show(parentFragmentManager, "delete_expense")
    }

    private fun navigateToEditExpense(tripId: String, expenseId: String) {
        (activity as? DashboardActivity)?.showEditExpenseFromCosts(tripId, expenseId)
    }

    private fun updateFilterButtons(filter: ExpenseFilter) {
        listOf(filterAll, filterMine, filterPaidByMe, filterPaidByOthers).forEach {
            it.alpha = 0.6f
        }
        val activeButton = when (filter) {
            ExpenseFilter.ALL -> filterAll
            ExpenseFilter.MINE -> filterMine
            ExpenseFilter.PAID_BY_ME -> filterPaidByMe
            ExpenseFilter.PAID_BY_OTHERS -> filterPaidByOthers
        }
        activeButton.alpha = 1.0f
    }

    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID) ?: ""
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