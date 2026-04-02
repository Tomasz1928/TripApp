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
 * Fragment rozliczeń — Propozycja C: Floating Sections
 *
 * Zmiany vs oryginał:
 * - Brak header image (top bar z back + tytuł)
 * - Hero balance: duża kwota centralnie z kolorem wg statusu
 * - Opis tekstowy pod balansem (kto komu ile jest winien)
 * - Participant cards — bez zmian strukturalnych, dodana opacity dla settled
 * - balanceSummaryCard ukryty (kompatybilność z ViewModel)
 *
 * Logika przycisków (BEZ ZMIAN):
 * - Zaliczka: ZAWSZE widoczny
 * - Szczegóły: tylko gdy hasSettlementRelation = true
 * - Rozlicz: tylko gdy hasSettlementRelation = true AND balanceStatus != SETTLED
 */
class TripSettlementsFragment : BaseFragment<TripSettlementsViewModel>(R.layout.fragment_trip_settlements) {

    override val viewModel: TripSettlementsViewModel by viewModels {
        TripSettlementsViewModelFactory(getTripId())
    }

    // ================================
    // VIEWS
    // ================================
    private lateinit var backButton: ImageView
    private lateinit var scrollParticipants: ScrollView
    private lateinit var participantsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout

    // Hero balance (nowe w Propozycji C)
    private lateinit var balanceAmount: TextView
    private lateinit var balanceCurrency: TextView
    private lateinit var balanceDescription: TextView
    private lateinit var heroBalance: LinearLayout

    // Hidden (kompatybilność z ViewModel)
    private lateinit var balanceSummaryCard: MaterialCardView

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
        scrollParticipants = view.findViewById(R.id.scrollParticipants)
        participantsContainer = view.findViewById(R.id.participantsContainer)
        emptyState = view.findViewById(R.id.emptyState)

        // Hero balance
        balanceAmount = view.findViewById(R.id.balanceAmount)
        balanceCurrency = view.findViewById(R.id.balanceCurrency)
        balanceDescription = view.findViewById(R.id.balanceDescription)
        heroBalance = view.findViewById(R.id.heroBalance)

        // Hidden
        balanceSummaryCard = view.findViewById(R.id.balanceSummaryCard)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            onBackClicked()
        }
    }

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

    // ================================================================
    // STATE HANDLING
    // ================================================================

    private fun handleSettlementsState(state: TripSettlementsState) {
        when (state) {
            is TripSettlementsState.Loading -> {
                heroBalance.hide()
                scrollParticipants.hide()
                emptyState.hide()
            }
            is TripSettlementsState.Success -> {
                emptyState.hide()
                heroBalance.show()
                scrollParticipants.show()

                displayHeroBalance(
                    state.myTotalBalance,
                    state.formattedMyTotalBalance,
                    state.myBalanceStatus,
                    state.tripCurrency
                )
                displayParticipants(state.participants, state.tripCurrency)
            }
            is TripSettlementsState.Empty -> {
                heroBalance.hide()
                scrollParticipants.hide()
                emptyState.show()
            }
            is TripSettlementsState.Error -> {
                heroBalance.hide()
                scrollParticipants.hide()
                emptyState.hide()
                showError(state.message)
            }
        }
    }

    // ================================================================
    // HERO BALANCE
    // ================================================================

    private fun displayHeroBalance(
        totalBalance: Float,
        formattedBalance: String,
        balanceStatus: ParticipantBalanceStatus,
        currency: String
    ) {
        // Kwota
        balanceAmount.text = "%.2f".format(totalBalance)
        balanceCurrency.text = currency

        // Kolor na podstawie statusu
        val colorRes = when (balanceStatus) {
            ParticipantBalanceStatus.POSITIVE -> R.color.success
            ParticipantBalanceStatus.NEGATIVE -> R.color.error
            ParticipantBalanceStatus.SETTLED -> R.color.text_secondary
        }
        balanceAmount.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        // Opis
        balanceDescription.text = when (balanceStatus) {
            ParticipantBalanceStatus.POSITIVE ->
                "Inni są Ci winni łącznie $formattedBalance"
            ParticipantBalanceStatus.NEGATIVE ->
                "Jesteś winien innym łącznie $formattedBalance"
            ParticipantBalanceStatus.SETTLED ->
                "Wszystko rozliczone"
        }
    }

    // ================================================================
    // PARTICIPANTS
    // ================================================================

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

    private fun createParticipantCard(
        participant: SettlementParticipantUiModel,
        currency: String
    ): View {
        val inflater = LayoutInflater.from(requireContext())
        val cardView = inflater.inflate(R.layout.item_settlement_participant, participantsContainer, false)

        val nicknameText = cardView.findViewById<TextView>(R.id.participantNickname)
        val balanceText = cardView.findViewById<TextView>(R.id.balanceAmount)
        val prepaymentButton = cardView.findViewById<MaterialButton>(R.id.prepaymentButton)
        val detailsButton = cardView.findViewById<MaterialButton>(R.id.settlementDetailsButton)
        val settleButton = cardView.findViewById<MaterialButton>(R.id.settleButton)

        // Dane
        nicknameText.text = participant.nickname
        if (participant.isPlaceholder) {
            nicknameText.text = "${participant.nickname} (placeholder)"
        }

        // Balans i kolor
        balanceText.text = participant.formattedBalance

        val (colorRes, _) = when (participant.balanceStatus) {
            ParticipantBalanceStatus.POSITIVE -> R.color.success to R.string.settlements_balance_positive
            ParticipantBalanceStatus.NEGATIVE -> R.color.error to R.string.settlements_balance_negative
            ParticipantBalanceStatus.SETTLED -> R.color.text_secondary to R.string.settlements_balance_settled
        }
        balanceText.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        // Opacity dla rozliczonych (Propozycja C)
        if (participant.isSettled) {
            cardView.alpha = 0.7f
        }

        // ==========================================
        // LOGIKA PRZYCISKÓW (BEZ ZMIAN)
        // ==========================================

        // Zaliczka - ZAWSZE widoczny
        prepaymentButton.visibility = View.VISIBLE
        prepaymentButton.setOnClickListener {
            viewModel.onPrepaymentClicked(participant)
        }

        // Szczegóły - tylko gdy hasSettlementRelation
        if (participant.hasSettlementRelation) {
            detailsButton.visibility = View.VISIBLE
            detailsButton.setOnClickListener {
                viewModel.onDetailsClicked(participant)
            }
        } else {
            detailsButton.visibility = View.GONE
        }

        // Rozlicz - tylko gdy hasSettlementRelation AND balance != 0
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

    // ================================================================
    // MODALS (BEZ ZMIAN)
    // ================================================================

    private fun showPrepaymentModal(model: PrepaymentUiModel) {
        val modal = PrepaymentModalFragment.newInstance(model) { request ->
            val fullRequest = request.copy(tripId = getTripId())
            viewModel.onPrepaymentConfirmed(fullRequest)
        }
        modal.show(parentFragmentManager, "prepayment_modal")
    }

    private fun showDetailsModal(participant: SettlementParticipantUiModel) {
        val tripData = viewModel.getTripData() ?: return
        val currentUserId = viewModel.getCurrentUserId()

        val relation = tripData.settlement?.relations?.find {
            it.relatedId == participant.participantId
        }

        val detailsModel = createSettlementDetailsModel(
            participant = participant,
            tripCurrency = tripData.currency,
            expenses = tripData.expenses,
            currentUserId = currentUserId,
            relation = relation
        )

        val modal = SettlementDetailsModalFragment.newInstance(detailsModel)
        modal.show(parentFragmentManager, "settlement_details_modal")
    }

    private fun showSettleModal(model: SettleModalUiModel) {
        val modal = SettleModalFragment.newInstance(
            model = model,
            tripId = getTripId(),
            currentUserId = viewModel.getCurrentUserId(),
            tripData = viewModel.getTripData(),
            onConfirm = { request ->
                viewModel.onSettleConfirmedFromModal(request)
            },
            onConfirmByCosts = { request ->
                viewModel.onSettleByCostsConfirmed(request)
            }
        )
        modal.show(parentFragmentManager, "settle_modal")
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID)
            ?: throw IllegalStateException("Trip ID is required")
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
}
