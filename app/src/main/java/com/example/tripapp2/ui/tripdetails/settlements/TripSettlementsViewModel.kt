package com.example.tripapp2.ui.tripdetails.settlements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.model.TripDto
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

/**
 * ViewModel dla ekranu Settlements
 *
 * Przepływ danych:
 * 1. Pobierz TripDto z cache (TripRepository)
 * 2. Użyj TripDto.participants do stworzenia listy kart
 * 3. Użyj TripDto.settlement.relations do uzupełnienia balansu
 *
 * Logika balansu:
 * - fromUserId != myId → jestem na + (ktoś mi jest winien)
 * - fromUserId == myId → jestem na - (ja jestem winien)
 * - brak relacji dla uczestnika → balance = 0
 *
 * Widoczność przycisków:
 * - Zaliczka: zawsze widoczny
 * - Szczegóły: tylko gdy istnieje SettlementRelationDto dla uczestnika
 * - Rozlicz: tylko gdy istnieje SettlementRelationDto i kwota != 0
 */
class TripSettlementsViewModel(
    private val tripId: String
) : BaseViewModel() {

    private val tripRepository = TripRepository.getInstance()

    // ID aktualnego użytkownika - pobierane z repository
    private var currentUserId: String = ""

    // Stan ekranu
    private val _settlementsState = MutableLiveData<TripSettlementsState>()
    val settlementsState: LiveData<TripSettlementsState> = _settlementsState

    // Event otwarcia modala zaliczki
    private val _showPrepaymentModalEvent = MutableLiveData<Event<PrepaymentUiModel>>()
    val showPrepaymentModalEvent: LiveData<Event<PrepaymentUiModel>> = _showPrepaymentModalEvent

    // Event otwarcia modala szczegółów
    private val _showDetailsModalEvent = MutableLiveData<Event<SettlementParticipantUiModel>>()
    val showDetailsModalEvent: LiveData<Event<SettlementParticipantUiModel>> =
        _showDetailsModalEvent

    // Event otwarcia modala rozliczenia
    private val _showSettleModalEvent = MutableLiveData<Event<SettleModalUiModel>>()
    val showSettleModalEvent: LiveData<Event<SettleModalUiModel>> = _showSettleModalEvent

    // Event potwierdzenia akcji (feedback dla użytkownika)
    private val _actionConfirmedEvent = MutableLiveData<Event<String>>()
    val actionConfirmedEvent: LiveData<Event<String>> = _actionConfirmedEvent

    // Event rozliczenia per koszty
    private val _settleByCostsEvent = MutableLiveData<Event<SettleByCostsRequest>>()
    val settleByCostsEvent: LiveData<Event<SettleByCostsRequest>> = _settleByCostsEvent

    init {
        loadCurrentUserAndSettlements()
    }

    /**
     * Ładuje informacje o użytkowniku, a następnie dane rozliczeń
     */
    private fun loadCurrentUserAndSettlements() {
        viewModelScope.launch {
            try {
                // Najpierw pobierz info o aktualnym użytkowniku
                val userInfo = tripRepository.getCurrentUserInfo()
                currentUserId = userInfo.id

                // Następnie załaduj rozliczenia
                loadSettlements()
            } catch (e: Exception) {
                _settlementsState.value = TripSettlementsState.Error(
                    "Nie udało się załadować danych użytkownika"
                )
            }
        }
    }

    /**
     * Zwraca ID aktualnego użytkownika
     * Potrzebne dla modala rozliczenia
     */
    fun getCurrentUserId(): String = currentUserId

    fun getTripData(): TripDto? {
        return tripRepository.getTripDetails(tripId)
    }

    /**
     * Ładuje dane rozliczeń z cache
     */
    fun loadSettlements() {
        viewModelScope.launch {
            try {
                if (currentUserId.isEmpty()) {
                    val userInfo = tripRepository.getCurrentUserInfo()
                    currentUserId = userInfo.id
                }

                _settlementsState.value = TripSettlementsState.Loading

                // Pobierz trip z cache
                val trip = tripRepository.getTripDetails(tripId)

                if (trip == null) {
                    _settlementsState.value = TripSettlementsState.Error(
                        "Nie znaleziono wycieczki"
                    )
                    return@launch
                }

                // Filtruj uczestników - bez mnie
                val otherParticipants = trip.participants.filter { it.id != currentUserId }

                if (otherParticipants.isEmpty()) {
                    _settlementsState.value = TripSettlementsState.Empty
                    return@launch
                }

                // Pobierz wszystkie relacje rozliczeniowe
                val allRelations = trip.settlement?.relations ?: emptyList()

                // Mapuj uczestników na modele UI
                val settlementParticipants = otherParticipants.map { participant ->
                    participant.toSettlementUiModel(
                        myUserId = currentUserId,
                        allRelations = allRelations,
                        currency = trip.currency
                    )
                }.sortByBalance()

                // Oblicz całkowity balans
                val totalBalance = trip.settlement?.balance ?: 0f
                val balanceStatus = totalBalance.toBalanceStatus()
                val formattedTotalBalance = totalBalance.formatTotalBalance(trip.currency)

                _settlementsState.value = TripSettlementsState.Success(
                    participants = settlementParticipants,
                    tripCurrency = trip.currency,
                    myTotalBalance = totalBalance,
                    formattedMyTotalBalance = formattedTotalBalance,
                    myBalanceStatus = balanceStatus
                )

            } catch (e: Exception) {
                _settlementsState.value = TripSettlementsState.Error(
                    e.message ?: "Nie udało się załadować rozliczeń"
                )
            }
        }
    }

    // ==========================================
    // AKCJE UŻYTKOWNIKA
    // ==========================================

    /**
     * Kliknięcie przycisku "Zaliczka" przy uczestniku
     * Zawsze dostępny dla każdego uczestnika
     */
    fun onPrepaymentClicked(participant: SettlementParticipantUiModel) {
        val prepaymentModel = PrepaymentUiModel(
            participantId = participant.participantId,
            participantNickname = participant.nickname,
            availableCurrencies = listOf("PLN", "EUR", "USD", "GBP", "CHF", "CZK"),
            currentBalance = participant.balance,
            formattedCurrentBalance = participant.formattedBalance,
            balanceStatus = participant.balanceStatus
        )
        _showPrepaymentModalEvent.value = Event(prepaymentModel)
    }

    /**
     * Kliknięcie przycisku "Szczegóły" przy uczestniku
     * Dostępny tylko gdy hasSettlementRelation = true
     */
    fun onDetailsClicked(participant: SettlementParticipantUiModel) {
        if (!participant.hasSettlementRelation) {
            return // Zabezpieczenie - przycisk nie powinien być widoczny
        }
        _showDetailsModalEvent.value = Event(participant)
    }

    /**
     * Kliknięcie przycisku "Rozlicz" przy uczestniku
     * Dostępny tylko gdy hasSettlementRelation = true i balance != 0
     */
    fun onSettleClicked(participant: SettlementParticipantUiModel) {
        viewModelScope.launch {
            try {
                // Pobierz trip z cache
                val trip = tripRepository.getTripDetails(tripId) ?: return@launch

                // Znajdź relację dla tego uczestnika
                val relations = trip.settlement?.relations ?: return@launch
                val relation = relations.find { relation ->
                    (relation.fromUserId == participant.participantId && relation.toUserId == currentUserId) ||
                            (relation.fromUserId == currentUserId && relation.toUserId == participant.participantId)
                } ?: return@launch

                // Utwórz model dla modala
                val settleModel = createSettleModalModel(
                    participant = participant,
                    relation = relation,
                    tripCurrency = trip.currency,
                    currentUserId = currentUserId
                )

                // Emituj event do pokazania modala
                _showSettleModalEvent.value = Event(settleModel)

            } catch (e: Exception) {
                showError("Nie udało się przygotować rozliczenia: ${e.message}")
            }
        }
    }

    /**
     * Przetwarza potwierdzenie rozliczenia z modala
     */
    fun onSettleConfirmedFromModal(request: SettleRequest) {
        viewModelScope.launch {
            try {
                setLoading(true)

                val result = tripRepository.markSettlementAsPaid(
                    tripId = request.tripId,
                    fromUserId = request.fromUserId,
                    toUserId = request.toUserId,
                    amount = request.amount,
                    currency = request.currency,
                    isMainCurrency = request.isMainCurrency
                )

                result.onSuccess {
                    _actionConfirmedEvent.value = Event(
                        "Rozliczono %.2f %s".format(request.amount, request.currency)
                    )

                    // Odśwież dane
                    loadSettlements()

                }.onFailure { error ->
                    showError(error.message ?: "Nie udało się rozliczyć")
                }

            } catch (e: Exception) {
                showError(e.message ?: "Nie udało się rozliczyć")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Przetwarza rozliczenie per koszty
     * Oznacza wybrane wydatki jako rozliczone (isSettlement = true)
     */
    fun onSettleByCostsConfirmed(request: SettleByCostsRequest) {
        viewModelScope.launch {
            try {
                setLoading(true)

                val result = tripRepository.settleByCosts(request)

                result.onSuccess {
                    _actionConfirmedEvent.value = Event(
                        "Rozliczono ${request.items.size} kosztów"
                    )
                    // Odśwież dane
                    loadSettlements()

                }.onFailure { error ->
                    showError(error.message ?: "Nie udało się rozliczyć")
                }

            } catch (e: Exception) {
                showError(e.message ?: "Błąd rozliczenia")
            } finally {
                setLoading(false)
            }
        }
    }

    // ==========================================
    // OBSŁUGA MODALI
    // ==========================================

    /**
     * Potwierdzenie zaliczki z modala
     */
    fun onPrepaymentConfirmed(request: PrepaymentRequest) {
        viewModelScope.launch {
            try {
                setLoading(true)

                // Wywołaj repository aby zapisać zaliczkę
                val result = tripRepository.addPrepayment(
                    tripId = request.tripId,
                    participantId = request.participantId,
                    amount = request.amount,
                    currency = request.currency,
                    direction = request.direction.name
                )

                result.onSuccess {
                    val directionText = when (request.direction) {
                        PrepaymentDirection.TO_ME -> "od uczestnika"
                        PrepaymentDirection.FROM_ME -> "dla uczestnika"
                    }

                    _actionConfirmedEvent.value = Event(
                        "Zaliczka %.2f %s %s została zapisana".format(
                            request.amount,
                            request.currency,
                            directionText
                        )
                    )

                    // Odśwież dane
                    loadSettlements()

                }.onFailure { error ->
                    showError(error.message ?: "Nie udało się zapisać zaliczki")
                }

            } catch (e: Exception) {
                showError(e.message ?: "Nie udało się zapisać zaliczki")
            } finally {
                setLoading(false)
            }
        }
    }
}