package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.extension.hide
import com.example.tripapp2.ui.common.extension.show
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Fragment rozliczeń (NOWA WERSJA)
 *
 * Funkcjonalności:
 * - Wyświetlanie listy uczestników z informacją o balansie względem mnie
 * - Przyciski: Zaliczka (dla wszystkich), Rozlicz (tylko dla nie-zerowych)
 * - Modal zaliczki z kierunkiem i kwotą
 * - Brak logiki n-do-n, tylko moje relacje
 */
class TripSettlementsFragment : BaseFragment<TripSettlementsViewModel>(R.layout.fragment_trip_settlements) {

    override val viewModel: TripSettlementsViewModel by viewModels {
        TripSettlementsViewModelFactory(getTripId())
    }

    // Views
    private lateinit var backButton: ImageView
    private lateinit var balanceSummaryCard: MaterialCardView
    private lateinit var balanceAmount: TextView
    private lateinit var scrollParticipants: ScrollView
    private lateinit var participantsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout

    override fun setupUI() {
        initializeViews()
        setupClickListeners()
    }

    override fun setupCustomObservers() {
        // Stan ekranu
        viewModel.settlementsState.observe(viewLifecycleOwner) { state ->
            handleSettlementsState(state)
        }

        // Event otwarcia modala zaliczki
        viewModel.showPrepaymentModalEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { model ->
                showPrepaymentModal(model)
            }
        }

        // Event otwarcia modala rozliczenia (na przyszłość)
        viewModel.showSettleModalEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { participant ->
                // TODO: Implementacja modala rozliczenia
                showMessage("Rozliczenie z ${participant.nickname} - do implementacji")
            }
        }


        // Event potwierdzenia akcji
        viewModel.actionConfirmedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                showMessage(message)
            }
        }
    }

    private fun initializeViews() {
        val view = requireView()
        backButton = view.findViewById(R.id.backButton)
        balanceSummaryCard = view.findViewById(R.id.balanceSummaryCard)
        balanceAmount = view.findViewById(R.id.balanceAmount)
        scrollParticipants = view.findViewById(R.id.scrollParticipants)
        participantsContainer = view.findViewById(R.id.participantsContainer)
        emptyState = view.findViewById(R.id.emptyState)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            onBackClicked()
        }
    }

    /**
     * Powrót do TripDetails
     */
    private fun onBackClicked() {
        (activity as? DashboardActivity)?.apply {
            // Pokaż bottom nav z powrotem
            tripBottomNav.visibility = View.VISIBLE

            // Wróć do TripDetails
            val fragment = com.example.tripapp2.ui.tripdetails.TripDetailsFragment.newInstance(getTripId())
            supportFragmentManager.beginTransaction()
                .replace(R.id.tripContainer, fragment, "tripDetails")
                .commit()

            // Ustaw wybrany item
            tripBottomNav.selectedItemId = R.id.menu_overview
        }
    }

    /**
     * Obsługa różnych stanów ekranu
     */
    private fun handleSettlementsState(state: TripSettlementsState) {
        when (state) {
            is TripSettlementsState.Loading -> {
                balanceSummaryCard.hide()
                participantsContainer.hide()
                emptyState.hide()
            }
            is TripSettlementsState.Success -> {
                emptyState.hide()
                balanceSummaryCard.show()
                scrollParticipants.show()

                displayBalanceSummary(state.myTotalBalance, state.formattedMyTotalBalance)
                displayParticipants(state.participants, state.tripCurrency)
            }
            is TripSettlementsState.Empty -> {
                balanceSummaryCard.hide()
                scrollParticipants.hide()
                emptyState.show()
            }
            is TripSettlementsState.Error -> {
                balanceSummaryCard.hide()
                participantsContainer.hide()
                emptyState.hide()
                showError(state.message)
            }
        }
    }

    /**
     * Wyświetla podsumowanie mojego całkowitego balansu
     */
    private fun displayBalanceSummary(totalBalance: Float, formattedBalance: String) {
        balanceAmount.text = formattedBalance

        when {
            totalBalance > 0.01f -> {
                balanceAmount.setTextColor(resources.getColor(R.color.success, null))
            }
            totalBalance < -0.01f -> {
                balanceAmount.setTextColor(resources.getColor(R.color.error, null))
            }
            else -> {
                balanceAmount.setTextColor(resources.getColor(R.color.text_secondary, null))
            }
        }
    }

    /**
     * Wyświetla listę uczestników z balansami
     */
    private fun displayParticipants(
        participants: List<SettlementParticipantUiModel>,
        tripCurrency: String
    ) {
        participantsContainer.removeAllViews()

        participants.forEach { participant ->
            val itemView = createParticipantView(participant)
            participantsContainer.addView(itemView)
        }
    }

    /**
     * Tworzy widok pojedynczego uczestnika
     */
    private fun createParticipantView(participant: SettlementParticipantUiModel): View {
        val view = layoutInflater.inflate(R.layout.item_settlement_participant, participantsContainer, false)

        // Nickname
        view.findViewById<TextView>(R.id.participantNickname).text = participant.nickname

        // Kwota balansu
        val balanceAmountView = view.findViewById<TextView>(R.id.balanceAmount)
        balanceAmountView.text = participant.formattedBalance

        // Ustaw kolor
        when (participant.balanceStatus) {
            ParticipantBalanceStatus.POSITIVE -> {
                balanceAmountView.setTextColor(resources.getColor(R.color.success, null))
            }
            ParticipantBalanceStatus.NEGATIVE -> {
                balanceAmountView.setTextColor(resources.getColor(R.color.error, null))
            }
            ParticipantBalanceStatus.SETTLED -> {
                balanceAmountView.setTextColor(resources.getColor(R.color.text_secondary, null))
            }
        }

        // Przyciski akcji
        val detailsButton = view.findViewById<MaterialButton>(R.id.settlementDetailsButton)
        val prepaymentButton = view.findViewById<MaterialButton>(R.id.prepaymentButton)
        val settleButton = view.findViewById<MaterialButton>(R.id.settleButton)

        // Przycisk Szczegóły - tylko jeśli są dane rozliczeń
        if (participant.hasSettlementDetails) {
            detailsButton.visibility = View.VISIBLE
            detailsButton.setOnClickListener {
                viewModel.onDetailsClicked(participant)
            }
        } else {
            detailsButton.visibility = View.GONE
        }

        // Przycisk Zaliczka - zawsze widoczny
        prepaymentButton.setOnClickListener {
            viewModel.onPrepaymentClicked(participant)
        }

        // Przycisk Rozlicz - tylko jeśli nie na 0
        if (participant.balanceStatus != ParticipantBalanceStatus.SETTLED) {
            settleButton.visibility = View.VISIBLE
            settleButton.setOnClickListener {
                viewModel.onSettleClicked(participant)
            }
        } else {
            settleButton.visibility = View.GONE
        }

        return view
    }

    /**
     * Pokazuje modal zaliczki
     */
    private fun showPrepaymentModal(model: PrepaymentUiModel) {
        val modal = PrepaymentModalFragment.newInstance(model) { request ->
            // Uzupełnij tripId
            val fullRequest = request.copy(tripId = getTripId())
            viewModel.onPrepaymentConfirmed(fullRequest)
        }
        modal.show(parentFragmentManager, "prepayment_modal")
    }

    /**
     * Pobiera ID wycieczki
     */
    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID) ?: ""
    }

    companion object {
        private const val ARG_TRIP_ID = "trip_id"

        fun newInstance(tripId: String) = TripSettlementsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TRIP_ID, tripId)
            }
        }
    }
}
