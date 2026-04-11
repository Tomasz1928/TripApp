package com.example.tripapp2.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.tripapp2.R
import com.example.tripapp2.ui.tripdetails.costs.addexpense.AddExpenseFragment
import com.example.tripapp2.ui.dashboard.create.CreateTripFragment
import com.example.tripapp2.ui.dashboard.join.JoinTripFragment
import com.example.tripapp2.ui.tripdetails.costs.editexpense.EditExpenseFragment
import com.example.tripapp2.ui.tripdetails.costs.TripCostsFragment
import com.example.tripapp2.ui.tripdetails.TripDetailsFragment
import com.example.tripapp2.ui.tripdetails.participants.TripParticipantsFragment
import com.example.tripapp2.ui.tripdetails.settlements.TripSettlementsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.tripapp2.ui.common.TripNotificationManager
import com.example.tripapp2.data.repository.TripRepository
import androidx.lifecycle.lifecycleScope
import com.example.tripapp2.ui.common.extension.setupIconsInOriginalColor
import com.example.tripapp2.ui.dashboard.options.OptionsFragment
import com.example.tripapp2.ui.dashboard.tutorial.TutorialFragment

class DashboardActivity : AppCompatActivity() {

    lateinit var dashboardBottomNav: BottomNavigationView
    lateinit var tripBottomNav: BottomNavigationView
    private lateinit var notificationManager: TripNotificationManager

    private var currentTripId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        dashboardBottomNav = findViewById(R.id.dashboardBottomNav)
        tripBottomNav = findViewById(R.id.tripBottomNav)

        dashboardBottomNav.setupIconsInOriginalColor()
        tripBottomNav.setupIconsInOriginalColor()

        showDashboardFragment(R.id.menu_dashboard)
        dashboardBottomNav.selectedItemId = R.id.menu_dashboard

        dashboardBottomNav.setOnItemSelectedListener { item ->
            showDashboardFragment(item.itemId)
            true
        }

        tripBottomNav.setOnItemSelectedListener { item ->
            currentTripId?.let { tripId ->
                showTripFragment(item.itemId, tripId)
            }
            true
        }

        val rootView = findViewById<View>(android.R.id.content)
        notificationManager = TripNotificationManager(
            activity = this,
            rootView = rootView,
            lifecycleScope = lifecycleScope,
            repository = TripRepository.getInstance(),
        )
        notificationManager.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.stop()
    }

    // =====================================================
    // DASHBOARD FLOW
    // =====================================================
    fun showDashboardFragment(itemId: Int) {
        val fragmentTag = when (itemId) {
            R.id.menu_dashboard -> "dashboard"
            R.id.menu_add_trip -> "createTrip"
            R.id.menu_join_trip -> "joinTrip"
            R.id.menu_settings -> "options"
            else -> "dashboard"
        }

        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
            ?: when (itemId) {
                R.id.menu_dashboard -> DashboardFragment()
                R.id.menu_add_trip -> CreateTripFragment()
                R.id.menu_join_trip -> JoinTripFragment()
                R.id.menu_settings -> OptionsFragment()
                else -> DashboardFragment()
            }

        supportFragmentManager.beginTransaction()
            .replace(R.id.dashboardContainer, fragment, fragmentTag)
            .commit()
    }

    // =====================================================
    // TRIP DETAILS FLOW
    // =====================================================
    fun openTripDetails(tripId: String) {
        // Zapisz aktualny tripId
        currentTripId = tripId

        // Pokaż trip container i ukryj dashboard
        findViewById<View>(R.id.dashboardContainer).visibility = View.GONE
        findViewById<View>(R.id.tripContainer).visibility = View.VISIBLE
        dashboardBottomNav.visibility = View.GONE
        tripBottomNav.visibility = View.VISIBLE

        // Startowy fragment tripDetails
        showTripFragment(R.id.menu_overview, tripId)

        // Ustaw domyślny item
        tripBottomNav.selectedItemId = R.id.menu_overview
    }

    fun closeTripDetails() {
        currentTripId = null

        listOf("tripDetails", "addExpense", "tripCosts", "tripParticipants", "tripSettlements").forEach { tag ->
            supportFragmentManager.findFragmentByTag(tag)?.let { fragment ->
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNow()
            }
        }

        findViewById<View>(R.id.tripContainer).visibility = View.GONE
        tripBottomNav.visibility = View.GONE

        findViewById<View>(R.id.dashboardContainer).visibility = View.VISIBLE
        dashboardBottomNav.visibility = View.VISIBLE

        // Reset do dashboard
        showDashboardFragment(R.id.menu_dashboard)
        dashboardBottomNav.selectedItemId = R.id.menu_dashboard

        // NOWE — wymuś odświeżenie dashboardu z aktualnego cache
        val dashboardFragment = supportFragmentManager.findFragmentByTag("dashboard")
        if (dashboardFragment is DashboardFragment) {
            dashboardFragment.refreshFromCache()
        }
    }

    // =====================================================
// OTWÓRZ TRIP COSTS (lista kosztów) z dashboardu
// =====================================================
    fun openTripCosts(tripId: String) {
        currentTripId = tripId

        // Pokaż trip container i ukryj dashboard
        findViewById<View>(R.id.dashboardContainer).visibility = View.GONE
        findViewById<View>(R.id.tripContainer).visibility = View.VISIBLE
        dashboardBottomNav.visibility = View.GONE
        tripBottomNav.visibility = View.VISIBLE

        // Otwórz od razu zakładkę kosztów
        showTripFragment(R.id.menu_costs, tripId)
        tripBottomNav.selectedItemId = R.id.menu_costs
    }

    // =====================================================
// OTWÓRZ ADD EXPENSE (formularz dodawania) z dashboardu
// =====================================================
    fun openTripAddExpense(tripId: String) {
        currentTripId = tripId

        // Pokaż trip container i ukryj dashboard
        findViewById<View>(R.id.dashboardContainer).visibility = View.GONE
        findViewById<View>(R.id.tripContainer).visibility = View.VISIBLE
        dashboardBottomNav.visibility = View.GONE

        // Użyj istniejącej metody — ukrywa trip bottom nav i otwiera AddExpenseFragment
        showAddExpenseFromCosts(tripId)
    }

    // =====================================================
    // TRIP BOTTOM NAVIGATION FLOW
    // =====================================================
    private fun showTripFragment(itemId: Int, tripId: String) {
        val fragmentTag = when (itemId) {
            R.id.menu_overview -> "tripDetails"
            R.id.menu_costs -> "tripCosts"
            R.id.menu_participants -> "tripParticipants"
            else -> "tripDetails"
        }

        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
            ?: when (itemId) {
                R.id.menu_overview -> TripDetailsFragment.newInstance(tripId)
                R.id.menu_costs -> TripCostsFragment.newInstance(tripId)
                R.id.menu_participants -> TripParticipantsFragment.newInstance(tripId)
                else -> TripDetailsFragment.newInstance(tripId)
            }

        supportFragmentManager.beginTransaction()
            .replace(R.id.tripContainer, fragment, fragmentTag)
            .commit()
    }

    // =====================================================
    // ADD EXPENSE FLOW (bez bottom nav)
    // =====================================================

    fun showAddExpenseFromCosts(tripId: String) {
        tripBottomNav.visibility = View.GONE

        val fragment = AddExpenseFragment.newInstance(tripId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.tripContainer, fragment, "addExpense")
            .commit()
    }

    fun closeAddExpenseAndShowCosts(tripId: String) {
        tripBottomNav.visibility = View.VISIBLE
        showTripFragment(R.id.menu_costs, tripId)
        tripBottomNav.selectedItemId = R.id.menu_costs
    }

    // =====================================================
// EDIT EXPENSE FLOW (bez bottom nav)
// =====================================================

    fun showEditExpenseFromCosts(tripId: String, expenseId: String) {
        tripBottomNav.visibility = View.GONE

        val fragment = EditExpenseFragment.newInstance(tripId, expenseId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.tripContainer, fragment, "editExpense")
            .commit()
    }

    fun closeEditExpenseAndShowCosts(tripId: String) {
        tripBottomNav.visibility = View.VISIBLE
        showTripFragment(R.id.menu_costs, tripId)
        tripBottomNav.selectedItemId = R.id.menu_costs
    }

    // =====================================================
    // SETTLEMENTS FLOW (bez bottom nav)
    // =====================================================
    fun showSettlements(tripId: String) {
        // NAJPIERW schowaj bottom nav
        tripBottomNav.visibility = View.GONE

        // POTEM zmień fragment
        val fragment = TripSettlementsFragment.newInstance(tripId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.tripContainer, fragment, "tripSettlements")
            .commit()
    }

    // =====================================================
    // HELPER - powrót z settlements do trip details
    // =====================================================
    fun closeSettlements() {
        currentTripId?.let { tripId ->
            tripBottomNav.visibility = View.VISIBLE
            showTripFragment(R.id.menu_overview, tripId)
            tripBottomNav.selectedItemId = R.id.menu_overview
        }
    }
    // =====================================================
// TUTORIAL FLOW (bez bottom nav)
// =====================================================
    fun showTutorial() {
        dashboardBottomNav.visibility = View.GONE

        val fragment = TutorialFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.dashboardContainer, fragment, "tutorial")
            .commit()
    }

    fun closeTutorial() {
        dashboardBottomNav.visibility = View.VISIBLE
        showDashboardFragment(R.id.menu_settings)
        dashboardBottomNav.selectedItemId = R.id.menu_settings
    }

}