package com.example.tripapp2.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.R
import com.example.tripapp2.data.repository.TripRepository
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
    private val tripRepository = TripRepository.getInstance()

    // Pola formularza
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    // ✅ ZMIANA: Typ zmieniony na Int? (resource ID)
    private val _usernameError = MutableLiveData<Int?>()
    val usernameError: LiveData<Int?> = _usernameError

    private val _passwordError = MutableLiveData<Int?>()
    val passwordError: LiveData<Int?> = _passwordError

    // ✅ ZMIANA: Pozostawiam String (message do wyświetlenia)
    private val _registerSuccessEvent = MutableLiveData<Event<Unit>>()
    val registerSuccessEvent: LiveData<Event<Unit>> = _registerSuccessEvent

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
        if (!validateForm()) { return }

        viewModelScope.launch {
            setLoading(true)
                val result = tripRepository.register(
                    username = _username.value!!,
                    password = _password.value!!
                )
                result.onSuccess{ auth ->
                    val success = auth.success
                    if (success) { _registerSuccessEvent.value = Event(Unit) }
                    else { showError(auth.message) }
                }
            setLoading(false)
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
            _usernameError.value = R.string.error_username_required
            isValid = false
        } else if (username.length < 3) {
            _usernameError.value = R.string.error_username_too_short
            isValid = false
        }

        val password = _password.value
        if (password.isNullOrBlank()) {
            _passwordError.value = R.string.error_password_required
            isValid = false
        } else if (password.length < 6) {
            _passwordError.value = R.string.error_password_too_short
            isValid = false
        }

        return isValid
    }
}