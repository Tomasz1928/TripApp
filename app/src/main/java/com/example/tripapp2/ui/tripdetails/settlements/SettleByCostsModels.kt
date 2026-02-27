package com.example.tripapp2.ui.tripdetails.settlements

/**
 * UI Models dla taba "Per koszty" w SettleModal
 */

/**
 * Kto płacił za dany koszt z perspektywy rozliczenia
 */
enum class CostPayerDirection {
    I_PAID,           // Ja płaciłem → participant mi jest winien tę kwotę (zielony)
    PARTICIPANT_PAID  // Participant płacił → ja jestem winien tę kwotę (czerwony)
}

/**
 * Model wiersza kosztu w liście "Per koszty"
 *
 * Filtrowanie:
 * - Koszt pojawia się gdy: ja płaciłem i participant jest w sharedWith,
 *   LUB participant płacił i ja jestem w sharedWith
 * - ORAZ sharedWith[participant/ja].isSettlement == false
 *
 * Kwota = splitValue z sharedWith (kwota udziału, nie cały koszt)
 */
data class SettleCostItemUiModel(
    val expenseId: String,
    val expenseName: String,
    val amount: Float,                      // Kwota udziału (splitValue)
    val currency: String,                   // Waluta wydatku
    val formattedAmount: String,            // "225,00 EUR"
    val payerDirection: CostPayerDirection, // Kto płacił
    val payerId: String,                    // ID osoby która płaciła
    val participantId: String,              // ID drugiej osoby w rozliczeniu
    var isChecked: Boolean = false          // Czy zaznaczony checkbox
)

/**
 * Request rozliczenia per koszty
 * Wysyłany do TripRepository → później do BE
 *
 * Zawiera listę kosztów do oznaczenia jako rozliczone
 */
data class SettleByCostsRequest(
    val tripId: String,
    val items: List<SettleByCostsItemInput>
)

/**
 * Pojedynczy koszt do rozliczenia
 */
data class SettleByCostsItemInput(
    val expenseId: String,
    val payerId: String,
    val participantId: String
)