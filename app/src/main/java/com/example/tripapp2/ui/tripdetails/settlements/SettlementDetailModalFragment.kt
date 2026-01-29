package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Modal ze szczegółami rozliczenia
 */
class SettlementDetailModalFragment : DialogFragment() {

    private var detail: SettlementDetailUiModel? = null
    private var onSettle: ((currency: String, amount: Float?) -> Unit)? = null

    private lateinit var mainCurrencyCard: MaterialCardView
    private lateinit var mainCurrencyRadio: RadioButton
    private lateinit var mainCurrencyAmount: TextView
    private lateinit var otherCurrenciesContainer: LinearLayout
    private lateinit var settleFullAmountCheckbox: CheckBox
    private lateinit var customAmountLayout: TextInputLayout
    private lateinit var customAmountInput: TextInputEditText
    private lateinit var confirmButton: Button

    private var selectedCurrency: String = ""
    private val currencyRadios = mutableMapOf<String, RadioButton>()

    companion object {
        fun newInstance(
            detail: SettlementDetailUiModel,
            onSettle: (currency: String, amount: Float?) -> Unit
        ): SettlementDetailModalFragment {
            return SettlementDetailModalFragment().apply {
                this.detail = detail
                this.onSettle = onSettle
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.modal_settlement_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settlementDetail = detail ?: return

        initializeViews(view)
        setupData(settlementDetail)
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun initializeViews(view: View) {
        mainCurrencyCard = view.findViewById(R.id.mainCurrencyCard)
        mainCurrencyRadio = view.findViewById(R.id.mainCurrencyRadio)
        mainCurrencyAmount = view.findViewById(R.id.mainCurrencyAmount)
        otherCurrenciesContainer = view.findViewById(R.id.otherCurrenciesContainer)
        settleFullAmountCheckbox = view.findViewById(R.id.settleFullAmountCheckbox)
        customAmountLayout = view.findViewById(R.id.customAmountLayout)
        customAmountInput = view.findViewById(R.id.customAmountInput)
        confirmButton = view.findViewById(R.id.confirmSettlementButton)
    }

    private fun setupData(detail: SettlementDetailUiModel) {
        // ✅ ZMIANA: Używamy getString() dla resource stringów
        // Title and description
        val relationDesc = if (detail.isCurrentUserDebtor) {
            getString(R.string.settlement_you_owe_user, detail.toUserName)
        } else {
            getString(R.string.settlement_user_owes_you, detail.fromUserName)
        }
        view?.findViewById<TextView>(R.id.relationDescription)?.text = relationDesc

        // Main currency
        selectedCurrency = detail.mainCurrency
        view?.findViewById<TextView>(R.id.mainCurrencyLabel)?.text = detail.mainCurrency
        mainCurrencyAmount.text = detail.formattedAmountMain
        mainCurrencyRadio.isChecked = true
        currencyRadios[detail.mainCurrency] = mainCurrencyRadio

        // Other currencies
        otherCurrenciesContainer.removeAllViews()

        if (detail.otherCurrencies.isEmpty()) {
            view?.findViewById<TextView>(R.id.otherCurrenciesLabel)?.visibility = View.GONE
        } else {
            detail.otherCurrencies.forEach { currency ->
                val currencyView = createCurrencyView(currency)
                otherCurrenciesContainer.addView(currencyView)
            }
        }
    }

    private fun createCurrencyView(currency: CurrencyAmountUiModel): View {
        val view = layoutInflater.inflate(
            R.layout.item_settlement_currency,
            otherCurrenciesContainer,
            false
        )

        val radio = view.findViewById<RadioButton>(R.id.currencyRadio)
        val label = view.findViewById<TextView>(R.id.currencyLabel)
        val amount = view.findViewById<TextView>(R.id.currencyAmount)

        label.text = currency.currency
        amount.text = currency.formattedAmount

        currencyRadios[currency.currency] = radio

        // Radio button listener
        radio.setOnClickListener {
            selectCurrency(currency.currency)
        }

        // Card click listener
        view.setOnClickListener {
            selectCurrency(currency.currency)
        }

        return view
    }

    private fun setupListeners() {
        // Close button
        view?.findViewById<ImageView>(R.id.closeButton)?.setOnClickListener {
            dismiss()
        }

        // Main currency selection
        mainCurrencyRadio.setOnClickListener {
            selectCurrency(detail?.mainCurrency ?: "")
        }

        mainCurrencyCard.setOnClickListener {
            selectCurrency(detail?.mainCurrency ?: "")
        }

        // Settle full amount checkbox
        settleFullAmountCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                customAmountLayout.visibility = View.GONE
                customAmountInput.text?.clear()
            } else {
                customAmountLayout.visibility = View.VISIBLE
            }
        }

        // Custom amount input
        customAmountInput.addTextChangedListener {
            // Możesz dodać walidację
        }

        // Confirm button
        confirmButton.setOnClickListener {
            handleConfirm()
        }
    }

    private fun selectCurrency(currency: String) {
        selectedCurrency = currency

        // Odznacz wszystkie radio buttony
        currencyRadios.values.forEach { it.isChecked = false }

        // Zaznacz wybrany
        currencyRadios[currency]?.isChecked = true

        // Highlight selected card
        if (currency == detail?.mainCurrency) {
            mainCurrencyCard.strokeColor = resources.getColor(R.color.primary, null)
            mainCurrencyCard.strokeWidth = 4
        } else {
            mainCurrencyCard.strokeColor = resources.getColor(R.color.divider, null)
            mainCurrencyCard.strokeWidth = 2
        }

        // Update other currency cards
        for (i in 0 until otherCurrenciesContainer.childCount) {
            val child = otherCurrenciesContainer.getChildAt(i) as? MaterialCardView
            child?.let {
                val currencyLabel = it.findViewById<TextView>(R.id.currencyLabel).text.toString()
                if (currencyLabel == currency) {
                    it.strokeColor = resources.getColor(R.color.primary, null)
                    it.strokeWidth = 4
                } else {
                    it.strokeColor = resources.getColor(R.color.divider, null)
                    it.strokeWidth = 2
                }
            }
        }
    }

    private fun handleConfirm() {
        val amount = if (settleFullAmountCheckbox.isChecked) {
            null  // Rozlicz całość
        } else {
            customAmountInput.text.toString().toFloatOrNull()
        }

        // ✅ ZMIANA: Używamy getString() zamiast .toString()
        // Walidacja
        if (!settleFullAmountCheckbox.isChecked && amount == null) {
            customAmountLayout.error = getString(R.string.error_amount_required)
            return
        }

        onSettle?.invoke(selectedCurrency, amount)
        dismiss()
    }
}