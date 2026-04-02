package com.example.tripapp2.ui.common.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tripapp2.R

/**
 * Helper do budowania avatar stacków (nakładające się kółka z inicjałami)
 *
 * Używany w:
 * - TripParticipantsFragment (summary bar)
 * - AddExpenseFragment (split row)
 * - TripSettlementsFragment (opcjonalnie)
 *
 * Użycie:
 * ```
 * AvatarStackHelper.buildAvatarStack(
 *     context = requireContext(),
 *     container = avatarStackContainer,
 *     names = participants.map { it.nickname },
 *     maxVisible = 5,
 *     sizeDp = 28
 * )
 * ```
 */
object AvatarStackHelper {

    private val AVATAR_COLORS = intArrayOf(
        R.color.primary,
        R.color.category_food,
        R.color.category_transport,
        R.color.category_shopping,
        R.color.category_entertainment,
        R.color.category_other
    )

    /**
     * Buduje avatar stack w podanym kontenerze LinearLayout
     *
     * @param context Context
     * @param container LinearLayout (orientation=horizontal) do wypełnienia
     * @param names Lista imion/nicków — inicjały brane z first 2 chars
     * @param maxVisible Maksymalna liczba widocznych awatarów (reszta → "+N")
     * @param sizeDp Rozmiar pojedynczego awatara w dp
     */
    fun buildAvatarStack(
        context: Context,
        container: LinearLayout,
        names: List<String>,
        maxVisible: Int = 5,
        sizeDp: Int = 28
    ) {
        container.removeAllViews()

        if (names.isEmpty()) return

        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val overlapPx = (-6 * density).toInt()
        val borderPx = (2 * density).toInt()

        val visibleNames = names.take(maxVisible)

        visibleNames.forEachIndexed { index, name ->
            val avatar = createAvatarView(
                context = context,
                initials = name.take(2).uppercase(),
                colorRes = AVATAR_COLORS[index % AVATAR_COLORS.size],
                sizePx = sizePx,
                borderPx = borderPx,
                marginStartPx = if (index > 0) overlapPx else 0
            )
            container.addView(avatar)
        }

        // "+N" overflow indicator
        if (names.size > maxVisible) {
            val moreLabel = createOverflowView(
                context = context,
                count = names.size - maxVisible,
                sizePx = sizePx,
                borderPx = borderPx,
                marginStartPx = overlapPx
            )
            container.addView(moreLabel)
        }
    }

    private fun createAvatarView(
        context: Context,
        initials: String,
        colorRes: Int,
        sizePx: Int,
        borderPx: Int,
        marginStartPx: Int
    ): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                if (marginStartPx != 0) marginStart = marginStartPx
            }
            gravity = Gravity.CENTER
            textSize = (sizePx / context.resources.displayMetrics.density * 0.28f)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            text = initials
            elevation = 1f

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, colorRes))
                setStroke(borderPx, Color.WHITE)
            }
        }
    }

    private fun createOverflowView(
        context: Context,
        count: Int,
        sizePx: Int,
        borderPx: Int,
        marginStartPx: Int
    ): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                marginStart = marginStartPx
            }
            gravity = Gravity.CENTER
            textSize = (sizePx / context.resources.displayMetrics.density * 0.28f)
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            typeface = Typeface.DEFAULT_BOLD
            text = "+$count"
            elevation = 1f

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, R.color.background))
                setStroke(borderPx, ContextCompat.getColor(context, R.color.divider))
            }
        }
    }
}
