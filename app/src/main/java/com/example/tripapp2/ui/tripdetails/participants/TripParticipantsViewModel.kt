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
 * - Dodawanie placeholderów
 * - Usuwanie placeholderów
 * - Kopiowanie kodów dostępu
 */
class TripParticipantsViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    // Stan ekranu
    private val _participantsState = MutableLiveData<TripParticipantsState>()
    val participantsState: LiveData<TripParticipantsState> = _participantsState

    // Event kopiowania kodu
    private val _copyCodeEvent = MutableLiveData<Event<CopyAccessCodeEvent>>()
    val copyCodeEvent: LiveData<Event<CopyAccessCodeEvent>> = _copyCodeEvent

    // Event pokazania dialogu dodawania placeholdera
    private val _showAddPlaceholderDialogEvent = MutableLiveData<Event<Unit>>()
    val showAddPlaceholderDialogEvent: LiveData<Event<Unit>> = _showAddPlaceholderDialogEvent

    // Event dodania placeholdera
    private val _placeholderAddedEvent = MutableLiveData<Event<PlaceholderAddedEvent>>()
    val placeholderAddedEvent: LiveData<Event<PlaceholderAddedEvent>> = _placeholderAddedEvent

    // Cache
    private var currentUserId: String = "user_1" // Mock - w prawdziwej app z SessionManager
    private var tripOwnerId: String = "user_1"   // Mock
    private var tripCurrency: String = "PLN"     // Mock

    init {
        loadParticipants()
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
                    val participants = trip.participants.map { participant ->
                        participant.toUiModel(
                            ownerId = tripOwnerId,
                            currentUserId = currentUserId,
                            currency = trip.currency,
                            isPlaceholder = participant.isPlaceholder,
                            accessCode = participant.accessCode
                        )
                    }.sortByType()

                    _participantsState.value = TripParticipantsState.Success(
                        participants = participants,
                        isCurrentUserOwner = currentUserId == tripOwnerId,
                        tripCurrency = trip.currency
                    )
                }

            } catch (e: Exception) {
                _participantsState.value = TripParticipantsState.Error(
                    e.message ?: "Nie udało się załadować uczestników"
                )
            }
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

            _placeholderAddedEvent.value = Event(
                PlaceholderAddedEvent(
                    nickname = nickname,
                    accessCode = mockAccessCode // Kod z backendu
                )
            )

            // Odśwież listę
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
            // Użyj showError (która istnieje w BaseViewModel) lub można dodać success event
            // showError to zła nazwa ale działa - lepiej zrobić osobny event

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

            // Pokaż dialog z nowym kodem dostępu
            _placeholderAddedEvent.value = Event(
                PlaceholderAddedEvent(
                    nickname = participantName,
                    accessCode = mockAccessCode,
                    message = "Użytkownik $participantName został odłączony. Nowy kod dostępu:"
                )
            )

            // Odśwież listę
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