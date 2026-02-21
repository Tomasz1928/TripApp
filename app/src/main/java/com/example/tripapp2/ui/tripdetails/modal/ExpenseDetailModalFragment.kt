package com.example.tripapp2.ui.tripdetails.modal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment
import com.example.tripapp2.ui.tripdetails.costs.ExpenseDetailUiModel

/**
 * Modal ze szczegółami wydatku.
 * ZMIGOWANY na BaseModalFragment — usunięto zduplikowany boilerplate.
 *
 * Logika biznesowa (createExpenseDetailBody) — 1:1 z oryginałem.
 */
class ExpenseDetailModalFragment : BaseModalFragment() {

    private var expenseDetail: ExpenseDetailUiModel? = null

    companion object {
        fun newInstance(detail: ExpenseDetailUiModel): ExpenseDetailModalFragment {
            return ExpenseDetailModalFragment().apply {
                expenseDetail = detail
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        expenseDetail?.let { setModalTitle(it.name) } ?: dismiss()
    }

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View? {
        val detail = expenseDetail ?: return null
        return createExpenseDetailBody(detail)
    }

    // ==========================================
    // LOGIKA BIZNESOWA — 1:1 Z ORYGINAŁEM
    // ==========================================

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

        // Info — z labelkami z string resources
        val descriptionLabel = getString(R.string.expense_detail_description_label)
        body.findViewById<TextView>(R.id.expenseDescription).text = "$descriptionLabel\n${detail.description}"

        val dateLabel = getString(R.string.expense_detail_date_label)
        body.findViewById<TextView>(R.id.expenseDate).text = "$dateLabel ${detail.date}"

        val payerLabel = getString(R.string.expense_detail_payer_label)
        body.findViewById<TextView>(R.id.expensePayer).text = "$payerLabel ${detail.payerName}"

        // Dynamiczne nagłówki kolumn
        setupDynamicHeaders(body, detail)

        // Shared With
        val sharedContainer = body.findViewById<LinearLayout>(R.id.sharedWithContainer)
        sharedContainer.removeAllViews()

        detail.sharedWith.forEach { share ->
            val shareRow = layoutInflater.inflate(R.layout.item_share_expensts, sharedContainer, false)

            shareRow.findViewById<TextView>(R.id.sharePerson).text = share.personName
            val settlementIcon = shareRow.findViewById<ImageView>(R.id.shareSettlement)
            if (share.isSettlement) {
                settlementIcon.setImageResource(R.drawable.ic_success)
                settlementIcon.imageTintList = ContextCompat.getColorStateList(requireContext(),
                    R.color.success
                )
            } else {
                settlementIcon.setImageResource(R.drawable.ic_cross)
                settlementIcon.imageTintList = ContextCompat.getColorStateList(requireContext(),
                    R.color.error
                )
            }

            shareRow.findViewById<TextView>(R.id.shareAmountMain).text = share.formattedAmountCostCurrency

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

    private fun setupDynamicHeaders(body: View, detail: ExpenseDetailUiModel) {
        val headerTripCurrency = body.findViewById<TextView>(R.id.headerTripCurrency)
        val headerCostCurrency = body.findViewById<TextView>(R.id.headerCostCurrency)

        headerCostCurrency.text = detail.currencyCost

        if (detail.currencyTrip != detail.currencyCost) {
            headerTripCurrency.text = detail.currencyTrip
            headerTripCurrency.visibility = View.VISIBLE
        } else {
            headerTripCurrency.visibility = View.GONE
        }
    }
}