package com.example.tripapp2.ui.tripdetails.modal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R
import com.example.tripapp2.ui.tripdetails.CurrencyExpenseUiModel

/**
 * Modal z listą wydatków w różnych walutach
 */
class ExpensesListModalFragment : DialogFragment() {

    private var expenses: List<CurrencyExpenseUiModel>? = null

    companion object {
        fun newInstance(expenses: List<CurrencyExpenseUiModel>): ExpensesListModalFragment {
            return ExpensesListModalFragment().apply {
                this.expenses = expenses
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_generic_modal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val expensesList = expenses ?: return

        // Setup modal
        view.findViewById<TextView>(R.id.modalTitle).text = "Twoje wydatki"
        view.findViewById<ImageView>(R.id.closeButton).setOnClickListener { dismiss() }

        // Setup body
        val bodyContainer = view.findViewById<ViewGroup>(R.id.modalBodyContainer)
        val bodyView = createExpensesBody(expensesList)
        bodyContainer.addView(bodyView)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun createExpensesBody(expenses: List<CurrencyExpenseUiModel>): View {
        val body = layoutInflater.inflate(R.layout.modal_expenses_body, null, false)

        val totalCostText = body.findViewById<TextView>(R.id.totalCostText)
        val expensesListContainer = body.findViewById<LinearLayout>(R.id.expensesListContainer)

        // Total in main currency (pierwszy w liście) - PRIMARY
        if (expenses.isNotEmpty()) {
            val mainExpense = expenses.first()
            totalCostText.text = "Suma: ${mainExpense.formattedAmount}"
            totalCostText.textSize = 18f
            totalCostText.setTextColor(resources.getColor(R.color.primary, null))
        }

        // List of OTHER currencies (pomijamy pierwszą - główną)
        expenses.drop(1).forEach { expense ->
            val itemView = layoutInflater.inflate(R.layout.item_modal_expense, expensesListContainer, false)

            // Currency code - SECONDARY
            val currencyCode = itemView.findViewById<TextView>(R.id.expenseName)
            currencyCode.text = expense.currency
            currencyCode.setTextColor(resources.getColor(R.color.secondary, null))

            // Amount - SECONDARY
            val amountView = itemView.findViewById<TextView>(R.id.expenseAmount)
            amountView.text = expense.formattedAmount
            amountView.setTextColor(resources.getColor(R.color.secondary, null))

            expensesListContainer.addView(itemView)
        }

        return body
    }
}