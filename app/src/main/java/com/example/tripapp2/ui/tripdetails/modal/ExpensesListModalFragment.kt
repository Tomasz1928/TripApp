package com.example.tripapp2.ui.tripdetails.modal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment
import com.example.tripapp2.ui.tripdetails.CurrencyExpenseUiModel

/**
 * Modal z listą wydatków w różnych walutach.
 * ZMIGOWANY na BaseModalFragment — usunięto zduplikowany boilerplate.
 *
 * Logika biznesowa (createExpensesBody) bez zmian.
 */
class ExpensesListModalFragment : BaseModalFragment() {

    private var expenses: List<CurrencyExpenseUiModel>? = null

    companion object {
        fun newInstance(expenses: List<CurrencyExpenseUiModel>): ExpensesListModalFragment {
            return ExpensesListModalFragment().apply {
                this.expenses = expenses
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setModalTitle(getString(R.string.modal_expenses_title))
    }

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View? {
        val expensesList = expenses ?: return null
        return createExpensesBody(expensesList)
    }

    // ==========================================
    // LOGIKA BIZNESOWA — BEZ ZMIAN
    // ==========================================

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

            val currencyCode = itemView.findViewById<TextView>(R.id.expenseName)
            currencyCode.text = expense.currency
            currencyCode.setTextColor(resources.getColor(R.color.secondary, null))

            val amountView = itemView.findViewById<TextView>(R.id.expenseAmount)
            amountView.text = expense.formattedAmount
            amountView.setTextColor(resources.getColor(R.color.secondary, null))

            expensesListContainer.addView(itemView)
        }

        return body
    }
}