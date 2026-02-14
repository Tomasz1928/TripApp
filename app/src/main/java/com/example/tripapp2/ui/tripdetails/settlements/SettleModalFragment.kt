package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R
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
 * - Wybór waluty (główna + dodatkowe) przez RadioButtons
 * - Input kwoty z walidacją (min 0, max = dostępna kwota)
 * - Przycisk "Wszystko" do uzupełnienia max wartości
 * - Przycisk "Rozlicz" do potwierdzenia
 */
class SettleModalFragment : DialogFragment() {

    private var settleModel: SettleModalUiModel? = null
    private var currentUserId: String = ""
    private var tripId: String = ""
    private var onConfirm: ((SettleRequest) -> Unit)? = null

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
    private lateinit var otherCurrenciesContainer: LinearLayout
    private lateinit var amountInputLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var settleButton: MaterialButton

    // Mapa RadioButton -> CurrencyOption (dla dodatkowych walut)
    private val radioToCurrencyMap = mutableMapOf<Int, SettleCurrencyOption>()

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
    }

    /**
     * Ustawia maksymalną wysokość ScrollView dla walut
     * Max 3 waluty widoczne, powyżej - scroll
     */
    private fun setupCurrencyScrollViewMaxHeight(model: SettleModalUiModel) {
        val totalCurrencies = 1 + model.otherCurrencies.size  // główna + dodatkowe

        if (totalCurrencies > 3) {
            // Oblicz wysokość dla 3 elementów (około 56dp na element)
            val itemHeightDp = 56
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
        otherCurrenciesContainer = view.findViewById(R.id.otherCurrenciesContainer)
        amountInputLayout = view.findViewById(R.id.amountInputLayout)
        amountInput = view.findViewById(R.id.amountInput)
        settleButton = view.findViewById(R.id.settleButton)
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
        // Kolor opisu relacji
        val descColor = if (model.isOwedToMe) {
            R.color.success  // On mi jest winien = zielony
        } else {
            R.color.error    // Ja jestem winien = czerwony
        }
    }

    // ==========================================
    // CURRENCY SELECTION
    // ==========================================

    private fun setupCurrencySelection(model: SettleModalUiModel) {
        // Główna waluta
        mainCurrencyLabel.text = "${model.mainCurrency.currency} (główna)"
        mainCurrencyAmount.text = "Do rozliczenia: ${model.mainCurrency.formattedAmount}"

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

    private fun selectMainCurrency(model: SettleModalUiModel) {
        // Odznacz wszystkie inne radio buttons
        radioToCurrencyMap.keys.forEach { radioId ->
            view?.findViewById<RadioButton>(radioId)?.isChecked = false
        }
        // Odznacz karty dodatkowych walut
        updateOtherCurrencyCardsState(null)

        // Zaznacz główną walutę
        mainCurrencyRadio.isChecked = true
        selectedCurrencyOption = model.mainCurrency
        updateMainCurrencyCardState(true)
        updateAmountUI(model.mainCurrency)
        clearAmountInput()
    }

    private fun addOtherCurrencyCard(currencyOption: SettleCurrencyOption, model: SettleModalUiModel) {
        val inflater = LayoutInflater.from(requireContext())
        val cardView = inflater.inflate(R.layout.item_currency_option, otherCurrenciesContainer, false)

        // cardView IS the MaterialCardView (root of item_currency_option.xml)
        val card = cardView as MaterialCardView
        val radio = card.findViewById<RadioButton>(R.id.currencyRadio)
        val label = card.findViewById<TextView>(R.id.currencyLabel)
        val amount = card.findViewById<TextView>(R.id.currencyAmount)

        // Generuj unikalne ID dla RadioButton
        radio.id = View.generateViewId()
        radioToCurrencyMap[radio.id] = currencyOption

        label.text = currencyOption.currency
        amount.text = "Do rozliczenia: ${currencyOption.formattedAmount}"

        // Kliknięcie na kartę
        val selectThisCurrency: () -> Unit = {
            // Odznacz główną walutę
            mainCurrencyRadio.isChecked = false
            updateMainCurrencyCardState(false)

            // Odznacz inne dodatkowe waluty
            radioToCurrencyMap.keys.forEach { radioId ->
                if (radioId != radio.id) {
                    view?.findViewById<RadioButton>(radioId)?.isChecked = false
                }
            }
            updateOtherCurrencyCardsState(radio.id)

            // Zaznacz tę walutę
            radio.isChecked = true
            selectedCurrencyOption = currencyOption
            updateAmountUI(currencyOption)
            clearAmountInput()
        }

        card.setOnClickListener { selectThisCurrency() }
        radio.setOnClickListener { selectThisCurrency() }

        otherCurrenciesContainer.addView(cardView)
    }

    private fun updateMainCurrencyCardState(isSelected: Boolean) {
        val strokeColor = if (isSelected) R.color.primary else R.color.divider
        mainCurrencyCard.strokeColor = ContextCompat.getColor(requireContext(), strokeColor)
    }

    private fun updateOtherCurrencyCardsState(selectedRadioId: Int?) {
        for (i in 0 until otherCurrenciesContainer.childCount) {
            val childView = otherCurrenciesContainer.getChildAt(i)
            // Child IS the MaterialCardView (root of item_currency_option.xml)
            val card = childView as? MaterialCardView ?: continue
            val radio = card.findViewById<RadioButton>(R.id.currencyRadio) ?: continue

            val isSelected = radio.id == selectedRadioId
            val strokeColor = if (isSelected) R.color.primary else R.color.divider
            card.strokeColor = ContextCompat.getColor(requireContext(), strokeColor)
        }
    }

    // ==========================================
    // AMOUNT INPUT
    // ==========================================

    private fun setupAmountInput(model: SettleModalUiModel) {
        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateAmount()
            }
        })
    }

    private fun updateAmountUI(currencyOption: SettleCurrencyOption) {
        // Aktualizuj suffix w TextInputLayout
        amountInputLayout.suffixText = currencyOption.currency
    }

    private fun clearAmountInput() {
        amountInput.setText("")
        amountInputLayout.error = null
    }

    private fun validateAmount(): Boolean {
        val amountText = amountInput.text?.toString() ?: ""
        val currencyOption = selectedCurrencyOption ?: return false

        if (amountText.isEmpty()) {
            amountInputLayout.error = null
            return false
        }

        val amount = amountText.replace(",", ".").toFloatOrNull()

        return when {
            amount == null -> {
                amountInputLayout.error = "Nieprawidłowa kwota"
                false
            }
            amount <= 0 -> {
                amountInputLayout.error = "Kwota musi być większa od 0"
                false
            }
            amount > currencyOption.availableAmount + 0.01f -> {
                amountInputLayout.error = "Maksymalna kwota: %.2f %s".format(
                    currencyOption.availableAmount,
                    currencyOption.currency
                )
                false
            }
            else -> {
                amountInputLayout.error = null
                true
            }
        }
    }

    private fun getEnteredAmount(): Float? {
        val amountText = amountInput.text?.toString() ?: return null
        return amountText.replace(",", ".").toFloatOrNull()
    }

    // ==========================================
    // LISTENERS
    // ==========================================

    private fun setupListeners(model: SettleModalUiModel) {
        // Zamknij modal
        closeButton.setOnClickListener {
            dismiss()
        }

        // Ikonka "Wszystko" w inputie - wypełnij maksymalną kwotą
        amountInputLayout.setEndIconOnClickListener {
            selectedCurrencyOption?.let { currency ->
                amountInput.setText("%.2f".format(currency.availableAmount))
            }
        }

        // Przycisk "Rozlicz"
        settleButton.setOnClickListener {
            if (validateAmount()) {
                confirmSettle(model)
            }
        }
    }

    private fun confirmSettle(model: SettleModalUiModel) {
        val amount = getEnteredAmount() ?: return
        val currency = selectedCurrencyOption ?: return

        // Określ fromUserId i toUserId na podstawie relacji
        val (fromUserId, toUserId) = if (model.isOwedToMe) {
            // On mi jest winien → on jest dłużnikiem (from), ja wierzycielem (to)
            model.participantId to currentUserId
        } else {
            // Ja jestem winien → ja jestem dłużnikiem (from), on wierzycielem (to)
            currentUserId to model.participantId
        }

        val request = SettleRequest(
            tripId = tripId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = amount,
            currency = currency.currency,
            isMainCurrency = currency.isMainCurrency
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
         * @param onConfirm Callback wywoływany po potwierdzeniu rozliczenia
         */
        fun newInstance(
            model: SettleModalUiModel,
            tripId: String,
            currentUserId: String,
            onConfirm: (SettleRequest) -> Unit
        ): SettleModalFragment {
            return SettleModalFragment().apply {
                this.settleModel = model
                this.tripId = tripId
                this.currentUserId = currentUserId
                this.onConfirm = onConfirm
            }
        }
    }
}