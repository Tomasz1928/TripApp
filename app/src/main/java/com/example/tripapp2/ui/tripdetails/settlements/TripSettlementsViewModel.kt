package com.example.tripapp2.ui.tripdetails.settlements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.model.TripDto
import com.example.tripapp2.data.model.isSettled
import com.example.tripapp2.data.model.hasOutstandingAmount
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

/**
 * ViewModel dla ekranu Settlements
 *
 * Przepływ danych (po refaktorze):
 * 1. Pobierz TripDto z cache (TripRepository)
 * 2. Użyj TripDto.participants do stworzenia listy kart
 * 3. Użyj TripDto.settlement.relations (nowy format z relatedId) do uzupełnienia balansu
 *
 * Logika balansu (nowa):
 * - Każda relacja jest ZAWSZE w odniesieniu do mnie
 * - relatedId = ID drugiego uczestnika
 * - SimpleMoneyValueDto.amount > 0 → on mi jest winien
 * - SimpleMoneyValueDto.amount < 0 → ja jestem winien
 *
 * Widoczność przycisków:
 * - Zaliczka: zawsze widoczny
 * - Szczegóły: tylko gdy istnieje relacja dla uczestnika
 * - Rozlicz: tylko gdy istnieje relacja i hasOutstandingAmount
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
                val userInfo = tripRepository.getCurrentUserInfo()
                currentUserId = userInfo.id
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

                // Mapuj uczestników na modele UI (nie trzeba już myUserId - relacje już są "moje")
                val settlementParticipants = otherParticipants.map { participant ->
                    participant.toSettlementUiModel(
                        allRelations = allRelations,
                        currency = trip.currency
                    )
                }.sortByBalance()

                // Oblicz całkowity balans z relacji
                val totalBalance = calculateMyTotalBalance(allRelations)
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
     * Kliknięcie "Zaliczka" - otwiera modal zaliczki
     */
    fun onPrepaymentClicked(participant: SettlementParticipantUiModel) {
        viewModelScope.launch {
            try {
                val trip = tripRepository.getTripDetails(tripId) ?: return@launch

                // Zbierz dostępne waluty z relacji uczestnika
                val availableCurrencies = mutableListOf(trip.currency)
                participant.leftForSettled.forEach { money ->
                    if (!money.isMainCurrency && !availableCurrencies.contains(money.currency)) {
                        availableCurrencies.add(money.currency)
                    }
                }

                val prepaymentModel = PrepaymentUiModel(
                    participantId = participant.participantId,
                    participantNickname = participant.nickname,
                    availableCurrencies = availableCurrencies,
                    currentBalance = participant.balance,
                    formattedCurrentBalance = participant.formattedBalance,
                    balanceStatus = participant.balanceStatus
                )

                _showPrepaymentModalEvent.value = Event(prepaymentModel)

            } catch (e: Exception) {
                showError("Nie udało się otworzyć zaliczki: ${e.message}")
            }
        }
    }

    /**
     * Kliknięcie "Szczegóły" - otwiera modal szczegółów
     */
    fun onDetailsClicked(participant: SettlementParticipantUiModel) {
        _showDetailsModalEvent.value = Event(participant)
    }

    /**
     * Kliknięcie "Rozlicz" - otwiera modal rozliczenia
     */
    fun onSettleClicked(participant: SettlementParticipantUiModel) {
        viewModelScope.launch {
            try {
                val trip = tripRepository.getTripDetails(tripId) ?: return@launch

                // Znajdź relację dla tego uczestnika
                val relations = trip.settlement?.relations ?: return@launch
                val relation = relations.find { it.relatedId == participant.participantId }
                    ?: return@launch

                // Utwórz model dla modala
                val settleModel = createSettleModalModel(
                    participant = participant,
                    relation = relation,
                    tripCurrency = trip.currency
                )

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