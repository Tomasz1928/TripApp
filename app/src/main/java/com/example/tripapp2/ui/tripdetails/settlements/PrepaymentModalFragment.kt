package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView

/**
 * Modal zaliczki (przedpłaty)
 * Wzorowany na SettlementDetailModalFragment - używa DialogFragment
 */
class PrepaymentModalFragment : DialogFragment() {

    private var prepaymentModel: PrepaymentUiModel? = null
    private var onConfirm: ((PrepaymentRequest) -> Unit)? = null
    private var selectedCurrency: String = ""

    // Views
    private lateinit var participantNickname: TextView
    private lateinit var currentBalance: TextView
    private lateinit var directionToMeCard: MaterialCardView
    private lateinit var directionFromMeCard: MaterialCardView
    private lateinit var directionToMeRadio: RadioButton
    private lateinit var directionFromMeRadio: RadioButton
    private lateinit var amountInputLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var confirmButton: MaterialButton
    private lateinit var currencyInputLayout: TextInputLayout
    private lateinit var currencyDropdown: AutoCompleteTextView

    private var selectedDirection: PrepaymentDirection = PrepaymentDirection.TO_ME

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.modal_prepayment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val model = prepaymentModel ?: return

        initializeViews(view)
        setupData(model)
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
        participantNickname = view.findViewById(R.id.participantNickname)
        currentBalance = view.findViewById(R.id.currentBalance)
        directionToMeCard = view.findViewById(R.id.directionToMeCard)
        directionFromMeCard = view.findViewById(R.id.directionFromMeCard)
        directionToMeRadio = view.findViewById(R.id.directionToMeRadio)
        directionFromMeRadio = view.findViewById(R.id.directionFromMeRadio)
        amountInputLayout = view.findViewById(R.id.amountInputLayout)
        amountInput = view.findViewById(R.id.amountInput)
        confirmButton = view.findViewById(R.id.confirmButton)
        currencyInputLayout = view.findViewById(R.id.currencyInputLayout)
        currencyDropdown = view.findViewById(R.id.currencyDropdown)
    }

    private fun setupData(model: PrepaymentUiModel) {
        participantNickname.text = model.participantNickname
        currentBalance.text = model.formattedCurrentBalance
        setupCurrencyDropdown(model)

        // Ustaw kolor balansu
        val balanceColor = when {
            model.currentBalance > 0 -> R.color.success
            model.currentBalance < 0 -> R.color.error
            else -> R.color.text_secondary
        }
        currentBalance.setTextColor(resources.getColor(balanceColor, null))

        // Domyślnie zaznacz kierunek w zależności od balansu
        if (model.currentBalance < 0) {
            // Jestem dłużny - domyślnie "Ja daję pieniądze"
            selectDirection(PrepaymentDirection.FROM_ME)
        } else {
            // On mi jest winny lub jesteśmy na 0 - domyślnie "Pieniądze do mnie"
            selectDirection(PrepaymentDirection.TO_ME)
        }
    }

    private fun setupCurrencyDropdown(model: PrepaymentUiModel) {
        val currencies = model.availableCurrencies
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        currencyDropdown.setAdapter(adapter)

        currencyDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCurrency = currencies[position]
        }
    }

    private fun setupListeners() {
        // Przycisk zamknięcia
        view?.findViewById<ImageView>(R.id.closeButton)?.setOnClickListener {
            dismiss()
        }

        // Kierunek: Do mnie
        directionToMeCard.setOnClickListener {
            selectDirection(PrepaymentDirection.TO_ME)
        }
        directionToMeRadio.setOnClickListener {
            selectDirection(PrepaymentDirection.TO_ME)
        }

        // Kierunek: Ode mnie
        directionFromMeCard.setOnClickListener {
            selectDirection(PrepaymentDirection.FROM_ME)
        }
        directionFromMeRadio.setOnClickListener {
            selectDirection(PrepaymentDirection.FROM_ME)
        }

        // Przycisk potwierdzenia
        confirmButton.setOnClickListener {
            handleConfirm()
        }
    }

    private fun selectDirection(direction: PrepaymentDirection) {
        selectedDirection = direction

        // Aktualizuj radio buttony
        directionToMeRadio.isChecked = direction == PrepaymentDirection.TO_ME
        directionFromMeRadio.isChecked = direction == PrepaymentDirection.FROM_ME

        // Aktualizuj wygląd kart (stroke)
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
        // Walidacja
        val amountText = amountInput.text?.toString()
        val amount = amountText?.toFloatOrNull()

        if (amountText.isNullOrBlank()) {
            amountInputLayout.error = getString(R.string.error_amount_required)
            return
        }

        if (amount == null || amount <= 0) {
            amountInputLayout.error = getString(R.string.error_amount_invalid)
            return
        }

        amountInputLayout.error = null

        // Utwórz request
        prepaymentModel?.let { model ->
            val request = PrepaymentRequest(
                tripId = "",
                participantId = model.participantId,
                amount = amount,
                currency = selectedCurrency,  // <-- zmiana z model.currency
                direction = selectedDirection
            )

            onConfirm?.invoke(request)
            dismiss()
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
