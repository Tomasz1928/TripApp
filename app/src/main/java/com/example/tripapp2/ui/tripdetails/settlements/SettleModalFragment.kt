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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R
import com.example.tripapp2.data.model.ExpenseDto
import com.example.tripapp2.data.model.TripDto
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettleModalFragment : DialogFragment() {

    private var settleModel: SettleModalUiModel? = null
    private var currentUserId: String = ""
    private var tripId: String = ""
    private var tripData: TripDto? = null
    private var onConfirm: ((SettleRequest) -> Unit)? = null
    private var onConfirmByCosts: ((SettleByCostsRequest) -> Unit)? = null

    private var selectedCurrencyOption: SettleCurrencyOption? = null

    private lateinit var closeButton: ImageView
    private lateinit var participantNickname: TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_TripApp_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.modal_settle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model = settleModel ?: run { dismiss(); return }

        initializeViews(view)
        setupTabLayout()
        populateData(model)
        setupCurrencySelection(model)
        setupCurrencyScrollViewMaxHeight(model)
        setupAmountInput(model)
        setupListeners(model)
        setupByCostsTab(model)
    }

    private fun setupCurrencyScrollViewMaxHeight(model: SettleModalUiModel) {
        val totalCurrencies = model.currencies.size
        if (totalCurrencies > 3) {
            val density = resources.displayMetrics.density
            val maxHeightPx = (60 * 3 * density).toInt()
            currencyScrollView.layoutParams = currencyScrollView.layoutParams.apply { height = maxHeightPx }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initializeViews(view: View) {
        closeButton = view.findViewById(R.id.closeButton)
        participantNickname = view.findViewById(R.id.participantNickname)
        tabLayout = view.findViewById(R.id.tabLayout)
        tabByValue = view.findViewById(R.id.tabByValue)
        tabByCosts = view.findViewById(R.id.tabByCosts)
        currencyScrollView = view.findViewById(R.id.currencyScrollView)
        currencyRadioGroup = view.findViewById(R.id.currencyRadioGroup)
        mainCurrencyCard = view.findViewById(R.id.mainCurrencyCard)
        mainCurrencyRadio = view.findViewById(R.id.mainCurrencyRadio)
        mainCurrencyLabel = view.findViewById(R.id.mainCurrencyLabel)
        mainCurrencyAmount = view.findViewById(R.id.mainCurrencyAmount)
        mainCurrencyDirectionIcon = view.findViewById(R.id.mainCurrencyDirectionIcon)
        otherCurrenciesContainer = view.findViewById(R.id.otherCurrenciesContainer)
        amountInputLayout = view.findViewById(R.id.amountInputLayout)
        amountInput = view.findViewById(R.id.amountInput)
        settleButton = view.findViewById(R.id.settleButton)
        costsScrollView = view.findViewById(R.id.costsScrollView)
        costsListContainer = view.findViewById(R.id.costsListContainer)
        costsSummaryContainer = view.findViewById(R.id.costsSummaryContainer)
        summaryCard = view.findViewById(R.id.summaryCard)
        summaryAmountsContainer = view.findViewById(R.id.summaryAmountsContainer)
        costsEmptySelection = view.findViewById(R.id.costsEmptySelection)
        settleByCostsButton = view.findViewById(R.id.settleByCostsButton)
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
        participantNickname.text = model.participantNickname
    }

    // ==========================================
    // CURRENCY SELECTION (Tab 1)
    // Wyswietlanie tekstow JAK W ORYGINALE:
    // mainCurrencyAmount -> "150,00" (sama kwota)
    // mainCurrencyLabel -> "PLN (glowna)"
    // directionIcon -> strzalka z kolorem
    // ==========================================

    private fun setupCurrencySelection(model: SettleModalUiModel) {
        val currencies = model.currencies
        if (currencies.isEmpty()) { settleButton.isEnabled = false; return }

        val mainOption = currencies.firstOrNull { it.isMainCurrency }

        if (mainOption != null) {
            mainCurrencyLabel.text = "${mainOption.currency} (g${"\u0142"}${"\u00F3"}wna)"
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
            val firstOption = currencies.firstOrNull()
            if (firstOption != null) { selectedCurrencyOption = firstOption; updateAmountUI(firstOption) }
        }

        val otherCurrencies = currencies.filter { !it.isMainCurrency }
        otherCurrenciesContainer.removeAllViews()
        radioToCurrencyMap.clear()
        otherCurrencies.forEach { addOtherCurrencyCard(it) }
    }

    private fun updateDirectionIcon(iconView: ImageView, direction: SettleAmountDirection) {
        when (direction) {
            SettleAmountDirection.TO_RECEIVE -> {
                iconView.setImageResource(R.drawable.ic_arrow_downward)
                iconView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
            }
            SettleAmountDirection.TO_GIVE -> {
                iconView.setImageResource(R.drawable.ic_arrow_upward)
                iconView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
            }
        }
    }

    private fun selectMainCurrency(mainOption: SettleCurrencyOption) {
        mainCurrencyRadio.isChecked = true
        selectedCurrencyOption = mainOption
        updateMainCurrencyCardState(true)
        updateAmountUI(mainOption)
        radioToCurrencyMap.keys.forEach { radioId -> view?.findViewById<RadioButton>(radioId)?.isChecked = false }
        updateOtherCurrencyCardsState(null)
    }

    private fun addOtherCurrencyCard(currencyOption: SettleCurrencyOption) {
        val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_currency_option, otherCurrenciesContainer, false)
        val radio = cardView.findViewById<RadioButton>(R.id.currencyRadio)
        val amountText = cardView.findViewById<TextView>(R.id.currencyAmount)
        val labelText = cardView.findViewById<TextView>(R.id.currencyLabel)
        val directionIcon = cardView.findViewById<ImageView>(R.id.currencyDirectionIcon)
        val card = cardView.findViewById<MaterialCardView>(R.id.currencyCard)

        radio.id = View.generateViewId()
        amountText.text = "%.2f".format(currencyOption.availableAmount)
        labelText.text = currencyOption.currency
        updateDirectionIcon(directionIcon, currencyOption.direction)

        radioToCurrencyMap[radio.id] = currencyOption

        val selectThisCurrency = {
            mainCurrencyRadio.isChecked = false
            radioToCurrencyMap.keys.forEach { otherRadioId ->
                if (otherRadioId != radio.id) view?.findViewById<RadioButton>(otherRadioId)?.isChecked = false
            }
            radio.isChecked = true
            selectedCurrencyOption = currencyOption
            updateMainCurrencyCardState(false)
            updateOtherCurrencyCardsState(radio.id)
            updateAmountUI(currencyOption)
        }

        card.setOnClickListener { selectThisCurrency() }
        radio.setOnClickListener { selectThisCurrency() }
        otherCurrenciesContainer.addView(cardView)
    }

    private fun updateMainCurrencyCardState(isSelected: Boolean) {
        mainCurrencyCard.strokeColor = ContextCompat.getColor(requireContext(), if (isSelected) R.color.primary else R.color.divider)
        mainCurrencyCard.strokeWidth = if (isSelected) 2 else 1
    }

    private fun updateOtherCurrencyCardsState(selectedRadioId: Int?) {
        for ((radioId, _) in radioToCurrencyMap) {
            val card = view?.findViewById<RadioButton>(radioId)?.parent?.parent as? MaterialCardView ?: continue
            val isSelected = radioId == selectedRadioId
            card.strokeColor = ContextCompat.getColor(requireContext(), if (isSelected) R.color.primary else R.color.divider)
            card.strokeWidth = if (isSelected) 2 else 1
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
        closeButton.setOnClickListener { dismiss() }

        amountInputLayout.setEndIconOnClickListener {
            selectedCurrencyOption?.let { currency -> amountInput.setText("%.2f".format(currency.availableAmount)) }
        }

        settleButton.setOnClickListener { onSettleByValueClicked(model) }
    }

    /**
     * Walidacja i wyslanie rozliczenia per wartosc
     *
     * Walidacja:
     * 1. Pole nie puste
     * 2. Parsowalna liczba (obsluga przecinka i kropki)
     * 3. Kwota > 0
     * 4. Waluta wybrana
     * 5. Kwota <= availableAmount (+0.01 margines na zaokraglenia)
     */
    private fun onSettleByValueClicked(model: SettleModalUiModel) {
        val amountText = amountInput.text?.toString()?.trim()

        if (amountText.isNullOrBlank()) {
            amountInputLayout.error = getString(R.string.settle_error_invalid_amount)
            return
        }

        val amount = amountText.replace(",", ".").toFloatOrNull()

        if (amount == null) {
            amountInputLayout.error = getString(R.string.settle_error_invalid_amount)
            return
        }

        if (amount <= 0) {
            amountInputLayout.error = getString(R.string.settle_error_amount_too_low)
            return
        }

        val currency = selectedCurrencyOption
        if (currency == null) {
            amountInputLayout.error = getString(R.string.settle_error_invalid_amount)
            return
        }

        if (amount > currency.availableAmount + 0.01f) {
            amountInputLayout.error = getString(R.string.settle_error_amount_too_high, "%.2f".format(currency.availableAmount))
            return
        }

        val finalAmount = minOf(amount, currency.availableAmount)

        val (fromUserId, toUserId) = when (currency.direction) {
            SettleAmountDirection.TO_RECEIVE -> model.participantId to currentUserId
            SettleAmountDirection.TO_GIVE -> currentUserId to model.participantId
        }

        val request = SettleRequest(
            tripId = tripId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = finalAmount,
            currency = currency.currency,
            isMainCurrency = currency.isMainCurrency,
            direction = currency.direction
        )

        onConfirm?.invoke(request)
        dismiss()
    }

    // ==========================================
    // TAB 2: PER KOSZTY
    // ==========================================

    private fun setupByCostsTab(model: SettleModalUiModel) {
        val trip = tripData ?: return
        val participantId = model.participantId

        costItems.clear()
        costItems.addAll(filterExpensesForSettlement(trip.expenses, participantId))
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

    private fun filterExpensesForSettlement(expenses: List<ExpenseDto>, participantId: String): List<SettleCostItemUiModel> {
        val result = mutableListOf<SettleCostItemUiModel>()

        for (expense in expenses) {
            if (expense.payerId == currentUserId) {
                val share = expense.sharedWith.find { it.participantId == participantId && !it.isSettlement }
                if (share != null) {
                    result.add(SettleCostItemUiModel(
                        expenseId = expense.id, expenseName = expense.name,
                        amount = share.splitValue.valueMainCurrency, currency = expense.currency,
                        formattedAmount = "%.2f %s".format(share.splitValue.valueMainCurrency, expense.currency),
                        payerDirection = CostPayerDirection.I_PAID, payerId = currentUserId, participantId = participantId
                    ))
                }
            }

            if (expense.payerId == participantId) {
                val share = expense.sharedWith.find { it.participantId == currentUserId && !it.isSettlement }
                if (share != null) {
                    result.add(SettleCostItemUiModel(
                        expenseId = expense.id, expenseName = expense.name,
                        amount = share.splitValue.valueMainCurrency, currency = expense.currency,
                        formattedAmount = "%.2f %s".format(share.splitValue.valueMainCurrency, expense.currency),
                        payerDirection = CostPayerDirection.PARTICIPANT_PAID, payerId = participantId, participantId = currentUserId
                    ))
                }
            }
        }
        return result
    }

    private fun createCostRow(item: SettleCostItemUiModel): View {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_settle_cost, costsListContainer, false)
        val checkbox = view.findViewById<CheckBox>(R.id.costCheckbox)
        val title = view.findViewById<TextView>(R.id.costTitle)
        val amount = view.findViewById<TextView>(R.id.costAmount)

        title.text = item.expenseName
        amount.text = item.formattedAmount

        val color = when (item.payerDirection) {
            CostPayerDirection.I_PAID -> ContextCompat.getColor(requireContext(), R.color.success)
            CostPayerDirection.PARTICIPANT_PAID -> ContextCompat.getColor(requireContext(), R.color.error)
        }
        amount.setTextColor(color)

        title.setOnLongClickListener { Toast.makeText(requireContext(), item.expenseName, Toast.LENGTH_SHORT).show(); true }
        checkbox.setOnCheckedChangeListener { _, isChecked -> item.isChecked = isChecked; updateCostsSummary() }
        view.setOnClickListener { checkbox.isChecked = !checkbox.isChecked }
        return view
    }

    private fun updateCostsSummary() {
        val checkedItems = costItems.filter { it.isChecked }
        if (checkedItems.isEmpty()) {
            summaryCard.visibility = View.GONE
            costsEmptySelection.text = getString(R.string.settle_by_costs_empty_selection)
            costsEmptySelection.visibility = View.VISIBLE
            settleByCostsButton.isEnabled = false
            return
        }

        costsEmptySelection.visibility = View.GONE
        summaryCard.visibility = View.VISIBLE

        val netPerCurrency = mutableMapOf<String, Float>()
        for (item in checkedItems) {
            val current = netPerCurrency.getOrDefault(item.currency, 0f)
            val delta = when (item.payerDirection) {
                CostPayerDirection.I_PAID -> item.amount
                CostPayerDirection.PARTICIPANT_PAID -> -item.amount
            }
            netPerCurrency[item.currency] = current + delta
        }

        settleByCostsButton.isEnabled = true
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

        val request = SettleByCostsRequest(
            tripId = tripId,
            items = checkedItems.map { item -> SettleByCostsItem(expenseId = item.expenseId, participantId = item.participantId, payerId = item.payerId) }
        )
        onConfirmByCosts?.invoke(request)
        dismiss()
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