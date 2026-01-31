package com.example.tripapp2.ui.auth.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.tripapp2.R
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
            binding.registerBtn.text = if (isLoading) {
                getString(R.string.register_button_loading)
            } else {
                getString(R.string.register_button)
            }
        }

        // ✅ ZMIANA: Błędy walidacji - konwertuj Int? na String?
        viewModel.usernameError.observe(this) { errorResId ->
            binding.usernameLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.passwordError.observe(this) { errorResId ->
            binding.passwordLayout.error = errorResId?.let { getString(it) }
        }

        // ✅ ZMIANA: Sukces rejestracji - parsuj message i konwertuj jeśli potrzeba
        viewModel.registerSuccessEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                // Sprawdź czy message zawiera resource ID
                val displayMessage = if (message.startsWith("RES_ID:")) {
                    val resId = message.substringAfter(":").toIntOrNull()
                    resId?.let { getString(it) } ?: message
                } else {
                    message
                }
                android.widget.Toast.makeText(this, displayMessage, android.widget.Toast.LENGTH_SHORT).show()
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