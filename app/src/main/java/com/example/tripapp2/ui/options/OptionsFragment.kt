package com.example.tripapp2.ui.dashboard.options

import android.content.Intent
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.auth.login.LoginActivity
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.baseModals.ConfirmModalFragment
import com.google.android.material.card.MaterialCardView

class OptionsFragment : BaseFragment<OptionsViewModel>(R.layout.fragment_options) {

    override val viewModel: OptionsViewModel by viewModels()

    private lateinit var logoutCard: MaterialCardView

    override fun setupUI() {
        val view = requireView()
        logoutCard = view.findViewById(R.id.logoutCard)

        logoutCard.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    override fun setupCustomObservers() {
        viewModel.logoutEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                navigateToLogin()
            }
        }
    }

    override fun onLoadingStateChanged(isLoading: Boolean) {
        logoutCard.isEnabled = !isLoading
        logoutCard.alpha = if (isLoading) 0.5f else 1.0f
    }

    private fun showLogoutConfirmation() {
        ConfirmModalFragment.newInstance(
            title = getString(R.string.options_logout_confirm_title),
            message = getString(R.string.options_logout_confirm_message),
            confirmText = getString(R.string.options_logout),
            onConfirm = {
                viewModel.onLogoutClicked()
            }
        ).show(parentFragmentManager, "logout_confirm")
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}