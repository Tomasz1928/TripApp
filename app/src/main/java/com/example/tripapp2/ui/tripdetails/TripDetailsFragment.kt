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
 * Fragment szczegółów wycieczki
 */
class TripDetailsFragment : BaseFragment<TripDetailsViewModel>(R.layout.fragment_trip_details) {

    override val viewModel: TripDetailsViewModel by viewModels {
        TripDetailsViewModelFactory(getTripId())
    }

    private lateinit var scrollViewTripDetails: NestedScrollView
    private lateinit var tripTitle: TextView
    private lateinit var tripSubtitle: TextView
    private lateinit var tripDate: TextView
    private lateinit var tripAccessCode: TextView
    private lateinit var totalExpenses: TextView
    private lateinit var backButton: ImageView
    private lateinit var settlementsCard: MaterialCardView

    override fun setupUI() {
        initializeViews()
        setupClickListeners()
    }

    override fun setupCustomObservers() {
        // Stan szczegółów wycieczki
        viewModel.tripDetailsState.observe(viewLifecycleOwner) { state ->
            handleTripDetailsState(state)
        }

        // Event kopiowania kodu
        viewModel.copyCodeEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { copyEvent ->
                copyToClipboard(copyEvent.code)
                showMessage(copyEvent.message)
            }
        }
    }

    private fun initializeViews() {
        val view = requireView()
        scrollViewTripDetails = view.findViewById(R.id.scrollViewTripDetails)
        tripTitle = view.findViewById(R.id.tripTitle)
        tripSubtitle = view.findViewById(R.id.tripSubtitle)
        tripDate = view.findViewById(R.id.tripDate)
        tripAccessCode = view.findViewById(R.id.tripAccessCode)
        totalExpenses = view.findViewById(R.id.totalExpenses)
        backButton = view.findViewById(R.id.backButton)
        settlementsCard = view.findViewById(R.id.settlementsCard)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            viewModel.onBackClicked()
        }

        tripAccessCode.setOnClickListener {
            viewModel.copyAccessCode(tripAccessCode.text.toString())
        }

        totalExpenses.setOnClickListener {
            showExpensesModal()
        }

        settlementsCard.setOnClickListener {
            navigateToSettlements()
        }
    }

    /**
     * Obsługa różnych stanów ekranu
     */
    private fun handleTripDetailsState(state: TripDetailsState) {
        when (state) {
            is TripDetailsState.Loading -> {
                // Opcjonalnie: pokazać ProgressBar
            }
            is TripDetailsState.Success -> {
                displayTripDetails(state.details)
            }
            // ✅ OPCJONALNE: Możesz usunąć Error state (obsługiwany przez BaseFragment)
            is TripDetailsState.Error -> {
                showError(state.message)
            }
        }
    }

    /**
     * Wyświetla dane wycieczki
     */
    private fun displayTripDetails(details: TripDetailsUiModel) {
        tripTitle.text = details.title
        tripSubtitle.text = details.description
        tripDate.text = details.dateRange
        tripAccessCode.text = details.accessCode
        totalExpenses.text = details.myTotalExpenses
    }

    /**
     * Pokazuje modal z rozbiciem wydatków
     */
    private fun showExpensesModal() {
        val state = viewModel.tripDetailsState.value
        if (state is TripDetailsState.Success) {
            val modal = ExpensesListModalFragment.newInstance(
                state.details.myExpensesBreakdown
            )
            modal.show(parentFragmentManager, "expenses_modal")
        }
    }

    /**
     * Nawigacja do rozliczeń
     */
    private fun navigateToSettlements() {
        (activity as? DashboardActivity)?.showSettlements(getTripId())
    }

    /**
     * Kopiuje tekst do schowka
     */
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.trip_details_access_code), text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Obsługa nawigacji
     */
    override fun handleNavigation(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.Back -> {
                (activity as? DashboardActivity)?.closeTripDetails()
            }
            else -> super.handleNavigation(command)
        }
    }

    /**
     * Pobiera ID wycieczki (z argumentów lub mock)
     */
    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID)?: ""
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

/**
 * Factory dla ViewModel z parametrem tripId
 */
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