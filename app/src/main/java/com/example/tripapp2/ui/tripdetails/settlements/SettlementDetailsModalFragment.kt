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
import com.example.tripapp2.data.model.SettlementHistoryEventType
import android.app.AlertDialog

/**
 * Modal szczegółów rozliczenia z uczestnikiem.
 * ZMIGOWANY na BaseModalFragment.
 *
 * Body = modal_settlement_details_body.xml (TabLayout + 4 taby z ikonkami)
 *
 * Tab 1: Ogólne (podsumowanie kwot)
 * Tab 2: Wydatki (koszty z breakdown icons) — ZMIENIONE: dual-currency
 * Tab 3: Zaliczki (amountLeft + historia)
 * Tab 4: Historia rozliczeń (expandable + filtry)
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

    // Tab 4: Historia — views
    private lateinit var tabHistory: LinearLayout
    private lateinit var historyListContainer: LinearLayout
    private lateinit var historyEmptyState: TextView
    private lateinit var historyFilteredEmptyState: TextView

    // Tab 4: Filtry — type icons
    private lateinit var filterManualAmount: ImageView
    private lateinit var filterManualCosts: ImageView
    private lateinit var filterAutoPrepayment: ImageView
    private lateinit var filterAutoCross: ImageView
    private lateinit var filterNoExpenses: ImageView
    private lateinit var filterExpenseDropdownBtn: TextView

    // Tab 4: Filtry — stan
    private val activeTypeFilters = mutableSetOf(
        SettlementHistoryEventType.MANUAL_BY_AMOUNT,
        SettlementHistoryEventType.MANUAL_BY_COSTS,
        SettlementHistoryEventType.MANUAL_BY_PREPAYMENT,
        SettlementHistoryEventType.AUTO_PREPAYMENT,
        SettlementHistoryEventType.AUTO_CROSS_SETTLE
    )
    private var showNoExpenseEntries = true
    private val selectedExpenseNames = mutableSetOf<String>()
    private var allExpenseNames = listOf<String>()
    private var allHistoryRows = listOf<SettlementHistoryRow>()

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View? {
        return inflater.inflate(R.layout.modal_settlement_details_body, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model = detailsModel ?: run { dismiss(); return }

        setModalTitle(getString(R.string.settlement_details_title))
        setModalSubtitle(model.participantNickname)

        initializeViews()
        setupTabLayout()

        populateSummaryTab(model)
        populateCostsTab(model)
        populatePrepaymentTab(model)
        populateHistoryTab(model)
    }

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

        tabHistory = body.findViewById(R.id.tabHistory)
        historyListContainer = body.findViewById(R.id.historyListContainer)
        historyEmptyState = body.findViewById(R.id.historyEmptyState)
        historyFilteredEmptyState = body.findViewById(R.id.historyFilteredEmptyState)

        filterManualAmount = body.findViewById(R.id.filterManualAmount)
        filterManualCosts = body.findViewById(R.id.filterManualCosts)
        filterAutoPrepayment = body.findViewById(R.id.filterAutoPrepayment)
        filterAutoCross = body.findViewById(R.id.filterAutoCross)
        filterNoExpenses = body.findViewById(R.id.filterNoExpenses)
        filterExpenseDropdownBtn = body.findViewById(R.id.filterExpenseDropdownBtn)
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tabSummary.visibility = View.GONE
                tabCosts.visibility = View.GONE
                tabPrepayment.visibility = View.GONE
                tabHistory.visibility = View.GONE

                when (tab?.position) {
                    0 -> tabSummary.visibility = View.VISIBLE
                    1 -> tabCosts.visibility = View.VISIBLE
                    2 -> tabPrepayment.visibility = View.VISIBLE
                    3 -> tabHistory.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ==========================================
    // TAB 1: PODSUMOWANIE
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
    // TAB 2: KOSZTY — ZMIENIONE: dual-currency + breakdown icons
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
     * ZMIENIONE: dual-currency w wierszu kosztu
     *
     * Logika wyświetlania kwoty:
     * - isMultiCurrency = true:
     *     → costAmount (niebieskie/primary): formattedAmountCostCurrency (waluta kosztu)
     *     → costAmountSecondary (pomarańczowe/secondary): formattedAmountTripCurrency (waluta tripu)
     * - isMultiCurrency = false:
     *     → costAmount: formattedAmountTripCurrency (jedyna waluta)
     *     → costAmountSecondary: ukryte
     */
    private fun createCostRow(costRow: SettlementDetailCostRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_settlement_detail_cost, costsListContainer, false)

        view.findViewById<TextView>(R.id.costTitle).text = costRow.expenseName

        val costAmount = view.findViewById<TextView>(R.id.costAmount)
        val costAmountSecondary = view.findViewById<TextView>(R.id.costAmountSecondary)

        val amountColorRes = if (costRow.isAmountPositive) R.color.success else R.color.error

        if (costRow.isMultiCurrency) {
            // Multi-currency: niebiesko = waluta kosztu, pomarańczowo = waluta tripu
            costAmount.text = costRow.formattedAmountCostCurrency
            costAmount.setTextColor(ContextCompat.getColor(requireContext(), amountColorRes))

            costAmountSecondary.text = costRow.formattedAmountTripCurrency
            costAmountSecondary.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            costAmountSecondary.visibility = View.VISIBLE
        } else {
            // Single-currency: niebiesko = waluta tripu (jedyna)
            costAmount.text = costRow.formattedAmountTripCurrency
            costAmount.setTextColor(ContextCompat.getColor(requireContext(), amountColorRes))

            costAmountSecondary.visibility = View.GONE
        }

        // Breakdown icons
        val mainIcon = view.findViewById<ImageView>(R.id.settlementMainIcon)
        mainIcon.setImageResource(getBreakdownIconRes(costRow.dominantType))
        mainIcon.contentDescription = getBreakdownContentDescription(costRow.dominantType)
        mainIcon.clearColorFilter()

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
    // TAB 3: ZALICZKI
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
    // TAB 4: HISTORIA ROZLICZEŃ
    // ==========================================

    private fun populateHistoryTab(model: SettlementDetailsUiModel) {
        allHistoryRows = model.settlementHistoryRows
        historyListContainer.removeAllViews()

        if (!model.hasSettlementHistory) {
            historyEmptyState.visibility = View.VISIBLE
            return
        }

        historyEmptyState.visibility = View.GONE

        // Zbierz wszystkie unikalne nazwy kosztów do filtrów
        allExpenseNames = allHistoryRows
            .mapNotNull { it.relatedExpenses }
            .flatMap { it.split(", ") }
            .distinct()
            .sorted()

        selectedExpenseNames.addAll(allExpenseNames)

        setupHistoryFilters()
        applyHistoryFilters()
    }

    private fun setupHistoryFilters() {
        // Type filter icons
        setupTypeFilterIcon(filterManualAmount, SettlementHistoryEventType.MANUAL_BY_AMOUNT)
        setupTypeFilterIcon(filterManualCosts, SettlementHistoryEventType.MANUAL_BY_COSTS)
        setupTypeFilterIcon(filterAutoPrepayment, SettlementHistoryEventType.AUTO_PREPAYMENT)
        setupTypeFilterIcon(filterAutoCross, SettlementHistoryEventType.AUTO_CROSS_SETTLE)

        filterNoExpenses.apply {
            alpha = if (showNoExpenseEntries) 1.0f else 0.3f
            setOnClickListener {
                showNoExpenseEntries = !showNoExpenseEntries
                alpha = if (showNoExpenseEntries) 1.0f else 0.3f
                applyHistoryFilters()
            }
        }

        filterExpenseDropdownBtn.setOnClickListener {
            showExpenseFilterDialog()
        }

        updateExpenseFilterButtonText()
    }

    private fun setupTypeFilterIcon(icon: ImageView, type: SettlementHistoryEventType) {
        icon.alpha = if (activeTypeFilters.contains(type)) 1.0f else 0.3f
        icon.setOnClickListener {
            if (activeTypeFilters.contains(type)) {
                activeTypeFilters.remove(type)
                icon.alpha = 0.3f
            } else {
                activeTypeFilters.add(type)
                icon.alpha = 1.0f
            }
            applyHistoryFilters()
        }
    }

    private fun showExpenseFilterDialog() {
        val items = allExpenseNames.toTypedArray()
        val checked = BooleanArray(items.size) { selectedExpenseNames.contains(items[it]) }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settlement_history_filter_expenses_title))
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                if (isChecked) selectedExpenseNames.add(items[which])
                else selectedExpenseNames.remove(items[which])
            }
            .setPositiveButton("OK") { _, _ ->
                updateExpenseFilterButtonText()
                applyHistoryFilters()
            }
            .setNeutralButton(getString(R.string.settlement_history_filter_select_all)) { _, _ ->
                selectedExpenseNames.addAll(allExpenseNames)
                updateExpenseFilterButtonText()
                applyHistoryFilters()
            }
            .setNegativeButton(getString(R.string.settlement_history_filter_select_none)) { _, _ ->
                selectedExpenseNames.clear()
                updateExpenseFilterButtonText()
                applyHistoryFilters()
            }
            .show()
    }

    private fun updateExpenseFilterButtonText() {
        filterExpenseDropdownBtn.text = when {
            selectedExpenseNames.size == allExpenseNames.size ->
                getString(R.string.settlement_history_filter_all_expenses)
            selectedExpenseNames.isEmpty() ->
                getString(R.string.settlement_history_filter_no_expenses_selected)
            else ->
                "${selectedExpenseNames.size}/${allExpenseNames.size}"
        }
    }

    private fun applyHistoryFilters() {
        val filtered = allHistoryRows.filter { row ->
            // Filtr typu
            val typeMatch = when (row.eventType) {
                SettlementHistoryEventType.MANUAL_BY_PREPAYMENT ->
                    activeTypeFilters.contains(SettlementHistoryEventType.AUTO_PREPAYMENT)
                else -> activeTypeFilters.contains(row.eventType)
            }
            if (!typeMatch) return@filter false

            // Filtr kosztów
            if (row.relatedExpenses == null) {
                return@filter showNoExpenseEntries
            }

            val rowExpenses = row.relatedExpenses.split(", ")
            rowExpenses.any { selectedExpenseNames.contains(it) }
        }

        historyListContainer.removeAllViews()

        if (filtered.isEmpty()) {
            historyFilteredEmptyState.visibility = View.VISIBLE
        } else {
            historyFilteredEmptyState.visibility = View.GONE
            filtered.forEach { row ->
                historyListContainer.addView(createHistoryRow(row))
            }
        }
    }

    private fun createHistoryRow(row: SettlementHistoryRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_settlement_history_entry, historyListContainer, false)

        // Ikona typu
        val icon = view.findViewById<ImageView>(R.id.historyEntryIcon)
        icon.setImageResource(getHistoryEventIconRes(row.eventType))
        icon.clearColorFilter()

        // Kwota
        val amountView = view.findViewById<TextView>(R.id.historyEntryAmount)
        amountView.text = row.formattedAmount
        val colorRes = if (row.isPositive) R.color.success else R.color.error
        amountView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        // Data
        view.findViewById<TextView>(R.id.historyEntryDate).text = row.formattedDate
        view.findViewById<TextView>(R.id.historyEntryTime).text = row.formattedTime

        // Expandable content
        val chevron = view.findViewById<ImageView>(R.id.historyEntryChevron)
        val expandableContainer = view.findViewById<LinearLayout>(R.id.historyEntryExpandable)

        if (row.hasExpandableContent) {
            chevron.visibility = View.VISIBLE
            expandableContainer.visibility = View.GONE

            if (row.actorNickname != null) {
                val actorRow = view.findViewById<LinearLayout>(R.id.historyEntryActorRow)
                actorRow.visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.historyEntryActorValue).text = row.actorNickname
            }

            if (row.formattedTripAmount != null) {
                val tripAmountRow = view.findViewById<LinearLayout>(R.id.historyEntryTripAmountRow)
                tripAmountRow.visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.historyEntryTripAmountValue).text = row.formattedTripAmount
            }

            if (row.relatedExpenses != null) {
                val expensesRow = view.findViewById<LinearLayout>(R.id.historyEntryExpensesRow)
                expensesRow.visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.historyEntryExpensesValue).text = row.relatedExpenses
            }

            val rootView = view.findViewById<LinearLayout>(R.id.historyEntryRoot)
            rootView.setOnClickListener {
                val isExpanded = expandableContainer.visibility == View.VISIBLE
                if (isExpanded) {
                    expandableContainer.visibility = View.GONE
                    chevron.animate().rotation(0f).setDuration(200).start()
                } else {
                    expandableContainer.visibility = View.VISIBLE
                    chevron.animate().rotation(180f).setDuration(200).start()
                }
            }
        } else {
            chevron.visibility = View.GONE
        }

        return view
    }

    /**
     * Mapuje SettlementHistoryEventType na istniejące drawable breakdown.
     * MANUAL_BY_PREPAYMENT → ic_breakdown_auto_prepayment (ta sama ikonka co AUTO_PREPAYMENT)
     */
    private fun getHistoryEventIconRes(type: SettlementHistoryEventType): Int {
        return when (type) {
            SettlementHistoryEventType.MANUAL_BY_AMOUNT     -> R.drawable.ic_breakdown_manual_amount
            SettlementHistoryEventType.MANUAL_BY_COSTS      -> R.drawable.ic_breakdown_manual_costs
            SettlementHistoryEventType.MANUAL_BY_PREPAYMENT -> R.drawable.ic_breakdown_auto_prepayment
            SettlementHistoryEventType.AUTO_PREPAYMENT      -> R.drawable.ic_breakdown_auto_prepayment
            SettlementHistoryEventType.AUTO_CROSS_SETTLE    -> R.drawable.ic_breakdown_auto_cross
        }
    }

    companion object {
        fun newInstance(model: SettlementDetailsUiModel): SettlementDetailsModalFragment {
            return SettlementDetailsModalFragment().apply { detailsModel = model }
        }
    }
}