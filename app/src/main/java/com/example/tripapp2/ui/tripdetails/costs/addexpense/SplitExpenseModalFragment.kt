package com.example.tripapp2.ui.tripdetails.costs.addexpense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment
import com.google.android.material.button.MaterialButton

class SplitExpenseModalFragment : BaseModalFragment() {

    private var expenseSplit: ExpenseSplit? = null
    private var totalAmount: Float = 0f
    private var onSplitSaved: ((ExpenseSplit) -> Unit)? = null

    private lateinit var participantsContainer: LinearLayout
    private lateinit var totalLabel: TextView
    private lateinit var differenceLabel: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var totalExpenseLabel: TextView
    private lateinit var equalSplitButton: MaterialButton

    companion object {
        fun newInstance(
            split: ExpenseSplit,
            totalAmount: Float,
            onSplitSaved: (ExpenseSplit) -> Unit
        ): SplitExpenseModalFragment {
            return SplitExpenseModalFragment().apply {
                this.expenseSplit = split
                this.totalAmount = totalAmount
                this.onSplitSaved = onSplitSaved
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setModalTitle(getString(R.string.split_modal_title))
    }

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View? {
        val body = inflater.inflate(R.layout.fragment_split_expense_modal, container, false)

        initializeBodyViews(body)
        setupParticipants()
        setupButtons()
        updateTotal()
        totalExpenseLabel.text = String.format(getString(R.string.split_total_cost_label), totalAmount)

        return body
    }

    // ==========================================
    // LOGIKA BIZNESOWA — BEZ ZMIAN
    // ==========================================

    private fun initializeBodyViews(view: View) {
        participantsContainer = view.findViewById(R.id.participantsContainer)
        totalLabel = view.findViewById(R.id.totalLabel)
        differenceLabel = view.findViewById(R.id.differenceLabel)
        saveButton = view.findViewById(R.id.saveButton)
        totalExpenseLabel = view.findViewById(R.id.totalExpenseLabel)
        equalSplitButton = view.findViewById(R.id.equalSplitButton)
    }

    private fun setupParticipants() {
        participantsContainer.removeAllViews()
        expenseSplit?.participants?.forEach { participant ->
            val itemView = createParticipantView(participant)
            participantsContainer.addView(itemView)
        }

        // Ograniczenie scrolla jeśli >5 uczestników
        val participantCount = expenseSplit?.participants?.size ?: 0
        if (participantCount > 5) {
            val scrollParent = participantsContainer.parent
            if (scrollParent is ScrollView) {
                limitListHeightIfNeeded(scrollParent, participantCount, 56, 5)
            }
        }
    }

    private fun createParticipantView(participant: SplitParticipant): View {
        val view = layoutInflater.inflate(R.layout.item_split_participant, participantsContainer, false)

        val checkbox = view.findViewById<CheckBox>(R.id.participantCheckbox)
        val amountInput = view.findViewById<EditText>(R.id.participantAmount)

        checkbox.text = participant.name
        checkbox.isChecked = participant.isSelected
        amountInput.visibility = if (participant.isSelected) View.VISIBLE else View.GONE

        if (participant.amount > 0) {
            amountInput.setText(String.format("%.2f", participant.amount))
        }

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            participant.isSelected = isChecked
            if (isChecked) {
                amountInput.visibility = View.VISIBLE
            } else {
                participant.amount = 0f
                amountInput.setText("")
                amountInput.visibility = View.GONE
            }
            updateTotal()
        }

        amountInput.addTextChangedListener { text ->
            val amount = text.toString().toFloatOrNull() ?: 0f
            participant.amount = amount
            updateTotal()
        }

        return view
    }

    private fun setupButtons() {
        saveButton.setOnClickListener {
            expenseSplit?.let { split ->
                onSplitSaved?.invoke(split)
                dismissAnimated()
            }
        }

        equalSplitButton.setOnClickListener {
            splitEqually()
        }
    }

    private fun splitEqually() {
        val selectedParticipants = expenseSplit?.participants?.filter { it.isSelected } ?: return
        if (selectedParticipants.isEmpty()) return

        val equalAmount = totalAmount / selectedParticipants.size

        selectedParticipants.forEach { it.amount = equalAmount }

        // Odśwież UI
        setupParticipants()
        updateTotal()
    }

    private fun updateTotal() {
        val totalAllocated = expenseSplit?.participants
            ?.filter { it.isSelected }
            ?.sumOf { it.amount.toDouble() }?.toFloat() ?: 0f

        totalLabel.text = String.format("%.2f", totalAllocated)

        val difference = totalAmount - totalAllocated
        differenceLabel.text = String.format("%.2f", difference)

        val color = if (kotlin.math.abs(difference) < 0.01f) {
            resources.getColor(R.color.success, null)
        } else {
            resources.getColor(R.color.error, null)
        }
        differenceLabel.setTextColor(color)
    }
}