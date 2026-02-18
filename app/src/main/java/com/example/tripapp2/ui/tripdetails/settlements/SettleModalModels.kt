package com.example.tripapp2.ui.tripdetails.settlements

import com.example.tripapp2.data.model.SettlementRelationDto
import com.example.tripapp2.data.model.SimpleMoneyValueDto

// ==========================================
// UI MODELS - Settle Modal
// ==========================================

/**
 * Kierunek kwoty w rozliczeniu
 */
enum class SettleAmountDirection {
    TO_RECEIVE,  // Do odebrania (kwota dodatnia - on mi jest winien)
    TO_GIVE      // Do oddania (kwota ujemna - ja jestem winien)
}

/**
 * Model waluty do rozliczenia
 * Zawiera informacje o dostępnej kwocie w danej walucie
 */
data class SettleCurrencyOption(
    val currency: String,
    val availableAmount: Float,              // Zawsze wartość bezwzględna (bez minusa)
    val isMainCurrency: Boolean,
    val direction: SettleAmountDirection,    // Kierunek: do odebrania czy do oddania
    val formattedAmount: String              // "200,00 PLN" (bez znaku)
)

/**
 * Model dla modala rozliczenia
 * Zawiera wszystkie dane potrzebne do wyświetlenia modala
 */
data class SettleModalUiModel(
    val participantId: String,
    val participantNickname: String,
    val isOwedToMe: Boolean,                    // true = on mi jest winien (główna waluta), false = ja jestem winien
    val relationDescription: String,            // "Jesteś winien" lub "Jest Ci winien"
    val currencies: List<SettleCurrencyOption>,  // Wszystkie waluty do rozliczenia (główna + dodatkowe)
    val tripCurrency: String                    // Waluta wycieczki (do referencji)
)

/**
 * Request rozliczenia - wysyłany do ViewModel
 */
data class SettleRequest(
    val tripId: String,
    val fromUserId: String,         // ID dłużnika
    val toUserId: String,           // ID wierzyciela
    val amount: Float,              // Kwota do rozliczenia (zawsze dodatnia)
    val currency: String,           // Waluta rozliczenia
    val isMainCurrency: Boolean,    // Czy to główna waluta wycieczki
    val direction: SettleAmountDirection  // Kierunek: TO_RECEIVE lub TO_GIVE
)

// ==========================================
// MAPPERS
// ==========================================

/**
 * Tworzy SettleModalUiModel z nowego SettlementRelationDto
 *
 * Nowa logika:
 * - leftForSettled zawiera listę SimpleMoneyValueDto ze znakiem (+/-)
 * - Każda waluta z leftForSettled staje się SettleCurrencyOption
 * - Pomijamy waluty z amount ~= 0
 *
 * @param participant Model UI uczestnika
 * @param relation Relacja rozliczeniowa (nowy format)
 * @param tripCurrency Główna waluta wycieczki
 */
fun createSettleModalModel(
    participant: SettlementParticipantUiModel,
    relation: SettlementRelationDto,
    tripCurrency: String
): SettleModalUiModel {

    // Buduj listę opcji walutowych z leftForSettled
    val currencyOptions = relation.leftForSettled
        .filter { kotlin.math.abs(it.amount) > 0.01f }  // Pomijaj zerowe
        .map { money ->
            val direction = if (money.amount > 0) {
                SettleAmountDirection.TO_RECEIVE
            } else {
                SettleAmountDirection.TO_GIVE
            }
            val absAmount = kotlin.math.abs(money.amount)

            SettleCurrencyOption(
                currency = money.currency,
                availableAmount = absAmount,
                isMainCurrency = money.isMainCurrency,
                direction = direction,
                formattedAmount = "%.2f %s".format(absAmount, money.currency)
            )
        }
        // Sortuj: główna waluta pierwsza
        .sortedByDescending { it.isMainCurrency }

    // Kierunek główny - na podstawie głównej waluty (lub pierwszej dostępnej)
    val mainDirection = currencyOptions.firstOrNull { it.isMainCurrency }?.direction
        ?: currencyOptions.firstOrNull()?.direction
        ?: SettleAmountDirection.TO_RECEIVE

    val isOwedToMe = mainDirection == SettleAmountDirection.TO_RECEIVE

    val relationDescription = if (isOwedToMe) {
        "Jest Ci winien/winna"
    } else {
        "Jesteś winien/winna"
    }

    return SettleModalUiModel(
        participantId = relation.relatedId,
        participantNickname = relation.relatedName,
        isOwedToMe = isOwedToMe,
        relationDescription = relationDescription,
        currencies = currencyOptions,
        tripCurrency = tripCurrency
    )
}