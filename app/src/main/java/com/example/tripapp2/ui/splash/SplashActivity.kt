package com.example.tripapp2.ui.splash

import android.content.Intent
import android.os.Bundle
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
 * 3. Jesli TAK -> strzela do BE (checkSession) aby zweryfikowac waznosc sesji
 *    - Sesja wazna (isAuthenticated) -> DashboardActivity
 *    - Sesja niewazna lub blad -> czysci sesje i LoginActivity
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Brak setContentView - uzywamy windowBackground z Theme.TripApp.Splash

        val sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            // Brak zapisanej sesji -> od razu login
            navigateToLogin()
            return
        }

        // Sesja istnieje -> weryfikuj z BE
        lifecycleScope.launch {
            val repository = TripRepository.getInstance()
            val result = repository.checkSession()

            result.fold(
                onSuccess = { session ->
                    if (session.isAuthenticated) {
                        navigateToDashboard()
                    } else {
                        // Sesja wygasla na serwerze
                        sessionManager.clearSession()
                        repository.clearCache()
                        navigateToLogin()
                    }
                },
                onFailure = {
                    // Blad sieci lub serwera -> nie mozemy potwierdzic sesji -> login
                    sessionManager.clearSession()
                    repository.clearCache()
                    navigateToLogin()
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