package com.example.tripapp2.ui.tripdetails.costs.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.tripapp2.R
import com.example.tripapp2.ui.tripdetails.costs.ExpenseDetailUiModel
import com.example.tripapp2.ui.tripdetails.costs.ExpenseFilter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Adapter do tworzenia view'ów wydatków
 * Używany zarówno w RecyclerView jak i LinearLayout
 */
class ExpenseAdapter(
    private val onExpenseClick: (ExpenseDetailUiModel) -> Unit,
    private val onEditExpense: ((ExpenseDetailUiModel) -> Unit)? = null,
    private val onDeleteExpense: ((ExpenseDetailUiModel) -> Unit)? = null,
    private val currentFilter: ExpenseFilter = ExpenseFilter.ALL
) {

    /**
     * Tworzy view wydatku z dynamicznym wyświetlaniem walut
     * oraz przyciskami akcji dla PAID_BY_ME
     */
    fun createExpenseView(parent: ViewGroup, expense: ExpenseDetailUiModel): View {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_expense, parent, false) as MaterialCardView

        // Ikonka kategorii
        val categoryIcon = view.findViewById<ImageView>(R.id.expenseCategoryIcon)
        if (expense.categoryIconName != 0) {
            categoryIcon.setImageResource(expense.categoryIconName)
            categoryIcon.visibility = View.VISIBLE
        } else {
            categoryIcon.visibility = View.GONE
        }

        // Nazwa wydatku - ZAWSZE widoczna
        view.findViewById<TextView>(R.id.expenseName).text = expense.name

        // Płatnik - ZAWSZE widoczny
        view.findViewById<TextView>(R.id.expensePayer).text = expense.payerName

        // Kwota główna (cost currency) - ZAWSZE widoczna
        view.findViewById<TextView>(R.id.expenseAmount).text = expense.formattedAmountCostCurrency

        // Kwota drugorzędna (trip currency) - tylko gdy INNA niż cost currency i są dane
        val secondaryAmount = view.findViewById<TextView>(R.id.expenseAmountSecondary)
        if (expense.currencyTrip != expense.currencyCost && expense.formattedAmountTripCurrency.isNotEmpty()) {
            secondaryAmount.text = expense.formattedAmountTripCurrency
            secondaryAmount.visibility = View.VISIBLE
        } else {
            secondaryAmount.visibility = View.GONE
        }

        val actionsContainer = view.findViewById<LinearLayout>(R.id.actionsContainer)
        val editButton = view.findViewById<MaterialButton>(R.id.editButton)
        val deleteButton = view.findViewById<MaterialButton>(R.id.deleteButton)

        if (currentFilter == ExpenseFilter.PAID_BY_ME) {
            actionsContainer.visibility = View.VISIBLE

            editButton.setOnClickListener {
                onEditExpense?.invoke(expense)
            }

            deleteButton.setOnClickListener {
                onDeleteExpense?.invoke(expense)
            }
        } else {
            actionsContainer.visibility = View.GONE
        }

        view.setOnClickListener {
            onExpenseClick(expense)
        }

        return view
    }
}