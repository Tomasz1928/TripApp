package com.example.tripapp2.ui.auth.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.tripapp2.databinding.ActivityLoginBinding
import com.example.tripapp2.ui.auth.register.RegisterActivity
import com.example.tripapp2.ui.dashboard.DashboardActivity

/**
 * Activity logowania
 *
 * PRZED: 60 linii, duplikacja onClick, logika w UI
 * PO: 50 linii, delegacja do ViewModel, reaktywność
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputListeners()
        setupObservers()
    }

    private fun setupInputListeners() {
        // Username
        binding.usernameInput.addTextChangedListener { text ->
            viewModel.onUsernameChanged(text.toString())
        }

        // Password
        binding.passwordInput.addTextChangedListener { text ->
            viewModel.onPasswordChanged(text.toString())
        }

        // Login button
        binding.loginBtn.setOnClickListener {
            viewModel.onLoginClicked()
        }

        // Register redirect
        binding.registerRedirect.setOnClickListener {
            viewModel.onRegisterClicked()
        }
    }

    private fun setupObservers() {
        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loginBtn.isEnabled = !isLoading
            binding.loginBtn.text = if (isLoading) "Logowanie..." else "Zaloguj się"
        }

        // Błędy walidacji
        viewModel.usernameError.observe(this) { error ->
            binding.usernameLayout.error = error
        }

        viewModel.passwordError.observe(this) { error ->
            binding.passwordLayout.error = error
        }

        // Sukces logowania
        viewModel.loginSuccessEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                navigateToDashboard()
            }
        }

        // Przejście do rejestracji
        viewModel.navigateToRegisterEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                navigateToRegister()
            }
        }

        // Błędy
        viewModel.error.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                // Możesz pokazać Snackbar zamiast Toast
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}