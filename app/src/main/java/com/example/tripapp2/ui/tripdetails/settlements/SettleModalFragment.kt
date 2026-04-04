package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tripapp2.R
import com.example.tripapp2.data.model.ExpenseDto
import com.example.tripapp2.data.model.TripDto
import com.example.tripapp2.data.model.mainCurrencyAmount
import com.example.tripapp2.data.model.notMainCurrencyAmount
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Modal rozliczenia z uczestnikiem.
 * ZMIGOWANY na BaseModalFragment.
 *
 * Body = modal_settle_body.xml (TabLayout + 2 taby: Per wartość / Per koszty)
 *
 * ZMIENIONE: dual-currency w tab "Per koszty"
 *
 * FIX RADIO BUTTONÓW:
 * Radio buttony są wewnątrz MaterialCardView, więc RadioGroup ich NIE zarządza.
 * Selekcja jest zarządzana ręcznie.
 * KLUCZOWE: view?.findViewById() szuka w root (fragment_base_modal overlay)
 * i NIE znajduje dynamicznych radio. Trzeba szukać w modalBodyContainer.
 */
class SettleModalFragment : BaseModalFragment() {

    private var settleModel: SettleModalUiModel? = null
    private var currentUserId: String = ""
    private var tripId: String = ""
    private var tripData: TripDto? = null
    private var onConfirm: ((SettleRequest) -> Unit)? = null
    private var onConfirmByCosts: ((SettleByCostsRequest) -> Unit)? = null

    private var selectedCurrencyOption: SettleCurrencyOption? = null

    private lateinit var tabLayout: TabLayout
    private lateinit var tabByValue: ScrollView
    private lateinit var tabByCosts: LinearLayout
    private lateinit var currencyScrollView: ScrollView
    private lateinit var currencyRadioGroup: RadioGroup
    private lateinit var mainCurrencyCard: MaterialCardView
    private lateinit var mainCurrencyRadio: RadioButton
    private lateinit var mainCurrencyLabel: TextView
    private lateinit var mainCurrencyAmount: TextView
    private lateinit var mainCurrencyDirectionIcon: ImageView
    private lateinit var otherCurrenciesContainer: LinearLayout
    private lateinit var amountInputLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var settleButton: MaterialButton
    private lateinit var costsScrollView: ScrollView
    private lateinit var costsListContainer: LinearLayout
    private lateinit var costsSummaryContainer: LinearLayout
    private lateinit var summaryCard: MaterialCardView
    private lateinit var summaryAmountsContainer: LinearLayout
    private lateinit var costsEmptySelection: TextView
    private lateinit var settleByCostsButton: MaterialButton

    private val radioToCurrencyMap = mutableMapOf<Int, SettleCurrencyOption>()
    private val costItems = mutableListOf<SettleCostItemUiModel>()

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View? {
        return inflater.inflate(R.layout.modal_settle_body, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model = settleModel ?: run { dismissAnimated(); return }

        setModalTitle(getString(R.string.settle_modal_title))
        setModalSubtitle(model.participantNickname)

        initializeViews()
        setupTabLayout()
        populateData(model)
        setupCurrencySelection(model)
        setupCurrencyScrollViewMaxHeight(model)
        setupAmountInput(model)
        setupListeners(model)
        setupByCostsTab(model)
    }

    private fun initializeViews() {
        val body = modalBodyContainer ?: return
        tabLayout = body.findViewById(R.id.tabLayout)
        tabByValue = body.findViewById(R.id.tabByValue)
        tabByCosts = body.findViewById(R.id.tabByCosts)
        currencyScrollView = body.findViewById(R.id.currencyScrollView)
        currencyRadioGroup = body.findViewById(R.id.currencyRadioGroup)
        mainCurrencyCard = body.findViewById(R.id.mainCurrencyCard)
        mainCurrencyRadio = body.findViewById(R.id.mainCurrencyRadio)
        mainCurrencyLabel = body.findViewById(R.id.mainCurrencyLabel)
        mainCurrencyAmount = body.findViewById(R.id.mainCurrencyAmount)
        mainCurrencyDirectionIcon = body.findViewById(R.id.mainCurrencyDirectionIcon)
        otherCurrenciesContainer = body.findViewById(R.id.otherCurrenciesContainer)
        amountInputLayout = body.findViewById(R.id.amountInputLayout)
        amountInput = body.findViewById(R.id.amountInput)
        settleButton = body.findViewById(R.id.settleButton)
        costsScrollView = body.findViewById(R.id.costsScrollView)
        costsListContainer = body.findViewById(R.id.costsListContainer)
        costsSummaryContainer = body.findViewById(R.id.costsSummaryContainer)
        summaryCard = body.findViewById(R.id.summaryCard)
        summaryAmountsContainer = body.findViewById(R.id.summaryAmountsContainer)
        costsEmptySelection = body.findViewById(R.id.costsEmptySelection)
        settleByCostsButton = body.findViewById(R.id.settleByCostsButton)
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { tabByValue.visibility = View.VISIBLE; tabByCosts.visibility = View.GONE }
                    1 -> { tabByValue.visibility = View.GONE; tabByCosts.visibility = View.VISIBLE }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun populateData(model: SettleModalUiModel) {
        // participantNickname jest teraz w subtitle z BaseModal
    }

    private fun setupCurrencyScrollViewMaxHeight(model: SettleModalUiModel) {
        if (model.currencies.size > 3) {
            val maxHeightPx = (60 * 3 * resources.displayMetrics.density).toInt()
            currencyScrollView.layoutParams = currencyScrollView.layoutParams.apply { height = maxHeightPx }
        }
    }

    // ==========================================
    // CURRENCY SELECTION
    // ==========================================

    private fun setupCurrencySelection(model: SettleModalUiModel) {
        val currencies = model.currencies
        if (currencies.isEmpty()) { settleButton.isEnabled = false; return }

        val mainOption = currencies.firstOrNull { it.isMainCurrency }

        if (mainOption != null) {
            mainCurrencyLabel.text = "${mainOption.currency} (główna)"
            mainCurrencyAmount.text = "%.2f".format(mainOption.availableAmount)
            updateDirectionIcon(mainCurrencyDirectionIcon, mainOption.direction)

            selectedCurrencyOption = mainOption
            mainCurrencyRadio.isChecked = true
            updateMainCurrencyCardState(true)
            updateAmountUI(mainOption)

            mainCurrencyCard.setOnClickListener { selectMainCurrency(mainOption) }
            mainCurrencyRadio.setOnClickListener { selectMainCurrency(mainOption) }
        } else {
            mainCurrencyCard.visibility = View.GONE
            currencies.firstOrNull()?.let { selectedCurrencyOption = it; updateAmountUI(it) }
        }

        val otherCurrencies = currencies.filter { !it.isMainCurrency }
        otherCurrenciesContainer.removeAllViews()
        radioToCurrencyMap.clear()

        otherCurrencies.forEach { option ->
            val card = layoutInflater.inflate(R.layout.item_currency_option, otherCurrenciesContainer, false)
            val radio = card.findViewById<RadioButton>(R.id.currencyRadio)
            val label = card.findViewById<TextView>(R.id.currencyLabel)
            val amount = card.findViewById<TextView>(R.id.currencyAmount)
            val directionIcon = card.findViewById<ImageView>(R.id.currencyDirectionIcon)

            radio.id = View.generateViewId()
            label.text = option.currency
            amount.text = "%.2f".format(option.availableAmount)
            updateDirectionIcon(directionIcon, option.direction)

            radioToCurrencyMap[radio.id] = option

            val cardView = card as? MaterialCardView
            cardView?.setOnClickListener {
                radio.isChecked = true
                onCurrencyRadioSelected(radio.id)
            }
            radio.setOnClickListener { onCurrencyRadioSelected(radio.id) }

            otherCurrenciesContainer.addView(card)
        }
    }

    private fun selectMainCurrency(option: SettleCurrencyOption) {
        mainCurrencyRadio.isChecked = true
        selectedCurrencyOption = option
        updateMainCurrencyCardState(true)
        updateAmountUI(option)
        // FIX #1: modalBodyContainer zamiast view
        radioToCurrencyMap.keys.forEach { radioId ->
            modalBodyContainer?.findViewById<RadioButton>(radioId)?.isChecked = false
        }
        updateOtherCurrencyCardsState(-1)
    }

    private fun onCurrencyRadioSelected(selectedRadioId: Int) {
        val option = radioToCurrencyMap[selectedRadioId] ?: return
        selectedCurrencyOption = option
        mainCurrencyRadio.isChecked = false
        updateMainCurrencyCardState(false)
        // FIX #2: modalBodyContainer zamiast view
        radioToCurrencyMap.keys.forEach { radioId ->
            if (radioId != selectedRadioId) {
                modalBodyContainer?.findViewById<RadioButton>(radioId)?.isChecked = false
            }
        }
        updateOtherCurrencyCardsState(selectedRadioId)
        updateAmountUI(option)
    }

    private fun updateMainCurrencyCardState(isSelected: Boolean) {
        mainCurrencyCard.strokeColor = ContextCompat.getColor(requireContext(), if (isSelected) R.color.primary else R.color.divider)
        mainCurrencyCard.strokeWidth = if (isSelected) 2 else 1
    }

    private fun updateOtherCurrencyCardsState(selectedRadioId: Int) {
        for ((radioId, _) in radioToCurrencyMap) {
            // FIX #3: modalBodyContainer zamiast view
            val radioButton = modalBodyContainer?.findViewById<RadioButton>(radioId) ?: continue
            val card = radioButton.parent?.parent as? MaterialCardView ?: continue
            val isSelected = radioId == selectedRadioId
            card.strokeColor = ContextCompat.getColor(requireContext(), if (isSelected) R.color.primary else R.color.divider)
            card.strokeWidth = if (isSelected) 2 else 1
        }
    }

    private fun updateDirectionIcon(icon: ImageView, direction: SettleAmountDirection) {
        when (direction) {
            SettleAmountDirection.TO_RECEIVE -> {
                icon.setImageResource(R.drawable.ic_arrow_downward)
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
            }
            SettleAmountDirection.TO_GIVE -> {
                icon.setImageResource(R.drawable.ic_arrow_upward)
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
            }
        }
    }

    // ==========================================
    // AMOUNT INPUT
    // ==========================================

    private fun updateAmountUI(currencyOption: SettleCurrencyOption) {
        amountInput.setText("")
        amountInputLayout.error = null
        amountInputLayout.suffixText = currencyOption.currency

        val directionText = when (currencyOption.direction) {
            SettleAmountDirection.TO_RECEIVE -> getString(R.string.settle_to_receive, currencyOption.formattedAmount)
            SettleAmountDirection.TO_GIVE -> getString(R.string.settle_to_give, currencyOption.formattedAmount)
        }
        amountInputLayout.helperText = directionText
    }

    private fun setupAmountInput(model: SettleModalUiModel) {
        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { amountInputLayout.error = null }
        })
    }

    // ==========================================
    // LISTENERS
    // ==========================================

    private fun setupListeners(model: SettleModalUiModel) {
        amountInputLayout.setEndIconOnClickListener {
            selectedCurrencyOption?.let { currency -> amountInput.setText("%.2f".format(currency.availableAmount)) }
        }

        settleButton.setOnClickListener { onSettleByValueClicked(model) }
    }

    private fun onSettleByValueClicked(model: SettleModalUiModel) {
        val amountText = amountInput.text?.toString()?.trim()

        if (amountText.isNullOrBlank()) {
            amountInputLayout.error = getString(R.string.settle_error_invalid_amount); return
        }

        val amount = amountText.replace(",", ".").toFloatOrNull()
        if (amount == null) { amountInputLayout.error = getString(R.string.settle_error_invalid_amount); return }
        if (amount <= 0) { amountInputLayout.error = getString(R.string.settle_error_amount_too_low); return }

        val currency = selectedCurrencyOption
        if (currency == null) { amountInputLayout.error = getString(R.string.settle_error_invalid_amount); return }

        if (amount > currency.availableAmount + 0.01f) {
            amountInputLayout.error = getString(R.string.settle_error_amount_too_high, "%.2f".format(currency.availableAmount)); return
        }

        val finalAmount = minOf(amount, currency.availableAmount)
        val (fromUserId, toUserId) = when (currency.direction) {
            SettleAmountDirection.TO_RECEIVE -> model.participantId to currentUserId
            SettleAmountDirection.TO_GIVE -> currentUserId to model.participantId
        }

        onConfirm?.invoke(SettleRequest(tripId, fromUserId, toUserId, finalAmount, currency.currency, currency.isMainCurrency, currency.direction))
        dismissAnimated()
    }

    // ==========================================
    // TAB 2: PER KOSZTY — ZMIENIONE: dual-currency
    // ==========================================

    private fun setupByCostsTab(model: SettleModalUiModel) {
        val trip = tripData ?: return
        val participantId = model.participantId

        costItems.clear()
        costItems.addAll(filterExpensesForSettlement(trip.expenses, participantId, trip.currency))
        costsListContainer.removeAllViews()

        if (costItems.isEmpty()) {
            costsEmptySelection.text = getString(R.string.settle_by_costs_no_costs)
            costsEmptySelection.visibility = View.VISIBLE
            summaryCard.visibility = View.GONE
            settleByCostsButton.isEnabled = false
            return
        }

        costItems.forEach { item -> costsListContainer.addView(createCostRow(item)) }

        costsScrollView.post {
            val maxHeightPx = (200 * resources.displayMetrics.density).toInt()
            if (costsListContainer.height > maxHeightPx) {
                costsScrollView.layoutParams = costsScrollView.layoutParams.apply { height = maxHeightPx }
            }
        }

        settleByCostsButton.setOnClickListener { onSettleByCostsClicked(model) }
        updateCostsSummary()
    }

    /**
     * ZMIENIONE: dual-currency — poprawne mapowanie kwot
     *
     * leftForSettled zawiera List<SimpleMoneyValueDto>:
     *   isMainCurrency=true  → kwota w walucie tripu
     *   isMainCurrency=false → kwota w walucie kosztu (gdy inna niż trip)
     *
     * Logika:
     * - Gdy expense.currency == tripCurrency: amount = mainCurrencyAmount(), currency = tripCurrency
     * - Gdy expense.currency != tripCurrency: amount = notMainCurrencyAmount(), currency = expense.currency
     *   + dodatkowe amountTrip/formattedAmountTrip dla secondary display
     */
    private fun filterExpensesForSettlement(
        expenses: List<ExpenseDto>,
        participantId: String,
        tripCurrency: String
    ): List<SettleCostItemUiModel> {
        val result = mutableListOf<SettleCostItemUiModel>()
        for (expense in expenses) {
            val isMultiCurrency = expense.currency != tripCurrency

            if (expense.payerId == currentUserId) {
                val share = expense.sharedWith.find { it.participantId == participantId && !it.isSettlement }
                if (share != null) {
                    val amountTrip = share.leftForSettled.mainCurrencyAmount()
                    val amountCost = if (isMultiCurrency) share.leftForSettled.notMainCurrencyAmount() else null

                    result.add(SettleCostItemUiModel(
                        expenseId = expense.id,
                        expenseName = expense.name,
                        // Kwota główna: w walucie kosztu jeśli multi, w walucie tripu jeśli single
                        amount = if (isMultiCurrency) (amountCost ?: 0f) else amountTrip,
                        currency = if (isMultiCurrency) expense.currency else tripCurrency,
                        formattedAmount = if (isMultiCurrency) {
                            "%.2f %s".format(amountCost ?: 0f, expense.currency)
                        } else {
                            "%.2f %s".format(amountTrip, tripCurrency)
                        },
                        // Dual-currency: secondary (waluta tripu) gdy multi
                        amountTrip = if (isMultiCurrency) amountTrip else null,
                        formattedAmountTrip = if (isMultiCurrency) "%.2f %s".format(amountTrip, tripCurrency) else null,
                        isMultiCurrency = isMultiCurrency,
                        payerDirection = CostPayerDirection.I_PAID,
                        payerId = currentUserId,
                        participantId = participantId
                    ))
                }
            } else if (expense.payerId == participantId) {
                val share = expense.sharedWith.find { it.participantId == currentUserId && !it.isSettlement }
                if (share != null) {
                    val amountTrip = share.leftForSettled.mainCurrencyAmount()
                    val amountCost = if (isMultiCurrency) share.leftForSettled.notMainCurrencyAmount() else null

                    result.add(SettleCostItemUiModel(
                        expenseId = expense.id,
                        expenseName = expense.name,
                        amount = if (isMultiCurrency) (amountCost ?: 0f) else amountTrip,
                        currency = if (isMultiCurrency) expense.currency else tripCurrency,
                        formattedAmount = if (isMultiCurrency) {
                            "%.2f %s".format(amountCost ?: 0f, expense.currency)
                        } else {
                            "%.2f %s".format(amountTrip, tripCurrency)
                        },
                        amountTrip = if (isMultiCurrency) amountTrip else null,
                        formattedAmountTrip = if (isMultiCurrency) "%.2f %s".format(amountTrip, tripCurrency) else null,
                        isMultiCurrency = isMultiCurrency,
                        payerDirection = CostPayerDirection.PARTICIPANT_PAID,
                        payerId = participantId,
                        participantId = currentUserId
                    ))
                }
            }
        }
        return result
    }

    /**
     * ZMIENIONE: dual-currency w wierszu kosztu
     *
     * - isMultiCurrency = true:
     *     → costAmount (niebieskie/zielone/czerwone): formattedAmount (waluta kosztu)
     *     → costAmountSecondary (pomarańczowe): formattedAmountTrip (waluta tripu)
     * - isMultiCurrency = false:
     *     → costAmount: formattedAmount (waluta tripu = jedyna)
     *     → costAmountSecondary: ukryte
     */
    private fun createCostRow(item: SettleCostItemUiModel): View {
        val view = layoutInflater.inflate(R.layout.item_settle_cost, costsListContainer, false)
        val checkbox = view.findViewById<CheckBox>(R.id.costCheckbox)
        val nameView = view.findViewById<TextView>(R.id.costTitle)
        val amountView = view.findViewById<TextView>(R.id.costAmount)
        val amountSecondaryView = view.findViewById<TextView>(R.id.costAmountSecondary)

        nameView.text = item.expenseName
        amountView.text = item.formattedAmount

        val colorRes = if (item.payerId == currentUserId) R.color.success else R.color.error
        amountView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        // Secondary amount (waluta tripu, pomarańczowo) — tylko gdy multi-currency
        if (item.isMultiCurrency && item.formattedAmountTrip != null && amountSecondaryView != null) {
            amountSecondaryView.text = item.formattedAmountTrip
            amountSecondaryView.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            amountSecondaryView.visibility = View.VISIBLE
        } else {
            amountSecondaryView?.visibility = View.GONE
        }

        checkbox.isChecked = item.isChecked
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.isChecked = isChecked
            updateCostsSummary()
        }

        view.setOnClickListener { checkbox.isChecked = !checkbox.isChecked }
        return view
    }

    private fun updateCostsSummary() {
        val checkedItems = costItems.filter { it.isChecked }

        if (checkedItems.isEmpty()) {
            costsEmptySelection.visibility = View.VISIBLE
            summaryCard.visibility = View.GONE
            settleByCostsButton.isEnabled = false
            return
        }

        costsEmptySelection.visibility = View.GONE
        summaryCard.visibility = View.VISIBLE
        settleByCostsButton.isEnabled = true

        val netPerCurrency = mutableMapOf<String, Float>()
        checkedItems.forEach { item ->
            val sign = if (item.payerId == currentUserId) 1f else -1f
            netPerCurrency[item.currency] = (netPerCurrency[item.currency] ?: 0f) + (item.amount * sign)
        }

        summaryAmountsContainer.removeAllViews()
        netPerCurrency.forEach { (currency, netAmount) ->
            val absAmount = kotlin.math.abs(netAmount)
            val colorRes = when {
                netAmount > 0 -> R.color.success
                netAmount < 0 -> R.color.error
                else -> R.color.text_secondary
            }
            val amountText = TextView(requireContext()).apply {
                text = "%.2f %s".format(absAmount, currency)
                setTextColor(ContextCompat.getColor(requireContext(), colorRes))
                textSize = 14f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_tiny)
                }
            }
            summaryAmountsContainer.addView(amountText)
        }
    }

    private fun onSettleByCostsClicked(model: SettleModalUiModel) {
        val checkedItems = costItems.filter { it.isChecked }
        if (checkedItems.isEmpty()) return

        onConfirmByCosts?.invoke(SettleByCostsRequest(
            tripId = tripId,
            items = checkedItems.map { SettleByCostsItemInput(it.expenseId, it.participantId) }
        ))
        dismissAnimated()
    }

    companion object {
        fun newInstance(
            model: SettleModalUiModel, tripId: String, currentUserId: String,
            tripData: TripDto? = null, onConfirm: (SettleRequest) -> Unit,
            onConfirmByCosts: ((SettleByCostsRequest) -> Unit)? = null
        ): SettleModalFragment {
            return SettleModalFragment().apply {
                this.settleModel = model; this.tripId = tripId; this.currentUserId = currentUserId
                this.tripData = tripData; this.onConfirm = onConfirm; this.onConfirmByCosts = onConfirmByCosts
            }
        }
    }
}