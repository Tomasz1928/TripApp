package com.example.tripapp2.ui.tripdetails.settlements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tripapp2.R
import com.example.tripapp2.data.repository.TripRepository
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.example.tripapp2.ui.common.base.Event
import kotlinx.coroutines.launch

class TripSettlementsViewModel(
    private val tripId: String,
    private val currentUserId: String = "current_user", // TODO: Pobierz z UserManager
    private val tripRepository: TripRepository = TripRepository.getInstance()
) : BaseViewModel() {

    private val _settlementsState = MutableLiveData<TripSettlementsState>()
    val settlementsState: LiveData<TripSettlementsState> = _settlementsState

    private val _showSettlementDetailEvent = MutableLiveData<Event<SettlementDetailUiModel>>()
    val showSettlementDetailEvent: LiveData<Event<SettlementDetailUiModel>> = _showSettlementDetailEvent

    private val _settlementConfirmedEvent = MutableLiveData<Event<String>>()
    val settlementConfirmedEvent: LiveData<Event<String>> = _settlementConfirmedEvent

    init {
        loadSettlements()
    }

    /**
     * Ładuje rozliczenia dla wycieczki
     */
    fun loadSettlements() {
        viewModelScope.launch {
            _settlementsState.value = TripSettlementsState.Loading

            try {
                val trip = tripRepository.getTripDetails(tripId)

                if (trip == null) {
                    _settlementsState.value = TripSettlementsState.Error(
                        "Nie znaleziono wycieczki"
                    )
                    return@launch
                }

                val settlement = trip.settlement

                // Sprawdź czy są jakieś nierozliczone relacje
                val hasUnpaidRelations = settlement.relations?.any { !it.isSettled } ?: false

                if (!hasUnpaidRelations) {
                    _settlementsState.value = TripSettlementsState.AllSettled
                } else {
                    _settlementsState.value = settlement.toSuccessState(
                        currentUserId = currentUserId,
                        tripTitle = trip.title,
                        tripCurrency = trip.currency
                    )
                }

            } catch (e: Exception) {
                _settlementsState.value = TripSettlementsState.Error(
                    e.message ?: "Nie udało się załadować rozliczeń"
                )
            }
        }
    }

    /**
     * Kliknięcie w relację - pokazuje szczegóły
     */
    fun onRelationClicked(relation: SettlementRelationUiModel) {
        viewModelScope.launch {
            try {
                val trip = tripRepository.getTripDetails(tripId)

                if (trip == null) {
                    showError("Nie znaleziono wycieczki")
                    return@launch
                }

                // Znajdź odpowiednią relację w danych z backendu
                val settlementRelation = trip.settlement.relations?.find {
                    it.fromUserId == relation.fromUserId && it.toUserId == relation.toUserId
                }

                if (settlementRelation != null) {
                    val detail = settlementRelation.toDetailUiModel(
                        currentUserId = currentUserId,
                        tripCurrency = trip.currency
                    )

                    _showSettlementDetailEvent.value = Event(detail)
                }

            } catch (e: Exception) {
                showError(e.message ?:  R.string.error_trip_details_load_failed.toString())
            }
        }
    }

    /**
     * Rozliczenie w wybranej walucie
     */
    fun onSettleInCurrency(
        relationId: String,
        currency: String,
        amount: Float?
    ) {
        viewModelScope.launch {
            try {
                setLoading(true)

                // Parsuj relationId na fromUserId i toUserId
                val parts = relationId.split("_")
                if (parts.size != 2) {
                    showError("Nieprawidłowy ID relacji")
                    return@launch
                }

                val fromUserId = parts[0]
                val toUserId = parts[1]

                // Wywołaj API
                val result = tripRepository.markSettlementAsPaid(
                    tripId = tripId,
                    fromUserId = fromUserId,
                    toUserId = toUserId,
                    amount = amount ?: 0f, // Backend powinien obsłużyć null jako "całość"
                    currency = currency
                )

                result.onSuccess {
                    _settlementConfirmedEvent.value = Event("Rozliczenie potwierdzone!")
                    loadSettlements() // Odśwież dane
                }.onFailure { error ->
                    showError(error.message ?: "Nie udało się potwierdzić rozliczenia")
                }

            } catch (e: Exception) {
                showError(e.message ?: "Wystąpił błąd")
            } finally {
                setLoading(false)
            }
        }
    }
}