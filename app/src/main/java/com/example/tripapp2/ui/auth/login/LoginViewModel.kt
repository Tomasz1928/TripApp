package com.example.tripapp2.ui.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.R
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

/**
 * ViewModel dla Login
 * Odpowiedzialny za:
 * - Walidację danych logowania
 * - Logowanie użytkownika
 */
class LoginViewModel : BaseViewModel() {
    private val tripRepository = TripRepository.getInstance()

    // Pola formularza
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    private val _usernameError = MutableLiveData<Int?>()
    val usernameError: LiveData<Int?> = _usernameError

    private val _passwordError = MutableLiveData<Int?>()
    val passwordError: LiveData<Int?> = _passwordError

    // Event sukcesu logowania
    private val _loginSuccessEvent = MutableLiveData<Event<Unit>>()
    val loginSuccessEvent: LiveData<Event<Unit>> = _loginSuccessEvent

    // Event przejścia do rejestracji
    private val _navigateToRegisterEvent = MutableLiveData<Event<Unit>>()
    val navigateToRegisterEvent: LiveData<Event<Unit>> = _navigateToRegisterEvent

    /**
     * Aktualizacja pól formularza
     */
    fun onUsernameChanged(username: String) {
        _username.value = username
        _usernameError.value = null
    }

    fun onPasswordChanged(password: String) {
        _password.value = password
        _passwordError.value = null
    }

    /**
     * Logowanie
     */
    fun onLoginClicked() {
        if (!validateForm()) { return }
        viewModelScope.launch {
            setLoading(true)

            val result = tripRepository.login(
                username = _username.value!!,
                password = _password.value!!
            )
            result
                .onSuccess { auth ->
                    if (auth.success) { _loginSuccessEvent.value = Event(Unit) }
                    else { showError(auth.message) }
                }
                .onFailure { exception ->
                    showError(exception.message ?: "Network error")
                }
            setLoading(false)
        }
    }

    /**
     * Przejście do rejestracji
     */
    fun onRegisterClicked() {
        _navigateToRegisterEvent.value = Event(Unit)
    }

    /**
     * Waliduje formularz
     */
    private fun validateForm(): Boolean {
        var isValid = true

        if (_username.value.isNullOrBlank()) {
            _usernameError.value = R.string.error_username_required
            isValid = false
        }

        if (_password.value.isNullOrBlank()) {
            _passwordError.value = R.string.error_password_required
            isValid = false
        }

        return isValid
    }
}