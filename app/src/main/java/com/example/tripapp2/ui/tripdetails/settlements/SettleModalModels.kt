package com.example.tripapp2.ui.tripdetails.settlements

import com.example.tripapp2.data.model.MoneyValueDto
import com.example.tripapp2.data.model.SettlementRelationDto

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
    val isOwedToMe: Boolean,                    // true = on mi jest winien, false = ja jestem winien
    val relationDescription: String,            // "Jesteś winien" lub "Jest Ci winien"
    val mainCurrency: SettleCurrencyOption,     // Główna waluta wycieczki
    val otherCurrencies: List<SettleCurrencyOption>,  // Dodatkowe waluty
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
 * Tworzy SettleModalUiModel z danych uczestnika i relacji
 *
 * @param participant Model UI uczestnika
 * @param relation Relacja rozliczeniowa z MockData
 * @param tripCurrency Główna waluta wycieczki
 * @param currentUserId ID aktualnego użytkownika
 */
fun createSettleModalModel(
    participant: SettlementParticipantUiModel,
    relation: SettlementRelationDto,
    tripCurrency: String,
    currentUserId: String
): SettleModalUiModel {

    // Określ czy on mi jest winien czy ja jemu (na podstawie relacji, nie kwoty)
    val isOwedToMe = relation.toUserId == currentUserId

    // Opis relacji
    val relationDescription = if (isOwedToMe) {
        "Jest Ci winien/winna"
    } else {
        "Jesteś winien/winna"
    }

    // Główna waluta - obsługa ujemnych wartości
    val mainCurrencyValue = relation.amount.valueMainCurrency
    val mainCurrencyDirection = if (mainCurrencyValue >= 0) {
        SettleAmountDirection.TO_RECEIVE
    } else {
        SettleAmountDirection.TO_GIVE
    }
    val mainCurrencyAbsValue = kotlin.math.abs(mainCurrencyValue)

    val mainCurrencyOption = SettleCurrencyOption(
        currency = tripCurrency,
        availableAmount = mainCurrencyAbsValue,
        isMainCurrency = true,
        direction = mainCurrencyDirection,
        formattedAmount = "%.2f %s".format(mainCurrencyAbsValue, tripCurrency)
    )

    // Dodatkowe waluty - obsługa ujemnych wartości
    val otherCurrencyOptions = relation.amount.valueOtherCurrencies.map { moneyDetail ->
        val direction = if (moneyDetail.value >= 0) {
            SettleAmountDirection.TO_RECEIVE
        } else {
            SettleAmountDirection.TO_GIVE
        }
        val absValue = kotlin.math.abs(moneyDetail.value)

        SettleCurrencyOption(
            currency = moneyDetail.currency,
            availableAmount = absValue,
            isMainCurrency = false,
            direction = direction,
            formattedAmount = "%.2f %s".format(absValue, moneyDetail.currency)
        )
    }

    return SettleModalUiModel(
        participantId = participant.participantId,
        participantNickname = participant.nickname,
        isOwedToMe = isOwedToMe,
        relationDescription = relationDescription,
        mainCurrency = mainCurrencyOption,
        otherCurrencies = otherCurrencyOptions,
        tripCurrency = tripCurrency
    )
}

/**
 * Pobiera wszystkie dostępne opcje walut (główna + dodatkowe)
 */
fun SettleModalUiModel.getAllCurrencyOptions(): List<SettleCurrencyOption> {
    return listOf(mainCurrency) + otherCurrencies
}

/**
 * Sprawdza czy kwota jest w prawidłowym zakresie dla danej waluty
 */
fun SettleCurrencyOption.isAmountValid(amount: Float): Boolean {
    return amount > 0f && amount <= availableAmount + 0.01f // Mały margines na błędy zaokrąglenia
}

/**
 * Formatuje zakres kwot dla wybranej waluty
 */
fun SettleCurrencyOption.formatAmountRange(): String {
    return "Zakres: 0,01 - %.2f %s".format(availableAmount, currency)
}