package com.example.tripapp2.ui.auth.register

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.tripapp2.R
import com.example.tripapp2.databinding.ActivityRegisterBinding
import com.example.tripapp2.data.network.ApolloClientProvider
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.auth.login.LoginActivity
import com.example.tripapp2.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    private var repository = TripRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
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

        binding.registerBtn.setOnClickListener {
            viewModel.onRegisterClicked()
        }

        binding.loginRedirect.setOnClickListener {
            viewModel.onLoginClicked()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.registerBtn.isEnabled = !isLoading
            binding.registerBtn.text = if (isLoading) {
                getString(R.string.register_button_loading)
            } else {
                getString(R.string.register_button)
            }
        }

        viewModel.usernameError.observe(this) { errorResId ->
            binding.usernameLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.passwordError.observe(this) { errorResId ->
            binding.passwordLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.registerSuccessEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                navigateToDashboard()
            }
        }

        viewModel.navigateToLoginEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                navigateToLogin()
            }
        }

        viewModel.error.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToDashboard() {
        // POPRAWKA: resetAndRebuild() + stopAllSubscriptions() przed loadInitialData
        ApolloClientProvider.resetAndRebuild()
        repository.stopAllSubscriptions()

        lifecycleScope.launch {
            try {
                // loadInitialData() automatycznie startuje subskrypcje WS
                // (po rejestracji prawdopodobnie 0 tripów, więc 0 subskrypcji — OK)
                repository.loadInitialData()
            } catch (e: Exception) {
                Log.w("RegisterActivity", "loadInitialData failed", e)
            }

            val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}