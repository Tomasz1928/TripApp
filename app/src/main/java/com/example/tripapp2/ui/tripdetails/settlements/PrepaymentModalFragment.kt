package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Modal zaliczki (przedpłaty)
 * ZMIGOWANY na BaseModalFragment.
 *
 * Ten modal ma złożony własny layout (radio buttons, dropdown, karty kierunku)
 * więc zachowuje własny layout XML i override'uje onCreateView().
 * Z BaseModalFragment korzysta z: animacji, overlay click, back button.
 *
 * Logika biznesowa — BEZ ZMIAN.
 */
class PrepaymentModalFragment : BaseModalFragment() {

    private var prepaymentModel: PrepaymentUiModel? = null
    private var onConfirm: ((PrepaymentRequest) -> Unit)? = null

    private var selectedCurrency: String = ""
    private var selectedDirection: PrepaymentDirection = PrepaymentDirection.TO_ME

    // Views
    private lateinit var participantNickname: TextView
    private lateinit var currentBalance: TextView
    private lateinit var directionToMeCard: MaterialCardView
    private lateinit var directionFromMeCard: MaterialCardView
    private lateinit var directionToMeRadio: RadioButton
    private lateinit var directionFromMeRadio: RadioButton
    private lateinit var amountInputLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var currencyInputLayout: TextInputLayout
    private lateinit var currencyDropdown: AutoCompleteTextView
    private lateinit var confirmButton: MaterialButton

    /**
     * Override onCreateView — ten modal używa własnego layoutu zamiast fragment_base_modal.
     * BaseModalFragment zapewnia animacje i overlay przez onStart/onCreate.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Używamy własnego layoutu opakowanego w overlay z BaseModal
        val baseView = super.onCreateView(inflater, container, savedInstanceState)
        return baseView
    }

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View? {
        // Inflate oryginalny body z modal_prepayment, ale tylko zawartość ScrollView
        return inflater.inflate(R.layout.modal_prepayment_body, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val model = prepaymentModel ?: run { dismissAnimated(); return }

        setModalTitle(getString(R.string.settlements_prepayment_title))

        initializeViews(view)
        setupData(model)
        setupListeners(model)
    }

    // ==========================================
    // LOGIKA BIZNESOWA — 1:1 Z ORYGINAŁEM
    // ==========================================

    private fun initializeViews(view: View) {
        // Szukamy w baseModalBodyContainer (bo body jest wstrzyknięte przez BaseModal)
        val body = modalBodyContainer ?: return
        participantNickname = body.findViewById(R.id.participantNickname)
        currentBalance = body.findViewById(R.id.currentBalance)
        directionToMeCard = body.findViewById(R.id.directionToMeCard)
        directionFromMeCard = body.findViewById(R.id.directionFromMeCard)
        directionToMeRadio = body.findViewById(R.id.directionToMeRadio)
        directionFromMeRadio = body.findViewById(R.id.directionFromMeRadio)
        amountInputLayout = body.findViewById(R.id.amountInputLayout)
        amountInput = body.findViewById(R.id.amountInput)
        currencyInputLayout = body.findViewById(R.id.currencyInputLayout)
        currencyDropdown = body.findViewById(R.id.currencyDropdown)
        confirmButton = body.findViewById(R.id.confirmButton)
    }

    private fun setupData(model: PrepaymentUiModel) {
        participantNickname.text = model.participantNickname

        currentBalance.text = model.formattedCurrentBalance
        val balanceColor = when (model.balanceStatus) {
            ParticipantBalanceStatus.POSITIVE -> R.color.success
            ParticipantBalanceStatus.NEGATIVE -> R.color.error
            ParticipantBalanceStatus.SETTLED -> R.color.text_secondary
        }
        currentBalance.setTextColor(ContextCompat.getColor(requireContext(), balanceColor))

        setupCurrencyDropdown(model)

        val defaultDirection = when (model.balanceStatus) {
            ParticipantBalanceStatus.NEGATIVE -> PrepaymentDirection.FROM_ME
            ParticipantBalanceStatus.POSITIVE -> PrepaymentDirection.TO_ME
            ParticipantBalanceStatus.SETTLED -> PrepaymentDirection.TO_ME
        }
        selectDirection(defaultDirection)
    }

    private fun setupCurrencyDropdown(model: PrepaymentUiModel) {
        val currencies = model.availableCurrencies
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            currencies
        )
        currencyDropdown.setAdapter(adapter)

        if (currencies.isNotEmpty()) {
            selectedCurrency = currencies.first()
            currencyDropdown.setText(selectedCurrency, false)
        }

        currencyDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCurrency = currencies[position]
        }
    }

    private fun setupListeners(model: PrepaymentUiModel) {
        directionToMeCard.setOnClickListener { selectDirection(PrepaymentDirection.TO_ME) }
        directionToMeRadio.setOnClickListener { selectDirection(PrepaymentDirection.TO_ME) }
        directionFromMeCard.setOnClickListener { selectDirection(PrepaymentDirection.FROM_ME) }
        directionFromMeRadio.setOnClickListener { selectDirection(PrepaymentDirection.FROM_ME) }
        confirmButton.setOnClickListener { handleConfirm() }
    }

    private fun selectDirection(direction: PrepaymentDirection) {
        selectedDirection = direction
        directionToMeRadio.isChecked = (direction == PrepaymentDirection.TO_ME)
        directionFromMeRadio.isChecked = (direction == PrepaymentDirection.FROM_ME)

        val selectedStrokeWidth = 4
        val defaultStrokeWidth = 2
        val selectedStrokeColor = resources.getColor(R.color.primary, null)
        val defaultStrokeColor = resources.getColor(R.color.divider, null)

        directionToMeCard.strokeWidth = if (direction == PrepaymentDirection.TO_ME) selectedStrokeWidth else defaultStrokeWidth
        directionToMeCard.strokeColor = if (direction == PrepaymentDirection.TO_ME) selectedStrokeColor else defaultStrokeColor
        directionFromMeCard.strokeWidth = if (direction == PrepaymentDirection.FROM_ME) selectedStrokeWidth else defaultStrokeWidth
        directionFromMeCard.strokeColor = if (direction == PrepaymentDirection.FROM_ME) selectedStrokeColor else defaultStrokeColor
    }

    private fun handleConfirm() {
        val amountText = amountInput.text?.toString()?.trim()
        val amount = amountText?.replace(",", ".")?.toFloatOrNull()

        if (amountText.isNullOrBlank()) {
            amountInputLayout.error = getString(R.string.error_amount_required)
            return
        }

        if (amount == null || amount <= 0) {
            amountInputLayout.error = getString(R.string.error_amount_invalid)
            return
        }

        if (selectedCurrency.isBlank()) {
            selectedCurrency = prepaymentModel?.availableCurrencies?.firstOrNull() ?: "PLN"
        }

        amountInputLayout.error = null

        prepaymentModel?.let { model ->
            val request = PrepaymentRequest(
                tripId = "",
                participantId = model.participantId,
                amount = amount,
                currency = selectedCurrency,
                direction = selectedDirection
            )
            onConfirm?.invoke(request)
            dismissAnimated()
        }
    }

    companion object {
        fun newInstance(
            model: PrepaymentUiModel,
            onConfirm: (PrepaymentRequest) -> Unit
        ): PrepaymentModalFragment {
            return PrepaymentModalFragment().apply {
                this.prepaymentModel = model
                this.onConfirm = onConfirm
            }
        }
    }
}