package com.example.tripapp2.ui.auth.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.tripapp2.databinding.ActivityRegisterBinding
import com.example.tripapp2.ui.auth.login.LoginActivity

/**
 * Activity rejestracji
 *
 * PRZED: 50 linii, podstawowa walidacja
 * PO: 50 linii, pełna walidacja w ViewModel, lepsze UX
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
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

        // Register button
        binding.registerBtn.setOnClickListener {
            viewModel.onRegisterClicked()
        }

        // Login redirect
        binding.loginRedirect.setOnClickListener {
            viewModel.onLoginClicked()
        }
    }

    private fun setupObservers() {
        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.registerBtn.isEnabled = !isLoading
            binding.registerBtn.text = if (isLoading) "Tworzenie..." else "Utwórz konto"
        }

        // Błędy walidacji
        viewModel.usernameError.observe(this) { error ->
            binding.usernameLayout.error = error
        }

        viewModel.passwordError.observe(this) { error ->
            binding.passwordLayout.error = error
        }

        // Sukces rejestracji
        viewModel.registerSuccessEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // Przejście do logowania
        viewModel.navigateToLoginEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                navigateToLogin()
            }
        }

        // Błędy
        viewModel.error.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}