package com.example.tripapp2.ui.dashboard.tutorial

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.tripapp2.R
import com.example.tripapp2.ui.dashboard.DashboardActivity

/**
 * Fragment samouczka — "Jak korzystać z aplikacji"
 *
 * Wyświetla listę sekcji pomocy w stylu Settings.
 * Kliknięcie w wiersz otwiera TutorialDetailModalFragment
 * z pełnym opisem danej funkcji.
 */
class TutorialFragment : Fragment(R.layout.fragment_tutorial) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.backButton).setOnClickListener {
            (activity as? DashboardActivity)?.closeTutorial()
        }

        // Row click listeners — każdy otwiera modal ze szczegółami
        setupRow(view, R.id.rowCreateTrip, TutorialTopic.CREATE_TRIP)
        setupRow(view, R.id.rowJoinTrip, TutorialTopic.JOIN_TRIP)
        setupRow(view, R.id.rowAddExpense, TutorialTopic.ADD_EXPENSE)
        setupRow(view, R.id.rowSplitCosts, TutorialTopic.SPLIT_COSTS)
        setupRow(view, R.id.rowFilters, TutorialTopic.FILTERS)
        setupRow(view, R.id.rowSettlements, TutorialTopic.SETTLEMENTS)
        setupRow(view, R.id.rowPrepayments, TutorialTopic.PREPAYMENTS)
        setupRow(view, R.id.rowIcons, TutorialTopic.ICONS)
        setupRow(view, R.id.rowParticipants, TutorialTopic.PARTICIPANTS)
        setupRow(view, R.id.rowNavigation, TutorialTopic.NAVIGATION)
    }

    private fun setupRow(view: View, rowId: Int, topic: TutorialTopic) {
        view.findViewById<View>(rowId).setOnClickListener {
            TutorialDetailModalFragment.newInstance(topic)
                .show(parentFragmentManager, "tutorial_detail_${topic.name}")
        }
    }

    companion object {
        fun newInstance() = TutorialFragment()
    }
}