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
import com.example.tripapp2.R
import com.example.tripapp2.data.model.SettlementBreakdownType
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment
import com.google.android.material.tabs.TabLayout

/**
 * Modal szczegółów rozliczenia z uczestnikiem.
 * ZMIGOWANY na BaseModalFragment.
 *
 * Body = modal_settlement_details_body.xml (TabLayout + 3 taby)
 *
 * ZMIENIONE: Tab 2 (Koszty) — ikonki per SettlementBreakdownType zamiast ✓/✗
 * Każdy rozliczony typ ma kolorową ikonę + zielone V badge.
 * UNSETTLED ma czerwony zegar + X badge.
 * Ikonki są kolorowe (JPG/vector) — NIE stosujemy tint.
 */
class SettlementDetailsModalFragment : BaseModalFragment() {

    private var detailsModel: SettlementDetailsUiModel? = null

    // Views
    private lateinit var tabLayout: TabLayout
    private lateinit var tabSummary: ScrollView
    private lateinit var allRelatedContainer: LinearLayout
    private lateinit var leftForSettledContainer: LinearLayout
    private lateinit var tabCosts: LinearLayout
    private lateinit var costsScrollView: ScrollView
    private lateinit var costsListContainer: LinearLayout
    private lateinit var costsEmptyState: TextView
    private lateinit var tabPrepayment: ScrollView
    private lateinit var prepaymentAmountLeftLabel: TextView
    private lateinit var prepaymentAmountLeftContainer: LinearLayout
    private lateinit var prepaymentSeparator: View
    private lateinit var prepaymentHistoryLabel: TextView
    private lateinit var prepaymentHistoryContainer: LinearLayout
    private lateinit var prepaymentEmptyState: TextView

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View? {
        return inflater.inflate(R.layout.modal_settlement_details_body, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model = detailsModel ?: run { dismissAnimated(); return }

        setModalTitle(getString(R.string.settlement_details_title))
        setModalSubtitle(model.participantNickname)

        initializeViews()
        setupTabLayout()
        populateSummaryTab(model)
        populateCostsTab(model)
        populatePrepaymentTab(model)
    }

    // ==========================================
    // INITIALIZATION — 1:1 Z ORYGINAŁEM
    // ==========================================

    private fun initializeViews() {
        val body = modalBodyContainer ?: return
        tabLayout = body.findViewById(R.id.tabLayout)

        tabSummary = body.findViewById(R.id.tabSummary)
        allRelatedContainer = body.findViewById(R.id.allRelatedContainer)
        leftForSettledContainer = body.findViewById(R.id.leftForSettledContainer)

        tabCosts = body.findViewById(R.id.tabCosts)
        costsScrollView = body.findViewById(R.id.costsScrollView)
        costsListContainer = body.findViewById(R.id.costsListContainer)
        costsEmptyState = body.findViewById(R.id.costsEmptyState)

        tabPrepayment = body.findViewById(R.id.tabPrepayment)
        prepaymentAmountLeftLabel = body.findViewById(R.id.prepaymentAmountLeftLabel)
        prepaymentAmountLeftContainer = body.findViewById(R.id.prepaymentAmountLeftContainer)
        prepaymentSeparator = body.findViewById(R.id.prepaymentSeparator)
        prepaymentHistoryLabel = body.findViewById(R.id.prepaymentHistoryLabel)
        prepaymentHistoryContainer = body.findViewById(R.id.prepaymentHistoryContainer)
        prepaymentEmptyState = body.findViewById(R.id.prepaymentEmptyState)
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { tabSummary.visibility = View.VISIBLE; tabCosts.visibility = View.GONE; tabPrepayment.visibility = View.GONE }
                    1 -> { tabSummary.visibility = View.GONE; tabCosts.visibility = View.VISIBLE; tabPrepayment.visibility = View.GONE }
                    2 -> { tabSummary.visibility = View.GONE; tabCosts.visibility = View.GONE; tabPrepayment.visibility = View.VISIBLE }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ==========================================
    // TAB 1: PODSUMOWANIE — 1:1
    // ==========================================

    private fun populateSummaryTab(model: SettlementDetailsUiModel) {
        allRelatedContainer.removeAllViews()
        leftForSettledContainer.removeAllViews()

        model.allRelatedRows.forEach { row -> allRelatedContainer.addView(createAmountRow(row)) }
        model.leftForSettledRows.forEach { row -> leftForSettledContainer.addView(createAmountRow(row)) }
    }

    private fun createAmountRow(row: SettlementDetailAmountRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_settlement_detail_row, allRelatedContainer, false)

        view.findViewById<TextView>(R.id.settleDetailModalCurrencyLabel).text = row.formattedCurrency
        val amountValue = view.findViewById<TextView>(R.id.settleDetailModalAmountValue)
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
    // TAB 2: KOSZTY — ZMIENIONE (breakdown icons)
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

        model.costRows.forEach { costRow -> costsListContainer.addView(createCostRow(costRow)) }

        costsScrollView.post {
            val maxHeightPx = (48 * 10 * resources.displayMetrics.density).toInt()
            if (costsListContainer.height > maxHeightPx) {
                costsScrollView.layoutParams = costsScrollView.layoutParams.apply { height = maxHeightPx }
            }
        }
    }

    /**
     * ZMIENIONE: Zamiast prostego ✓/✗ — renderuje ikony per SettlementBreakdownType
     *
     * Layout: [tytuł] [kwota] [duża ikona 24dp] [małe ikony 16dp...]
     *
     * Ikonki są kolorowe same w sobie — NIE stosujemy tint/colorFilter.
     * Kolory:
     * - SELF: szary motyw + zielone V
     * - MANUAL_BY_*: zielony motyw + zielone V
     * - AUTO_*: fioletowy motyw + zielone V
     * - UNSETTLED: czerwony motyw + czerwony X
     */
    private fun createCostRow(costRow: SettlementDetailCostRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_settlement_detail_cost, costsListContainer, false)

        // Tytuł kosztu
        view.findViewById<TextView>(R.id.costTitle).text = costRow.expenseName

        // Kwota + kolor kierunku
        val costAmount = view.findViewById<TextView>(R.id.costAmount)
        costAmount.text = costRow.formattedAmount
        val amountColorRes = if (costRow.isAmountPositive) R.color.success else R.color.error
        costAmount.setTextColor(ContextCompat.getColor(requireContext(), amountColorRes))

        // Ikona dominująca (24dp) — NIE stosujemy tint, ikonki są kolorowe
        val mainIcon = view.findViewById<ImageView>(R.id.settlementMainIcon)
        mainIcon.setImageResource(getBreakdownIconRes(costRow.dominantType))
        mainIcon.contentDescription = getBreakdownContentDescription(costRow.dominantType)
        // Wyczyść ewentualny stary tint z recyklingu widoku
        mainIcon.clearColorFilter()

        // Małe ikony secondary (16dp) — dynamicznie dodawane
        val secondaryContainer = view.findViewById<LinearLayout>(R.id.settlementSecondaryIcons)
        secondaryContainer.removeAllViews()

        costRow.secondaryTypes.forEach { type ->
            val sizePx = (16 * resources.displayMetrics.density).toInt()
            val marginPx = (2 * resources.displayMetrics.density).toInt()

            val smallIcon = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginStart = marginPx
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(getBreakdownIconRes(type))
                contentDescription = getBreakdownContentDescription(type)
            }
            secondaryContainer.addView(smallIcon)
        }

        return view
    }

    /**
     * Zwraca drawable resource dla danego typu breakdown.
     *
     * Kolory ikon (wbudowane w drawable — bez tinta):
     * - SELF:              szary   + zielone V badge
     * - MANUAL_BY_AMOUNT:  zielony + zielone V badge (monetki)
     * - MANUAL_BY_COSTS:   zielony + zielone V badge (rachunek + długopis)
     * - AUTO_PREPAYMENT:   fioletowy + zielone V badge (moneta + strzałka)
     * - AUTO_CROSS_SETTLE: fioletowy + zielone V badge (strzałki krzyżowe)
     * - UNSETTLED:         czerwony + czerwone X badge (zegar)
     */
    private fun getBreakdownIconRes(type: SettlementBreakdownType): Int {
        return when (type) {
            SettlementBreakdownType.SELF              -> R.drawable.ic_breakdown_self
            SettlementBreakdownType.MANUAL_BY_AMOUNT  -> R.drawable.ic_breakdown_manual_amount
            SettlementBreakdownType.MANUAL_BY_COSTS   -> R.drawable.ic_breakdown_manual_costs
            SettlementBreakdownType.AUTO_PREPAYMENT   -> R.drawable.ic_breakdown_auto_prepayment
            SettlementBreakdownType.AUTO_CROSS_SETTLE -> R.drawable.ic_breakdown_auto_cross
            SettlementBreakdownType.UNSETTLED         -> R.drawable.ic_breakdown_unsettled
        }
    }

    /**
     * Content description dla accessibility
     */
    private fun getBreakdownContentDescription(type: SettlementBreakdownType): String {
        return when (type) {
            SettlementBreakdownType.SELF              -> getString(R.string.breakdown_self)
            SettlementBreakdownType.MANUAL_BY_AMOUNT  -> getString(R.string.breakdown_manual_amount)
            SettlementBreakdownType.MANUAL_BY_COSTS   -> getString(R.string.breakdown_manual_costs)
            SettlementBreakdownType.AUTO_PREPAYMENT   -> getString(R.string.breakdown_auto_prepayment)
            SettlementBreakdownType.AUTO_CROSS_SETTLE -> getString(R.string.breakdown_auto_cross)
            SettlementBreakdownType.UNSETTLED         -> getString(R.string.breakdown_unsettled)
        }
    }

    // ==========================================
    // TAB 3: ZALICZKI — 1:1
    // ==========================================

    private fun populatePrepaymentTab(model: SettlementDetailsUiModel) {
        prepaymentAmountLeftContainer.removeAllViews()
        prepaymentHistoryContainer.removeAllViews()

        if (!model.hasPrepaymentData) {
            prepaymentEmptyState.visibility = View.VISIBLE
            prepaymentAmountLeftLabel.visibility = View.GONE
            prepaymentAmountLeftContainer.visibility = View.GONE
            prepaymentSeparator.visibility = View.GONE
            prepaymentHistoryLabel.visibility = View.GONE
            prepaymentHistoryContainer.visibility = View.GONE
            return
        }

        prepaymentEmptyState.visibility = View.GONE
        prepaymentAmountLeftLabel.visibility = View.VISIBLE
        prepaymentAmountLeftContainer.visibility = View.VISIBLE
        prepaymentSeparator.visibility = View.VISIBLE
        prepaymentHistoryLabel.visibility = View.VISIBLE
        prepaymentHistoryContainer.visibility = View.VISIBLE

        model.prepaymentAmountLeftRows.forEach { row ->
            prepaymentAmountLeftContainer.addView(createPrepaymentAmountRow(row))
        }
        model.prepaymentHistoryRows.forEach { row ->
            prepaymentHistoryContainer.addView(createPrepaymentHistoryRow(row))
        }
    }

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

    private fun createPrepaymentHistoryRow(row: PrepaymentHistoryRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_prepayment_history_row, prepaymentHistoryContainer, false)

        view.findViewById<TextView>(R.id.historyAmount).apply {
            text = row.formattedAmount
            val colorRes = when (row.direction) {
                PrepaymentAmountDirection.TO_ME -> R.color.success
                PrepaymentAmountDirection.FROM_ME -> R.color.error
            }
            setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        }
        view.findViewById<TextView>(R.id.historyCurrency).apply {
            text = row.currency
            val colorRes = when (row.direction) {
                PrepaymentAmountDirection.TO_ME -> R.color.success
                PrepaymentAmountDirection.FROM_ME -> R.color.error
            }
            setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        }
        view.findViewById<TextView>(R.id.historyDate).text = row.formattedDate
        return view
    }

    companion object {
        fun newInstance(model: SettlementDetailsUiModel): SettlementDetailsModalFragment {
            return SettlementDetailsModalFragment().apply { detailsModel = model }
        }
    }
}