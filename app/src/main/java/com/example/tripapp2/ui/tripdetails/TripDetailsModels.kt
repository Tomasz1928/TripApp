package com.example.tripapp2.ui.tripdetails

import com.example.tripapp2.data.model.TripDto
import com.example.tripapp2.ui.common.extension.toDateRange

/**
 * UI Models dla Trip Details
 */

/**
 * Model szczegółów wycieczki dla UI
 */
data class TripDetailsUiModel(
    val id: String,
    val title: String,
    val description: String,
    val dateRange: String,              // "12.08 - 18.08.2024"
    val accessCode: String,
    val myTotalExpenses: String,        // "450,00 PLN"
    val myExpensesBreakdown: List<CurrencyExpenseUiModel>
)

/**
 * Wydatek w konkretnej walucie
 */
data class CurrencyExpenseUiModel(
    val currency: String,
    val amount: Float,
    val formattedAmount: String        // "120,00 PLN"
)

/**
 * Stan ekranu Trip Details
 */
sealed class TripDetailsState {
    object Loading : TripDetailsState()
    data class Success(val details: TripDetailsUiModel) : TripDetailsState()
    data class Error(val message: String) : TripDetailsState()
}

/**
 * Event kopiowania kodu dostępu
 */
data class CopyAccessCodeEvent(
    val code: String,
    val message: String = "Skopiowano kod dostępu"
)

// ==========================================
// MAPPERS
// ==========================================

/**
 * Konwertuje TripDto na TripDetailsUiModel
 */
fun TripDto.toDetailsUiModel(): TripDetailsUiModel {
    // Oblicz wydatki użytkownika
    val myExpenses = calculateUserExpenses()
    val totalInBaseCurrency = myExpenses.sumOf { it.amount.toDouble() }.toFloat()

    return TripDetailsUiModel(
        id = id,
        title = title,
        description = description.toString(),
        dateRange = (dateStart to dateEnd).toDateRange(),
        accessCode = accessCode,
        myTotalExpenses = "%.2f %s".format(totalInBaseCurrency, currency),
        myExpensesBreakdown = myExpenses.map { expense ->
            CurrencyExpenseUiModel(
                currency = expense.currency,
                amount = expense.amount,
                formattedAmount = "%.2f %s".format(expense.amount, expense.currency)
            )
        }
    )
}

/**
 * Oblicza wydatki użytkownika z danej wycieczki
 */
private fun TripDto.calculateUserExpenses(): List<CurrencyExpenseUiModel> {
    val expensesByCurrency = mutableMapOf<String, Float>()

    expensesByCurrency[currency] = myCost.valueMainCurrency

    myCost.valueOtherCurrencies.forEach { expense ->
        expensesByCurrency[expense.currency] = expense.value
    }

    return expensesByCurrency.map { (currency, amount) ->
        CurrencyExpenseUiModel(
            currency = currency,
            amount = amount,
            formattedAmount = "%.2f %s".format(amount, currency)
        )
    }
}