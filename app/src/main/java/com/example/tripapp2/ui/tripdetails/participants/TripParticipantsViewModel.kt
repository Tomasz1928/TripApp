package com.example.tripapp2.ui.tripdetails.participants

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

/**
 * ViewModel dla Trip Participants
 * Odpowiedzialny za:
 * - Ładowanie listy uczestników
 * - Sprawdzanie kto jest zalogowany (UserInfo)
 * - Dodawanie placeholderów
 * - Usuwanie placeholderów
 * - Kopiowanie kodów dostępu
 * - Obsługa różnych trybów widoku (ALL, ADD, DETACH, DELETE)
 */
class TripParticipantsViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    // Stan ekranu
    private val _participantsState = MutableLiveData<TripParticipantsState>()
    val participantsState: LiveData<TripParticipantsState> = _participantsState

    // Aktualny tryb widoku
    private val _currentViewMode = MutableLiveData<ParticipantViewMode>(ParticipantViewMode.ALL)
    val currentViewMode: LiveData<ParticipantViewMode> = _currentViewMode

    // Event kopiowania kodu
    private val _copyCodeEvent = MutableLiveData<Event<CopyAccessCodeEvent>>()
    val copyCodeEvent: LiveData<Event<CopyAccessCodeEvent>> = _copyCodeEvent

    // Event pokazania dialogu dodawania placeholdera
    private val _showAddPlaceholderDialogEvent = MutableLiveData<Event<Unit>>()
    val showAddPlaceholderDialogEvent: LiveData<Event<Unit>> = _showAddPlaceholderDialogEvent

    // Cache
    private var currentUserId: String = ""
    private var tripOwnerId: String = ""
    private var tripCurrency: String = "PLN"
    private var allParticipants: List<ParticipantUiModel> = emptyList()

    init {
        loadUserInfo()
    }

    /**
     * Ładuje informacje o zalogowanym użytkowniku
     */
    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val userInfo = tripRepository.getCurrentUserInfo()
                currentUserId = userInfo.id

                // Po załadowaniu user info, załaduj uczestników
                loadParticipants()
            } catch (e: Exception) {
                showError("Nie udało się załadować informacji o użytkowniku")
            }
        }
    }

    /**
     * Ładuje listę uczestników
     */
    fun loadParticipants() {
        viewModelScope.launch {
            _participantsState.value = TripParticipantsState.Loading

            try {
                // Pobierz trip z repository
                val trip = tripRepository.getTripDetails(tripId)

                if (trip == null) {
                    _participantsState.value = TripParticipantsState.Error(
                        "Nie znaleziono wycieczki"
                    )
                    return@launch
                }

                // Zapisz dane wycieczki
                tripCurrency = trip.currency
                tripOwnerId = trip.ownerId

                if (trip.participants.isEmpty()) {
                    _participantsState.value = TripParticipantsState.Empty
                } else {
                    // Konwertuj na UI modele
                    allParticipants = trip.participants.map { participant ->
                        participant.toUiModel(
                            ownerId = tripOwnerId,
                            currentUserId = currentUserId,
                            currency = trip.currency,
                            isPlaceholder = participant.isPlaceholder,
                            accessCode = participant.accessCode
                        )
                    }.sortByType()

                    // Zastosuj aktualny filtr
                    applyViewMode(_currentViewMode.value ?: ParticipantViewMode.ALL)
                }

            } catch (e: Exception) {
                _participantsState.value = TripParticipantsState.Error(
                    e.message ?: "Nie udało się załadować uczestników"
                )
            }
        }
    }

    /**
     * Zmienia tryb widoku
     */
    fun changeViewMode(mode: ParticipantViewMode) {
        // Jeśli tryb ADD, pokaż modal bez zmiany aktualnego trybu
        if (mode == ParticipantViewMode.ADD) {
            _showAddPlaceholderDialogEvent.value = Event(Unit)
            return // Nie zmieniaj trybu, pozostań w aktualnym
        }

        _currentViewMode.value = mode
        applyViewMode(mode)
    }

    /**
     * Aplikuje filtr według trybu
     */
    private fun applyViewMode(mode: ParticipantViewMode) {
        val filteredParticipants = allParticipants.filterByMode(mode, currentUserId)

        val isOwner = currentUserId == tripOwnerId

        if (filteredParticipants.isEmpty() && mode != ParticipantViewMode.ALL) {
            // Dla trybów DETACH i DELETE pokaż komunikat jeśli lista jest pusta
            _participantsState.value = TripParticipantsState.Empty
        } else {
            _participantsState.value = TripParticipantsState.Success(
                participants = filteredParticipants,
                isCurrentUserOwner = isOwner,
                tripCurrency = tripCurrency,
                currentMode = mode
            )
        }
    }

    /**
     * Kopiuje kod dostępu placeholdera
     */
    fun onCopyAccessCode(participant: ParticipantUiModel) {
        participant.accessCode?.let { code ->
            _copyCodeEvent.value = Event(
                CopyAccessCodeEvent(
                    code = code,
                    participantName = participant.nickname
                )
            )
        }
    }

    /**
     * Pokazuje dialog dodawania placeholdera
     */
    fun onAddPlaceholderClicked() {
        _showAddPlaceholderDialogEvent.value = Event(Unit)
    }

    /**
     * Dodaje nowego placeholdera
     */
    fun addPlaceholder(nickname: String) {
        if (nickname.isBlank()) {
            showError("Podaj nazwę uczestnika")
            return
        }

        if (nickname.length < 2) {
            showError("Nazwa musi mieć min. 2 znaki")
            return
        }

        if (nickname.length > 30) {
            showError("Nazwa nie może być dłuższa niż 30 znaków")
            return
        }

        viewModelScope.launch {
            setLoading(true)

            // TODO: Zastąpić prawdziwym API call
            // val result = tripRepository.addPlaceholder(tripId, nickname)

            // Mock API call - dodanie placeholdera
            kotlinx.coroutines.delay(500)

            // MOCK - Backend zwróci placeholder z wygenerowanym kodem
            val mockAccessCode = "MOCK-${System.currentTimeMillis().toString().takeLast(4)}"

            setLoading(false)

            // Pokaż prosty komunikat sukcesu
//            showMessage("Dodano uczestnika: $nickname (kod: $mockAccessCode)")

            // Odśwież listę - pozostanie w aktualnym trybie
            loadParticipants()
        }
    }

    /**
     * Usuwa placeholdera
     */
    fun removePlaceholder(participantId: String, participantName: String) {
        viewModelScope.launch {
            setLoading(true)

            // TODO: Zastąpić prawdziwym API call
            // val result = tripRepository.removePlaceholder(tripId, participantId)

            // Mock API call - usunięcie placeholdera
            kotlinx.coroutines.delay(500)

            setLoading(false)
//            showMessage("Usunięto uczestnika: $participantName")

            // Odśwież listę
            loadParticipants()
        }
    }

    /**
     * Odłącza aktywnego użytkownika od wycieczki
     * Użytkownik staje się placeholderem z nowym kodem dostępu
     */
    fun detachUser(participantId: String, participantName: String) {
        viewModelScope.launch {
            setLoading(true)

            // TODO: Zastąpić prawdziwym API call
            // val result = tripRepository.detachParticipant(tripId, participantId)

            // Mock API call - odłączenie użytkownika
            kotlinx.coroutines.delay(500)

            // MOCK - Backend zwróci nowy kod dostępu dla placeholdera
            val mockAccessCode = "DTCH-${System.currentTimeMillis().toString().takeLast(4)}"

            setLoading(false)

            // Pokaż prosty komunikat sukcesu
//            showMessage("Odłączono użytkownika: $participantName (nowy kod: $mockAccessCode)")

            // Odśwież listę - pozostanie w aktualnym trybie
            loadParticipants()
        }
    }

    /**
     * Obsługa kliknięcia w uczestnika
     */
    fun onParticipantClicked(participant: ParticipantUiModel) {
        // Możliwość rozszerzenia - np. pokazanie szczegółów uczestnika
    }

    // ==========================================
    // HELPERS
    // ==========================================
    // Brak lokalnych helperów - wszystkie dane z backendu
}

/**
 * Factory dla ViewModel
 */
class TripParticipantsViewModelFactory(
    private val tripId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripParticipantsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripParticipantsViewModel(tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}