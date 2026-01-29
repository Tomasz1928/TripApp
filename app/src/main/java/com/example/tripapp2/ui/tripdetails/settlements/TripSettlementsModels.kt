package com.example.tripapp2.ui.tripdetails.settlements

import com.example.tripapp2.data.model.BalanceStatus
import com.example.tripapp2.data.model.SettlementDto
import com.example.tripapp2.data.model.SettlementRelationDto

// ==========================================
// UI MODELS
// ==========================================

/**
 * Model bilansu użytkownika
 */
data class UserBalanceUiModel(
    val totalBalance: Float,
    val mainCurrency: String,
    val formattedBalance: String,
    val balanceStatus: BalanceStatusUi,
    val owedToYou: Float,
    val youOwe: Float,
    val formattedOwedToYou: String,
    val formattedYouOwe: String
)

enum class BalanceStatusUi {
    NA_PLUSIE,
    NA_MINUSIE,
    ROZLICZONY
}

/**
 * Model pojedynczej relacji rozliczeniowej
 */
data class SettlementRelationUiModel(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val fromUserName: String,
    val toUserName: String,
    val amountMainCurrency: Float,
    val mainCurrency: String,
    val formattedAmount: String,
    val isCurrentUserInvolved: Boolean,
    val currentUserIsDebtor: Boolean,
    val isSettled: Boolean
)

/**
 * Model szczegółów rozliczenia (dla modala)
 */
data class SettlementDetailUiModel(
    val relationId: String,
    val fromUserName: String,
    val toUserName: String,
    val amountMainCurrency: Float,
    val mainCurrency: String,
    val formattedAmountMain: String,
    val otherCurrencies: List<CurrencyAmountUiModel>,
    val isCurrentUserDebtor: Boolean
)

/**
 * Model kwoty w konkretnej walucie
 */
data class CurrencyAmountUiModel(
    val currency: String,
    val amount: Float,
    val formattedAmount: String
)

/**
 * Stan ekranu rozliczeń
 */
sealed class TripSettlementsState {
    object Loading : TripSettlementsState()
    data class Success(
        val userBalance: UserBalanceUiModel,
        val relations: List<SettlementRelationUiModel>,
        val tripName: String
    ) : TripSettlementsState()
    object AllSettled : TripSettlementsState()
    data class Error(val message: String) : TripSettlementsState()
}

// ==========================================
// MAPPERS
// ==========================================

/**
 * Mapuje SettlementDto na Success state
 */
fun SettlementDto.toSuccessState(
    currentUserId: String,
    tripTitle: String,
    tripCurrency: String
): TripSettlementsState.Success {

    // Mapuj status balansu
    val balanceStatusUi = when (balanceStatus) {
        BalanceStatus.PLUS -> BalanceStatusUi.NA_PLUSIE
        BalanceStatus.MINUS -> BalanceStatusUi.NA_MINUSIE
    }

    // Oblicz ile Ci winni i ile ty winien
    val owedToYou = relations?.filter {
        it.toUserId == currentUserId && !it.isSettled
    }?.sumOf { it.amount.valueMainCurrency.toDouble() }?.toFloat() ?: 0f

    val youOwe = relations?.filter {
        it.fromUserId == currentUserId && !it.isSettled
    }?.sumOf { it.amount.valueMainCurrency.toDouble() }?.toFloat() ?: 0f

    // Stwórz bilans użytkownika
    val userBalance = UserBalanceUiModel(
        totalBalance = balance,
        mainCurrency = tripCurrency,
        formattedBalance = "%.0f %s".format(kotlin.math.abs(balance), tripCurrency),
        balanceStatus = balanceStatusUi,
        owedToYou = owedToYou,
        youOwe = youOwe,
        formattedOwedToYou = "%.0f %s".format(owedToYou, tripCurrency),
        formattedYouOwe = "%.0f %s".format(youOwe, tripCurrency)
    )

    // Mapuj relacje
    val relationModels = relations?.map { it.toUiModel(currentUserId, tripCurrency) } ?: emptyList()

    return TripSettlementsState.Success(
        userBalance = userBalance,
        relations = relationModels,
        tripName = tripTitle
    )
}

/**
 * Mapuje SettlementRelationDto na UI model
 */
fun SettlementRelationDto.toUiModel(
    currentUserId: String,
    tripCurrency: String
): SettlementRelationUiModel {
    return SettlementRelationUiModel(
        id = "${fromUserId}_${toUserId}",
        fromUserId = fromUserId,
        toUserId = toUserId,
        fromUserName = fromUserName,
        toUserName = toUserName,
        amountMainCurrency = amount.valueMainCurrency,
        mainCurrency = tripCurrency,
        formattedAmount = "%.2f %s".format(amount.valueMainCurrency, tripCurrency),
        isCurrentUserInvolved = fromUserId == currentUserId || toUserId == currentUserId,
        currentUserIsDebtor = fromUserId == currentUserId,
        isSettled = isSettled
    )
}

/**
 * Tworzy szczegóły rozliczenia dla modala
 */
fun SettlementRelationDto.toDetailUiModel(
    currentUserId: String,
    tripCurrency: String
): SettlementDetailUiModel {
    // Mapuj inne waluty
    val otherCurrencies = amount.valueOtherCurrencies.map { currency ->
        CurrencyAmountUiModel(
            currency = currency.currency,
            amount = currency.value,
            formattedAmount = "%.2f %s".format(currency.value, currency.currency)
        )
    }

    return SettlementDetailUiModel(
        relationId = "${fromUserId}_${toUserId}",
        fromUserName = fromUserName,
        toUserName = toUserName,
        amountMainCurrency = amount.valueMainCurrency,
        mainCurrency = tripCurrency,
        formattedAmountMain = "%.2f %s".format(amount.valueMainCurrency, tripCurrency),
        otherCurrencies = otherCurrencies,
        isCurrentUserDebtor = fromUserId == currentUserId
    )
}