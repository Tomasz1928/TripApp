package com.example.tripapp2.ui.tripdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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

/**
 * Fragment szczegółów wycieczki — Propozycja C: Floating Sections
 *
 * Zmiany vs oryginał:
 * - Brak header image (top bar z back button)
 * - Hero sekcja: nazwa + opis + data wycieczki
 * - Metric cards: łączne wydatki + mój koszt obok siebie
 * - Floating rows: wydatki wg waluty + rozliczenia (settings-style)
 * - settlementsCard zastąpiony przez rowSettlements (ukryty settlementsCard dla kompatybilności)
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

    // Section rows
    private lateinit var rowExpensesByCurrency: View
    private lateinit var currencyCount: TextView
    private lateinit var rowSettlements: View
    private lateinit var settlementsStatus: TextView

    // Hidden (kompatybilność z ViewModel)
    private lateinit var settlementsCard: MaterialCardView

    override fun setupUI() {
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

        // Kliknięcie w kartę łącznych wydatków → modal walutowy
        totalExpensesCard.setOnClickListener {
            showExpensesModal()
        }

        // Row: wydatki wg waluty → modal walutowy
        rowExpensesByCurrency.setOnClickListener {
            showExpensesModal()
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
     */
    private fun displayTripDetails(details: TripDetailsUiModel) {
        // Hero
        tripTitle.text = details.title
        tripSubtitle.text = details.description
        tripDate.text = details.dateRange

        // Metric cards — rozdziel kwotę i walutę
        val breakdown = details.myExpensesBreakdown
        if (breakdown.isNotEmpty()) {
            val mainExpense = breakdown.first()
            totalExpenses.text = "%.2f".format(mainExpense.amount)
            totalExpensesCurrency.text = mainExpense.currency
            myCostAmount.text = "%.2f".format(mainExpense.amount)
            myCostCurrency.text = mainExpense.currency
        } else {
            totalExpenses.text = details.myTotalExpenses
        }

        // Currency count
        currencyCount.text = "${breakdown.size} walut"

        // Settlements status (uproszczony — rozbuduj wg potrzeb)
        settlementsStatus.text = "Zobacz szczegóły"
    }

    private fun showExpensesModal() {
        val state = viewModel.tripDetailsState.value
        if (state is TripDetailsState.Success) {
            val modal = ExpensesListModalFragment.newInstance(
                state.details.myExpensesBreakdown
            )
            modal.show(parentFragmentManager, "expenses_modal")
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
