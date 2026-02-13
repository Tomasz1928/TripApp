package com.example.tripapp2.ui.tripdetails.settlements

import com.example.tripapp2.data.model.ParticipantDto
import com.example.tripapp2.data.model.SettlementRelationDto

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
 */
data class SettlementParticipantUiModel(
    val participantId: String,
    val nickname: String,
    val isPlaceholder: Boolean,
    val balance: Float,                         // Dodatni = on mi jest winien, Ujemny = ja jestem winien, 0 = brak relacji
    val formattedBalance: String,               // "+150,00 PLN" lub "-75,00 PLN" lub "0,00 PLN"
    val balanceStatus: ParticipantBalanceStatus,
    val currency: String,
    val hasSettlementRelation: Boolean,         // Czy istnieje relacja w SettlementRelationDto
    val isSettled: Boolean                      // Czy rozliczenie jest oznaczone jako settled
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

/**
 * Mapuje uczestnika na model UI z informacją o balansie
 *
 * Logika:
 * - Szukamy relacji gdzie fromUserId lub toUserId to ten participant
 * - Jeśli fromUserId != myUserId (ktoś inny jest dłużnikiem) → jestem na + (on mi jest winien)
 * - Jeśli fromUserId == myUserId (ja jestem dłużnikiem) → jestem na - (ja mu jestem winien)
 * - Jeśli brak relacji → balance = 0, hasSettlementRelation = false
 */
fun ParticipantDto.toSettlementUiModel(
    myUserId: String,
    allRelations: List<SettlementRelationDto>,
    currency: String
): SettlementParticipantUiModel {

    // Szukamy relacji między mną a tym uczestnikiem
    val relationWithMe = allRelations.find { relation ->
        (relation.fromUserId == this.id && relation.toUserId == myUserId) ||
                (relation.fromUserId == myUserId && relation.toUserId == this.id)
    }

    val hasRelation = relationWithMe != null
    val isSettled = relationWithMe?.isSettled ?: false

    val balance: Float
    val balanceStatus: ParticipantBalanceStatus

    if (relationWithMe == null) {
        // Brak relacji - kwota 0
        balance = 0f
        balanceStatus = ParticipantBalanceStatus.SETTLED
    } else if (isSettled) {
        // Rozliczone - pokazujemy kwotę ale status SETTLED
        balance = if (relationWithMe.fromUserId != myUserId) {
            // On mi był winien (fromUserId to ten participant)
            relationWithMe.amount.valueMainCurrency
        } else {
            // Ja mu byłem winien
            -relationWithMe.amount.valueMainCurrency
        }
        balanceStatus = ParticipantBalanceStatus.SETTLED
    } else {
        // Nierozliczone - oblicz balans
        if (relationWithMe.fromUserId != myUserId) {
            // fromUserId to ten participant, więc ON mi jest winien → jestem na +
            balance = relationWithMe.amount.valueMainCurrency
            balanceStatus = ParticipantBalanceStatus.POSITIVE
        } else {
            // fromUserId to ja, więc JA mu jestem winien → jestem na -
            balance = -relationWithMe.amount.valueMainCurrency
            balanceStatus = ParticipantBalanceStatus.NEGATIVE
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
        isSettled = isSettled
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