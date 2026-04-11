package com.example.tripapp2.ui.dashboard.options

import android.content.Intent
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.tripapp2.R
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.auth.login.LoginActivity
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.baseModals.ConfirmModalFragment
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import com.example.tripapp2.ui.common.extension.applyStatusBarInsets
import android.view.View

/**
 * Fragment ustawień — Propozycja C: Floating Sections
 *
 * Zmiany vs oryginał:
 * - Brak header image (top bar z tytułem)
 * - User card na górze (awatar + nickname)
 * - Logout card z czerwoną ikoną w settings-style
 * - logoutCard zachowuje ID (kompatybilność z ViewModel)
 */
class OptionsFragment : BaseFragment<OptionsViewModel>(R.layout.fragment_options) {

    override val viewModel: OptionsViewModel by viewModels()

    private lateinit var logoutCard: MaterialCardView
    private lateinit var tutorialCard: MaterialCardView  // <-- DODAJ TO
    private lateinit var userAvatar: TextView
    private lateinit var userName: TextView

    private val repository = TripRepository.getInstance()

    override fun setupUI() {
        val view = requireView()
        view.findViewById<View>(R.id.topBar).applyStatusBarInsets()
        logoutCard = view.findViewById(R.id.logoutCard)
        userAvatar = view.findViewById(R.id.userAvatar)
        userName = view.findViewById(R.id.userName)
        tutorialCard = view.findViewById(R.id.tutorialCard) // <-- działa bo view = requireView()

        tutorialCard.setOnClickListener {
            (activity as? DashboardActivity)?.showTutorial()
        }

        logoutCard.setOnClickListener {
            showLogoutConfirmation()
        }

        loadUserInfo()
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

    /**
     * Ładuje dane użytkownika i wypełnia user card
     */
    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val userInfo = repository.getCurrentUserInfo()
                userName.text = userInfo.nickname
                userAvatar.text = userInfo.nickname.take(2).uppercase()
            } catch (e: Exception) {
                userName.text = "Użytkownik"
                userAvatar.text = "?"
            }
        }
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
