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
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Modal zaliczki (przedpłaty)
 *
 * Przepływ:
 * 1. Fragment wywołuje newInstance() z PrepaymentUiModel i callbackiem
 * 2. User wypełnia formularz (kierunek, kwota, waluta)
 * 3. Po kliknięciu "Dodaj zaliczkę" wywoływany jest callback z PrepaymentRequest
 * 4. Fragment przekazuje request do ViewModel
 * 5. ViewModel zapisuje przez Repository → cache jest aktualizowany
 */
class PrepaymentModalFragment : DialogFragment() {

    private var prepaymentModel: PrepaymentUiModel? = null
    private var onConfirm: ((PrepaymentRequest) -> Unit)? = null

    // Wybrana waluta
    private var selectedCurrency: String = ""

    // Wybrany kierunek zaliczki
    private var selectedDirection: PrepaymentDirection = PrepaymentDirection.TO_ME

    // Views
    private lateinit var closeButton: ImageView
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.modal_prepayment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val model = prepaymentModel ?: run {
            dismiss()
            return
        }

        initializeViews(view)
        setupData(model)
        setupListeners(model)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun initializeViews(view: View) {
        closeButton = view.findViewById(R.id.closeButton)
        participantNickname = view.findViewById(R.id.participantNickname)
        currentBalance = view.findViewById(R.id.currentBalance)
        directionToMeCard = view.findViewById(R.id.directionToMeCard)
        directionFromMeCard = view.findViewById(R.id.directionFromMeCard)
        directionToMeRadio = view.findViewById(R.id.directionToMeRadio)
        directionFromMeRadio = view.findViewById(R.id.directionFromMeRadio)
        amountInputLayout = view.findViewById(R.id.amountInputLayout)
        amountInput = view.findViewById(R.id.amountInput)
        currencyInputLayout = view.findViewById(R.id.currencyInputLayout)
        currencyDropdown = view.findViewById(R.id.currencyDropdown)
        confirmButton = view.findViewById(R.id.confirmButton)
    }

    private fun setupData(model: PrepaymentUiModel) {
        // Nazwa uczestnika
        participantNickname.text = model.participantNickname

        // Aktualny balans z kolorem
        currentBalance.text = model.formattedCurrentBalance
        val balanceColor = when (model.balanceStatus) {
            ParticipantBalanceStatus.POSITIVE -> R.color.success
            ParticipantBalanceStatus.NEGATIVE -> R.color.error
            ParticipantBalanceStatus.SETTLED -> R.color.text_secondary
        }
        currentBalance.setTextColor(ContextCompat.getColor(requireContext(), balanceColor))

        // Setup dropdown waluty
        setupCurrencyDropdown(model)

        // Ustaw domyślny kierunek na podstawie balansu
        val defaultDirection = when (model.balanceStatus) {
            ParticipantBalanceStatus.NEGATIVE -> PrepaymentDirection.FROM_ME  // Jestem winien → ja daję
            ParticipantBalanceStatus.POSITIVE -> PrepaymentDirection.TO_ME    // On mi winien → on daje mi
            ParticipantBalanceStatus.SETTLED -> PrepaymentDirection.TO_ME     // Neutral → domyślnie TO_ME
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

        // Ustaw pierwszą walutę jako domyślną
        if (currencies.isNotEmpty()) {
            selectedCurrency = currencies.first()
            currencyDropdown.setText(selectedCurrency, false)
        }

        currencyDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCurrency = currencies[position]
        }
    }

    private fun setupListeners(model: PrepaymentUiModel) {
        // Zamknij modal
        closeButton.setOnClickListener {
            dismiss()
        }

        // Wybór kierunku - karta "Pieniądze do mnie"
        directionToMeCard.setOnClickListener {
            selectDirection(PrepaymentDirection.TO_ME)
        }
        directionToMeRadio.setOnClickListener {
            selectDirection(PrepaymentDirection.TO_ME)
        }

        // Wybór kierunku - karta "Ja daję pieniądze"
        directionFromMeCard.setOnClickListener {
            selectDirection(PrepaymentDirection.FROM_ME)
        }
        directionFromMeRadio.setOnClickListener {
            selectDirection(PrepaymentDirection.FROM_ME)
        }

        // Potwierdzenie
        confirmButton.setOnClickListener {
            handleConfirm()
        }
    }

    /**
     * Zmienia wybrany kierunek zaliczki i aktualizuje UI
     */
    private fun selectDirection(direction: PrepaymentDirection) {
        selectedDirection = direction

        // Aktualizuj radio buttons
        directionToMeRadio.isChecked = (direction == PrepaymentDirection.TO_ME)
        directionFromMeRadio.isChecked = (direction == PrepaymentDirection.FROM_ME)

        // Aktualizuj wygląd kart (stroke)
        val selectedStrokeWidth = 4
        val defaultStrokeWidth = 2
        val selectedStrokeColor = resources.getColor(R.color.primary, null)
        val defaultStrokeColor = resources.getColor(R.color.divider, null)

        // Karta TO_ME
        directionToMeCard.strokeWidth = if (direction == PrepaymentDirection.TO_ME) {
            selectedStrokeWidth
        } else {
            defaultStrokeWidth
        }
        directionToMeCard.strokeColor = if (direction == PrepaymentDirection.TO_ME) {
            selectedStrokeColor
        } else {
            defaultStrokeColor
        }

        // Karta FROM_ME
        directionFromMeCard.strokeWidth = if (direction == PrepaymentDirection.FROM_ME) {
            selectedStrokeWidth
        } else {
            defaultStrokeWidth
        }
        directionFromMeCard.strokeColor = if (direction == PrepaymentDirection.FROM_ME) {
            selectedStrokeColor
        } else {
            defaultStrokeColor
        }
    }

    /**
     * Walidacja i potwierdzenie zaliczki
     */
    private fun handleConfirm() {
        // Walidacja kwoty
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

        // Walidacja waluty
        if (selectedCurrency.isBlank()) {
            // Wybierz pierwszą dostępną
            selectedCurrency = prepaymentModel?.availableCurrencies?.firstOrNull() ?: "PLN"
        }

        // Wyczyść błąd
        amountInputLayout.error = null

        // Utwórz request i wywołaj callback
        prepaymentModel?.let { model ->
            val request = PrepaymentRequest(
                tripId = "", // Zostanie uzupełnione w Fragment
                participantId = model.participantId,
                amount = amount,
                currency = selectedCurrency,
                direction = selectedDirection
            )

            onConfirm?.invoke(request)
            dismiss()
        }
    }

    companion object {
        /**
         * Tworzy nową instancję modala
         *
         * @param model Dane do wyświetlenia (uczestnik, balans, waluty)
         * @param onConfirm Callback wywoływany po potwierdzeniu z PrepaymentRequest
         */
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