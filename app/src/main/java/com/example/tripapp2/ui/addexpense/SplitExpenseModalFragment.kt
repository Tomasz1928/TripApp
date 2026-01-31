package com.example.tripapp2.ui.addexpense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R
import com.google.android.material.button.MaterialButton

class SplitExpenseModalFragment : DialogFragment() {

    private var expenseSplit: ExpenseSplit? = null
    private var totalAmount: Float = 0f
    private var onSplitSaved: ((ExpenseSplit) -> Unit)? = null

    private lateinit var participantsContainer: LinearLayout
    private lateinit var totalLabel: TextView
    private lateinit var differenceLabel: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var closeButton: ImageView
    private lateinit var totalExpenseLabel: TextView
    private lateinit var equalSplitButton: MaterialButton

    companion object {
        private const val ARG_SPLIT = "split"
        private const val ARG_AMOUNT = "amount"

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_split_expense_modal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupParticipants()
        setupButtons()
        updateTotal()
        totalExpenseLabel.text = String.format(getString(R.string.split_total_cost_label), totalAmount)
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
        participantsContainer = view.findViewById(R.id.participantsContainer)
        totalLabel = view.findViewById(R.id.totalLabel)
        differenceLabel = view.findViewById(R.id.differenceLabel)
        saveButton = view.findViewById(R.id.saveButton)
        closeButton = view.findViewById(R.id.closeButton)
        totalExpenseLabel = view.findViewById(R.id.totalExpenseLabel)
        equalSplitButton = view.findViewById(R.id.equalSplitButton)
    }

    private fun setupParticipants() {
        participantsContainer.removeAllViews()

        expenseSplit?.participants?.forEach { participant ->
            val itemView = createParticipantView(participant)
            participantsContainer.addView(itemView)
        }
    }

    private fun createParticipantView(participant: SplitParticipant): View {
        val view = layoutInflater.inflate(R.layout.item_split_participant, participantsContainer, false)

        val checkbox = view.findViewById<CheckBox>(R.id.participantCheckbox)
        val amountInput = view.findViewById<EditText>(R.id.participantAmount)

        checkbox.text = participant.name
        checkbox.isChecked = participant.isSelected

        // Zawsze pokazuj input
        amountInput.visibility = if (participant.isSelected) View.VISIBLE else View.GONE

        if (participant.amount > 0) {
            amountInput.setText(String.format("%.2f", participant.amount))
        }

        // Checkbox listener
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            participant.isSelected = isChecked

            if (isChecked) {
                amountInput.visibility = View.VISIBLE
                // Nie obliczaj automatycznie - użytkownik użyje przycisku "Po równo"
            } else {
                // Jak odznaczamy - wyzeruj kwotę i schowaj input
                participant.amount = 0f
                amountInput.setText("")
                amountInput.visibility = View.GONE
            }

            updateTotal()
        }

        // Amount input listener
        amountInput.addTextChangedListener { text ->
            val amount = text.toString().toFloatOrNull() ?: 0f
            participant.amount = amount
            updateTotal()
        }

        return view
    }

    private fun updateParticipantsUI() {
        setupParticipants()
    }

    private fun updateTotal() {
        val split = expenseSplit ?: return
        val selected = split.getSelectedParticipants()

        if (selected.isEmpty()) {
            totalLabel.text = getString(R.string.split_sum_zero)
            differenceLabel.visibility = View.GONE
            totalLabel.setTextColor(resources.getColor(R.color.text_secondary, null))
            return
        }

        val total = split.getManualTotal()
        totalLabel.text = String.format(getString(R.string.split_sum_format), total)

        // Oblicz różnicę
        val difference = total - totalAmount
        val isValid = kotlin.math.abs(difference) < 0.01f

        if (isValid) {
            // Zgadza się - zielony kolor, ukryj komunikat
            totalLabel.setTextColor(resources.getColor(R.color.success, null))
            differenceLabel.visibility = View.GONE
        } else {
            // Nie zgadza się - czerwony kolor, pokaż różnicę
            totalLabel.setTextColor(resources.getColor(R.color.error, null))
            differenceLabel.visibility = View.VISIBLE

            if (difference > 0) {
                differenceLabel.text = String.format(getString(R.string.split_excess_format), difference)
            } else {
                differenceLabel.text = String.format(getString(R.string.split_missing_format), -difference)
            }
        }
    }

    private fun setupButtons() {
        // Przycisk "Po równo"
        equalSplitButton.setOnClickListener {
            expenseSplit = expenseSplit?.calculateEqualSplit(totalAmount)

            updateParticipantsUI()
            updateTotal()
        }

        closeButton.setOnClickListener {
            dismiss()
        }

        saveButton.setOnClickListener {
            val split = expenseSplit ?: return@setOnClickListener

            if (!split.isValid(totalAmount)) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_split_invalid),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            onSplitSaved?.invoke(split)
            dismiss()
        }
    }
}