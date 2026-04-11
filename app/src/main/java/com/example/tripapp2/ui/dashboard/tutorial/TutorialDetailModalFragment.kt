package com.example.tripapp2.ui.dashboard.tutorial

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment

/**
 * Tematy samouczka
 */
enum class TutorialTopic {
    CREATE_TRIP,
    JOIN_TRIP,
    ADD_EXPENSE,
    SPLIT_COSTS,
    FILTERS,
    SETTLEMENTS,
    PREPAYMENTS,
    ICONS,
    PARTICIPANTS,
    NAVIGATION
}

/**
 * Modal ze szczegółami wybranego tematu samouczka.
 * Rozszerza BaseModalFragment — spójny wygląd z resztą aplikacji.
 */
class TutorialDetailModalFragment : BaseModalFragment() {

    private var topic: TutorialTopic = TutorialTopic.CREATE_TRIP

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val content = getTopicContent(topic)
        setModalTitle(content.title)
        if (content.subtitle.isNotEmpty()) {
            setModalSubtitle(content.subtitle)
        }
    }

    override fun onCreateBodyView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View {
        val content = getTopicContent(topic)
        val context = requireContext()

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Opis główny
        if (content.description.isNotEmpty()) {
            val descView = TextView(context).apply {
                text = content.description
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 13f
                lineHeight = (18 * resources.displayMetrics.density).toInt()
                setPadding(0, 0, 0, dpToPx(12))
            }
            body.addView(descView)
        }

        // Kroki (numerowane)
        content.steps.forEachIndexed { index, stepText ->
            val stepRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                setPadding(0, 0, 0, dpToPx(10))
            }

            // Numer kroku (kółko)
            val numView = TextView(context).apply {
                val sizePx = dpToPx(22)
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginEnd = dpToPx(10)
                    topMargin = dpToPx(1)
                }
                gravity = Gravity.CENTER
                text = "${index + 1}"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_on_primary))
                setBackgroundResource(R.drawable.bg_avatar_circle_primary)
            }
            stepRow.addView(numView)

            // Tekst kroku
            val textView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = stepText
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 13f
                lineHeight = (18 * resources.displayMetrics.density).toInt()
            }
            stepRow.addView(textView)

            body.addView(stepRow)
        }

        // Sekcja ikon (tylko dla ICONS topic)
        if (topic == TutorialTopic.ICONS) {
            addIconLegend(body)
        }

        // Uwaga końcowa
        if (content.note.isNotEmpty()) {
            val noteView = TextView(context).apply {
                text = content.note
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 12f
                lineHeight = (17 * resources.displayMetrics.density).toInt()
                setPadding(0, dpToPx(8), 0, 0)
            }
            body.addView(noteView)
        }

        return body
    }

    private fun addIconLegend(body: LinearLayout) {
        val context = requireContext()

        data class IconItem(val iconRes: Int, val name: String, val desc: String)

        val icons = listOf(
            IconItem(R.drawable.ic_breakdown_self, "Własny koszt (SELF)", "Sam płaciłeś i sam uczestniczysz — nie trzeba rozliczać"),
            IconItem(R.drawable.ic_breakdown_unsettled, "Nierozliczony", "Koszt jeszcze nie został rozliczony — czeka na akcję"),
            IconItem(R.drawable.ic_breakdown_manual_amount, "Rozliczony kwotą", "Rozliczony ręcznie przez wpisanie kwoty"),
            IconItem(R.drawable.ic_breakdown_manual_costs, "Rozliczony kosztami", "Rozliczony przez zaznaczenie konkretnych wydatków"),
            IconItem(R.drawable.ic_breakdown_auto_prepayment, "Auto-rozliczony zaliczką", "System automatycznie rozliczył koszt na podstawie zaliczki"),
            IconItem(R.drawable.ic_breakdown_auto_cross, "Auto-rozliczony krzyżowo", "Wzajemne długi się zniosły — system rozliczył automatycznie")
        )

        icons.forEach { item ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                setBackgroundResource(R.drawable.bg_rounded_surface)
                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = dpToPx(8)
                layoutParams = params
            }

            // Ikona
            val icon = ImageView(context).apply {
                val sizePx = dpToPx(28)
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginEnd = dpToPx(10)
                }
                setImageResource(item.iconRes)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            row.addView(icon)

            // Tekst
            val textContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameView = TextView(context).apply {
                text = item.name
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            textContainer.addView(nameView)

            val descView = TextView(context).apply {
                text = item.desc
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(0, dpToPx(1), 0, 0)
                lineHeight = (15 * resources.displayMetrics.density).toInt()
            }
            textContainer.addView(descView)

            row.addView(textContainer)
            body.addView(row)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getTopicContent(topic: TutorialTopic): TopicContent {
        return when (topic) {
            TutorialTopic.CREATE_TRIP -> TopicContent(
                title = getString(R.string.tutorial_create_trip_title),
                subtitle = "",
                description = getString(R.string.tutorial_create_trip_desc),
                steps = listOf(
                    getString(R.string.tutorial_create_trip_step1),
                    getString(R.string.tutorial_create_trip_step2),
                    getString(R.string.tutorial_create_trip_step3),
                    getString(R.string.tutorial_create_trip_step4)
                ),
                note = ""
            )
            TutorialTopic.JOIN_TRIP -> TopicContent(
                title = getString(R.string.tutorial_join_trip_title),
                subtitle = "",
                description = getString(R.string.tutorial_join_trip_desc),
                steps = listOf(
                    getString(R.string.tutorial_join_trip_step1),
                    getString(R.string.tutorial_join_trip_step2),
                    getString(R.string.tutorial_join_trip_step3)
                ),
                note = ""
            )
            TutorialTopic.ADD_EXPENSE -> TopicContent(
                title = getString(R.string.tutorial_add_expense_title),
                subtitle = "",
                description = getString(R.string.tutorial_add_expense_desc),
                steps = listOf(
                    getString(R.string.tutorial_add_expense_step1),
                    getString(R.string.tutorial_add_expense_step2),
                    getString(R.string.tutorial_add_expense_step3),
                    getString(R.string.tutorial_add_expense_step4),
                    getString(R.string.tutorial_add_expense_step5),
                    getString(R.string.tutorial_add_expense_step6)
                ),
                note = getString(R.string.tutorial_add_expense_note)
            )
            TutorialTopic.SPLIT_COSTS -> TopicContent(
                title = getString(R.string.tutorial_split_costs_title),
                subtitle = "",
                description = getString(R.string.tutorial_split_costs_desc),
                steps = listOf(),
                note = getString(R.string.tutorial_split_costs_note)
            )
            TutorialTopic.FILTERS -> TopicContent(
                title = getString(R.string.tutorial_filters_title),
                subtitle = "",
                description = getString(R.string.tutorial_filters_desc),
                steps = listOf(),
                note = ""
            )
            TutorialTopic.SETTLEMENTS -> TopicContent(
                title = getString(R.string.tutorial_settlements_title),
                subtitle = "",
                description = getString(R.string.tutorial_settlements_desc),
                steps = listOf(),
                note = ""
            )
            TutorialTopic.PREPAYMENTS -> TopicContent(
                title = getString(R.string.tutorial_prepayments_title),
                subtitle = "",
                description = getString(R.string.tutorial_prepayments_desc),
                steps = listOf(),
                note = getString(R.string.tutorial_prepayments_note)
            )
            TutorialTopic.ICONS -> TopicContent(
                title = getString(R.string.tutorial_icons_title),
                subtitle = "",
                description = getString(R.string.tutorial_icons_desc),
                steps = listOf(),
                note = getString(R.string.tutorial_icons_note)
            )
            TutorialTopic.PARTICIPANTS -> TopicContent(
                title = getString(R.string.tutorial_participants_title),
                subtitle = "",
                description = getString(R.string.tutorial_participants_desc),
                steps = listOf(),
                note = getString(R.string.tutorial_participants_note)
            )
            TutorialTopic.NAVIGATION -> TopicContent(
                title = getString(R.string.tutorial_navigation_title),
                subtitle = "",
                description = getString(R.string.tutorial_navigation_desc),
                steps = listOf(),
                note = ""
            )
        }
    }

    private data class TopicContent(
        val title: String,
        val subtitle: String,
        val description: String,
        val steps: List<String>,
        val note: String
    )

    companion object {
        fun newInstance(topic: TutorialTopic): TutorialDetailModalFragment {
            return TutorialDetailModalFragment().apply {
                this.topic = topic
            }
        }
    }
}