package com.example.tripapp2.ui.tripdetails.settlements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R
import com.google.android.material.tabs.TabLayout

/**
 * Modal szczegółów rozliczenia z uczestnikiem
 *
 * Tab 1 - Podsumowanie:
 *   - Relacja wszystkich kosztów (allRelatedAmount)
 *   - Pozostało do rozliczenia (leftForSettled)
 *
 * Tab 2 - Koszty:
 *   - Lista wydatków dotyczących relacji ja ↔ participant
 *   - Kolor kwoty: zielony (do mnie) / czerwony (ode mnie)
 *   - Ikona: ic_success zielony (rozliczone) / ic_cross czerwony (nierozliczone)
 *
 * Tab 3 - Zaliczki:
 *   - Placeholder (do implementacji)
 */
class SettlementDetailsModalFragment : DialogFragment() {

    private var detailsModel: SettlementDetailsUiModel? = null

    // Views
    private lateinit var closeButton: ImageView
    private lateinit var participantNickname: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var tabSummary: ScrollView
    private lateinit var tabCosts: LinearLayout
    private lateinit var tabPrepayment: LinearLayout
    private lateinit var allRelatedContainer: LinearLayout
    private lateinit var leftForSettledContainer: LinearLayout
    private lateinit var costsScrollView: ScrollView
    private lateinit var costsListContainer: LinearLayout
    private lateinit var costsEmptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_TripApp_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.modal_settlement_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model = detailsModel ?: run { dismiss(); return }

        initializeViews(view)
        setupTabLayout()
        populateHeader(model)
        populateSummaryTab(model)
        populateCostsTab(model)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ==========================================
    // INITIALIZATION
    // ==========================================

    private fun initializeViews(view: View) {
        closeButton = view.findViewById(R.id.closeButton)
        participantNickname = view.findViewById(R.id.participantNickname)
        tabLayout = view.findViewById(R.id.tabLayout)
        tabSummary = view.findViewById(R.id.tabSummary)
        tabCosts = view.findViewById(R.id.tabCosts)
        tabPrepayment = view.findViewById(R.id.tabPrepayment)
        allRelatedContainer = view.findViewById(R.id.allRelatedContainer)
        leftForSettledContainer = view.findViewById(R.id.leftForSettledContainer)
        costsScrollView = view.findViewById(R.id.costsScrollView)
        costsListContainer = view.findViewById(R.id.costsListContainer)
        costsEmptyState = view.findViewById(R.id.costsEmptyState)
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        tabSummary.visibility = View.VISIBLE
                        tabCosts.visibility = View.GONE
                        tabPrepayment.visibility = View.GONE
                    }
                    1 -> {
                        tabSummary.visibility = View.GONE
                        tabCosts.visibility = View.VISIBLE
                        tabPrepayment.visibility = View.GONE
                    }
                    2 -> {
                        tabSummary.visibility = View.GONE
                        tabCosts.visibility = View.GONE
                        tabPrepayment.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun populateHeader(model: SettlementDetailsUiModel) {
        participantNickname.text = model.participantNickname
        closeButton.setOnClickListener { dismiss() }
    }

    // ==========================================
    // TAB 1: PODSUMOWANIE
    // ==========================================

    private fun populateSummaryTab(model: SettlementDetailsUiModel) {
        allRelatedContainer.removeAllViews()
        leftForSettledContainer.removeAllViews()

        // Całkowite rozliczenie
        model.allRelatedRows.forEach { row ->
            allRelatedContainer.addView(createAmountRow(row))
        }

        // Pozostało do rozliczenia
        model.leftForSettledRows.forEach { row ->
            leftForSettledContainer.addView(createAmountRow(row))
        }
    }

    /**
     * Tworzy wiersz kwoty: [waluta]  [kwota]
     *
     * Kolory:
     * - amount > 0 → zielony (success) — participant jest mi winien
     * - amount < 0 → czerwony (error) — ja jestem winien
     * - amount == 0 → szary (text_secondary)
     */
    private fun createAmountRow(row: SettlementDetailAmountRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_settlement_detail_row, allRelatedContainer, false)

        val currencyLabel = view.findViewById<TextView>(R.id.settleDetailModalCurrencyLabel)
        val amountValue = view.findViewById<TextView>(R.id.settleDetailModalAmountValue)

        currencyLabel.text = row.formattedCurrency
        amountValue.text = row.formattedAmount

        // Kolor kwoty na podstawie znaku
        val colorRes = when {
            row.amount > 0.01f -> R.color.success
            row.amount < -0.01f -> R.color.error
            else -> R.color.text_secondary
        }
        amountValue.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        return view
    }

    // ==========================================
    // TAB 2: KOSZTY
    // ==========================================

    private fun populateCostsTab(model: SettlementDetailsUiModel) {
        costsListContainer.removeAllViews()

        if (model.costRows.isEmpty()) {
            costsEmptyState.visibility = View.VISIBLE
            costsScrollView.visibility = View.GONE
            return
        }

        costsEmptyState.visibility = View.GONE
        costsScrollView.visibility = View.VISIBLE

        model.costRows.forEach { costRow ->
            costsListContainer.addView(createCostRow(costRow))
        }

        // Ogranicz wysokość scrolla do ~10 wierszy (każdy ~48dp)
        costsScrollView.post {
            val maxHeightPx = (48 * 10 * resources.displayMetrics.density).toInt()
            if (costsListContainer.height > maxHeightPx) {
                costsScrollView.layoutParams = costsScrollView.layoutParams.apply {
                    height = maxHeightPx
                }
            }
        }
    }

    /**
     * Tworzy wiersz kosztu: [tytuł]  [kwota waluta]  [✓/✗]
     *
     * Kolor kwoty:
     * - isAmountPositive = true → zielony (pieniądze do mnie)
     * - isAmountPositive = false → czerwony (pieniądze ode mnie)
     *
     * Ikona rozliczenia:
     * - isSettled = true → ✓ zielony
     * - isSettled = false → ✗ czerwony
     */
    private fun createCostRow(costRow: SettlementDetailCostRow): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_settlement_detail_cost, costsListContainer, false)

        val costTitle = view.findViewById<TextView>(R.id.costTitle)
        val costAmount = view.findViewById<TextView>(R.id.costAmount)
        val settlementStatusIcon = view.findViewById<ImageView>(R.id.settlementStatusIcon)

        costTitle.text = costRow.expenseName
        costAmount.text = costRow.formattedAmount

        // Kolor kwoty na podstawie kierunku
        val amountColorRes = if (costRow.isAmountPositive) R.color.success else R.color.error
        costAmount.setTextColor(ContextCompat.getColor(requireContext(), amountColorRes))

        // Ikona rozliczenia
        if (costRow.isSettled) {
            settlementStatusIcon.setImageResource(R.drawable.ic_success)
            settlementStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
        } else {
            settlementStatusIcon.setImageResource(R.drawable.ic_cross)
            settlementStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
        }

        return view
    }

    // ==========================================
    // COMPANION
    // ==========================================

    companion object {
        fun newInstance(model: SettlementDetailsUiModel): SettlementDetailsModalFragment {
            return SettlementDetailsModalFragment().apply {
                this.detailsModel = model
            }
        }
    }
}