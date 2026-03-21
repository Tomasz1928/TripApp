package com.example.tripapp2.ui.tripdetails.settlements

import com.example.tripapp2.data.model.ExpenseDto
import com.example.tripapp2.data.model.PrepaymentDetailsDto
import com.example.tripapp2.data.model.PrepaymentHistoryDto
import com.example.tripapp2.data.model.ShareDto
import com.example.tripapp2.data.model.SettlementBreakdownType
import com.example.tripapp2.data.model.SimpleMoneyValueDto
import com.example.tripapp2.data.model.mainCurrencyAmount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
 * Logika ikon (settlement breakdown):
 * - SELF → 1 duża ikona SELF (szara osoba + zielone V)
 * - Częściowo rozliczony (UNSETTLED w breakdown):
 *     → duża ikona UNSETTLED (zegar + czerwone X)
 *     + małe ikonki typów które już rozliczyły część
 * - W pełni rozliczony:
 *     → 1 duża ikona dominującego typu (ten z największą amountTrip)
 *
 * Kierunek:
 * - isAmountPositive = true → pieniądze do mnie (zielony)
 * - isAmountPositive = false → pieniądze ode mnie (czerwony)
 */
data class SettlementDetailCostRow(
    val expenseId: String,
    val expenseName: String,
    val splitAmount: Float,
    val currency: String,
    val formattedAmount: String,        // "150,00 PLN"
    val isSettled: Boolean,
    val isAmountPositive: Boolean,      // true = do mnie (zielony), false = ode mnie (czerwony)
    val dominantType: SettlementBreakdownType,          // NOWE: typ do dużej ikony (24dp)
    val secondaryTypes: List<SettlementBreakdownType>   // NOWE: typy do małych ikon (16dp)
)

/**
 * Kierunek zaliczki w UI
 */
enum class PrepaymentAmountDirection {
    TO_ME,      // Participant dał mi zaliczkę (amount > 0) → strzałka w dół, zielona
    FROM_ME     // Ja dałem zaliczkę (amount < 0) → strzałka w górę, czerwona
}

/**
 * Model wiersza "Pozostało z zaliczek" (Tab 3 - sekcja górna)
 *
 * [kwota] [waluta] [strzałka kierunku]
 */
data class PrepaymentAmountLeftRow(
    val currency: String,
    val amount: Float,
    val formattedAmount: String,        // "100,00"
    val direction: PrepaymentAmountDirection
)

/**
 * Model wiersza historii zaliczki (Tab 3 - sekcja dolna)
 *
 * [kwota] [waluta] [strzałka kierunku] [data]
 */
data class PrepaymentHistoryRow(
    val currency: String,
    val amount: Float,
    val formattedAmount: String,        // "100,00"
    val formattedDate: String,          // "4 paź 2024"
    val direction: PrepaymentAmountDirection
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

    // Tab 2: Koszty
    val costRows: List<SettlementDetailCostRow>,

    // Tab 3: Zaliczki
    val prepaymentAmountLeftRows: List<PrepaymentAmountLeftRow>,
    val prepaymentHistoryRows: List<PrepaymentHistoryRow>,
    val hasPrepaymentData: Boolean      // Czy są jakiekolwiek dane o zaliczkach
)

// ==========================================
// MAPPER / FACTORY
// ==========================================

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("pl"))

/**
 * Tworzy SettlementDetailsUiModel na podstawie danych z SettlementParticipantUiModel + TripDto
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

    // --- Tab 2: Koszty ---

    val costRows = buildCostRows(
        expenses = expenses,
        participantId = participant.participantId,
        currentUserId = currentUserId
    )

    // --- Tab 3: Zaliczki ---

    val prepaymentDetails = participant.prepayment

    val amountLeftRows = buildPrepaymentAmountLeftRows(prepaymentDetails)
    val historyRows = buildPrepaymentHistoryRows(prepaymentDetails)

    val hasPrepaymentData = amountLeftRows.isNotEmpty() || historyRows.isNotEmpty()

    return SettlementDetailsUiModel(
        participantId = participant.participantId,
        participantNickname = participant.nickname,
        tripCurrency = tripCurrency,
        allRelatedRows = allRelatedRows,
        leftForSettledRows = leftForSettledRows,
        costRows = costRows,
        prepaymentAmountLeftRows = amountLeftRows,
        prepaymentHistoryRows = historyRows,
        hasPrepaymentData = hasPrepaymentData
    )
}

// ==========================================
// PRIVATE HELPERS — Tab 1
// ==========================================

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

// ==========================================
// PRIVATE HELPERS — Tab 2 (ZMIENIONE)
// ==========================================

/**
 * Buduje listę kosztów dotyczących relacji ja ↔ participant
 *
 * ZMIENIONE: dodano resolveBreakdownIcons() do obliczania dominantType i secondaryTypes
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
                val (dominant, secondary) = resolveBreakdownIcons(share)
                result.add(
                    SettlementDetailCostRow(
                        expenseId = expense.id,
                        expenseName = expense.name,
                        splitAmount = share.splitValue.mainCurrencyAmount(),
                        currency = expense.currency,
                        formattedAmount = "%.2f %s".format(share.splitValue.mainCurrencyAmount(), expense.currency),
                        isSettled = share.isSettlement,
                        isAmountPositive = true,  // Participant mi jest winien
                        dominantType = dominant,
                        secondaryTypes = secondary
                    )
                )
            }
        }

        // Przypadek 2: Participant płacił → ja w sharedWith
        if (expense.payerId == participantId) {
            val share = expense.sharedWith.find { it.participantId == currentUserId }
            if (share != null) {
                val (dominant, secondary) = resolveBreakdownIcons(share)
                result.add(
                    SettlementDetailCostRow(
                        expenseId = expense.id,
                        expenseName = expense.name,
                        splitAmount = share.splitValue.mainCurrencyAmount(),
                        currency = expense.currency,
                        formattedAmount = "%.2f %s".format(share.splitValue.mainCurrencyAmount(), expense.currency),
                        isSettled = share.isSettlement,
                        isAmountPositive = false,  // Ja jestem winien participantowi
                        dominantType = dominant,
                        secondaryTypes = secondary
                    )
                )
            }
        }
    }

    return result
}

/**
 * Logika doboru ikon na podstawie settlement_breakdown:
 *
 * 1. Jeśli SELF jest w breakdown → dominant = SELF, brak secondary
 *    (własny split — płacący = uczestnik)
 *
 * 2. Jeśli UNSETTLED jest w breakdown (nie w pełni rozliczone):
 *    - dominant = UNSETTLED (duża ikona — zegar + czerwony X)
 *    - secondary = pozostałe typy bez UNSETTLED (małe ikonki — co już rozliczono)
 *
 * 3. Jeśli w pełni rozliczone (brak UNSETTLED):
 *    - dominant = typ z największą amountTrip (główny sposób rozliczenia)
 *    - secondary = puste (wystarczy 1 duża ikona)
 *
 * 4. Fallback (brak danych breakdown — stare dane):
 *    - isSettlement=true → MANUAL_BY_AMOUNT
 *    - isSettlement=false → UNSETTLED
 */
private fun resolveBreakdownIcons(
    share: ShareDto
): Pair<SettlementBreakdownType, List<SettlementBreakdownType>> {

    val breakdown = share.settlementBreakdown

    // Fallback: brak danych breakdown (backward compatibility)
    if (breakdown.isEmpty()) {
        return if (share.isSettlement) {
            SettlementBreakdownType.MANUAL_BY_AMOUNT to emptyList()
        } else {
            SettlementBreakdownType.UNSETTLED to emptyList()
        }
    }

    val types = breakdown.map { it.type }.distinct()

    // Przypadek 1: SELF
    if (types.contains(SettlementBreakdownType.SELF)) {
        return SettlementBreakdownType.SELF to emptyList()
    }

    // Przypadek 2: jest UNSETTLED (częściowo rozliczone)
    if (types.contains(SettlementBreakdownType.UNSETTLED)) {
        val secondary = types.filter { it != SettlementBreakdownType.UNSETTLED }
        return SettlementBreakdownType.UNSETTLED to secondary
    }

    // Przypadek 3: w pełni rozliczone → dominant = typ z największą kwotą
    val dominant = breakdown
        .filter { it.type != SettlementBreakdownType.SELF }
        .maxByOrNull { it.amountTrip }
        ?.type ?: SettlementBreakdownType.MANUAL_BY_AMOUNT

    return dominant to emptyList()
}

// ==========================================
// PRIVATE HELPERS — Tab 3
// ==========================================

/**
 * Buduje wiersze "Pozostało z zaliczek" z PrepaymentDetailsDto.amountLeft
 *
 * Pomija waluty z kwotą ~0
 */
private fun buildPrepaymentAmountLeftRows(
    prepaymentDetails: PrepaymentDetailsDto
): List<PrepaymentAmountLeftRow> {

    return prepaymentDetails.amountLeft
        .filter { kotlin.math.abs(it.amount) > 0.01f }
        .sortedByDescending { it.isMainCurrency }
        .map { money ->
            PrepaymentAmountLeftRow(
                currency = money.currency,
                amount = money.amount,
                formattedAmount = "%.2f".format(kotlin.math.abs(money.amount)),
                direction = if (money.amount > 0) PrepaymentAmountDirection.TO_ME
                else PrepaymentAmountDirection.FROM_ME
            )
        }
}

/**
 * Buduje wiersze historii zaliczek z PrepaymentDetailsDto.history
 *
 * Sortuje od najnowszej do najstarszej
 */
private fun buildPrepaymentHistoryRows(
    prepaymentDetails: PrepaymentDetailsDto
): List<PrepaymentHistoryRow> {

    return prepaymentDetails.history
        .sortedByDescending { it.date }
        .map { entry ->
            PrepaymentHistoryRow(
                currency = entry.values.currency,
                amount = entry.values.amount,
                formattedAmount = "%.2f".format(kotlin.math.abs(entry.values.amount)),
                formattedDate = dateFormat.format(Date(entry.date)),
                direction = if (entry.values.amount > 0) PrepaymentAmountDirection.TO_ME
                else PrepaymentAmountDirection.FROM_ME
            )
        }
}