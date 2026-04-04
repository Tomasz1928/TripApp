package com.example.tripapp2.ui.tripdetails.settlements

import com.example.tripapp2.data.model.ParticipantDto
import com.example.tripapp2.data.model.PrepaymentDetailsDto
import com.example.tripapp2.data.model.SettlementRelationDto
import com.example.tripapp2.data.model.SimpleMoneyValueDto
import com.example.tripapp2.data.model.isSettled
import com.example.tripapp2.data.model.mainCurrencyBalance
import com.example.tripapp2.data.model.hasOutstandingAmount
import com.example.tripapp2.data.model.notMainCurrencyBalance

// ==========================================
// UI MODELS - Settlements
// ==========================================

/**
 * Status balansu względem uczestnika
 */
enum class ParticipantBalanceStatus {
    POSITIVE,      // On mi jest winien (jestem na +)
    NEGATIVE,      // Ja mu jestem winien (jestem na -)
    SETTLED        // Rozliczeni (0) lub brak relacji
}

/**
 * Model uczestnika z informacją o balansie względem mnie
 *
 * balance > 0 → on mi jest winien
 * balance < 0 → ja jestem winien
 */
data class SettlementParticipantUiModel(
    val participantId: String,
    val nickname: String,
    val isPlaceholder: Boolean,
    val balance: Float,                         // Główna waluta: + on mi jest winien, - ja jestem winien
    val formattedBalance: String,               // "+150,00 PLN" lub "-75,00 PLN" lub "0,00 PLN"
    val balanceStatus: ParticipantBalanceStatus,
    val currency: String,                       // Główna waluta tripu
    val hasSettlementRelation: Boolean,         // Czy istnieje relacja w SettlementRelationDto
    val isSettled: Boolean,                     // Czy rozliczenie jest w pełni zakończone
    val leftForSettled: List<SimpleMoneyValueDto>,       // Ile pozostało do rozliczenia per waluta
    val allRelatedAmount: List<SimpleMoneyValueDto>,     // Całkowita kwota relacji per waluta
    val prepayment: PrepaymentDetailsDto                 // Szczegóły zaliczek (amountLeft + history)
)

/**
 * Kierunek zaliczki
 */
enum class PrepaymentDirection {
    TO_ME,         // Pieniądze trafiają do mnie (uczestnik daje mi zaliczkę)
    FROM_ME        // Ja daję pieniądze (ja daję zaliczkę uczestnikowi)
}

/**
 * Model dla modala zaliczki
 */
data class PrepaymentUiModel(
    val participantId: String,
    val participantNickname: String,
    val availableCurrencies: List<String>,
    val currentBalance: Float,
    val formattedCurrentBalance: String,
    val balanceStatus: ParticipantBalanceStatus
)

/**
 * Request dodania zaliczki
 */
data class PrepaymentRequest(
    val tripId: String,
    val participantId: String,
    val amount: Float,
    val currency: String,
    val direction: PrepaymentDirection
)

/**
 * Stan ekranu rozliczeń
 */
sealed class TripSettlementsState {
    object Loading : TripSettlementsState()

    data class Success(
        val participants: List<SettlementParticipantUiModel>,
        val tripCurrency: String,
        val myTotalBalance: Float,
        val formattedMyTotalBalance: String,
        val myBalanceStatus: ParticipantBalanceStatus
    ) : TripSettlementsState()

    object Empty : TripSettlementsState()  // Brak innych uczestników

    data class Error(val message: String) : TripSettlementsState()
}

// ==========================================
// MAPPERS
// ==========================================

private val emptyPrepaymentDetails = PrepaymentDetailsDto(
    amountLeft = emptyList(),
    history = emptyList()
)

fun ParticipantDto.toSettlementUiModel(
    allRelations: List<SettlementRelationDto>,
    currency: String
): SettlementParticipantUiModel {

    // Szukamy relacji dla tego uczestnika (relatedId = participant.id)
    val relationWithMe = allRelations.find { it.relatedId == this.id }

    val hasRelation = relationWithMe != null
    val settled = relationWithMe?.isSettled ?: false

    val balance: Float
    val balanceStatus: ParticipantBalanceStatus

    if (relationWithMe == null) {
        // Brak relacji - kwota 0
        balance = 0f
        balanceStatus = ParticipantBalanceStatus.SETTLED
    } else if (settled) {
        // W pełni rozliczone
        balance = relationWithMe.mainCurrencyBalance
        balanceStatus = ParticipantBalanceStatus.SETTLED
    } else {
        // Nierozliczone - balans z głównej waluty w leftForSettled
        balance = relationWithMe.mainCurrencyBalance
        balanceStatus = when {
            balance > 0.01f -> ParticipantBalanceStatus.POSITIVE
            balance < -0.01f -> ParticipantBalanceStatus.NEGATIVE
            else -> ParticipantBalanceStatus.SETTLED
        }
    }

    val formattedBalance = formatBalance(balance, currency, balanceStatus)

    return SettlementParticipantUiModel(
        participantId = this.id,
        nickname = this.nickname,
        isPlaceholder = this.isPlaceholder,
        balance = balance,
        formattedBalance = formattedBalance,
        balanceStatus = balanceStatus,
        currency = currency,
        hasSettlementRelation = hasRelation,
        isSettled = settled,
        leftForSettled = relationWithMe?.leftForSettled ?: emptyList(),
        allRelatedAmount = relationWithMe?.allRelatedAmount ?: emptyList(),
        prepayment = relationWithMe?.prepayment ?: emptyPrepaymentDetails
    )
}

/**
 * Formatuje balans do wyświetlenia
 */
private fun formatBalance(balance: Float, currency: String, status: ParticipantBalanceStatus): String {
    return when (status) {
        ParticipantBalanceStatus.POSITIVE -> "+%.2f %s".format(balance, currency)
        ParticipantBalanceStatus.NEGATIVE -> "%.2f %s".format(balance, currency)
        ParticipantBalanceStatus.SETTLED -> "0,00 %s".format(currency)
    }
}

/**
 * Sortuje uczestników:
 * 1. Najpierw ci, którym jestem winien (NEGATIVE) - wg wielkości
 * 2. Potem ci, którzy mi są winni (POSITIVE) - wg wielkości
 * 3. Na końcu rozliczeni/bez relacji (SETTLED)
 */
fun List<SettlementParticipantUiModel>.sortByBalance(): List<SettlementParticipantUiModel> {
    return sortedWith(compareBy(
        { participant ->
            when (participant.balanceStatus) {
                ParticipantBalanceStatus.NEGATIVE -> 0
                ParticipantBalanceStatus.POSITIVE -> 1
                ParticipantBalanceStatus.SETTLED -> 2
            }
        },
        { -kotlin.math.abs(it.balance) }
    ))
}

/**
 * Określa status całkowitego balansu
 */
fun Float.toBalanceStatus(): ParticipantBalanceStatus {
    return when {
        this > 0.01f -> ParticipantBalanceStatus.POSITIVE
        this < -0.01f -> ParticipantBalanceStatus.NEGATIVE
        else -> ParticipantBalanceStatus.SETTLED
    }
}

/**
 * Formatuje całkowity balans
 */
fun Float.formatTotalBalance(currency: String): String {
    val status = this.toBalanceStatus()
    return when (status) {
        ParticipantBalanceStatus.POSITIVE -> "+%.2f %s".format(this, currency)
        ParticipantBalanceStatus.NEGATIVE -> "%.2f %s".format(this, currency)
        ParticipantBalanceStatus.SETTLED -> "0,00 %s".format(currency)
    }
}

/**
 * Oblicza mój całkowity balans na podstawie relacji
 * Sumuje mainCurrencyBalance ze wszystkich relacji
 */
fun calculateMyTotalBalance(relations: List<SettlementRelationDto>): Float {
    return relations.sumOf { it.mainCurrencyBalance.toDouble() }.toFloat()
}