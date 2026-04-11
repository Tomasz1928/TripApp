package com.example.tripapp2.ui.tripdetails

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.base.NavigationCommand
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.example.tripapp2.ui.tripdetails.modal.ExpensesListModalFragment
import com.google.android.material.card.MaterialCardView
import com.example.tripapp2.ui.common.extension.applyStatusBarInsets

/**
 * Fragment szczegółów wycieczki — Propozycja C: Floating Sections
 *
 * ZMIENIONE: totalExpenses i myCost pokazują RÓŻNE dane:
 * - totalExpenses = suma WSZYSTKICH kosztów wycieczki (klik → modal z rozbiciem na waluty)
 * - myCost = suma MOICH kosztów (klik → modal z rozbiciem moich kosztów na waluty)
 */
class TripDetailsFragment : BaseFragment<TripDetailsViewModel>(R.layout.fragment_trip_details) {

    override val viewModel: TripDetailsViewModel by viewModels {
        TripDetailsViewModelFactory(getTripId())
    }

    // ================================
    // VIEWS
    // ================================
    private lateinit var scrollViewTripDetails: NestedScrollView
    private lateinit var backButton: ImageView

    // Hero
    private lateinit var tripTitle: TextView
    private lateinit var tripSubtitle: TextView
    private lateinit var tripDate: TextView

    // Metric cards
    private lateinit var totalExpenses: TextView
    private lateinit var totalExpensesCurrency: TextView
    private lateinit var totalExpensesCard: MaterialCardView
    private lateinit var myCostAmount: TextView
    private lateinit var myCostCurrency: TextView
    private lateinit var myCostCard: MaterialCardView          // NOWE: referencja do karty myCost

    // Section rows
    private lateinit var rowExpensesByCurrency: View
    private lateinit var currencyCount: TextView
    private lateinit var rowSettlements: View
    private lateinit var settlementsStatus: TextView

    // Hidden (kompatybilność z ViewModel)
    private lateinit var settlementsCard: MaterialCardView

    override fun setupUI() {
        requireView().findViewById<View>(R.id.topBar).applyStatusBarInsets()
        initializeViews()
        setupClickListeners()
    }

    override fun setupCustomObservers() {
        viewModel.tripDetailsState.observe(viewLifecycleOwner) { state ->
            handleTripDetailsState(state)
        }
    }

    private fun initializeViews() {
        val view = requireView()
        scrollViewTripDetails = view.findViewById(R.id.scrollViewTripDetails)
        backButton = view.findViewById(R.id.backButton)

        // Hero
        tripTitle = view.findViewById(R.id.tripTitle)
        tripSubtitle = view.findViewById(R.id.tripSubtitle)
        tripDate = view.findViewById(R.id.tripDate)

        // Metric cards
        totalExpenses = view.findViewById(R.id.totalExpenses)
        totalExpensesCurrency = view.findViewById(R.id.totalExpensesCurrency)
        totalExpensesCard = view.findViewById(R.id.totalExpensesCard)
        myCostAmount = view.findViewById(R.id.myCostAmount)
        myCostCurrency = view.findViewById(R.id.myCostCurrency)
        myCostCard = view.findViewById(R.id.myCostCard)        // NOWE

        // Section rows
        rowExpensesByCurrency = view.findViewById(R.id.rowExpensesByCurrency)
        currencyCount = view.findViewById(R.id.currencyCount)
        rowSettlements = view.findViewById(R.id.rowSettlements)
        settlementsStatus = view.findViewById(R.id.settlementsStatus)

        // Hidden
        settlementsCard = view.findViewById(R.id.settlementsCard)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            viewModel.onBackClicked()
        }

        // Kliknięcie w kartę łącznych wydatków → modal z WSZYSTKIMI kosztami
        totalExpensesCard.setOnClickListener {
            showTotalExpensesModal()
        }

        // NOWE: Kliknięcie w kartę "Mój koszt" → modal z MOIMI kosztami
        myCostCard.setOnClickListener {
            showMyExpensesModal()
        }

        // Row: wydatki wg waluty → modal z WSZYSTKIMI kosztami
        rowExpensesByCurrency.setOnClickListener {
            showTotalExpensesModal()
        }

        // Row: rozliczenia → ekran rozliczeń
        rowSettlements.setOnClickListener {
            navigateToSettlements()
        }
    }

    private fun handleTripDetailsState(state: TripDetailsState) {
        when (state) {
            is TripDetailsState.Loading -> {
                // Opcjonalnie: ProgressBar
            }
            is TripDetailsState.Success -> {
                displayTripDetails(state.details)
            }
            is TripDetailsState.Error -> {
                showError(state.message)
            }
        }
    }

    /**
     * Wyświetla dane wycieczki w nowym layoucie
     *
     * ZMIENIONE: totalExpenses pokazuje sumę WSZYSTKICH kosztów,
     * myCost pokazuje sumę MOICH kosztów.
     */
    private fun displayTripDetails(details: TripDetailsUiModel) {
        // Hero
        tripTitle.text = details.title
        tripSubtitle.text = details.description
        tripDate.text = details.dateRange

        // --- TOTAL EXPENSES CARD (wszystkie koszty wycieczki) ---
        totalExpenses.text = "%.2f".format(details.tripTotalExpensesAmount)
        totalExpensesCurrency.text = details.tripTotalCurrency

        // --- MY COST CARD (moje koszty) ---
        val myBreakdown = details.myExpensesBreakdown
        if (myBreakdown.isNotEmpty()) {
            val mainMyExpense = myBreakdown.first()
            myCostAmount.text = "%.2f".format(mainMyExpense.amount)
            myCostCurrency.text = mainMyExpense.currency
        } else {
            myCostAmount.text = "0,00"
            myCostCurrency.text = details.tripTotalCurrency
        }

        // Currency count — łączna liczba walut w wycieczce
        val allCurrencies = (details.tripExpensesBreakdown.map { it.currency } +
                details.myExpensesBreakdown.map { it.currency }).distinct()
        currencyCount.text = "${allCurrencies.size} walut"

        // Settlements status (uproszczony — rozbuduj wg potrzeb)
        settlementsStatus.text = "Zobacz szczegóły"
    }

    /**
     * Modal z WSZYSTKIMI kosztami wycieczki (rozbicie na waluty)
     */
    private fun showTotalExpensesModal() {
        val state = viewModel.tripDetailsState.value
        if (state is TripDetailsState.Success) {
            val modal = ExpensesListModalFragment.newInstance(
                expenses = state.details.tripExpensesBreakdown,
                title = "Łączne wydatki"
            )
            modal.show(parentFragmentManager, "total_expenses_modal")
        }
    }

    /**
     * NOWE: Modal z MOIMI kosztami (rozbicie na waluty)
     */
    private fun showMyExpensesModal() {
        val state = viewModel.tripDetailsState.value
        if (state is TripDetailsState.Success) {
            val modal = ExpensesListModalFragment.newInstance(
                expenses = state.details.myExpensesBreakdown,
                title = "Moje koszty"
            )
            modal.show(parentFragmentManager, "my_expenses_modal")
        }
    }

    private fun navigateToSettlements() {
        (activity as? DashboardActivity)?.showSettlements(getTripId())
    }

    override fun handleNavigation(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.Back -> {
                (activity as? DashboardActivity)?.closeTripDetails()
            }
            else -> super.handleNavigation(command)
        }
    }

    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID) ?: ""
    }

    companion object {
        private const val ARG_TRIP_ID = ""

        fun newInstance(tripId: String) = TripDetailsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TRIP_ID, tripId)
            }
        }
    }
}

class TripDetailsViewModelFactory(
    private val tripId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripDetailsViewModel(tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}