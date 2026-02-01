package com.example.tripapp2.ui.dashboard

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.base.NavigationCommand
import com.example.tripapp2.ui.common.extension.hide
import com.example.tripapp2.ui.common.extension.show
import com.example.tripapp2.ui.dashboard.adapter.TripAdapter

class DashboardFragment : BaseFragment<DashboardViewModel>(R.layout.fragment_dashboard) {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var tripsRecycler: RecyclerView
    private lateinit var adapter: TripAdapter

    companion object {
        private const val CARD_WIDTH_RATIO = 0.85f
    }

    override fun setupUI() {
        setupRecyclerView()
    }

    override fun setupCustomObservers() {
        viewModel.dashboardState.observe(viewLifecycleOwner) { state ->
            handleDashboardState(state)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFromCache()
    }

    private fun setupRecyclerView() {
        tripsRecycler = requireView().findViewById(R.id.tripsRecycler)

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        tripsRecycler.layoutManager = layoutManager
        tripsRecycler.clipToPadding = false

        adapter = TripAdapter(
            onTripClick = { trip ->
                viewModel.onTripClicked(trip.id)
            },
            onJoinClick = {
                viewModel.onJoinTripClicked()
            },
            onCreateClick = {
                viewModel.onCreateTripClicked()
            }
        )
        tripsRecycler.adapter = adapter

        PagerSnapHelper().attachToRecyclerView(tripsRecycler)
        setupCardCentering()
    }

    private fun setupCardCentering() {
        tripsRecycler.post {
            val recyclerWidth = tripsRecycler.width
            val cardWidth = (recyclerWidth * CARD_WIDTH_RATIO).toInt()
            val sidePadding = (recyclerWidth - cardWidth) / 2
            tripsRecycler.setPadding(sidePadding, 0, sidePadding, 0)
        }
    }

    private fun handleDashboardState(state: DashboardState) {
        when (state) {
            is DashboardState.Loading -> {
                tripsRecycler.hide()
            }
            is DashboardState.Success -> {
                val uiModels = state.trips.map { it.toUiModel(requireContext()) }
                tripsRecycler.show()
                adapter.submitTrips(uiModels)
            }
            is DashboardState.Empty -> {
                tripsRecycler.show()
                adapter.submitEmptyState()
            }
            is DashboardState.Error -> {
                tripsRecycler.show()
                showError(state.message)
            }
        }
    }

    override fun handleNavigation(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.ToTripDetails -> {
                // ODBIERZ tripId z komendy!
                (activity as? DashboardActivity)?.openTripDetails(command.tripId)
            }
            is NavigationCommand.ToCreateTrip -> {
                (activity as? DashboardActivity)?.apply {
                    showDashboardFragment(R.id.menu_add_trip)
                    dashboardBottomNav.selectedItemId = R.id.menu_add_trip
                }
            }
            is NavigationCommand.ToJoinTrip -> {
                (activity as? DashboardActivity)?.apply {
                    showDashboardFragment(R.id.menu_join_trip)
                    dashboardBottomNav.selectedItemId = R.id.menu_join_trip
                }
            }
            else -> {}
        }
    }
}