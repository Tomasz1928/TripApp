package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.extension.hide
import com.example.tripapp2.ui.common.extension.show
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Fragment rozliczeń
 *
 * Przepływ danych:
 * - Lista uczestników pochodzi z TripDto.participants (bez mnie)
 * - Balans pochodzi z TripDto.settlement.relations
 *
 * Logika przycisków:
 * - Zaliczka: ZAWSZE widoczny dla każdego uczestnika
 * - Szczegóły: widoczny TYLKO gdy hasSettlementRelation = true
 * - Rozlicz: widoczny TYLKO gdy hasSettlementRelation = true AND balance != 0
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

        // Event otwarcia modala szczegółów
        viewModel.showDetailsModalEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { participant ->
                showDetailsModal(participant)
            }
        }

        // Event otwarcia modala rozliczenia
        viewModel.showSettleModalEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { model ->
                showSettleModal(model)
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
            tripBottomNav.visibility = View.VISIBLE

            val fragment = com.example.tripapp2.ui.tripdetails.TripDetailsFragment.newInstance(getTripId())
            supportFragmentManager.beginTransaction()
                .replace(R.id.tripContainer, fragment, "tripDetails")
                .commit()

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
                scrollParticipants.hide()
                participantsContainer.hide()
                emptyState.hide()
            }
            is TripSettlementsState.Success -> {
                emptyState.hide()
                balanceSummaryCard.show()
                scrollParticipants.show()
                participantsContainer.show()

                displayBalanceSummary(
                    state.myTotalBalance,
                    state.formattedMyTotalBalance,
                    state.myBalanceStatus
                )
                displayParticipants(state.participants, state.tripCurrency)
            }
            is TripSettlementsState.Empty -> {
                balanceSummaryCard.hide()
                scrollParticipants.hide()
                participantsContainer.hide()
                emptyState.show()
            }
            is TripSettlementsState.Error -> {
                balanceSummaryCard.hide()
                scrollParticipants.hide()
                participantsContainer.hide()
                emptyState.hide()
                showError(state.message)
            }
        }
    }

    /**
     * Wyświetla podsumowanie mojego całkowitego balansu
     */
    private fun displayBalanceSummary(
        totalBalance: Float,
        formattedBalance: String,
        balanceStatus: ParticipantBalanceStatus
    ) {
        balanceAmount.text = formattedBalance

        val (colorRes, statusTextRes) = when (balanceStatus) {
            ParticipantBalanceStatus.POSITIVE -> {
                R.color.success to R.string.settlements_balance_positive
            }
            ParticipantBalanceStatus.NEGATIVE -> {
                R.color.error to R.string.settlements_balance_negative
            }
            ParticipantBalanceStatus.SETTLED -> {
                R.color.text_secondary to R.string.settlements_balance_settled
            }
        }

        balanceAmount.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    /**
     * Wyświetla listę uczestników z ich balansem
     */
    private fun displayParticipants(
        participants: List<SettlementParticipantUiModel>,
        currency: String
    ) {
        participantsContainer.removeAllViews()

        participants.forEach { participant ->
            val cardView = createParticipantCard(participant, currency)
            participantsContainer.addView(cardView)
        }
    }

    /**
     * Tworzy kartę uczestnika z przyciskami
     *
     * Logika przycisków:
     * - Zaliczka: ZAWSZE widoczny
     * - Szczegóły: tylko gdy hasSettlementRelation = true
     * - Rozlicz: tylko gdy hasSettlementRelation = true AND balanceStatus != SETTLED
     */
    private fun createParticipantCard(
        participant: SettlementParticipantUiModel,
        currency: String
    ): View {
        val inflater = LayoutInflater.from(requireContext())
        val cardView = inflater.inflate(R.layout.item_settlement_participant, participantsContainer, false)

        // Znajdź views w karcie
        val nicknameText = cardView.findViewById<TextView>(R.id.participantNickname)
        val balanceText = cardView.findViewById<TextView>(R.id.balanceAmount)
        val prepaymentButton = cardView.findViewById<MaterialButton>(R.id.prepaymentButton)
        val detailsButton = cardView.findViewById<MaterialButton>(R.id.settlementDetailsButton)
        val settleButton = cardView.findViewById<MaterialButton>(R.id.settleButton)

        // Ustaw dane
        nicknameText.text = participant.nickname

        // Placeholder badge (jeśli potrzebny)
        if (participant.isPlaceholder) {
            nicknameText.text = "${participant.nickname} (placeholder)"
        }

        // Balans i kolor
        balanceText.text = participant.formattedBalance

        val (colorRes, statusTextRes) = when (participant.balanceStatus) {
            ParticipantBalanceStatus.POSITIVE -> {
                R.color.success to R.string.settlements_balance_positive
            }
            ParticipantBalanceStatus.NEGATIVE -> {
                R.color.error to R.string.settlements_balance_negative
            }
            ParticipantBalanceStatus.SETTLED -> {
                R.color.text_secondary to R.string.settlements_balance_settled
            }
        }

        balanceText.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        // ==========================================
        // LOGIKA PRZYCISKÓW
        // ==========================================

        // Zaliczka - ZAWSZE widoczny
        prepaymentButton.visibility = View.VISIBLE
        prepaymentButton.setOnClickListener {
            viewModel.onPrepaymentClicked(participant)
        }

        // Szczegóły - tylko gdy hasSettlementRelation = true
        if (participant.hasSettlementRelation) {
            detailsButton.visibility = View.VISIBLE
            detailsButton.setOnClickListener {
                viewModel.onDetailsClicked(participant)
            }
        } else {
            detailsButton.visibility = View.GONE
        }

        // Rozlicz - tylko gdy hasSettlementRelation = true AND balance != 0
        if (participant.hasSettlementRelation && participant.balanceStatus != ParticipantBalanceStatus.SETTLED) {
            settleButton.visibility = View.VISIBLE
            settleButton.setOnClickListener {
                viewModel.onSettleClicked(participant)
            }
        } else {
            settleButton.visibility = View.GONE
        }

        return cardView
    }

    /**
     * Pokazuje modal zaliczki
     *
     * Przepływ:
     * 1. Tworzy PrepaymentModalFragment z danymi uczestnika
     * 2. Po potwierdzeniu w modalu - callback z PrepaymentRequest
     * 3. Uzupełnia tripId i przekazuje do ViewModel
     * 4. ViewModel zapisuje przez Repository → cache aktualizowany
     * 5. UI odświeżane przez loadSettlements()
     */
    private fun showPrepaymentModal(model: PrepaymentUiModel) {
        val modal = PrepaymentModalFragment.newInstance(model) { request ->
            // Uzupełnij tripId (modal go nie zna)
            val fullRequest = request.copy(tripId = getTripId())
            // Przekaż do ViewModel - zapisze i odświeży dane
            viewModel.onPrepaymentConfirmed(fullRequest)
        }
        modal.show(parentFragmentManager, "prepayment_modal")
    }

    /**
     * Pokazuje modal szczegółów rozliczenia
     */
    private fun showDetailsModal(participant: SettlementParticipantUiModel) {
        // TODO: Implementacja modala szczegółów
        val balanceInfo = when (participant.balanceStatus) {
            ParticipantBalanceStatus.POSITIVE -> "${participant.nickname} jest Ci winien/winna ${participant.formattedBalance}"
            ParticipantBalanceStatus.NEGATIVE -> "Jesteś winien/winna ${participant.nickname} ${participant.formattedBalance}"
            ParticipantBalanceStatus.SETTLED -> "Rozliczenie z ${participant.nickname} jest zakończone"
        }
        showMessage(balanceInfo)
    }

    /**
     * Pokazuje modal rozliczenia
     */
    private fun showSettleModal(model: SettleModalUiModel) {
        val modal = SettleModalFragment.newInstance(
            model = model,
            tripId = getTripId(),
            currentUserId = viewModel.getCurrentUserId(),
            tripData = viewModel.getTripData(),  // NOWE: dane wycieczki dla tab 2
            onConfirm = { request ->
                viewModel.onSettleConfirmedFromModal(request)
            },
            onConfirmByCosts = { request ->        // NOWE: callback dla tab 2
                viewModel.onSettleByCostsConfirmed(request)
            }
        )
        modal.show(parentFragmentManager, "settle_modal")
    }

    companion object {
        private const val ARG_TRIP_ID = "tripId"

        fun newInstance(tripId: String): TripSettlementsFragment {
            return TripSettlementsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRIP_ID, tripId)
                }
            }
        }
    }

    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID)
            ?: throw IllegalStateException("Trip ID is required")
    }
}