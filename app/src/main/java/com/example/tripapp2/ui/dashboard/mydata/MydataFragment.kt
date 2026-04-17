package com.example.tripapp2.ui.dashboard.mydata

import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.data.network.ApolloClientProvider
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.auth.login.LoginActivity
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.baseModals.ConfirmModalFragment
import com.example.tripapp2.ui.common.baseModals.InputModalFragment
import com.example.tripapp2.ui.common.extension.applyStatusBarInsets
import com.example.tripapp2.ui.dashboard.DashboardActivity

class MyDataFragment : BaseFragment<MyDataViewModel>(R.layout.fragment_my_data) {

    override val viewModel: MyDataViewModel by viewModels()

    private lateinit var userAvatar: TextView
    private lateinit var usernameText: TextView
    private lateinit var currentEmailText: TextView
    private lateinit var emailRow: View
    private lateinit var passwordRow: View

    override fun setupUI() {
        val view = requireView()
        view.findViewById<View>(R.id.topBar).applyStatusBarInsets()

        view.findViewById<View>(R.id.backButton).setOnClickListener {
            (activity as? DashboardActivity)?.closeMyData()
        }

        userAvatar = view.findViewById(R.id.userAvatar)
        usernameText = view.findViewById(R.id.usernameText)
        currentEmailText = view.findViewById(R.id.currentEmailText)
        emailRow = view.findViewById(R.id.emailRow)
        passwordRow = view.findViewById(R.id.passwordRow)

        emailRow.setOnClickListener { showChangeEmailModal() }
        passwordRow.setOnClickListener { showChangePasswordModal() }
    }

    override fun setupCustomObservers() {
        viewModel.username.observe(viewLifecycleOwner) { name ->
            usernameText.text = name
            userAvatar.text = name.take(2).uppercase()
        }

        viewModel.currentEmail.observe(viewLifecycleOwner) { email ->
            currentEmailText.text = email.ifBlank { getString(R.string.my_data_email_not_set) }
        }

        viewModel.emailChangedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.logoutAfterPasswordChangeEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                navigateToLoginAfterPasswordChange()
            }
        }
    }

    // -------------------------------------------------------
    // Modals
    // -------------------------------------------------------

    private fun showChangeEmailModal() {
        InputModalFragment.newInstance(
            title = getString(R.string.my_data_change_email_title),
            hint = getString(R.string.my_data_new_email_hint),
            confirmText = getString(R.string.dialog_button_save),
            onConfirm = { newEmail ->
                viewModel.onChangeEmailConfirmed(newEmail)
            }
        ).show(parentFragmentManager, "change_email")
    }

    private fun showChangePasswordModal() {
        ChangePasswordModalFragment.newInstance(
            onConfirm = { newPassword, confirmPassword ->
                viewModel.onChangePasswordConfirmed(newPassword, confirmPassword)
            }
        ).show(parentFragmentManager, "change_password")
    }

    // -------------------------------------------------------

    override fun onLoadingStateChanged(isLoading: Boolean) {
        emailRow.isEnabled = !isLoading
        passwordRow.isEnabled = !isLoading
        emailRow.alpha = if (isLoading) 0.5f else 1.0f
        passwordRow.alpha = if (isLoading) 0.5f else 1.0f
    }

    private fun navigateToLoginAfterPasswordChange() {
        val repository = TripRepository.getInstance()
        ApolloClientProvider.resetAndRebuild()
        repository.stopAllSubscriptions()
        repository.clearCache()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    companion object {
        fun newInstance() = MyDataFragment()
    }
}