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

/**
 * Modal rozliczenia
 *
 * Funkcjonalności:
 * - 2 karty: "Per wartość" i "Per koszty"
 * - Tab 1: Wybór waluty (główna + dodatkowe) przez RadioButtons + input kwoty
 * - Tab 2: Lista kosztów z checkboxami + podsumowanie + rozlicz
 */
class SettleModalFragment : DialogFragment() {

    private var settleModel: SettleModalUiModel? = null
    private var currentUserId: String = ""
    private var tripId: String = ""
    private var tripData: TripDto? = null
    private var onConfirm: ((SettleRequest) -> Unit)? = null
    private var onConfirmByCosts: ((SettleByCostsRequest) -> Unit)? = null

    // Aktualnie wybrana opcja waluty
    private var selectedCurrencyOption: SettleCurrencyOption? = null

    // Views - Header
    private lateinit var closeButton: ImageView
    private lateinit var participantNickname: TextView

    // Views - TabLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var tabByValue: ScrollView
    private lateinit var tabByCosts: LinearLayout

    // Views - Tab 1 (Per wartość)
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

    // Views - Tab 2 (Per koszty)
    private lateinit var costsScrollView: ScrollView
    private lateinit var costsListContainer: LinearLayout
    private lateinit var costsSummaryContainer: LinearLayout
    private lateinit var summaryCard: MaterialCardView
    private lateinit var summaryAmountsContainer: LinearLayout
    private lateinit var costsEmptySelection: TextView
    private lateinit var settleByCostsButton: MaterialButton

    // Mapa RadioButton -> CurrencyOption (dla dodatkowych walut)
    private val radioToCurrencyMap = mutableMapOf<Int, SettleCurrencyOption>()

    // Lista kosztów do rozliczenia (tab 2)
    private val costItems = mutableListOf<SettleCostItemUiModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_TripApp_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.modal_settle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val model = settleModel ?: run {
            dismiss()
            return
        }

        initializeViews(view)
        setupTabLayout()
        populateData(model)
        setupCurrencySelection(model)
        setupCurrencyScrollViewMaxHeight(model)
        setupAmountInput(model)
        setupListeners(model)
        setupByCostsTab(model)
    }

    /**
     * Ustawia maksymalną wysokość ScrollView dla walut
     * Max 3 waluty widoczne, powyżej - scroll
     */
    private fun setupCurrencyScrollViewMaxHeight(model: SettleModalUiModel) {
        val totalCurrencies = 1 + model.otherCurrencies.size

        if (totalCurrencies > 3) {
            val itemHeightDp = 60
            val maxHeightDp = itemHeightDp * 3
            val density = resources.displayMetrics.density
            val maxHeightPx = (maxHeightDp * density).toInt()

            currencyScrollView.layoutParams = currencyScrollView.layoutParams.apply {
                height = maxHeightPx
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ==========================================
    // INITIALIZATION
    // ==========================================

    private fun initializeViews(view: View) {
        // Header
        closeButton = view.findViewById(R.id.closeButton)
        participantNickname = view.findViewById(R.id.participantNickname)

        // TabLayout
        tabLayout = view.findViewById(R.id.tabLayout)
        tabByValue = view.findViewById(R.id.tabByValue)
        tabByCosts = view.findViewById(R.id.tabByCosts)

        // Tab 1 - Per wartość
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

        // Tab 2 - Per koszty
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
                    0 -> {
                        tabByValue.visibility = View.VISIBLE
                        tabByCosts.visibility = View.GONE
                    }
                    1 -> {
                        tabByValue.visibility = View.GONE
                        tabByCosts.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ==========================================
    // POPULATE DATA
    // ==========================================

    private fun populateData(model: SettleModalUiModel) {
        participantNickname.text = model.participantNickname
    }

    // ==========================================
    // TAB 2: PER KOSZTY
    // ==========================================

    /**
     * Inicjalizacja taba "Per koszty"
     *
     * Przepływ:
     * 1. Pobierz wydatki z tripData
     * 2. Filtruj: (ja płaciłem i participant w sharedWith) LUB (participant płacił i ja w sharedWith)
     * 3. Dodatkowy filtr: sharedWith[osoba].isSettlement == false
     * 4. Utwórz listę SettleCostItemUiModel
     * 5. Wyświetl wiersze z checkboxami
     */
    private fun setupByCostsTab(model: SettleModalUiModel) {
        val trip = tripData ?: return
        val participantId = model.participantId

        // Filtruj wydatki
        costItems.clear()
        costItems.addAll(filterExpensesForSettlement(trip.expenses, participantId))

        // Wyświetl listę
        costsListContainer.removeAllViews()

        if (costItems.isEmpty()) {
            // Brak kosztów do rozliczenia
            costsEmptySelection.text = getString(R.string.settle_by_costs_no_costs)
            costsEmptySelection.visibility = View.VISIBLE
            summaryCard.visibility = View.GONE
            settleByCostsButton.isEnabled = false
            return
        }

        costItems.forEach { item ->
            val row = createCostRow(item)
            costsListContainer.addView(row)
        }

        // Ustaw maksymalną wysokość scroll view (max 5 wierszy po ~40dp = 200dp)
        costsScrollView.post {
            val maxHeightDp = 200
            val density = resources.displayMetrics.density
            val maxHeightPx = (maxHeightDp * density).toInt()

            if (costsListContainer.height > maxHeightPx) {
                costsScrollView.layoutParams = costsScrollView.layoutParams.apply {
                    height = maxHeightPx
                }
            }
        }

        // Setup przycisku rozlicz
        settleByCostsButton.setOnClickListener {
            onSettleByCostsClicked(model)
        }

        // Odśwież podsumowanie (na start = puste)
        updateCostsSummary()
    }

    /**
     * Filtruje wydatki dla rozliczenia między mną a uczestnikiem
     *
     * Warunki:
     * - Ja płaciłem i participant jest w sharedWith z isSettlement == false
     * - LUB participant płacił i ja jestem w sharedWith z isSettlement == false
     */
    private fun filterExpensesForSettlement(
        expenses: List<ExpenseDto>,
        participantId: String
    ): List<SettleCostItemUiModel> {
        val result = mutableListOf<SettleCostItemUiModel>()

        for (expense in expenses) {
            // Przypadek 1: Ja płaciłem, participant jest w sharedWith
            if (expense.payerId == currentUserId) {
                val share = expense.sharedWith.find {
                    it.participantId == participantId && !it.isSettlement
                }
                if (share != null) {
                    result.add(
                        SettleCostItemUiModel(
                            expenseId = expense.id,
                            expenseName = expense.name,
                            amount = share.splitValue.valueMainCurrency,
                            currency = expense.currency,
                            formattedAmount = "%.2f %s".format(share.splitValue.valueMainCurrency, expense.currency),
                            payerDirection = CostPayerDirection.I_PAID,
                            payerId = currentUserId,
                            participantId = participantId
                        )
                    )
                }
            }

            // Przypadek 2: Participant płacił, ja jestem w sharedWith
            if (expense.payerId == participantId) {
                val share = expense.sharedWith.find {
                    it.participantId == currentUserId && !it.isSettlement
                }
                if (share != null) {
                    result.add(
                        SettleCostItemUiModel(
                            expenseId = expense.id,
                            expenseName = expense.name,
                            amount = share.splitValue.valueMainCurrency,
                            currency = expense.currency,
                            formattedAmount = "%.2f %s".format(share.splitValue.valueMainCurrency, expense.currency),
                            payerDirection = CostPayerDirection.PARTICIPANT_PAID,
                            payerId = participantId,
                            participantId = currentUserId
                        )
                    )
                }
            }
        }

        return result
    }

    /**
     * Tworzy wiersz kosztu z checkboxem
     */
    private fun createCostRow(item: SettleCostItemUiModel): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_settle_cost, costsListContainer, false)

        val checkbox = view.findViewById<CheckBox>(R.id.costCheckbox)
        val title = view.findViewById<TextView>(R.id.costTitle)
        val amount = view.findViewById<TextView>(R.id.costAmount)

        title.text = item.expenseName
        amount.text = item.formattedAmount

        // Kolor zależny od kierunku
        val color = when (item.payerDirection) {
            CostPayerDirection.I_PAID -> ContextCompat.getColor(requireContext(), R.color.success)
            CostPayerDirection.PARTICIPANT_PAID -> ContextCompat.getColor(requireContext(), R.color.error)
        }
        amount.setTextColor(color)

        // Long press na tytuł - pokaż pełną nazwę
        title.setOnLongClickListener {
            Toast.makeText(requireContext(), item.expenseName, Toast.LENGTH_SHORT).show()
            true
        }

        // Checkbox listener
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.isChecked = isChecked
            updateCostsSummary()
        }

        // Kliknięcie na cały wiersz toggleuje checkbox
        view.setOnClickListener {
            checkbox.isChecked = !checkbox.isChecked
        }

        return view
    }

    /**
     * Aktualizuje podsumowanie zaznaczonych kosztów
     *
     * Logika netto per waluta:
     * - I_PAID → dodaj kwotę (participant mi jest winien)
     * - PARTICIPANT_PAID → odejmij kwotę (ja jestem winien)
     * - Wynik > 0 → zielony (kolor success) → participant mi jest winien netto
     * - Wynik < 0 → czerwony (kolor error) → ja jestem winien netto
     * - Wynik == 0 → pomijamy walutę (rozliczone)
     *
     * Wyświetlamy tylko kwotę bezwzględną + walutę, kolor mówi o kierunku.
     */
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

        // Oblicz netto per waluta
        val netPerCurrency = mutableMapOf<String, Float>()

        for (item in checkedItems) {
            val current = netPerCurrency.getOrDefault(item.currency, 0f)
            val delta = when (item.payerDirection) {
                CostPayerDirection.I_PAID -> item.amount           // participant mi jest winien
                CostPayerDirection.PARTICIPANT_PAID -> -item.amount // ja jestem winien
            }
            netPerCurrency[item.currency] = current + delta
        }

        // Przycisk aktywny gdy cokolwiek zaznaczono
        settleByCostsButton.isEnabled = true

        // Wyświetl sumy
        summaryAmountsContainer.removeAllViews()

        netPerCurrency.forEach { (currency, netAmount) ->
            val absAmount = kotlin.math.abs(netAmount)
            val colorRes = when {
                netAmount > 0 -> R.color.success
                netAmount < 0 -> R.color.error
                else -> R.color.text_secondary  // Netto == 0 → szary
            }

            val amountText = TextView(requireContext()).apply {
                text = "%.2f %s".format(absAmount, currency)
                setTextColor(ContextCompat.getColor(requireContext(), colorRes))
                textSize = 14f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_tiny)
                }
            }

            summaryAmountsContainer.addView(amountText)
        }
    }

    /**
     * Obsługa kliknięcia "Rozlicz" w tabie per koszty
     */
    private fun onSettleByCostsClicked(model: SettleModalUiModel) {
        val checkedItems = costItems.filter { it.isChecked }

        if (checkedItems.isEmpty()) return

        val request = SettleByCostsRequest(
            tripId = tripId,
            items = checkedItems.map { item ->
                SettleByCostsItem(
                    expenseId = item.expenseId,
                    payerId = item.payerId,
                    participantId = item.participantId
                )
            }
        )

        onConfirmByCosts?.invoke(request)
        dismiss()
    }

    // ==========================================
    // CURRENCY SELECTION (Tab 1)
    // ==========================================

    private fun setupCurrencySelection(model: SettleModalUiModel) {
        // Główna waluta - ustaw wartości
        mainCurrencyLabel.text = "${model.mainCurrency.currency} (główna)"
        mainCurrencyAmount.text = "%.2f".format(model.mainCurrency.availableAmount)
        updateDirectionIcon(mainCurrencyDirectionIcon, model.mainCurrency.direction)

        // Ustaw główną walutę jako domyślną
        selectedCurrencyOption = model.mainCurrency
        mainCurrencyRadio.isChecked = true
        updateMainCurrencyCardState(true)
        updateAmountUI(model.mainCurrency)

        // Kliknięcie na kartę głównej waluty
        mainCurrencyCard.setOnClickListener {
            selectMainCurrency(model)
        }
        mainCurrencyRadio.setOnClickListener {
            selectMainCurrency(model)
        }

        // Dodatkowe waluty
        otherCurrenciesContainer.removeAllViews()
        radioToCurrencyMap.clear()

        model.otherCurrencies.forEach { currencyOption ->
            addOtherCurrencyCard(currencyOption, model)
        }
    }

    /**
     * Ustawia ikonę kierunku:
     * TO_RECEIVE: strzałka w dół (zielona) - mam otrzymać
     * TO_GIVE: strzałka w górę (czerwona) - mam oddać
     */
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

    private fun selectMainCurrency(model: SettleModalUiModel) {
        mainCurrencyRadio.isChecked = true
        selectedCurrencyOption = model.mainCurrency
        updateMainCurrencyCardState(true)
        updateAmountUI(model.mainCurrency)

        // Odznacz wszystkie inne radio buttony
        radioToCurrencyMap.keys.forEach { radioId ->
            view?.findViewById<RadioButton>(radioId)?.isChecked = false
        }
        updateOtherCurrencyCardsState(null)
    }

    private fun addOtherCurrencyCard(currencyOption: SettleCurrencyOption, model: SettleModalUiModel) {
        val cardView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_currency_option, otherCurrenciesContainer, false)

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
            // Odznacz główną
            mainCurrencyRadio.isChecked = false
            // Odznacz wszystkie inne dodatkowe
            radioToCurrencyMap.keys.forEach { otherRadioId ->
                if (otherRadioId != radio.id) {
                    view?.findViewById<RadioButton>(otherRadioId)?.isChecked = false
                }
            }
            // Zaznacz tę
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
        val strokeColor = if (isSelected) {
            ContextCompat.getColor(requireContext(), R.color.primary)
        } else {
            ContextCompat.getColor(requireContext(), R.color.divider)
        }
        mainCurrencyCard.strokeColor = strokeColor
        mainCurrencyCard.strokeWidth = if (isSelected) 2 else 1
    }

    private fun updateOtherCurrencyCardsState(selectedRadioId: Int?) {
        for ((radioId, _) in radioToCurrencyMap) {
            val card = view?.findViewById<RadioButton>(radioId)
                ?.parent?.parent as? MaterialCardView ?: continue

            val isSelected = radioId == selectedRadioId
            card.strokeColor = if (isSelected) {
                ContextCompat.getColor(requireContext(), R.color.primary)
            } else {
                ContextCompat.getColor(requireContext(), R.color.divider)
            }
            card.strokeWidth = if (isSelected) 2 else 1
        }
    }

    // ==========================================
    // AMOUNT INPUT (Tab 1)
    // ==========================================

    private fun updateAmountUI(currencyOption: SettleCurrencyOption) {
        amountInput.setText("")
        amountInputLayout.error = null
        amountInputLayout.suffixText = currencyOption.currency
    }

    private fun setupAmountInput(model: SettleModalUiModel) {
        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                amountInputLayout.error = null
            }
        })
    }

    // ==========================================
    // LISTENERS
    // ==========================================

    private fun setupListeners(model: SettleModalUiModel) {
        closeButton.setOnClickListener { dismiss() }

        // Ikonka "Wszystko" w inputie - wypełnij maksymalną kwotą
        amountInputLayout.setEndIconOnClickListener {
            selectedCurrencyOption?.let { currency ->
                amountInput.setText("%.2f".format(currency.availableAmount))
            }
        }

        settleButton.setOnClickListener {
            onSettleByValueClicked(model)
        }
    }

    private fun onSettleByValueClicked(model: SettleModalUiModel) {
        val amountText = amountInput.text?.toString()
        val amount = amountText?.toFloatOrNull()

        if (amount == null || amount <= 0) {
            amountInputLayout.error = getString(R.string.settle_error_invalid_amount)
            return
        }

        val currency = selectedCurrencyOption ?: return

        if (amount > currency.availableAmount) {
            amountInputLayout.error = getString(
                R.string.settle_error_amount_too_high,
                "%.2f".format(currency.availableAmount)
            )
            return
        }

        val (fromUserId, toUserId) = when (currency.direction) {
            SettleAmountDirection.TO_RECEIVE -> {
                model.participantId to currentUserId
            }
            SettleAmountDirection.TO_GIVE -> {
                currentUserId to model.participantId
            }
        }

        val request = SettleRequest(
            tripId = tripId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = amount,
            currency = currency.currency,
            isMainCurrency = currency.isMainCurrency,
            direction = currency.direction
        )

        onConfirm?.invoke(request)
        dismiss()
    }

    // ==========================================
    // COMPANION - Factory
    // ==========================================

    companion object {
        /**
         * Tworzy nową instancję modala rozliczenia
         *
         * @param model Dane do wyświetlenia w modalu
         * @param tripId ID wycieczki
         * @param currentUserId ID aktualnego użytkownika
         * @param tripData Dane wycieczki (potrzebne do tab 2 - lista wydatków)
         * @param onConfirm Callback rozliczenia per wartość
         * @param onConfirmByCosts Callback rozliczenia per koszty
         */
        fun newInstance(
            model: SettleModalUiModel,
            tripId: String,
            currentUserId: String,
            tripData: TripDto? = null,
            onConfirm: (SettleRequest) -> Unit,
            onConfirmByCosts: ((SettleByCostsRequest) -> Unit)? = null
        ): SettleModalFragment {
            return SettleModalFragment().apply {
                this.settleModel = model
                this.tripId = tripId
                this.currentUserId = currentUserId
                this.tripData = tripData
                this.onConfirm = onConfirm
                this.onConfirmByCosts = onConfirmByCosts
            }
        }
    }
}