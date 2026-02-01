package com.example.tripapp2.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.tripapp2.R
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.addexpense.AddExpenseFragment
import com.example.tripapp2.ui.common.setupIconsInOriginalColor
import com.example.tripapp2.ui.dashboard.create.CreateTripFragment
import com.example.tripapp2.ui.dashboard.join.JoinTripFragment
import com.example.tripapp2.ui.tripdetails.costs.TripCostsFragment
import com.example.tripapp2.ui.tripdetails.TripDetailsFragment
import com.example.tripapp2.ui.tripdetails.participants.TripParticipantsFragment
import com.example.tripapp2.ui.tripdetails.settlements.TripSettlementsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardActivity : AppCompatActivity() {

    lateinit var dashboardBottomNav: BottomNavigationView
    lateinit var tripBottomNav: BottomNavigationView

    private val tripRepository = TripRepository.getInstance()
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
    }

    // =====================================================
    // DASHBOARD FLOW
    // =====================================================
    fun showDashboardFragment(itemId: Int) {
        val fragmentTag = when (itemId) {
            R.id.menu_dashboard -> "dashboard"
            R.id.menu_add_trip -> "createTrip"
            R.id.menu_join_trip -> "joinTrip"
            else -> "dashboard"
        }

        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
            ?: when (itemId) {
                R.id.menu_dashboard -> DashboardFragment()
                R.id.menu_add_trip -> CreateTripFragment()
                R.id.menu_join_trip -> JoinTripFragment()
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
    }

    // =====================================================
    // TRIP BOTTOM NAVIGATION FLOW
    // =====================================================
    private fun showTripFragment(itemId: Int, tripId: String) {
        val fragmentTag = when (itemId) {
            R.id.menu_overview -> "tripDetails"
            R.id.menu_add_expense -> "addExpense"
            R.id.menu_costs -> "tripCosts"
            R.id.menu_participants -> "tripParticipants"
            else -> "tripDetails"
        }

        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
            ?: when (itemId) {
                R.id.menu_overview -> TripDetailsFragment.newInstance(tripId)
                R.id.menu_add_expense -> AddExpenseFragment.newInstance(tripId)
                R.id.menu_costs -> TripCostsFragment.newInstance(tripId)
                R.id.menu_participants -> TripParticipantsFragment.newInstance(tripId)
                else -> TripDetailsFragment.newInstance(tripId)
            }

        supportFragmentManager.beginTransaction()
            .replace(R.id.tripContainer, fragment, fragmentTag)
            .commit()
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
}