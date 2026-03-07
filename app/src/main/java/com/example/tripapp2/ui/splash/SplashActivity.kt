package com.example.tripapp2.ui.splash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tripapp2.data.network.SessionManager
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.auth.login.LoginActivity
import com.example.tripapp2.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch

/**
 * SplashActivity - ekran startowy aplikacji.
 *
 * Logika:
 * 1. Sprawdza czy istnieje zapisana sesja (SessionManager.isLoggedIn)
 * 2. Jesli NIE -> od razu LoginActivity
 * 3. Jesli TAK:
 *    a) Wczytaj dane z persistent cache (jeśli są)
 *    b) Weryfikuj sesję z BE (checkSession)
 *    c) Sesja ważna → loadInitialData() → DashboardActivity
 *       (loadInitialData automatycznie startuje subskrypcje WS)
 *    d) Sesja nieważna → czyść sesję → LoginActivity
 */
class SplashActivity : AppCompatActivity() {

    private var repository = TripRepository.getInstance()
    private var sessionManager = SessionManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }

        lifecycleScope.launch {
            // Zatrzymaj ewentualne stare subskrypcje (np. po process death)
            repository.stopAllSubscriptions()

            // Szybki start: wczytaj dane z dysku
            repository.loadFromPersistentCache()

            val result = repository.checkSession()

            result.fold(
                onSuccess = { session ->
                    if (session.isAuthenticated) {
                        val loadResult = repository.loadInitialData()
                        loadResult.onFailure { e ->
                            Log.w("SplashActivity", "loadInitialData failed, using cache", e)
                        }
                        navigateToDashboard()
                    } else {
                        sessionManager.clearSession()
                        repository.clearCache()
                        navigateToLogin()
                    }
                },
                onFailure = {
                    if (repository.getAllTripsFromCache().isNotEmpty()) {
                        Log.w("SplashActivity", "Network error but cache available")
                        repository.startSubscriptionsForAllTrips()
                        navigateToDashboard()
                    } else {
                        sessionManager.clearSession()
                        repository.clearCache()
                        navigateToLogin()
                    }
                }
            )
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}