package com.example.tripapp2.ui.auth.forgotpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.R
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : BaseViewModel() {
    private val tripRepository = TripRepository.getInstance()

    private val _username = MutableLiveData<String>()
    private val _email = MutableLiveData<String>()

    private val _usernameError = MutableLiveData<Int?>()
    val usernameError: LiveData<Int?> = _usernameError

    private val _emailError = MutableLiveData<Int?>()
    val emailError: LiveData<Int?> = _emailError

    private val _successEvent = MutableLiveData<Event<Unit>>()
    val successEvent: LiveData<Event<Unit>> = _successEvent

    fun onUsernameChanged(username: String) {
        _username.value = username
        _usernameError.value = null
    }

    fun onEmailChanged(email: String) {
        _email.value = email
        _emailError.value = null
    }

    fun onResetClicked() {
        if (!validateForm()) return

        viewModelScope.launch {
            setLoading(true)
            val result = tripRepository.resetPassword(
                username = _username.value!!.trim(),
                email = _email.value!!.trim()
            )
            result.onSuccess { auth ->
                if (auth.success) _successEvent.value = Event(Unit)
                else showError(auth.message)
            }
            result.onFailure { showError(it.message ?: "Błąd połączenia") }
            setLoading(false)
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val username = _username.value?.trim()
        if (username.isNullOrBlank()) {
            _usernameError.value = R.string.error_username_required
            isValid = false
        }

        val email = _email.value?.trim()
        if (email.isNullOrBlank()) {
            _emailError.value = R.string.error_email_required
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = R.string.error_email_invalid
            isValid = false
        }

        return isValid
    }
}