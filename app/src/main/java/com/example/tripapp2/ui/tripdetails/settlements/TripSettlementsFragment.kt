package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.extension.hide
import com.example.tripapp2.ui.common.extension.show
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.card.MaterialCardView

/**
 * Fragment rozliczeń między uczestnikami
 */
class TripSettlementsFragment : BaseFragment<TripSettlementsViewModel>(R.layout.fragment_trip_settlements) {

    override val viewModel: TripSettlementsViewModel by viewModels {
        TripSettlementsViewModelFactory(getTripId())
    }

    private lateinit var backButton: ImageView
    private lateinit var scrollView: NestedScrollView
    private lateinit var balanceCard: MaterialCardView
    private lateinit var balanceAmount: TextView
    private lateinit var balanceStatus: TextView
    private lateinit var owedToYou: TextView
    private lateinit var youOwe: TextView
    private lateinit var tripNameLabel: TextView
    private lateinit var relationsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout

    override fun setupUI() {
        initializeViews()
        setupClickListeners()
    }

    override fun setupCustomObservers() {
        // Stan rozliczeń
        viewModel.settlementsState.observe(viewLifecycleOwner) { state ->
            handleSettlementsState(state)
        }

        // Event pokazania szczegółów
        viewModel.showSettlementDetailEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { detail ->
                showSettlementDetailModal(detail)
            }
        }

        // Event potwierdzenia rozliczenia
        viewModel.settlementConfirmedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                showMessage(message)
                viewModel.loadSettlements() // Odśwież dane
            }
        }
    }

    private fun initializeViews() {
        val view = requireView()
        backButton = view.findViewById(R.id.backButton)
        scrollView = view.findViewById(R.id.scrollViewSettlements)
        balanceCard = view.findViewById(R.id.balanceCard)
        balanceAmount = view.findViewById(R.id.balanceAmount)
        balanceStatus = view.findViewById(R.id.balanceStatus)
        owedToYou = view.findViewById(R.id.owedToYou)
        youOwe = view.findViewById(R.id.youOwe)
        tripNameLabel = view.findViewById(R.id.tripNameLabel)
        relationsContainer = view.findViewById(R.id.relationsContainer)
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

            // Wróć do TripDetails (bez wywoływania setOnItemSelectedListener)
            val fragment = com.example.tripapp2.ui.tripdetails.TripDetailsFragment.newInstance(getTripId())
            supportFragmentManager.beginTransaction()
                .replace(R.id.tripContainer, fragment, "tripDetails")
                .commit()

            // Ustaw wybrany item (to NIE wywoła listenera, bo fragment już został zmieniony)
            tripBottomNav.selectedItemId = R.id.menu_overview
        }
    }

    /**
     * Obsługa różnych stanów ekranu
     */
    private fun handleSettlementsState(state: TripSettlementsState) {
        when (state) {
            is TripSettlementsState.Loading -> {
                balanceCard.hide()
                relationsContainer.hide()
                emptyState.hide()
            }
            is TripSettlementsState.Success -> {
                emptyState.hide()
                balanceCard.show()
                relationsContainer.show()

                displayBalance(state.userBalance)
                displayRelations(state.relations)
                tripNameLabel.text = state.tripName
            }
            is TripSettlementsState.AllSettled -> {
                balanceCard.hide()
                relationsContainer.hide()
                emptyState.show()
            }
            is TripSettlementsState.Error -> {
                balanceCard.hide()
                relationsContainer.hide()
                emptyState.hide()
                showError(state.message)
            }
        }
    }

    /**
     * Wyświetla bilans użytkownika
     */
    private fun displayBalance(balance: UserBalanceUiModel) {
        balanceAmount.text = balance.formattedBalance

        when (balance.balanceStatus) {
            BalanceStatusUi.NA_PLUSIE -> {
                balanceAmount.setTextColor(resources.getColor(R.color.success, null))
                balanceStatus.text = "na plusie"
                balanceStatus.setTextColor(resources.getColor(R.color.success, null))
            }
            BalanceStatusUi.NA_MINUSIE -> {
                balanceAmount.setTextColor(resources.getColor(R.color.error, null))
                balanceStatus.text = "na minusie"
                balanceStatus.setTextColor(resources.getColor(R.color.error, null))
            }
            BalanceStatusUi.ROZLICZONY -> {
                balanceAmount.setTextColor(resources.getColor(R.color.text_secondary, null))
                balanceStatus.text = "rozliczony"
                balanceStatus.setTextColor(resources.getColor(R.color.text_secondary, null))
            }
        }

        owedToYou.text = balance.formattedOwedToYou
        youOwe.text = balance.formattedYouOwe
    }

    /**
     * Wyświetla listę relacji
     */
    private fun displayRelations(relations: List<SettlementRelationUiModel>) {
        relationsContainer.removeAllViews()

        relations.forEach { relation ->
            val view = createRelationView(relation)
            relationsContainer.addView(view)
        }
    }

    /**
     * Tworzy widok pojedynczej relacji
     */
    private fun createRelationView(relation: SettlementRelationUiModel): View {
        val view = layoutInflater.inflate(R.layout.item_settlement_relation, relationsContainer, false)

        // Description
        val description = if (relation.currentUserIsDebtor) {
            "Ty winien ${relation.toUserName}"
        } else if (relation.isCurrentUserInvolved) {
            "${relation.fromUserName} winna Tobie"
        } else {
            "${relation.fromUserName} winien ${relation.toUserName}"
        }

        view.findViewById<TextView>(R.id.relationDescription).text = description
        view.findViewById<TextView>(R.id.relationAmount).text = relation.formattedAmount

        // Button visibility
        val settleButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.settleButton)
        val settledBadge = view.findViewById<LinearLayout>(R.id.settledBadge)

        when {
            relation.isSettled -> {
                // Już rozliczone
                settleButton.visibility = View.GONE
                settledBadge.visibility = View.VISIBLE
            }
            relation.isCurrentUserInvolved -> {
                // Dotyczy zalogowanego użytkownika - pokaż przycisk
                settleButton.visibility = View.VISIBLE
                settledBadge.visibility = View.GONE
            }
            else -> {
                // Relacja innych osób - tylko podgląd
                settleButton.visibility = View.GONE
                settledBadge.visibility = View.GONE
            }
        }

        // Click listeners
        view.setOnClickListener {
            viewModel.onRelationClicked(relation)
        }

        settleButton.setOnClickListener {
            viewModel.onRelationClicked(relation)
        }

        return view
    }

    /**
     * Pokazuje modal ze szczegółami rozliczenia
     */
    private fun showSettlementDetailModal(detail: SettlementDetailUiModel) {
        val modal = SettlementDetailModalFragment.newInstance(detail) { currency, amount ->
            viewModel.onSettleInCurrency(detail.relationId, currency, amount)
        }
        modal.show(parentFragmentManager, "settlement_detail_modal")
    }

    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID) ?: "trip_2"
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