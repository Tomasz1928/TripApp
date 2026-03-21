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

/**
 * Modal ze szczegółami wydatku.
 * ZMIGOWANY na BaseModalFragment — usunięto zduplikowany boilerplate.
 *
 * ZMIENIONE: Ikonki settlement per split — zamiast ✓/✗ teraz breakdown icons
 * (identyczne jak w SettlementDetailsModalFragment)
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
    // LOGIKA BIZNESOWA — ZMIENIONE (breakdown icons)
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

        // Shared With — ZMIENIONE: breakdown icons zamiast ✓/✗
        val sharedContainer = body.findViewById<LinearLayout>(R.id.sharedWithContainer)
        sharedContainer.removeAllViews()

        detail.sharedWith.forEach { share ->
            val shareRow = layoutInflater.inflate(R.layout.item_share_expensts, sharedContainer, false)

            shareRow.findViewById<TextView>(R.id.sharePerson).text = share.personName

            // ZMIENIONE: breakdown icons zamiast prostego ✓/✗
            setupShareSettlementIcons(shareRow, share)

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

    /**
     * NOWE: Ustawia ikonki breakdown na wierszu share
     *
     * Duża ikona (24dp) = dominantType
     * Małe ikony (16dp) = secondaryTypes (dynamicznie dodawane)
     *
     * Ikonki są kolorowe — NIE stosujemy tint.
     */
    private fun setupShareSettlementIcons(shareRow: View, share: ShareItemUiModel) {
        // Ikona dominująca (24dp)
        val mainIcon = shareRow.findViewById<ImageView>(R.id.costShareSettlementMainIcon)
        mainIcon.setImageResource(getBreakdownIconRes(share.dominantType))
        mainIcon.contentDescription = getBreakdownContentDescription(share.dominantType)
        mainIcon.imageTintList = null  // Wyczyść tint — ikonki są kolorowe

        // Małe ikony secondary (16dp)
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

    /**
     * Zwraca drawable resource dla danego typu breakdown.
     * Identyczne mapowanie jak w SettlementDetailsModalFragment.
     */
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

    /**
     * Content description dla accessibility
     */
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

        headerCostCurrency.text = detail.currencyCost

        if (detail.currencyTrip != detail.currencyCost) {
            headerTripCurrency.text = detail.currencyTrip
            headerTripCurrency.visibility = View.VISIBLE
        } else {
            headerTripCurrency.visibility = View.GONE
        }
    }
}