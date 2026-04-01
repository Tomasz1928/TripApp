package com.example.tripapp2.ui.tripdetails.settlements

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.data.model.TripDto
import com.example.tripapp2.data.repository.CurrencyRepository
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * ViewModel dla ekranu Settlements
 *
 * Subskrypcje żyją w TripRepository.
 * Ten ViewModel obserwuje StateFlow i auto-odświeża rozliczenia.
 */
class TripSettlementsViewModel(
    private val tripId: String
) : BaseViewModel() {

    private val tripRepository = TripRepository.getInstance()
    private var currencyParticipantId: String = ""

    private val _settlementsState = MutableLiveData<TripSettlementsState>()
    val settlementsState: LiveData<TripSettlementsState> = _settlementsState

    private val _showPrepaymentModalEvent = MutableLiveData<Event<PrepaymentUiModel>>()
    val showPrepaymentModalEvent: LiveData<Event<PrepaymentUiModel>> = _showPrepaymentModalEvent

    private val _showDetailsModalEvent = MutableLiveData<Event<SettlementParticipantUiModel>>()
    val showDetailsModalEvent: LiveData<Event<SettlementParticipantUiModel>> = _showDetailsModalEvent

    private val _showSettleModalEvent = MutableLiveData<Event<SettleModalUiModel>>()
    val showSettleModalEvent: LiveData<Event<SettleModalUiModel>> = _showSettleModalEvent

    private val _actionConfirmedEvent = MutableLiveData<Event<String>>()
    val actionConfirmedEvent: LiveData<Event<String>> = _actionConfirmedEvent

    private val _settleByCostsEvent = MutableLiveData<Event<SettleByCostsRequest>>()
    val settleByCostsEvent: LiveData<Event<SettleByCostsRequest>> = _settleByCostsEvent

    init {
        loadCurrentUserAndSettlements()
        observeTripUpdates()
    }

    // ==========================================
    // REAL-TIME OBSERVATION
    // ==========================================

    private fun observeTripUpdates() {
        viewModelScope.launch {
            tripRepository.observeTrip(tripId)
                .filterNotNull()
                .collect { trip ->
                    if (currencyParticipantId.isNotEmpty()) {
                        Log.d(TAG, "Trip updated via StateFlow, refreshing settlements")
                        updateSettlementsFromTrip(trip)
                    }
                }
        }
    }

    private fun updateSettlementsFromTrip(trip: TripDto) {
        try {
            val otherParticipants = trip.participants.filter { it.id != currencyParticipantId }

            if (otherParticipants.isEmpty()) {
                _settlementsState.value = TripSettlementsState.Empty
                return
            }

            val allRelations = trip.settlement?.relations ?: emptyList()

            val settlementParticipants = otherParticipants.map { participant ->
                participant.toSettlementUiModel(allRelations = allRelations, currency = trip.currency)
            }.sortByBalance()

            val totalBalance = calculateMyTotalBalance(allRelations)

            _settlementsState.value = TripSettlementsState.Success(
                participants = settlementParticipants,
                tripCurrency = trip.currency,
                myTotalBalance = totalBalance,
                formattedMyTotalBalance = totalBalance.formatTotalBalance(trip.currency),
                myBalanceStatus = totalBalance.toBalanceStatus()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating settlements from StateFlow", e)
        }
    }

    // ==========================================
    // INITIAL LOAD
    // ==========================================

    private fun loadCurrentUserAndSettlements() {
        viewModelScope.launch {
            try {
                loadSettlements()
            } catch (e: Exception) {
                _settlementsState.value = TripSettlementsState.Error(
                    "Nie udało się załadować danych użytkownika"
                )
            }
        }
    }

    fun getCurrentUserId(): String = currencyParticipantId

    fun getTripData(): TripDto? {
        return tripRepository.getTripDetails(tripId)
    }

    fun loadSettlements() {
        viewModelScope.launch {
            try {
                if (currencyParticipantId.isEmpty()) {
                    val userInfo = tripRepository.getTripDetails(tripId)?.myParticipantId
                    currencyParticipantId = userInfo.toString()
                }

                _settlementsState.value = TripSettlementsState.Loading

                val trip = tripRepository.getTripDetails(tripId)

                if (trip == null) {
                    _settlementsState.value = TripSettlementsState.Error("Nie znaleziono wycieczki")
                    return@launch
                }

                val otherParticipants = trip.participants.filter { it.id != currencyParticipantId }

                if (otherParticipants.isEmpty()) {
                    _settlementsState.value = TripSettlementsState.Empty
                    return@launch
                }

                val allRelations = trip.settlement?.relations ?: emptyList()

                val settlementParticipants = otherParticipants.map { participant ->
                    participant.toSettlementUiModel(allRelations = allRelations, currency = trip.currency)
                }.sortByBalance()

                val totalBalance = calculateMyTotalBalance(allRelations)

                _settlementsState.value = TripSettlementsState.Success(
                    participants = settlementParticipants,
                    tripCurrency = trip.currency,
                    myTotalBalance = totalBalance,
                    formattedMyTotalBalance = totalBalance.formatTotalBalance(trip.currency),
                    myBalanceStatus = totalBalance.toBalanceStatus()
                )

            } catch (e: Exception) {
                _settlementsState.value = TripSettlementsState.Error(
                    e.message ?: "Nie udało się załadować rozliczeń"
                )
            }
        }
    }

    // ==========================================
    // USER ACTIONS
    // ==========================================

    fun onPrepaymentClicked(participant: SettlementParticipantUiModel) {
        viewModelScope.launch {
            try {
                val trip = tripRepository.getTripDetails(tripId) ?: return@launch
                val allCurrencies = CurrencyRepository.getInstance().getCurrencies()
                val availableCurrencies = buildList {
                    add(trip.currency)
                    addAll(allCurrencies.filter { it != trip.currency })
                }

                _showPrepaymentModalEvent.value = Event(PrepaymentUiModel(
                    participantId = participant.participantId,
                    participantNickname = participant.nickname,
                    availableCurrencies = availableCurrencies,
                    currentBalance = participant.balance,
                    formattedCurrentBalance = participant.formattedBalance,
                    balanceStatus = participant.balanceStatus
                ))
            } catch (e: Exception) {
                showError("Nie udało się otworzyć zaliczki: ${e.message}")
            }
        }
    }

    fun onDetailsClicked(participant: SettlementParticipantUiModel) {
        _showDetailsModalEvent.value = Event(participant)
    }

    fun onSettleClicked(participant: SettlementParticipantUiModel) {
        viewModelScope.launch {
            try {
                val trip = tripRepository.getTripDetails(tripId) ?: return@launch
                val relations = trip.settlement?.relations ?: return@launch
                val relation = relations.find { it.relatedId == participant.participantId } ?: return@launch

                val settleModel = createSettleModalModel(
                    participant = participant, relation = relation, tripCurrency = trip.currency
                )
                _showSettleModalEvent.value = Event(settleModel)
            } catch (e: Exception) {
                showError("Nie udało się przygotować rozliczenia: ${e.message}")
            }
        }
    }

    fun onSettleConfirmedFromModal(request: SettleRequest) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val result = tripRepository.settleByAmount(
                    tripId = request.tripId, fromUserId = request.fromUserId,
                    toUserId = request.toUserId, amount = request.amount,
                    currency = request.currency, isMainCurrency = request.isMainCurrency
                )
                result.onSuccess {
                    _actionConfirmedEvent.value = Event("Rozliczono %.2f %s".format(request.amount, request.currency))
                }.onFailure { showError(it.message ?: "Nie udało się rozliczyć") }
            } catch (e: Exception) {
                showError(e.message ?: "Nie udało się rozliczyć")
            } finally {
                setLoading(false)
            }
        }
    }

    fun onSettleByCostsConfirmed(request: SettleByCostsRequest) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val result = tripRepository.settleByCosts(request.tripId, request.items)
                result.onSuccess {
                    _actionConfirmedEvent.value = Event("Rozliczono ${request.items.size} kosztów")
                }.onFailure { showError(it.message ?: "Nie udało się rozliczyć") }
            } catch (e: Exception) {
                showError(e.message ?: "Błąd rozliczenia")
            } finally {
                setLoading(false)
            }
        }
    }

    fun onPrepaymentConfirmed(request: PrepaymentRequest) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val result = tripRepository.addPrepayment(
                    tripId = request.tripId, participantId = request.participantId,
                    amount = request.amount, currency = request.currency,
                    direction = request.direction.name
                )
                result.onSuccess {
                    val directionText = when (request.direction) {
                        PrepaymentDirection.TO_ME -> "od uczestnika"
                        PrepaymentDirection.FROM_ME -> "dla uczestnika"
                    }
                    _actionConfirmedEvent.value = Event(
                        "Zaliczka %.2f %s %s została zapisana".format(request.amount, request.currency, directionText)
                    )
                }.onFailure { showError(it.message ?: "Nie udało się zapisać zaliczki") }
            } catch (e: Exception) {
                showError(e.message ?: "Nie udało się zapisać zaliczki")
            } finally {
                setLoading(false)
            }
        }
    }

    companion object {
        private const val TAG = "TripSettlementsVM"
    }
}