package com.example.tripapp2.ui.auth.forgotpassword

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import com.example.tripapp2.R
import com.example.tripapp2.databinding.ActivityForgotPasswordBinding
import com.example.tripapp2.ui.auth.login.LoginActivity

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputListeners()
        setupObservers()
    }

    private fun setupInputListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.usernameInput.addTextChangedListener { text ->
            viewModel.onUsernameChanged(text.toString())
        }

        binding.emailInput.addTextChangedListener { text ->
            viewModel.onEmailChanged(text.toString())
        }

        binding.resetBtn.setOnClickListener {
            viewModel.onResetClicked()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.resetBtn.isEnabled = !isLoading
            binding.resetBtn.text = if (isLoading) {
                getString(R.string.forgot_password_button_loading)
            } else {
                getString(R.string.forgot_password_button)
            }
        }

        viewModel.usernameError.observe(this) { errorResId ->
            binding.usernameLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.emailError.observe(this) { errorResId ->
            binding.emailLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.successEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                showSuccessAndGoToLogin()
            }
        }

        viewModel.error.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showSuccessAndGoToLogin() {
        binding.formContainer.visibility = android.view.View.GONE
        binding.successContainer.visibility = android.view.View.VISIBLE

        binding.backToLoginBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}