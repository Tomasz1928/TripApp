package com.example.tripapp2.ui.dashboard.options

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.network.ApolloClientProvider
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

/**
 * ViewModel dla Options
 * Odpowiedzialny za:
 * - Wylogowanie użytkownika
 * - Czyszczenie cache
 */
class OptionsViewModel : BaseViewModel() {
    private val tripRepository = TripRepository.getInstance()
    private val _logoutEvent = MutableLiveData<Event<Unit>>()
    val logoutEvent: LiveData<Event<Unit>> = _logoutEvent

    /**
     * Wylogowanie użytkownika.
     * Wywołuje mutation logout, ale niezależnie od wyniku
     * czyści cache i przekierowuje do logowania.
     */
    fun onLogoutClicked() {
        viewModelScope.launch {
            setLoading(true)
            try {
                tripRepository.logout()
            } catch (_: Exception) { }
            tripRepository.clearCache()
            setLoading(false)
            _logoutEvent.value = Event(Unit)
        }
    }
}