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
import com.example.tripapp2.ui.tripdetails.costs.ExpenseDetailUiModel

/**
 * Modal ze szczegółami wydatku
 */
class ExpenseDetailModalFragment : DialogFragment() {

    private var expenseDetail: ExpenseDetailUiModel? = null

    companion object {
        private const val ARG_EXPENSE = "expense_detail"

        fun newInstance(detail: ExpenseDetailUiModel): ExpenseDetailModalFragment {
            return ExpenseDetailModalFragment().apply {
                expenseDetail = detail
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

        val detail = expenseDetail ?: return

        // Setup modal
        view.findViewById<TextView>(R.id.modalTitle).text = detail.name
        view.findViewById<ImageView>(R.id.closeButton).setOnClickListener { dismiss() }

        // Setup body
        val bodyContainer = view.findViewById<ViewGroup>(R.id.modalBodyContainer)
        val bodyView = createExpenseDetailBody(detail)
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

    private fun createExpenseDetailBody(detail: ExpenseDetailUiModel): View {
        val body = layoutInflater.inflate(R.layout.modal_expense_detail, null, false)

        // Category Icon
        val iconRes = detail.categoryIconName

        val categoryIcon = body.findViewById<ImageView>(R.id.expenseCategoryIcon)
        if (iconRes != 0) {
            categoryIcon.setImageResource(iconRes)
            categoryIcon.visibility = View.VISIBLE
        } else {
            categoryIcon.visibility = View.GONE
        }

        // Header - kwota główna (cost currency) ZAWSZE widoczna
        body.findViewById<TextView>(R.id.expenseName).text = detail.name
        body.findViewById<TextView>(R.id.expenseAmountMain).text = detail.formattedAmountCostCurrency

        // Header - kwota drugorzędna (trip currency) - tylko gdy INNA niż cost currency i niepusta
        val secondaryAmountView = body.findViewById<TextView>(R.id.expenseAmountSecondary)
        if (detail.currencyTrip != detail.currencyCost && detail.formattedAmountTripCurrency.isNotEmpty()) {
            secondaryAmountView.text = detail.formattedAmountTripCurrency
            secondaryAmountView.visibility = View.VISIBLE
        } else {
            secondaryAmountView.visibility = View.GONE
        }

        // Info
        body.findViewById<TextView>(R.id.expenseDescription).text = "Opis: \n${detail.description}"
        body.findViewById<TextView>(R.id.expenseDate).text = "Data: ${detail.date}"
        body.findViewById<TextView>(R.id.expensePayer).text = "Płacił: ${detail.payerName}"

        // Dynamiczne nagłówki kolumn
        setupDynamicHeaders(body, detail)

        // Shared With
        val sharedContainer = body.findViewById<LinearLayout>(R.id.sharedWithContainer)
        sharedContainer.removeAllViews()

        detail.sharedWith.forEach { share ->
            val shareRow = layoutInflater.inflate(R.layout.item_share_expensts, sharedContainer, false)

            // Imię osoby - ZAWSZE widoczne
            shareRow.findViewById<TextView>(R.id.sharePerson).text = share.personName

            // Kwota w cost currency - ZAWSZE widoczna
            shareRow.findViewById<TextView>(R.id.shareAmountMain).text = share.formattedAmountCostCurrency

            // Kwota w trip currency - tylko gdy INNA niż cost currency i są dane
            val amountSecondaryView = shareRow.findViewById<TextView>(R.id.shareAmountSecondary)
            if (detail.currencyTrip != detail.currencyCost && share.formattedAmountTripCurrency.isNotEmpty()) {
                amountSecondaryView.text = share.formattedAmountTripCurrency
                amountSecondaryView.visibility = View.VISIBLE
            } else {
                amountSecondaryView.visibility = View.GONE
            }

            sharedContainer.addView(shareRow)
        }

        return body
    }

    /**
     * Ustawia dynamiczne nagłówki kolumn w zależności od tego czy są dane w trip currency
     */
    private fun setupDynamicHeaders(body: View, detail: ExpenseDetailUiModel) {
        val headerCostCurrency = body.findViewById<TextView>(R.id.headerCostCurrency)
        val headerTripCurrency = body.findViewById<TextView>(R.id.headerTripCurrency)

        // Cost currency - ZAWSZE widoczna
        headerCostCurrency.text = detail.currencyCost
        headerCostCurrency.visibility = View.VISIBLE

        // Trip currency - widoczna gdy INNA niż cost currency i jakikolwiek wiersz ma dane
        val hasTripCurrencyData = detail.sharedWith.any { it.formattedAmountTripCurrency.isNotEmpty() }

        if (detail.currencyTrip != detail.currencyCost && hasTripCurrencyData) {
            headerTripCurrency.text = detail.currencyTrip
            headerTripCurrency.visibility = View.VISIBLE
        } else {
            headerTripCurrency.visibility = View.GONE
        }
    }
}