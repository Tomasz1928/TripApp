package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R
import com.google.android.material.tabs.TabLayout

/**
 * Modal szczegółów rozliczenia z uczestnikiem
 *
 * Tab 1 - Podsumowanie:
 *   - Relacja wszystkich kosztów (allRelatedAmount)
 *   - Pozostało do rozliczenia (leftForSettled)
 *
 * Tab 2 - Koszty:
 *   - Lista wydatków dotyczących relacji ja ↔ participant
 *   - Kolor kwoty: zielony (do mnie) / czerwony (ode mnie)
 *   - Ikona: ic_success zielony (rozliczone) / ic_cross czerwony (nierozliczone)
 *
 * Tab 3 - Zaliczki:
 *   - Pozostało z zaliczek (amountLeft) z kierunkiem
 *   - Historia zaliczek (history) posortowana od najnowszej
 */
class SettlementDetailsModalFragment : DialogFragment() {

    private var detailsModel: SettlementDetailsUiModel? = null

    // Views - common
    private lateinit var closeButton: ImageView
    private lateinit var participantNickname: TextView
    private lateinit var tabLayout: TabLayout

    // Views - Tab 1: Podsumowanie
    private lateinit var tabSummary: ScrollView
    private lateinit var allRelatedContainer: LinearLayout
    private lateinit var leftForSettledContainer: LinearLayout

    // Views - Tab 2: Koszty
    private lateinit var tabCosts: LinearLayout
    private lateinit var costsScrollView: ScrollView
    private lateinit var costsListContainer: LinearLayout
    private lateinit var costsEmptyState: TextView

    // Views - Tab 3: Zaliczki
    private lateinit var tabPrepayment: ScrollView
    private lateinit var prepaymentAmountLeftLabel: TextView
    private lateinit var prepaymentAmountLeftContainer: LinearLayout
    private lateinit var prepaymentSeparator: View
    private lateinit var prepaymentHistoryLabel: TextView
    private lateinit var prepaymentHistoryContainer: LinearLayout
    private lateinit var prepaymentEmptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_TripApp_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.modal_settlement_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model = detailsModel ?: run { dismiss(); return }

        initializeViews(view)
        setupTabLayout()
        populateHeader(model)
        populateSummaryTab(model)
        populateCostsTab(model)
        populatePrepaymentTab(model)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ==========================================
    // INITIALIZATION
    // ==========================================

    private fun initializeViews(view: View) {
        closeButton = view.findViewById(R.id.closeButton)
        participantNickname = view.findViewById(R.id.participantNickname)
        tabLayout = view.findViewById(R.id.tabLayout)

        // Tab 1
        tabSummary = view.findViewById(R.id.tabSummary)
        allRelatedContainer = view.findViewById(R.id.allRelatedContainer)
        leftForSettledContainer = view.findViewById(R.id.leftForSettledContainer)

        // Tab 2
        tabCosts = view.findViewById(R.id.tabCosts)
        costsScrollView = view.findViewById(R.id.costsScrollView)
        costsListContainer = view.findViewById(R.id.costsListContainer)
        costsEmptyState = view.findViewById(R.id.costsEmptyState)

        // Tab 3
        tabPrepayment = view.findViewById(R.id.tabPrepayment)
        prepaymentAmountLeftLabel = view.findViewById(R.id.prepaymentAmountLeftLabel)
        prepaymentAmountLeftContainer = view.findViewById(R.id.prepaymentAmountLeftContainer)
        prepaymentSeparator = view.findViewById(R.id.prepaymentSeparator)
        prepaymentHistoryLabel = view.findViewById(R.id.prepaymentHistoryLabel)
        prepaymentHistoryContainer = view.findViewById(R.id.prepaymentHistoryContainer)
        prepaymentEmptyState = view.findViewById(R.id.prepaymentEmptyState)
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        tabSummary.visibility = View.VISIBLE
                        tabCosts.visibility = View.GONE
                        tabPrepayment.visibility = View.GONE
                    }
                    1 -> {
                        tabSummary.visibility = View.GONE
                        tabCosts.visibility = View.VISIBLE
                        tabPrepayment.visibility = View.GONE
                    }
                    2 -> {
                        tabSummary.visibility = View.GONE
                        tabCosts.visibility = View.GONE
                        tabPrepayment.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun populateHeader(model: SettlementDetailsUiModel) {
        participantNickname.text = model.participantNickname
        closeButton.setOnClickListener { dismiss() }
    }

    // ==========================================
    // TAB 1: PODSUMOWANIE
    // ==========================================

    private fun populateSummaryTab(model: SettlementDetailsUiModel) {
        allRelatedContainer.removeAllViews()
        leftForSettledContainer.removeAllViews()

        model.allRelatedRows.forEach { row ->
            allRelatedContainer.addView(createAmountRow(row))
        }

        model.leftForSettledRows.forEach { row ->
            leftForSettledContainer.addView(createAmountRow(row))
        }
    }

    /**
     * Tworzy wiersz kwoty: [waluta]  [kwota]
     */
    private fun createAmountRow(row: SettlementDetailAmountRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_settlement_detail_row, allRelatedContainer, false)

        val currencyLabel = view.findViewById<TextView>(R.id.settleDetailModalCurrencyLabel)
        val amountValue = view.findViewById<TextView>(R.id.settleDetailModalAmountValue)

        currencyLabel.text = row.formattedCurrency
        amountValue.text = row.formattedAmount

        val colorRes = when {
            row.amount > 0.01f -> R.color.success
            row.amount < -0.01f -> R.color.error
            else -> R.color.text_secondary
        }
        amountValue.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        return view
    }

    // ==========================================
    // TAB 2: KOSZTY
    // ==========================================

    private fun populateCostsTab(model: SettlementDetailsUiModel) {
        costsListContainer.removeAllViews()

        if (model.costRows.isEmpty()) {
            costsEmptyState.visibility = View.VISIBLE
            costsScrollView.visibility = View.GONE
            return
        }

        costsEmptyState.visibility = View.GONE
        costsScrollView.visibility = View.VISIBLE

        model.costRows.forEach { costRow ->
            costsListContainer.addView(createCostRow(costRow))
        }

        // Ogranicz wysokość scrolla do ~10 wierszy (każdy ~48dp)
        costsScrollView.post {
            val maxHeightPx = (48 * 10 * resources.displayMetrics.density).toInt()
            if (costsListContainer.height > maxHeightPx) {
                costsScrollView.layoutParams = costsScrollView.layoutParams.apply {
                    height = maxHeightPx
                }
            }
        }
    }

    private fun createCostRow(costRow: SettlementDetailCostRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_settlement_detail_cost, costsListContainer, false)

        val costTitle = view.findViewById<TextView>(R.id.costTitle)
        val costAmount = view.findViewById<TextView>(R.id.costAmount)
        val settlementStatusIcon = view.findViewById<ImageView>(R.id.settlementStatusIcon)

        costTitle.text = costRow.expenseName
        costAmount.text = costRow.formattedAmount

        val amountColorRes = if (costRow.isAmountPositive) R.color.success else R.color.error
        costAmount.setTextColor(ContextCompat.getColor(requireContext(), amountColorRes))

        if (costRow.isSettled) {
            settlementStatusIcon.setImageResource(R.drawable.ic_success)
            settlementStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
        } else {
            settlementStatusIcon.setImageResource(R.drawable.ic_cross)
            settlementStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
        }

        return view
    }

    // ==========================================
    // TAB 3: ZALICZKI
    // ==========================================

    private fun populatePrepaymentTab(model: SettlementDetailsUiModel) {
        prepaymentAmountLeftContainer.removeAllViews()
        prepaymentHistoryContainer.removeAllViews()

        if (!model.hasPrepaymentData) {
            // Brak danych — pokaż empty state, ukryj resztę
            prepaymentEmptyState.visibility = View.VISIBLE
            prepaymentAmountLeftLabel.visibility = View.GONE
            prepaymentAmountLeftContainer.visibility = View.GONE
            prepaymentSeparator.visibility = View.GONE
            prepaymentHistoryLabel.visibility = View.GONE
            prepaymentHistoryContainer.visibility = View.GONE
            return
        }

        prepaymentEmptyState.visibility = View.GONE

        // Sekcja: Pozostało z zaliczek
        if (model.prepaymentAmountLeftRows.isNotEmpty()) {
            prepaymentAmountLeftLabel.visibility = View.VISIBLE
            prepaymentAmountLeftContainer.visibility = View.VISIBLE

            model.prepaymentAmountLeftRows.forEach { row ->
                prepaymentAmountLeftContainer.addView(createPrepaymentAmountRow(row))
            }
        } else {
            prepaymentAmountLeftLabel.visibility = View.GONE
            prepaymentAmountLeftContainer.visibility = View.GONE
        }

        // Separator — widoczny tylko gdy są obie sekcje
        prepaymentSeparator.visibility =
            if (model.prepaymentAmountLeftRows.isNotEmpty() && model.prepaymentHistoryRows.isNotEmpty())
                View.VISIBLE else View.GONE

        // Sekcja: Historia zaliczek
        if (model.prepaymentHistoryRows.isNotEmpty()) {
            prepaymentHistoryLabel.visibility = View.VISIBLE
            prepaymentHistoryContainer.visibility = View.VISIBLE

            model.prepaymentHistoryRows.forEach { row ->
                prepaymentHistoryContainer.addView(createPrepaymentHistoryRow(row))
            }
        } else {
            prepaymentHistoryLabel.visibility = View.GONE
            prepaymentHistoryContainer.visibility = View.GONE
        }
    }

    /**
     * Tworzy wiersz "Pozostało z zaliczek": [kwota] [waluta]
     *
     * TO_ME → zielony (pieniądze do mnie)
     * FROM_ME → czerwony (pieniądze ode mnie)
     */
    private fun createPrepaymentAmountRow(row: PrepaymentAmountLeftRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_prepayment_amount_row, prepaymentAmountLeftContainer, false)

        val amount = view.findViewById<TextView>(R.id.prepaymentAmount)
        val currency = view.findViewById<TextView>(R.id.prepaymentCurrency)

        amount.text = row.formattedAmount
        currency.text = row.currency

        val colorRes = when (row.direction) {
            PrepaymentAmountDirection.TO_ME -> R.color.success
            PrepaymentAmountDirection.FROM_ME -> R.color.error
        }
        amount.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        currency.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        return view
    }

    /**
     * Tworzy wiersz historii: [kwota] [waluta] [data]
     *
     * TO_ME → zielony (pieniądze do mnie)
     * FROM_ME → czerwony (pieniądze ode mnie)
     */
    private fun createPrepaymentHistoryRow(row: PrepaymentHistoryRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_prepayment_history_row, prepaymentHistoryContainer, false)

        val amount = view.findViewById<TextView>(R.id.historyAmount)
        val currency = view.findViewById<TextView>(R.id.historyCurrency)
        val date = view.findViewById<TextView>(R.id.historyDate)

        amount.text = row.formattedAmount
        currency.text = row.currency
        date.text = row.formattedDate

        val colorRes = when (row.direction) {
            PrepaymentAmountDirection.TO_ME -> R.color.success
            PrepaymentAmountDirection.FROM_ME -> R.color.error
        }
        amount.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        currency.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        return view
    }

    // ==========================================
    // COMPANION
    // ==========================================

    companion object {
        fun newInstance(model: SettlementDetailsUiModel): SettlementDetailsModalFragment {
            return SettlementDetailsModalFragment().apply {
                this.detailsModel = model
            }
        }
    }
}