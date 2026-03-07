package com.example.tripapp2.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.tripapp2.R
import com.example.tripapp2.databinding.ActivityLoginBinding
import com.example.tripapp2.data.network.ApolloClientProvider
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.auth.register.RegisterActivity
import com.example.tripapp2.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch

/**
 * Activity logowania
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var repository = TripRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputListeners()
        setupObservers()
    }

    private fun setupInputListeners() {
        binding.usernameInput.addTextChangedListener { text ->
            viewModel.onUsernameChanged(text.toString())
        }

        binding.passwordInput.addTextChangedListener { text ->
            viewModel.onPasswordChanged(text.toString())
        }

        binding.loginBtn.setOnClickListener {
            viewModel.onLoginClicked()
        }

        binding.registerRedirect.setOnClickListener {
            viewModel.onRegisterClicked()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loginBtn.isEnabled = !isLoading
            binding.loginBtn.text = if (isLoading) {
                getString(R.string.login_button_loading)
            } else {
                getString(R.string.login_button)
            }
        }

        viewModel.usernameError.observe(this) { errorResId ->
            binding.usernameLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.passwordError.observe(this) { errorResId ->
            binding.passwordLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.loginSuccessEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                lifecycleScope.launch {
                    // POPRAWKA: resetAndRebuild() tworzy nowy Apollo klient z nowym sessionId.
                    // Dzięki fix w GraphQLDataSource (client jako property z get()),
                    // wszystkie kolejne operacje automatycznie użyją nowego klienta.
                    ApolloClientProvider.resetAndRebuild()

                    // POPRAWKA: Zatrzymaj stare subskrypcje (jeśli były) przed loadInitialData
                    repository.stopAllSubscriptions()

                    try {
                        // loadInitialData() automatycznie startuje subskrypcje WS
                        repository.loadInitialData()
                    } catch (e: Exception) {
                        Log.w("LoginActivity", "Initial data load failed", e)
                    }
                    navigateToDashboard()
                }
            }
        }

        viewModel.navigateToRegisterEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                navigateToRegister()
            }
        }

        viewModel.error.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToDashboard() {
        // POPRAWKA: Usunięto repository.startSubscriptionsForAllTrips()
        // bo loadInitialData() robi to automatycznie
        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}