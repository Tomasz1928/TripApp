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
import com.example.tripapp2.data.model.SettlementBreakdownType
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment
import com.example.tripapp2.ui.tripdetails.costs.ExpenseDetailUiModel
import com.example.tripapp2.ui.tripdetails.costs.ShareItemUiModel

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

    private fun createExpenseDetailBody(detail: ExpenseDetailUiModel): View {
        val body = layoutInflater.inflate(R.layout.modal_expense_detail, null, false)

        val isMultiCurrency = detail.currencyTrip != detail.currencyCost

        // Category Icon
        val iconRes = detail.categoryIconName
        val categoryIcon = body.findViewById<ImageView>(R.id.expenseCategoryIcon)
        if (iconRes != 0) {
            categoryIcon.setImageResource(iconRes)
            categoryIcon.visibility = View.VISIBLE
        } else {
            categoryIcon.visibility = View.GONE
        }

        // Header — nazwa
        body.findViewById<TextView>(R.id.expenseName).text = detail.name

        // Header — kwoty
        val mainAmountView = body.findViewById<TextView>(R.id.expenseAmountMain)
        val secondaryAmountView = body.findViewById<TextView>(R.id.expenseAmountSecondary)

        if (isMultiCurrency) {
            // Wydatek w INNEJ walucie niż trip:
            // Niebiesko (main) = kwota w walucie wydatku (cost currency)
            // Pomarańczowo (secondary) = kwota w walucie tripu (trip/main currency)
            mainAmountView.text = detail.formattedAmountCostCurrency
            secondaryAmountView.text = detail.formattedAmountTripCurrency
            secondaryAmountView.visibility = View.VISIBLE
        } else {
            // Wydatek w walucie tripu:
            // Niebiesko (main) = kwota w walucie tripu
            // Brak secondary
            mainAmountView.text = detail.formattedAmountTripCurrency
            secondaryAmountView.visibility = View.GONE
        }

        // Info
        body.findViewById<TextView>(R.id.expenseDescription).text = detail.description
        body.findViewById<TextView>(R.id.expenseDate).text = detail.date
        body.findViewById<TextView>(R.id.expensePayer).text = detail.payerName

        // Dynamiczne nagłówki kolumn
        setupDynamicHeaders(body, detail)

        // Shared With
        val sharedContainer = body.findViewById<LinearLayout>(R.id.sharedWithContainer)
        sharedContainer.removeAllViews()

        detail.sharedWith.forEach { share ->
            val shareRow = layoutInflater.inflate(R.layout.item_share_expensts, sharedContainer, false)

            shareRow.findViewById<TextView>(R.id.sharePerson).text = share.personName

            setupShareSettlementIcons(shareRow, share)

            val shareMainAmount = shareRow.findViewById<TextView>(R.id.shareAmountMain)
            val shareSecondaryAmount = shareRow.findViewById<TextView>(R.id.shareAmountSecondary)

            if (isMultiCurrency) {
                // Niebiesko = kwota w walucie wydatku (cost currency)
                shareMainAmount.text = share.formattedAmountCostCurrency
                // Pomarańczowo = kwota w walucie tripu
                shareSecondaryAmount.text = share.formattedAmountTripCurrency
                shareSecondaryAmount.visibility = View.VISIBLE
            } else {
                // Niebiesko = kwota w walucie tripu (jedyna waluta)
                shareMainAmount.text = share.formattedAmountTripCurrency
                shareSecondaryAmount.visibility = View.GONE
            }

            sharedContainer.addView(shareRow)
        }

        return body
    }

    private fun setupShareSettlementIcons(shareRow: View, share: ShareItemUiModel) {
        val mainIcon = shareRow.findViewById<ImageView>(R.id.costShareSettlementMainIcon)
        mainIcon.setImageResource(getBreakdownIconRes(share.dominantType))
        mainIcon.contentDescription = getBreakdownContentDescription(share.dominantType)
        mainIcon.imageTintList = null

        val secondaryContainer = shareRow.findViewById<LinearLayout>(R.id.costShareSettlementSecondaryIcons)
        secondaryContainer.removeAllViews()

        share.secondaryTypes.forEach { type ->
            val sizePx = (16 * resources.displayMetrics.density).toInt()
            val marginPx = (2 * resources.displayMetrics.density).toInt()

            val smallIcon = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginStart = marginPx
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(getBreakdownIconRes(type))
                contentDescription = getBreakdownContentDescription(type)
            }
            secondaryContainer.addView(smallIcon)
        }
    }

    private fun getBreakdownIconRes(type: SettlementBreakdownType): Int {
        return when (type) {
            SettlementBreakdownType.SELF              -> R.drawable.ic_breakdown_self
            SettlementBreakdownType.MANUAL_BY_AMOUNT  -> R.drawable.ic_breakdown_manual_amount
            SettlementBreakdownType.MANUAL_BY_COSTS   -> R.drawable.ic_breakdown_manual_costs
            SettlementBreakdownType.AUTO_PREPAYMENT   -> R.drawable.ic_breakdown_auto_prepayment
            SettlementBreakdownType.AUTO_CROSS_SETTLE -> R.drawable.ic_breakdown_auto_cross
            SettlementBreakdownType.UNSETTLED         -> R.drawable.ic_breakdown_unsettled
        }
    }

    private fun getBreakdownContentDescription(type: SettlementBreakdownType): String {
        return when (type) {
            SettlementBreakdownType.SELF              -> getString(R.string.breakdown_self)
            SettlementBreakdownType.MANUAL_BY_AMOUNT  -> getString(R.string.breakdown_manual_amount)
            SettlementBreakdownType.MANUAL_BY_COSTS   -> getString(R.string.breakdown_manual_costs)
            SettlementBreakdownType.AUTO_PREPAYMENT   -> getString(R.string.breakdown_auto_prepayment)
            SettlementBreakdownType.AUTO_CROSS_SETTLE -> getString(R.string.breakdown_auto_cross)
            SettlementBreakdownType.UNSETTLED         -> getString(R.string.breakdown_unsettled)
        }
    }

    private fun setupDynamicHeaders(body: View, detail: ExpenseDetailUiModel) {
        val headerTripCurrency = body.findViewById<TextView>(R.id.headerTripCurrency)
        val headerCostCurrency = body.findViewById<TextView>(R.id.headerCostCurrency)

        if (detail.currencyTrip != detail.currencyCost) {
            // Multi-currency: pokaż obie kolumny
            // Main (niebieska kolumna) = waluta kosztu
            headerCostCurrency.text = detail.currencyCost
            // Secondary (pomarańczowa kolumna) = waluta tripu
            headerTripCurrency.text = detail.currencyTrip
            headerTripCurrency.visibility = View.VISIBLE
        } else {
            // Jedna waluta: pokaż tylko kolumnę z walutą tripu
            headerCostCurrency.text = detail.currencyTrip
            headerTripCurrency.visibility = View.GONE
        }
    }
}