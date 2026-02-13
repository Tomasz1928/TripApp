package com.example.tripapp2.ui.tripdetails.settlements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

/**
 * ViewModel dla ekranu Settlements
 *
 * Nowa koncepcja:
 * - Pokazuje listę uczestników (poza mną) z informacją o balansie
 * - Każdy uczestnik ma przyciski: Zaliczka (zawsze), Rozlicz (jeśli nie na 0)
 * - Brak logiki n-do-n, tylko moje relacje
 */
class TripSettlementsViewModel(
    private val tripId: String
) : BaseViewModel() {

    private val tripRepository = TripRepository.getInstance()

    // Mock - w prawdziwej aplikacji pobierane z auth
    private val currentUserId = "10"

    // Stan ekranu
    private val _settlementsState = MutableLiveData<TripSettlementsState>()
    val settlementsState: LiveData<TripSettlementsState> = _settlementsState

    // Event otwarcia modala zaliczki
    private val _showPrepaymentModalEvent = MutableLiveData<Event<PrepaymentUiModel>>()
    val showPrepaymentModalEvent: LiveData<Event<PrepaymentUiModel>> = _showPrepaymentModalEvent

    // Event otwarcia modala rozliczenia (na przyszłość)
    private val _showSettleModalEvent = MutableLiveData<Event<SettlementParticipantUiModel>>()
    val showSettleModalEvent: LiveData<Event<SettlementParticipantUiModel>> = _showSettleModalEvent

    // Event potwierdzenia akcji
    private val _actionConfirmedEvent = MutableLiveData<Event<String>>()
    val actionConfirmedEvent: LiveData<Event<String>> = _actionConfirmedEvent

    init {
        loadSettlements()
    }

    /**
     * Ładuje dane rozliczeń
     */
    fun loadSettlements() {
        viewModelScope.launch {
            try {
                _settlementsState.value = TripSettlementsState.Loading

                val trip = tripRepository.getTripDetails(tripId)

                if (trip == null) {
                    _settlementsState.value = TripSettlementsState.Error("Nie znaleziono wycieczki")
                    return@launch
                }

                // Pobierz uczestników (bez mnie)
                val otherParticipants = trip.participants.filter { it.id != currentUserId }

                if (otherParticipants.isEmpty()) {
                    _settlementsState.value = TripSettlementsState.Empty
                    return@launch
                }

                // Pobierz relacje rozliczeniowe
                val allRelations = trip.settlement?.relations ?: emptyList()

                // Moje relacje jako dłużnik (ja winien innym)
                val myRelationsAsDebtor = allRelations.filter { it.fromUserId == currentUserId }

                // Moje relacje jako wierzyciel (inni winni mnie)
                val myRelationsAsCreditor = allRelations.filter { it.toUserId == currentUserId }

                // Mapuj uczestników na model z balansem
                val settlementParticipants = otherParticipants.map { participant ->
                    participant.toSettlementUiModel(
                        myRelationsAsDebtor = myRelationsAsDebtor,
                        myRelationsAsCreditor = myRelationsAsCreditor,
                        currency = trip.currency
                    )
                }.sortByBalance()

                // Oblicz mój całkowity bilans
                val myTotalBalance = settlementParticipants.sumOf { it.balance.toDouble() }.toFloat()
                val formattedTotalBalance = when {
                    myTotalBalance > 0.01f -> "+%.2f %s".format(myTotalBalance, trip.currency)
                    myTotalBalance < -0.01f -> "%.2f %s".format(myTotalBalance, trip.currency)
                    else -> "0,00 %s".format(trip.currency)
                }

                _settlementsState.value = TripSettlementsState.Success(
                    participants = settlementParticipants,
                    tripName = trip.title,
                    tripCurrency = trip.currency,
                    myTotalBalance = myTotalBalance,
                    formattedMyTotalBalance = formattedTotalBalance
                )

            } catch (e: Exception) {
                _settlementsState.value = TripSettlementsState.Error(
                    e.message ?: "Nie udało się załadować rozliczeń"
                )
            }
        }
    }

    /**
     * Kliknięcie przycisku "Zaliczka" przy uczestniku
     */
    fun onPrepaymentClicked(participant: SettlementParticipantUiModel) {
        val prepaymentModel = PrepaymentUiModel(
            participantId = participant.participantId,
            participantNickname = participant.nickname,
            availableCurrencies = listOf("PLN", "EUR", "USD", "GBP", "CHF", "CZK"),
            currentBalance = participant.balance,
            formattedCurrentBalance = participant.formattedBalance
        )
        _showPrepaymentModalEvent.value = Event(prepaymentModel)
    }

    fun onDetailsClicked(participant: SettlementParticipantUiModel) {
        // TODO: Implementacja pokazania szczegółów rozliczeń
        // np. _showDetailsModalEvent.value = Event(participant)
    }

    /**
     * Kliknięcie przycisku "Rozlicz" przy uczestniku
     */
    fun onSettleClicked(participant: SettlementParticipantUiModel) {
        // Na razie tylko event - do implementacji później
        _showSettleModalEvent.value = Event(participant)
    }

    /**
     * Potwierdzenie zaliczki z modala
     */
    fun onPrepaymentConfirmed(request: PrepaymentRequest) {
        viewModelScope.launch {
            try {
                setLoading(true)

                // TODO: Wywołanie API do zapisania zaliczki
                // val result = tripRepository.addPrepayment(request)

                // Mock - symulacja sukcesu
                kotlinx.coroutines.delay(500)

                val directionText = when (request.direction) {
                    PrepaymentDirection.TO_ME -> "od uczestnika"
                    PrepaymentDirection.FROM_ME -> "dla uczestnika"
                }

                _actionConfirmedEvent.value = Event(
                    "Zaliczka ${request.amount} ${request.currency} $directionText została zapisana"
                )

                // Odśwież dane
                loadSettlements()

            } catch (e: Exception) {
                showError(e.message ?: "Nie udało się zapisać zaliczki")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Potwierdzenie rozliczenia (na przyszłość)
     */
    fun onSettleConfirmed(participantId: String) {
        viewModelScope.launch {
            try {
                setLoading(true)

                // TODO: Implementacja rozliczenia
                kotlinx.coroutines.delay(500)

                _actionConfirmedEvent.value = Event("Rozliczenie zostało potwierdzone")
                loadSettlements()

            } catch (e: Exception) {
                showError(e.message ?: "Nie udało się potwierdzić rozliczenia")
            } finally {
                setLoading(false)
            }
        }
    }
}
