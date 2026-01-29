package com.example.tripapp2.ui.common.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.tripapp2.ui.dashboard.PieCategory

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var categories: List<PieCategory> = emptyList()

    // DODANE: Stałe zamiast magic numbers
    companion object {
        private const val STROKE_RATIO = 0.25f   // 25% promienia
        private const val RADIUS_RATIO = 0.8f    // 80% max wymiaru
        private const val START_ANGLE = -90f
    }

    fun setData(data: List<PieCategory>) {
        categories = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (categories.isEmpty()) return

        val total = categories.sumOf { it.value.toDouble() }.toFloat()
        val widthF = width.toFloat()
        val heightF = height.toFloat()

        // promień koła (mniejsze)
        val radius = minOf(widthF, heightF) / 2 * RADIUS_RATIO
        val cx = widthF / 2
        val cy = heightF / 2
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // grubość pierścienia
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = radius * STROKE_RATIO

        var startAngle = START_ANGLE
        for (cat in categories) {
            val sweep = 360f * (cat.value / total)
            paint.color = cat.color
            canvas.drawArc(rect, startAngle, sweep, false, paint)
            startAngle += sweep
        }
    }
}