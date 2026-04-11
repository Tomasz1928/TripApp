package com.example.tripapp2.ui.tripdetails.costs.addexpense

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment
import com.google.android.material.button.MaterialButton

/**
 * PayerSplitModalFragment — połączony modal: Kto płacił + Podział kosztów.
 *
 * Zastępuje dwa osobne modale:
 * - ListPickerModalFragment (wybór płatnika)
 * - SplitExpenseModalFragment (podział kosztów)
 *
 * Funkcje:
 * - Dropdown z autocomplete do wyboru płatnika (domyślnie: currentUser)
 * - Toggle: po równo / ręcznie
 * - Scrollowalna lista uczestników z checkboxami
 * - Przyciski: Wszyscy / Nikt
 * - Wyróżnienie "siebie" (awatar z ramką, badge "(ty)")
 * - Pasek sumy + różnica
 *
 * Callback zwraca: wybranego payerId + zaktualizowany ExpenseSplit.
 */
class PayerSplitModalFragment : BaseModalFragment() {

    // ==========================================
    // DATA
    // ==========================================

    private var expenseSplit: ExpenseSplit? = null
    private var totalAmount: Float = 0f
    private var currentUserId: String = ""
    private var selectedPayerId: String? = null
    private var participants: List<SplitParticipant> = emptyList()

    private var onResult: ((payerId: String, split: ExpenseSplit) -> Unit)? = null

    // ==========================================
    // VIEWS
    // ==========================================

    // Payer dropdown
    private lateinit var payerDropdownField: LinearLayout
    private lateinit var payerSelectedAvatar: TextView
    private lateinit var payerSelectedName: TextView
    private lateinit var payerDropdownChevron: ImageView
    private lateinit var payerDropdownList: LinearLayout
    private lateinit var payerSearchInput: EditText
    private lateinit var payerResultsContainer: LinearLayout
    private lateinit var payerResultsScroll: ScrollView

    // Split section
    private lateinit var toggleEqual: TextView
    private lateinit var toggleManual: TextView
    private lateinit var splitParticipantsContainer: LinearLayout
    private lateinit var splitParticipantsScroll: ScrollView
    private lateinit var selectAllButton: TextView
    private lateinit var selectNoneButton: TextView

    // Summary
    private lateinit var totalExpenseLabel: TextView
    private lateinit var totalLabel: TextView
    private lateinit var differenceLabel: TextView
    private lateinit var saveButton: MaterialButton

    // State
    private var isDropdownOpen = false
    private var currentSplitType: SplitType = SplitType.EQUAL

    companion object {
        private const val MAX_VISIBLE_PAYER_ITEMS = 4
        private const val MAX_VISIBLE_SPLIT_ITEMS = 5
        private const val ITEM_HEIGHT_DP = 48
        private const val SPLIT_ITEM_HEIGHT_DP = 52

        fun newInstance(
            split: ExpenseSplit,
            totalAmount: Float,
            currentUserId: String,
            selectedPayerId: String?,
            onResult: (payerId: String, split: ExpenseSplit) -> Unit
        ): PayerSplitModalFragment {
            return PayerSplitModalFragment().apply {
                this.expenseSplit = split
                this.totalAmount = totalAmount
                this.currentUserId = currentUserId
                this.selectedPayerId = selectedPayerId
                this.onResult = onResult
            }
        }
    }

    // ==========================================
    // LIFECYCLE
    // ==========================================

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setModalTitle(getString(R.string.payer_split_modal_title))
    }

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View? {
        val body = inflater.inflate(R.layout.fragment_payer_split_modal, container, false)

        initViews(body)
        setupData()
        setupPayerDropdown()
        setupSplitToggle()
        setupSelectAllNone()
        setupParticipantsList()
        setupSaveButton()
        updatePayerDisplay()
        updateTotal()

        totalExpenseLabel.text = String.format("Kwota: %.2f", totalAmount)

        return body
    }

    // ==========================================
    // INIT
    // ==========================================

    private fun initViews(view: View) {
        // Payer
        payerDropdownField = view.findViewById(R.id.payerDropdownField)
        payerSelectedAvatar = view.findViewById(R.id.payerSelectedAvatar)
        payerSelectedName = view.findViewById(R.id.payerSelectedName)
        payerDropdownChevron = view.findViewById(R.id.payerDropdownChevron)
        payerDropdownList = view.findViewById(R.id.payerDropdownList)
        payerSearchInput = view.findViewById(R.id.payerSearchInput)
        payerResultsContainer = view.findViewById(R.id.payerResultsContainer)
        payerResultsScroll = view.findViewById(R.id.payerResultsScroll)

        // Split
        toggleEqual = view.findViewById(R.id.toggleEqual)
        toggleManual = view.findViewById(R.id.toggleManual)
        splitParticipantsContainer = view.findViewById(R.id.splitParticipantsContainer)
        splitParticipantsScroll = view.findViewById(R.id.splitParticipantsScroll)
        selectAllButton = view.findViewById(R.id.selectAllButton)
        selectNoneButton = view.findViewById(R.id.selectNoneButton)

        // Summary
        totalExpenseLabel = view.findViewById(R.id.totalExpenseLabel)
        totalLabel = view.findViewById(R.id.totalLabel)
        differenceLabel = view.findViewById(R.id.differenceLabel)
        saveButton = view.findViewById(R.id.saveButton)
    }

    private fun setupData() {
        participants = expenseSplit?.participants ?: emptyList()
        currentSplitType = expenseSplit?.splitType ?: SplitType.EQUAL

        // Domyślnie: payer = ja (currentUser), jeśli nie ustawiono inaczej
        if (selectedPayerId == null) {
            selectedPayerId = currentUserId
        }
    }

    // ==========================================
    // PAYER DROPDOWN
    // ==========================================

    private fun setupPayerDropdown() {
        payerDropdownField.setOnClickListener {
            toggleDropdown()
        }

        payerSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPayerResults(s?.toString() ?: "")
            }
        })

        // Populate initial dropdown list
        populatePayerList(participants)
    }

    private fun toggleDropdown() {
        isDropdownOpen = !isDropdownOpen

        if (isDropdownOpen) {
            payerDropdownList.visibility = View.VISIBLE
            payerDropdownChevron.rotation = 180f
            payerSearchInput.setText("")
            payerSearchInput.requestFocus()
            populatePayerList(participants)
            limitPayerListHeight()
        } else {
            payerDropdownList.visibility = View.GONE
            payerDropdownChevron.rotation = 0f
        }
    }

    private fun closeDropdown() {
        isDropdownOpen = false
        payerDropdownList.visibility = View.GONE
        payerDropdownChevron.rotation = 0f
    }

    private fun filterPayerResults(query: String) {
        val filtered = if (query.isBlank()) {
            participants
        } else {
            participants.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        populatePayerList(filtered)
    }

    private fun populatePayerList(items: List<SplitParticipant>) {
        payerResultsContainer.removeAllViews()

        // Sortuj: "ja" zawsze na górze
        val sorted = items.sortedByDescending { it.id == currentUserId }

        sorted.forEach { participant ->
            val itemView = layoutInflater.inflate(
                R.layout.item_payer_dropdown, payerResultsContainer, false
            )

            val avatar = itemView.findViewById<TextView>(R.id.payerItemAvatar)
            val name = itemView.findViewById<TextView>(R.id.payerItemName)

            val isMe = participant.id == currentUserId
            setupAvatar(avatar, participant, isMe)
            name.text = if (isMe) "${participant.name} (ty)" else participant.name

            // Highlight wybranego
            if (participant.id == selectedPayerId) {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.ripple)
                )
            }

            itemView.setOnClickListener {
                selectedPayerId = participant.id
                updatePayerDisplay()
                closeDropdown()
            }

            payerResultsContainer.addView(itemView)

            // Divider (nie po ostatnim)
            if (participant != sorted.last()) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (0.5f * resources.displayMetrics.density).toInt()
                    )
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.divider))
                }
                payerResultsContainer.addView(divider)
            }
        }
    }

    private fun limitPayerListHeight() {
        val count = participants.size
        if (count > MAX_VISIBLE_PAYER_ITEMS) {
            val density = resources.displayMetrics.density
            val maxHeight = (MAX_VISIBLE_PAYER_ITEMS * ITEM_HEIGHT_DP * density).toInt()
            payerResultsScroll.layoutParams = payerResultsScroll.layoutParams.apply {
                height = maxHeight
            }
        } else {
            payerResultsScroll.layoutParams = payerResultsScroll.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private fun updatePayerDisplay() {
        val payer = participants.find { it.id == selectedPayerId }
        if (payer != null) {
            val isMe = payer.id == currentUserId
            setupAvatar(payerSelectedAvatar, payer, isMe)
            payerSelectedName.text = if (isMe) "${payer.name} (ty)" else payer.name
        } else {
            payerSelectedAvatar.text = "?"
            payerSelectedName.text = getString(R.string.add_expense_payer_hint)
        }
    }

    // ==========================================
    // AVATAR HELPER
    // ==========================================

    private fun setupAvatar(avatarView: TextView, participant: SplitParticipant, isMe: Boolean) {
        if (isMe) {
            avatarView.text = "TY"
            avatarView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            avatarView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_avatar_circle_me)
        } else {
            avatarView.text = participant.name.take(2).uppercase()
            avatarView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            avatarView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_avatar_circle)
        }
    }

    // ==========================================
    // SPLIT TOGGLE
    // ==========================================

    private fun setupSplitToggle() {
        updateToggleUI()

        toggleEqual.setOnClickListener {
            if (currentSplitType != SplitType.EQUAL) {
                currentSplitType = SplitType.EQUAL
                updateToggleUI()
                recalculateEqualSplit()
                refreshParticipantsList()
            }
        }

        toggleManual.setOnClickListener {
            if (currentSplitType != SplitType.MANUAL) {
                currentSplitType = SplitType.MANUAL
                updateToggleUI()
                refreshParticipantsList()
            }
        }
    }

    private fun updateToggleUI() {
        if (currentSplitType == SplitType.EQUAL) {
            toggleEqual.setBackgroundResource(R.drawable.bg_toggle_active)
            toggleEqual.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            toggleManual.background = null
            toggleManual.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
        } else {
            toggleManual.setBackgroundResource(R.drawable.bg_toggle_active)
            toggleManual.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            toggleEqual.background = null
            toggleEqual.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
        }
    }

    // ==========================================
    // SELECT ALL / NONE
    // ==========================================

    private fun setupSelectAllNone() {
        selectAllButton.setOnClickListener {
            participants.forEach { it.isSelected = true }
            if (currentSplitType == SplitType.EQUAL) {
                recalculateEqualSplit()
            }
            refreshParticipantsList()
            updateTotal()
        }

        selectNoneButton.setOnClickListener {
            participants.forEach {
                it.isSelected = false
                it.amount = 0f
            }
            refreshParticipantsList()
            updateTotal()
        }
    }

    // ==========================================
    // PARTICIPANTS LIST (SPLIT)
    // ==========================================

    private fun setupParticipantsList() {
        refreshParticipantsList()
        limitSplitListHeight()
    }

    private fun refreshParticipantsList() {
        splitParticipantsContainer.removeAllViews()

        // Sortuj: "ja" zawsze na górze
        val sorted = participants.sortedByDescending { it.id == currentUserId }

        sorted.forEach { participant ->
            val itemView = createSplitParticipantView(participant)
            splitParticipantsContainer.addView(itemView)
        }
    }

    private fun createSplitParticipantView(participant: SplitParticipant): View {
        val view = layoutInflater.inflate(
            R.layout.item_payer_split_participant, splitParticipantsContainer, false
        )

        val checkbox = view.findViewById<CheckBox>(R.id.participantCheckbox)
        val avatar = view.findViewById<TextView>(R.id.participantAvatar)
        val nameView = view.findViewById<TextView>(R.id.participantName)
        val amountLabel = view.findViewById<TextView>(R.id.participantAmountLabel)
        val amountInput = view.findViewById<EditText>(R.id.participantAmountInput)

        val isMe = participant.id == currentUserId

        // Avatar
        setupAvatar(avatar, participant, isMe)

        // Name
        nameView.text = if (isMe) "${participant.name} (ty)" else participant.name

        // Checkbox
        checkbox.isChecked = participant.isSelected

        // Opacity dla niezaznaczonych
        val contentAlpha = if (participant.isSelected) 1.0f else 0.4f
        avatar.alpha = contentAlpha
        nameView.alpha = contentAlpha

        // Kwota — zależy od trybu
        if (participant.isSelected) {
            when (currentSplitType) {
                SplitType.EQUAL -> {
                    amountLabel.visibility = View.VISIBLE
                    amountInput.visibility = View.GONE
                    amountLabel.text = String.format("%.2f", participant.amount)
                }
                SplitType.MANUAL -> {
                    amountLabel.visibility = View.GONE
                    amountInput.visibility = View.VISIBLE
                    if (participant.amount > 0) {
                        amountInput.setText(String.format("%.2f", participant.amount))
                    } else {
                        amountInput.setText("")
                    }

                    amountInput.addTextChangedListener { text ->
                        participant.amount = text.toString().toFloatOrNull() ?: 0f
                        updateTotal()
                    }
                }
            }
        } else {
            amountLabel.visibility = View.GONE
            amountInput.visibility = View.GONE
        }

        // Checkbox listener
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            participant.isSelected = isChecked
            if (!isChecked) {
                participant.amount = 0f
            }
            if (currentSplitType == SplitType.EQUAL) {
                recalculateEqualSplit()
            }
            refreshParticipantsList()
            updateTotal()
        }

        return view
    }

    private fun limitSplitListHeight() {
        val count = participants.size
        if (count > MAX_VISIBLE_SPLIT_ITEMS) {
            val density = resources.displayMetrics.density
            val maxHeight = (MAX_VISIBLE_SPLIT_ITEMS * SPLIT_ITEM_HEIGHT_DP * density).toInt()
            splitParticipantsScroll.layoutParams = splitParticipantsScroll.layoutParams.apply {
                height = maxHeight
            }
        }
    }

    // ==========================================
    // EQUAL SPLIT CALC
    // ==========================================

    private fun recalculateEqualSplit() {
        val selected = participants.filter { it.isSelected }
        if (selected.isEmpty()) {
            participants.forEach { it.amount = 0f }
            return
        }

        val baseAmount = (totalAmount / selected.size * 100).toInt() / 100f
        val remainder = totalAmount - (baseAmount * selected.size)

        var lastSelectedIndex = -1
        participants.forEachIndexed { index, p ->
            if (p.isSelected) {
                p.amount = baseAmount
                lastSelectedIndex = index
            } else {
                p.amount = 0f
            }
        }

        // Reszta z dzielenia → ostatni zaznaczony
        if (lastSelectedIndex >= 0) {
            participants[lastSelectedIndex].amount += remainder
        }
    }

    // ==========================================
    // TOTAL / DIFFERENCE
    // ==========================================

    private fun updateTotal() {
        val totalAllocated = participants
            .filter { it.isSelected }
            .sumOf { it.amount.toDouble() }.toFloat()

        totalLabel.text = String.format("Suma: %.2f", totalAllocated)

        val difference = totalAmount - totalAllocated

        when {
            kotlin.math.abs(difference) < 0.01f -> {
                differenceLabel.text = String.format("Różnica: %.2f", 0f)
                differenceLabel.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.success)
                )
            }
            difference > 0 -> {
                differenceLabel.text = String.format("Brakuje: %.2f", difference)
                differenceLabel.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
            }
            else -> {
                differenceLabel.text = String.format("Nadmiar: %.2f", -difference)
                differenceLabel.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
            }
        }
    }

    // ==========================================
    // SAVE
    // ==========================================

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            val payerId = selectedPayerId
            if (payerId == null) {
                Toast.makeText(requireContext(), getString(R.string.error_payer_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selected = participants.filter { it.isSelected }
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_split_no_participants), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Walidacja sumy w trybie ręcznym
            if (currentSplitType == SplitType.MANUAL) {
                val totalAllocated = selected.sumOf { it.amount.toDouble() }.toFloat()
                if (kotlin.math.abs(totalAllocated - totalAmount) > 0.01f) {
                    Toast.makeText(requireContext(), getString(R.string.error_split_amount_mismatch), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val updatedSplit = ExpenseSplit(
                splitType = currentSplitType,
                participants = participants
            )

            onResult?.invoke(payerId, updatedSplit)
            dismissAnimated()
        }
    }
}