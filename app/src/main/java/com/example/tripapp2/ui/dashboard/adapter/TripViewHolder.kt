package com.example.tripapp2.ui.dashboard.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.extension.dp
import com.example.tripapp2.ui.common.widget.PieChartView
import com.example.tripapp2.ui.dashboard.TripUiModel

/**
 * ViewHolder dla karty wycieczki — Propozycja C
 *
 * Zmiany vs oryginał:
 * - tripTotal jako hero amount (duży, centralny)
 * - tripMyCost — nowe pole pod kwotą
 * - PieChart mniejszy (64dp) w szarym kontenerze obok legendy
 * - 3 inline buttony (2 szare + 1 primary) zamiast 3 stacked primary
 */
class TripViewHolder(
    itemView: View,
    private val onTripClick: (TripUiModel) -> Unit,
    private val onCostDetailsClick: (TripUiModel) -> Unit,
    private val onAddCostClick: (TripUiModel) -> Unit,
    private val onRefreshClick: () -> Unit = {}
) : RecyclerView.ViewHolder(itemView) {

    private val title: TextView = itemView.findViewById(R.id.tripTitle)
    private val date: TextView = itemView.findViewById(R.id.tripDate)
    private val total: TextView = itemView.findViewById(R.id.tripTotal)
    private val myCost: TextView = itemView.findViewById(R.id.tripMyCost)
    private val chart: PieChartView = itemView.findViewById(R.id.pieChart)
    private val legendContainer: LinearLayout = itemView.findViewById(R.id.legendContainer)
    private val detailsBtn: Button = itemView.findViewById(R.id.detailsBtn)
    private val costDetailsBtn: Button = itemView.findViewById(R.id.costDetailsBtn)
    private val addCostBtn: View = itemView.findViewById(R.id.addCostBtn)
    private val refreshBtn: ImageView = itemView.findViewById(R.id.refreshBtn)

    companion object {
        private const val CARD_WIDTH_RATIO = 0.85f
        private const val LEGEND_ITEM_MARGIN_DP = 4
        private const val DOT_SIZE_DP = 8
        private const val DOT_MARGIN_DP = 6
        private const val TEXT_SIZE = 11f
    }

    fun bind(trip: TripUiModel) {
        // Podstawowe dane
        title.text = trip.title
        date.text = trip.dateRange
        total.text = trip.totalFormatted

        // Mój koszt
        myCost.text = "Mój koszt: ${trip.myCostFormatted}"

        // Ustaw szerokość karty
        setCardWidth()

        // PieChart
        chart.setData(trip.categories.map { category ->
            com.example.tripapp2.ui.dashboard.PieCategory(
                label = category.label,
                value = category.value,
                color = category.color
            )
        })

        // Legenda (kompaktowa, 2 kolumny)
        setupLegend(trip)

        // Click listeners
        detailsBtn.setOnClickListener { onTripClick(trip) }
        costDetailsBtn.setOnClickListener { onCostDetailsClick(trip) }
        addCostBtn.setOnClickListener { onAddCostClick(trip) }
        refreshBtn.setOnClickListener { onRefreshClick() }
    }

    private fun setCardWidth() {
        val displayMetrics = itemView.context.resources.displayMetrics
        itemView.layoutParams.width = (displayMetrics.widthPixels * CARD_WIDTH_RATIO).toInt()
    }

    private fun setupLegend(trip: TripUiModel) {
        legendContainer.removeAllViews()
        legendContainer.isNestedScrollingEnabled = false

        trip.categories.forEach { category ->
            val item = createLegendItem(category)
            legendContainer.addView(item)
        }
    }

    private fun createLegendItem(category: com.example.tripapp2.ui.dashboard.PieCategoryUiModel): LinearLayout {
        val item = LinearLayout(itemView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = LEGEND_ITEM_MARGIN_DP.dp
            }
        }

        // Kolorowa kropka
        val dot = View(itemView.context).apply {
            layoutParams = LinearLayout.LayoutParams(DOT_SIZE_DP.dp, DOT_SIZE_DP.dp).apply {
                rightMargin = DOT_MARGIN_DP.dp
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(category.color)
            }
        }

        // Nazwa + wartość
        val textView = TextView(itemView.context).apply {
            text = "${category.label} ${category.formattedValue}"
            setTextColor(itemView.context.getColor(R.color.text_secondary))
            textSize = TEXT_SIZE
        }

        item.addView(dot)
        item.addView(textView)

        return item
    }
}

/**
 * ViewHolder dla placeholder (gdy brak wycieczek)
 */
class PlaceholderViewHolder(
    itemView: View,
    private val onJoinClick: () -> Unit,
    private val onCreateClick: () -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val message: TextView = itemView.findViewById(R.id.placeholderMessage)
    private val joinBtn: Button = itemView.findViewById(R.id.joinBtn)
    private val createBtn: Button = itemView.findViewById(R.id.createBtn)

    fun bind() {
        val displayMetrics = itemView.context.resources.displayMetrics
        itemView.layoutParams.width = (displayMetrics.widthPixels * 0.85).toInt()

        message.text = "Brak podróży"

        joinBtn.setOnClickListener { onJoinClick() }
        createBtn.setOnClickListener { onCreateClick() }
    }
}