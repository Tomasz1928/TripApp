package com.example.tripapp2.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel dla Register
 * Odpowiedzialny za:
 * - Walidację danych rejestracji
 * - Rejestrację użytkownika
 */
class RegisterViewModel : BaseViewModel() {

    // Pola formularza
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    // Błędy walidacji
    private val _usernameError = MutableLiveData<String?>()
    val usernameError: LiveData<String?> = _usernameError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    // Event sukcesu rejestracji
    private val _registerSuccessEvent = MutableLiveData<Event<String>>()
    val registerSuccessEvent: LiveData<Event<String>> = _registerSuccessEvent

    // Event przejścia do logowania
    private val _navigateToLoginEvent = MutableLiveData<Event<Unit>>()
    val navigateToLoginEvent: LiveData<Event<Unit>> = _navigateToLoginEvent

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
     * Rejestracja
     */
    fun onRegisterClicked() {
        if (!validateForm()) {
            return
        }

        viewModelScope.launch {
            setLoading(true)

            // Symulacja API call
            delay(1000)

            // Mock - zawsze sukces
            val success = true

            setLoading(false)

            if (success) {
                _registerSuccessEvent.value = Event("Konto utworzone pomyślnie!")
                // Automatyczne przejście do logowania po 1s
                delay(1000)
                _navigateToLoginEvent.value = Event(Unit)
            } else {
                showError("Nie udało się utworzyć konta")
            }
        }
    }

    /**
     * Przejście do logowania
     */
    fun onLoginClicked() {
        _navigateToLoginEvent.value = Event(Unit)
    }

    /**
     * Waliduje formularz
     */
    private fun validateForm(): Boolean {
        var isValid = true

        val username = _username.value
        if (username.isNullOrBlank()) {
            _usernameError.value = "Podaj nazwę użytkownika"
            isValid = false
        } else if (username.length < 3) {
            _usernameError.value = "Nazwa użytkownika musi mieć min. 3 znaki"
            isValid = false
        }

        val password = _password.value
        if (password.isNullOrBlank()) {
            _passwordError.value = "Podaj hasło"
            isValid = false
        } else if (password.length < 6) {
            _passwordError.value = "Hasło musi mieć min. 6 znaków"
            isValid = false
        }

        return isValid
    }
}