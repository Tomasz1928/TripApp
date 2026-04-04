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
 * ZMIENIONE: dual-currency — dodano amountTrip, formattedAmountTrip, isMultiCurrency
 *
 * Filtrowanie:
 * - Koszt pojawia się gdy: ja płaciłem i participant jest w sharedWith,
 *   LUB participant płacił i ja jestem w sharedWith
 * - ORAZ sharedWith[participant/ja].isSettlement == false
 *
 * Kwota:
 * - Single-currency (wydatek w walucie tripu):
 *     amount = mainCurrencyAmount (waluta tripu)
 *     currency = tripCurrency
 * - Multi-currency (wydatek w innej walucie):
 *     amount = notMainCurrencyAmount (waluta kosztu)
 *     currency = expense.currency
 *     amountTrip = mainCurrencyAmount (waluta tripu) → secondary display
 */
data class SettleCostItemUiModel(
    val expenseId: String,
    val expenseName: String,
    val amount: Float,                      // Kwota udziału (primary — w walucie kosztu lub tripu)
    val currency: String,                   // Waluta primary kwoty
    val formattedAmount: String,            // "225,00 EUR" lub "225,00 PLN"
    val payerDirection: CostPayerDirection, // Kto płacił
    val payerId: String,                    // ID osoby która płaciła
    val participantId: String,              // ID drugiej osoby w rozliczeniu
    var isChecked: Boolean = false,         // Czy zaznaczony checkbox

    // NOWE: dual-currency
    val amountTrip: Float? = null,                  // Kwota w walucie tripu (secondary), null gdy single-currency
    val formattedAmountTrip: String? = null,         // "950,00 PLN" — pomarańczowy secondary, null gdy single
    val isMultiCurrency: Boolean = false              // true = waluta kosztu != waluta tripu
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
    val participantId: String
)