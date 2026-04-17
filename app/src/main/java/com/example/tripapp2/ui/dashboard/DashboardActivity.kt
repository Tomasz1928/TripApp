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
import androidx.core.view.WindowCompat
import androidx.activity.OnBackPressedCallback
import com.example.tripapp2.ui.common.baseModals.ConfirmModalFragment
import com.example.tripapp2.ui.dashboard.mydata.MyDataFragment

class DashboardActivity : AppCompatActivity() {

    lateinit var dashboardBottomNav: BottomNavigationView
    lateinit var tripBottomNav: BottomNavigationView
    private lateinit var notificationManager: TripNotificationManager

    private var currentTripId: String? = null

    private var currentScreen: Screen = Screen.DASHBOARD

    enum class Screen {
        DASHBOARD,
        CREATE_TRIP,
        JOIN_TRIP,
        OPTIONS,
        TUTORIAL,
        TRIP_DETAILS,
        TRIP_COSTS,
        TRIP_PARTICIPANTS,
        TRIP_SETTLEMENTS,
        ADD_EXPENSE,
        EDIT_EXPENSE,
        MY_DATA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
        setupBackNavigation()

    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentScreen) {
                    // === DASHBOARD — dialog wyjścia ===
                    Screen.DASHBOARD -> {
                        showExitDialog()
                    }

                    // === Dashboard tabs → Dashboard ===
                    Screen.CREATE_TRIP,
                    Screen.JOIN_TRIP,
                    Screen.OPTIONS -> {
                        showDashboardFragment(R.id.menu_dashboard)
                        dashboardBottomNav.selectedItemId = R.id.menu_dashboard
                    }

                    // === Tutorial → Options ===
                    Screen.TUTORIAL -> {
                        closeTutorial()
                    }

                    Screen.MY_DATA -> closeMyData()

                    // === Trip Details → Dashboard ===
                    Screen.TRIP_DETAILS -> {
                        closeTripDetails()
                    }

                    // === Trip tabs → Trip Details ===
                    Screen.TRIP_COSTS,
                    Screen.TRIP_PARTICIPANTS -> {
                        currentTripId?.let { tripId ->
                            showTripFragment(R.id.menu_overview, tripId)
                            tripBottomNav.selectedItemId = R.id.menu_overview
                        }
                    }

                    // === Settlements → Trip Details ===
                    Screen.TRIP_SETTLEMENTS -> {
                        closeSettlements()
                    }

                    // === Add Expense → Costs ===
                    Screen.ADD_EXPENSE -> {
                        currentTripId?.let { tripId ->
                            closeAddExpenseAndShowCosts(tripId)
                        }
                    }

                    // === Edit Expense → Costs ===
                    Screen.EDIT_EXPENSE -> {
                        currentTripId?.let { tripId ->
                            closeEditExpenseAndShowCosts(tripId)
                        }
                    }
                }
            }
        })
    }

    private fun showExitDialog() {
        ConfirmModalFragment.newInstance(
            title = getString(R.string.exit_dialog_title),
            message = getString(R.string.exit_dialog_message),
            confirmText = getString(R.string.exit_dialog_confirm),
            onConfirm = {
                finish()
            }
        ).show(supportFragmentManager, "exit_confirm")
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.stop()
    }

    // =====================================================
    // DASHBOARD FLOW
    // =====================================================
    fun showDashboardFragment(itemId: Int) {
        currentScreen = when (itemId) {
            R.id.menu_dashboard -> Screen.DASHBOARD
            R.id.menu_add_trip -> Screen.CREATE_TRIP
            R.id.menu_join_trip -> Screen.JOIN_TRIP
            R.id.menu_settings -> Screen.OPTIONS
            else -> Screen.DASHBOARD
        }

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
        currentScreen = Screen.TRIP_DETAILS
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
        currentScreen = Screen.DASHBOARD
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
        currentScreen = Screen.TRIP_COSTS
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
        currentScreen = Screen.ADD_EXPENSE
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
        currentScreen = when (itemId) {
            R.id.menu_overview -> Screen.TRIP_DETAILS
            R.id.menu_costs -> Screen.TRIP_COSTS
            R.id.menu_participants -> Screen.TRIP_PARTICIPANTS
            else -> Screen.TRIP_DETAILS
        }

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
        currentScreen = Screen.ADD_EXPENSE
        tripBottomNav.visibility = View.GONE

        val fragment = AddExpenseFragment.newInstance(tripId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.tripContainer, fragment, "addExpense")
            .commit()
    }

    fun closeAddExpenseAndShowCosts(tripId: String) {
        currentScreen = Screen.TRIP_COSTS
        tripBottomNav.visibility = View.VISIBLE
        showTripFragment(R.id.menu_costs, tripId)
        tripBottomNav.selectedItemId = R.id.menu_costs
    }

    // =====================================================
// EDIT EXPENSE FLOW (bez bottom nav)
// =====================================================

    fun showEditExpenseFromCosts(tripId: String, expenseId: String) {
        currentScreen = Screen.EDIT_EXPENSE
        tripBottomNav.visibility = View.GONE

        val fragment = EditExpenseFragment.newInstance(tripId, expenseId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.tripContainer, fragment, "editExpense")
            .commit()
    }

    fun closeEditExpenseAndShowCosts(tripId: String) {
        currentScreen = Screen.TRIP_COSTS
        tripBottomNav.visibility = View.VISIBLE
        showTripFragment(R.id.menu_costs, tripId)
        tripBottomNav.selectedItemId = R.id.menu_costs
    }

    // =====================================================
    // SETTLEMENTS FLOW (bez bottom nav)
    // =====================================================
    fun showSettlements(tripId: String) {
        currentScreen = Screen.TRIP_SETTLEMENTS
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
        currentScreen = Screen.TRIP_DETAILS
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
        currentScreen = Screen.TUTORIAL
        dashboardBottomNav.visibility = View.GONE

        val fragment = TutorialFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.dashboardContainer, fragment, "tutorial")
            .commit()
    }

    fun closeTutorial() {
        currentScreen = Screen.OPTIONS
        dashboardBottomNav.visibility = View.VISIBLE
        showDashboardFragment(R.id.menu_settings)
        dashboardBottomNav.selectedItemId = R.id.menu_settings
    }


    fun showMyData() {
        currentScreen = Screen.MY_DATA
        dashboardBottomNav.visibility = View.GONE

        val fragment = MyDataFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.dashboardContainer, fragment, "myData")
            .commit()
    }

    fun closeMyData() {
        currentScreen = Screen.OPTIONS
        dashboardBottomNav.visibility = View.VISIBLE
        showDashboardFragment(R.id.menu_settings)
        dashboardBottomNav.selectedItemId = R.id.menu_settings
    }

}