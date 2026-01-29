package com.example.tripapp2.ui.common.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Bazowy ViewModel dla całej aplikacji
 * Dostarcza wspólną funkcjonalność:
 * - Loading state
 * - Error handling
 * - Event handling (single-shot events)
 */
abstract class BaseViewModel : ViewModel() {

    // Loading state - pokazywanie progressów
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error handling - wyświetlanie błędów
    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> = _error

    // Navigation events - nawigacja z ViewModel
    private val _navigationEvent = MutableLiveData<Event<NavigationCommand>>()
    val navigationEvent: LiveData<Event<NavigationCommand>> = _navigationEvent

    /**
     * Ustawia stan loading
     */
    protected fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    /**
     * Zgłasza błąd
     */
    protected fun showError(message: String) {
        _error.value = Event(message)
    }

    /**
     * Nawigacja
     */
    protected fun navigate(command: NavigationCommand) {
        _navigationEvent.value = Event(command)
    }

    /**
     * Wykonuje akcję z obsługą błędów i loading state
     */
    protected suspend fun <T> execute(
        showLoading: Boolean = true,
        action: suspend () -> T
    ): Result<T> {
        return try {
            if (showLoading) setLoading(true)
            Result.success(action())
        } catch (e: Exception) {
            showError(e.message ?: "Wystąpił nieznany błąd")
            Result.failure(e)
        } finally {
            if (showLoading) setLoading(false)
        }
    }
}

/**
 * Wrapper dla single-shot events
 * Zapobiega ponownemu wywołaniu event'u po rotacji ekranu
 */
class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * Pobiera zawartość jeśli nie została jeszcze obsłużona
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Zawsze zwraca zawartość (do peek)
     */
    fun peekContent(): T = content
}

/**
 * Komendy nawigacyjne
 */
sealed class NavigationCommand {
    object Back : NavigationCommand()
    data class ToTripDetails(val tripId: String) : NavigationCommand()
    object ToCreateTrip : NavigationCommand()
    object ToJoinTrip : NavigationCommand()
    object ToDashboard : NavigationCommand()
}