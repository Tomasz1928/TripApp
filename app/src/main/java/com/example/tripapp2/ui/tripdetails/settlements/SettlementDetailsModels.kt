package com.example.tripapp2.ui.tripdetails.settlements

import com.example.tripapp2.data.model.ExpenseDto
import com.example.tripapp2.data.model.SimpleMoneyValueDto

// ==========================================
// UI MODELS - Settlement Details Modal
// ==========================================

/**
 * Model wiersza kwoty w sekcji podsumowania (Tab 1)
 *
 * amount > 0 → participant jest mi winien (zielony)
 * amount < 0 → ja jestem winien (czerwony)
 * amount == 0 → szary
 */
data class SettlementDetailAmountRow(
    val currency: String,
    val amount: Float,
    val isMainCurrency: Boolean,
    val formattedAmount: String,        // "150,00" (wartość bezwzględna, bez znaku)
    val formattedCurrency: String       // "PLN (główna)" lub "EUR"
)

/**
 * Model wiersza kosztu w tab "Koszty" (Tab 2)
 *
 * Kierunek:
 * - isAmountPositive = true → pieniądze do mnie (zielony) — participant płacił, ja w sharedWith
 *   LUB ja płaciłem i participant jest w sharedWith → participant mi jest winien
 * - isAmountPositive = false → pieniądze ode mnie (czerwony)
 */
data class SettlementDetailCostRow(
    val expenseId: String,
    val expenseName: String,
    val splitAmount: Float,             // Kwota udziału (splitValue.valueMainCurrency)
    val currency: String,               // Waluta wydatku
    val formattedAmount: String,        // "150,00 PLN"
    val isSettled: Boolean,             // Czy rozliczone (ShareDto.isSettlement)
    val isAmountPositive: Boolean       // true = do mnie (zielony), false = ode mnie (czerwony)
)

/**
 * Model danych dla całego modala szczegółów
 */
data class SettlementDetailsUiModel(
    val participantId: String,
    val participantNickname: String,
    val tripCurrency: String,

    // Tab 1: Podsumowanie
    val allRelatedRows: List<SettlementDetailAmountRow>,
    val leftForSettledRows: List<SettlementDetailAmountRow>,
    val prepaymentRows: List<SettlementDetailAmountRow>,

    // Tab 2: Koszty
    val costRows: List<SettlementDetailCostRow>
)

// ==========================================
// MAPPER / FACTORY
// ==========================================

/**
 * Tworzy SettlementDetailsUiModel na podstawie danych z SettlementParticipantUiModel + TripDto
 *
 * @param participant Model UI uczestnika (zawiera allRelatedAmount, leftForSettled, prepayment, leftFromPrepayment)
 * @param tripCurrency Waluta główna wycieczki
 * @param expenses Lista wszystkich wydatków z TripDto
 * @param currentUserId ID aktualnego użytkownika
 */
fun createSettlementDetailsModel(
    participant: SettlementParticipantUiModel,
    tripCurrency: String,
    expenses: List<ExpenseDto>,
    currentUserId: String
): SettlementDetailsUiModel {

    // --- Tab 1: Sekcje kwotowe ---

    val allRelatedRows = mapMoneyListToRows(
        moneyList = participant.allRelatedAmount,
        tripCurrency = tripCurrency
    )

    val leftForSettledRows = mapMoneyListToRows(
        moneyList = participant.leftForSettled,
        tripCurrency = tripCurrency
    )

    val prepaymentRows = mapMoneyListToRows(
        moneyList = participant.leftFromPrepayment,
        tripCurrency = tripCurrency
    )

    // --- Tab 2: Koszty ---

    val costRows = buildCostRows(
        expenses = expenses,
        participantId = participant.participantId,
        currentUserId = currentUserId
    )

    return SettlementDetailsUiModel(
        participantId = participant.participantId,
        participantNickname = participant.nickname,
        tripCurrency = tripCurrency,
        allRelatedRows = allRelatedRows,
        leftForSettledRows = leftForSettledRows,
        prepaymentRows = prepaymentRows,
        costRows = costRows
    )
}

/**
 * Mapuje listę SimpleMoneyValueDto na listę wierszy do wyświetlenia
 *
 * Jeśli lista jest pusta → zwraca jeden wiersz z 0,00 i walutą główną
 */
private fun mapMoneyListToRows(
    moneyList: List<SimpleMoneyValueDto>,
    tripCurrency: String
): List<SettlementDetailAmountRow> {

    if (moneyList.isEmpty()) {
        return listOf(
            SettlementDetailAmountRow(
                currency = tripCurrency,
                amount = 0f,
                isMainCurrency = true,
                formattedAmount = "0,00",
                formattedCurrency = "$tripCurrency (główna)"
            )
        )
    }

    // Sortuj: główna waluta pierwsza
    return moneyList
        .sortedByDescending { it.isMainCurrency }
        .map { money ->
            val absAmount = kotlin.math.abs(money.amount)
            val currencyLabel = if (money.isMainCurrency) {
                "${money.currency} (główna)"
            } else {
                money.currency
            }
            SettlementDetailAmountRow(
                currency = money.currency,
                amount = money.amount,
                isMainCurrency = money.isMainCurrency,
                formattedAmount = "%.2f".format(absAmount),
                formattedCurrency = currencyLabel
            )
        }
}

/**
 * Buduje listę kosztów dotyczących relacji ja ↔ participant
 *
 * Koszt pojawia się gdy:
 * 1. Ja płaciłem (payerId == currentUserId) i participant jest w sharedWith
 *    → splitValue z sharedWith[participantId], isAmountPositive = true (do mnie)
 * 2. Participant płacił (payerId == participantId) i ja jestem w sharedWith
 *    → splitValue z sharedWith[currentUserId], isAmountPositive = false (ode mnie)
 */
private fun buildCostRows(
    expenses: List<ExpenseDto>,
    participantId: String,
    currentUserId: String
): List<SettlementDetailCostRow> {

    val result = mutableListOf<SettlementDetailCostRow>()

    for (expense in expenses) {
        // Przypadek 1: Ja płaciłem → participant w sharedWith
        if (expense.payerId == currentUserId) {
            val share = expense.sharedWith.find { it.participantId == participantId }
            if (share != null) {
                result.add(
                    SettlementDetailCostRow(
                        expenseId = expense.id,
                        expenseName = expense.name,
                        splitAmount = share.splitValue.valueMainCurrency,
                        currency = expense.currency,
                        formattedAmount = "%.2f %s".format(share.splitValue.valueMainCurrency, expense.currency),
                        isSettled = share.isSettlement,
                        isAmountPositive = true  // Participant mi jest winien
                    )
                )
            }
        }

        // Przypadek 2: Participant płacił → ja w sharedWith
        if (expense.payerId == participantId) {
            val share = expense.sharedWith.find { it.participantId == currentUserId }
            if (share != null) {
                result.add(
                    SettlementDetailCostRow(
                        expenseId = expense.id,
                        expenseName = expense.name,
                        splitAmount = share.splitValue.valueMainCurrency,
                        currency = expense.currency,
                        formattedAmount = "%.2f %s".format(share.splitValue.valueMainCurrency, expense.currency),
                        isSettled = share.isSettlement,
                        isAmountPositive = false  // Ja jestem winien participantowi
                    )
                )
            }
        }
    }

    return result
}