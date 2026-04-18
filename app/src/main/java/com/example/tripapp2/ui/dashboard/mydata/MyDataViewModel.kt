package com.example.tripapp2.ui.dashboard.mydata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

class MyDataViewModel : BaseViewModel() {
    private val tripRepository = TripRepository.getInstance()

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _currentEmail = MutableLiveData<String>()
    val currentEmail: LiveData<String> = _currentEmail

    private val _emailChangedEvent = MutableLiveData<Event<String>>()
    val emailChangedEvent: LiveData<Event<String>> = _emailChangedEvent

    private val _logoutAfterPasswordChangeEvent = MutableLiveData<Event<Unit>>()
    val logoutAfterPasswordChangeEvent: LiveData<Event<Unit>> = _logoutAfterPasswordChangeEvent

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            // Najpierw pokaż dane z cache żeby ekran nie był pusty
            try {
                val cached = tripRepository.getCurrentUserInfo()
                _username.value = cached.nickname
                _currentEmail.value = cached.email ?: ""
            } catch (_: Exception) {}

            // Potem świeży request do backendu — aktualizuje cache i UI
            try {
                val sessionResult = tripRepository.checkSession()
                sessionResult.onSuccess { session ->
                    session.user?.let { user ->
                        _username.value = user.nickname
                        _currentEmail.value = user.email ?: ""
                    }
                }
            } catch (e: Exception) {
                // sieć niedostępna — zostają dane z cache
            }
        }
    }

    fun onChangeEmailConfirmed(newEmail: String) {
        val email = newEmail.trim()
        if (email.isBlank()) {
            showError("Podaj adres email")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Podaj prawidłowy adres email")
            return
        }

        viewModelScope.launch {
            setLoading(true)
            val result = tripRepository.changeEmail(email)
            result.onSuccess { auth ->
                if (auth.success) {
                    _currentEmail.value = email
                    _emailChangedEvent.value = Event(auth.message)
                } else {
                    showError(auth.message)
                }
            }
            result.onFailure { showError(it.message ?: "Błąd połączenia") }
            setLoading(false)
        }
    }

    fun onChangePasswordConfirmed(newPassword: String, newPasswordConfirm: String) {
        if (newPassword.length < 6) {
            showError("Hasło musi mieć min. 6 znaków")
            return
        }
        if (newPassword != newPasswordConfirm) {
            showError("Hasła nie są identyczne")
            return
        }

        viewModelScope.launch {
            setLoading(true)
            val result = tripRepository.changePassword(newPassword, newPasswordConfirm)
            result.onSuccess { auth ->
                if (auth.success) _logoutAfterPasswordChangeEvent.value = Event(Unit)
                else showError(auth.message)
            }
            result.onFailure { showError(it.message ?: "Błąd połączenia") }
            setLoading(false)
        }
    }
}