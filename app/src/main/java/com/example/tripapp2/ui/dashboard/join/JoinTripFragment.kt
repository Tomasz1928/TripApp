package com.example.tripapp2.ui.dashboard.join

import android.view.View
import android.widget.Button
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.KeyboardAwareFragment
import com.example.tripapp2.ui.common.base.NavigationCommand
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class JoinTripFragment : KeyboardAwareFragment<JoinTripViewModel>(R.layout.fragment_join_trip) {

    override val viewModel: JoinTripViewModel by viewModels()

    private lateinit var tripJoinLayout: TextInputLayout
    private lateinit var tripJoinInput: TextInputEditText
    private lateinit var joinButton: Button

    override fun initKeyboardViews(view: View) {
        keyboardScrollView = view.findViewById(R.id.scrollViewJoinTrip)
        keyboardBottomNav = (activity as? DashboardActivity)?.dashboardBottomNav
    }

    override fun setupUI() {
        initializeViews()
        setupInputListeners()
    }

    override fun setupCustomObservers() {
        // ✅ ZMIANA: Konwertuj Int? na String?
        viewModel.accessCodeError.observe(viewLifecycleOwner) { errorResId ->
            tripJoinLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.tripJoinedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                showMessage(message)
            }
        }
    }

    private fun initializeViews() {
        val view = requireView()

        tripJoinLayout = view.findViewById(R.id.TripJoinLayout)
        tripJoinInput = view.findViewById(R.id.TripJoinInput)
        joinButton = view.findViewById(R.id.JoinTripBtn)
    }

    private fun setupInputListeners() {
        tripJoinInput.addTextChangedListener { text ->
            viewModel.onAccessCodeChanged(text.toString())
        }

        joinButton.setOnClickListener {
            viewModel.onJoinTripClicked()
        }
    }

    override fun handleNavigation(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.ToTripDetails -> {
                (activity as? DashboardActivity)?.openTripDetails(command.tripId)
            }

            is NavigationCommand.ToDashboard -> {
                (activity as? DashboardActivity)?.apply {
                    showDashboardFragment(R.id.menu_dashboard)
                    dashboardBottomNav.selectedItemId = R.id.menu_dashboard
                }
            }
            else -> super.handleNavigation(command)
        }
    }

    override fun onLoadingStateChanged(isLoading: Boolean) {
        joinButton.isEnabled = !isLoading
        // ✅ ZMIANA: Użyj getString() zamiast .toString()
        joinButton.text = if (isLoading) {
            getString(R.string.join_trip_button_loading)
        } else {
            getString(R.string.join_trip_button)
        }
    }
}