package com.example.tripapp2.ui.dashboard.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.extension.dp
import com.example.tripapp2.ui.common.widget.PieChartView
import com.example.tripapp2.ui.dashboard.TripUiModel

/**
 * ViewHolder dla karty wycieczki
 * Wydzielona logika bindowania dla czystości kodu
 */
class TripViewHolder(
    itemView: View,
    private val onTripClick: (TripUiModel) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val title: TextView = itemView.findViewById(R.id.tripTitle)
    private val date: TextView = itemView.findViewById(R.id.tripDate)
    private val total: TextView = itemView.findViewById(R.id.tripTotal)
    private val chart: PieChartView = itemView.findViewById(R.id.pieChart)
    private val legendContainer: LinearLayout = itemView.findViewById(R.id.legendContainer)
    private val detailsBtn: Button = itemView.findViewById(R.id.detailsBtn)

    companion object {
        private const val CARD_WIDTH_RATIO = 0.85f
        private const val LEGEND_ITEM_MARGIN_DP = 8
        private const val DOT_SIZE_DP = 12
        private const val DOT_MARGIN_DP = 8
        private const val TEXT_SIZE = 12f
    }

    fun bind(trip: TripUiModel) {
        // Podstawowe dane
        title.text = trip.title
        date.text = trip.dateRange
        total.text = trip.totalFormatted

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

        // Legenda
        setupLegend(trip)

        // Click listener
        detailsBtn.setOnClickListener {
            onTripClick(trip)
        }
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

        // Nazwa kategorii
        val nameView = TextView(itemView.context).apply {
            text = category.label
            setTextColor(Color.DKGRAY)
            textSize = TEXT_SIZE
        }

        // Wartość
        val valueView = TextView(itemView.context).apply {
            text = category.formattedValue
            setTextColor(Color.BLACK)
            textSize = TEXT_SIZE
        }

        // Lewa strona (kropka + nazwa)
        val leftContainer = LinearLayout(itemView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(dot)
            addView(nameView)
        }

        // Prawa strona (wartość)
        val rightContainer = LinearLayout(itemView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(valueView)
        }

        item.addView(leftContainer)
        item.addView(rightContainer)

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