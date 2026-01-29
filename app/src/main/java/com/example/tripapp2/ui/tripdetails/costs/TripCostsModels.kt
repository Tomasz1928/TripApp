package com.example.tripapp2.ui.tripdetails.costs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.tripapp2.data.model.CategoryRegistry
import com.example.tripapp2.data.model.ExpenseDto
import com.example.tripapp2.ui.common.extension.toShortDateString

/**
 * UI Models dla Trip Costs
 */

/**
 * Model szczegółów wydatku (dla modala)
 */
data class ExpenseDetailUiModel(
    val isMine: Boolean,
    val id: String,
    val name: String,
    @StringRes val category: Int,
    @DrawableRes val categoryIconName: Int,
    val description: String,
    val date: String,
    val payerId: String,
    val payerName: String,
    val amountCostCurrency: Float,
    val currencyCost: String,
    val currencyTrip: String,
    val formattedAmountCostCurrency: String,
    val amountTripCurrency: Float?,
    val formattedAmountTripCurrency: String,
    val sharedWith: List<ShareItemUiModel>
)

/**
 * Model podziału wydatku
 */
data class ShareItemUiModel(
    val personName: String,
    val amountCostCurrency: Float?,
    val formattedAmountCostCurrency: String,
    val amountTripCurrency: Float,
    val formattedAmountTripCurrency: String
)

/**
 * Typ filtra wydatków
 */
enum class ExpenseFilter {
    ALL,                // Wszystkie
    MINE,               // Dotyczące mnie
    PAID_BY_ME,         // Płaciłem ja
    PAID_BY_OTHERS      // Płacili inni
}

/**
 * Stan ekranu Trip Costs
 */
sealed class TripCostsState {
    object Loading : TripCostsState()
    data class Success(
        val expenses: List<ExpenseDetailUiModel>,
        val currentFilter: ExpenseFilter
    ) : TripCostsState()
    object Empty : TripCostsState()
    data class Error(val message: String) : TripCostsState()
}

/**
 * Konwertuje ExpenseDto na ExpenseDetailUiModel
 */
fun ExpenseDto.toDetailUiModel(
    currentUserId: String,
    mainCurrency: String,
): ExpenseDetailUiModel {

    return ExpenseDetailUiModel(
        id = id,
        name = name,
        category = CategoryRegistry.getById(categoryId).nameResId,
        categoryIconName = CategoryRegistry.getById(categoryId).iconResId,
        description = description ?: "",
        date = date.toShortDateString(),
        payerId = payerId,
        payerName = payerNickname,

        // Cost currency (waluta wydatku)
        amountCostCurrency = amount,
        currencyCost = currency,
        formattedAmountCostCurrency = "%.2f %s".format(amount, currency),

        // Trip currency (główna waluta wycieczki)
        currencyTrip = mainCurrency,
        amountTripCurrency = totalExpense.valueMainCurrency, // valueMainCurrency to wartość w trip currency!
        formattedAmountTripCurrency = "%.2f %s".format(totalExpense.valueMainCurrency, mainCurrency),

        isMine = sharedWith.any { it.participantId == currentUserId },
        sharedWith = sharedWith.map { share ->
            // shareValue.valueMainCurrency = kwota w COST currency (waluta wydatku)
            // shareValue.valueOtherCurrencies = lista z TRIP currency (waluta wycieczki)

            val shareTripCurrencyValue = share.splitValue.valueOtherCurrencies
                .firstOrNull { it.currency == mainCurrency }

            ShareItemUiModel(
                personName = share.participantNickname,

                // Kwota w cost currency - ZAWSZE dostępna jako valueMainCurrency
                amountCostCurrency = share.splitValue.valueMainCurrency,
                formattedAmountCostCurrency = "%.2f".format(share.splitValue.valueMainCurrency),

                amountTripCurrency = shareTripCurrencyValue?.value ?: 0f,
                formattedAmountTripCurrency = if (shareTripCurrencyValue != null) {
                    "%.2f".format(shareTripCurrencyValue.value)
                } else {
                    ""
                }
            )
        }
    )
}