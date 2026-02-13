package com.example.tripapp2.ui.tripdetails.settlements

import com.example.tripapp2.data.model.ParticipantDto
import com.example.tripapp2.data.model.SettlementRelationDto

// ==========================================
// UI MODELS - Settlements (moje relacje)
// ==========================================

/**
 * Model uczestnika z informacją o balansie względem mnie
 */
data class SettlementParticipantUiModel(
    val participantId: String,
    val nickname: String,
    val isPlaceholder: Boolean,
    val balance: Float,                    // Dodatni = on mi jest winien, Ujemny = ja jestem winien
    val formattedBalance: String,          // "+150,00 PLN" lub "-75,00 PLN"
    val balanceStatus: ParticipantBalanceStatus,
    val currency: String,
    val hasSettlementDetails: Boolean
)

/**
 * Status balansu względem uczestnika
 */
enum class ParticipantBalanceStatus {
    POSITIVE,      // On mi jest winien (jestem na +)
    NEGATIVE,      // Ja mu jestem winien (jestem na -)
    SETTLED        // Rozliczeni (0)
}

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
    val formattedCurrentBalance: String
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
        val tripName: String,
        val tripCurrency: String,
        val myTotalBalance: Float,
        val formattedMyTotalBalance: String
    ) : TripSettlementsState()

    object Empty : TripSettlementsState()  // Brak innych uczestników

    data class Error(val message: String) : TripSettlementsState()
}

// ==========================================
// MAPPERS
// ==========================================

/**
 * Mapuje uczestnika na model z balansem
 *
 * @param participant - uczestnik z listy
 * @param myRelationsAsDebtor - relacje gdzie JA jestem dłużnikiem (ja winien innym)
 * @param myRelationsAsCreditor - relacje gdzie JA jestem wierzycielem (inni winni mnie)
 * @param currency - waluta tripu
 */
fun ParticipantDto.toSettlementUiModel(
    myRelationsAsDebtor: List<SettlementRelationDto>,
    myRelationsAsCreditor: List<SettlementRelationDto>,
    currency: String
): SettlementParticipantUiModel {

    // Ile TEN uczestnik jest mi winien (relacje gdzie on jest dłużnikiem, ja wierzycielem)
    val theyOweMe = myRelationsAsCreditor
        .filter { it.fromUserId == this.id && !it.isSettled }
        .sumOf { it.amount.valueMainCurrency.toDouble() }
        .toFloat()

    // Ile JA jestem winien TEMU uczestnikowi (relacje gdzie ja jestem dłużnikiem, on wierzycielem)
    val iOweThem = myRelationsAsDebtor
        .filter { it.toUserId == this.id && !it.isSettled }
        .sumOf { it.amount.valueMainCurrency.toDouble() }
        .toFloat()

    // Bilans: dodatni = on mi jest winien, ujemny = ja mu jestem winien
    val balance = theyOweMe - iOweThem

    val balanceStatus = when {
        balance > 0.01f -> ParticipantBalanceStatus.POSITIVE
        balance < -0.01f -> ParticipantBalanceStatus.NEGATIVE
        else -> ParticipantBalanceStatus.SETTLED
    }

    val formattedBalance = when (balanceStatus) {
        ParticipantBalanceStatus.POSITIVE -> "+%.2f %s".format(balance, currency)
        ParticipantBalanceStatus.NEGATIVE -> "%.2f %s".format(balance, currency)
        ParticipantBalanceStatus.SETTLED -> "0,00 %s".format(currency)
    }

    // Sprawdź czy są jakiekolwiek relacje z tym uczestnikiem
    val hasDetails = myRelationsAsCreditor.any { it.fromUserId == this.id } ||
            myRelationsAsDebtor.any { it.toUserId == this.id }

    return SettlementParticipantUiModel(
        participantId = this.id,
        nickname = this.nickname,
        isPlaceholder = this.isPlaceholder,
        balance = balance,
        formattedBalance = formattedBalance,
        balanceStatus = balanceStatus,
        currency = currency,
        hasSettlementDetails = hasDetails
    )
}

/**
 * Sortuje uczestników: najpierw nie-rozliczeni (wg wielkości balansu), potem rozliczeni
 */
fun List<SettlementParticipantUiModel>.sortByBalance(): List<SettlementParticipantUiModel> {
    return sortedWith(compareBy(
        { participant ->
            when (participant.balanceStatus) {
                ParticipantBalanceStatus.NEGATIVE -> 0  // Najpierw ci, którym jestem winien
                ParticipantBalanceStatus.POSITIVE -> 1  // Potem ci, którzy mi są winni
                ParticipantBalanceStatus.SETTLED -> 2   // Na końcu rozliczeni
            }
        },
        { -kotlin.math.abs(it.balance) }  // W ramach grupy: wg wielkości balansu (malejąco)
    ))
}
