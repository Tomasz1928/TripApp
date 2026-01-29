package com.example.tripapp2.ui.addexpense

import androidx.annotation.DrawableRes
import com.example.tripapp2.R

/**
 * Kategoria wydatku z ikoną
 */
data class ExpenseCategory(
    val id: String,
    val nameResId: Int,
    @DrawableRes val iconResId: Int
)

/**
 * Dostępne kategorie wydatków
 */
object ExpenseCategories {

    val ALL = listOf(
        ExpenseCategory("accommodation", R.string.category_accommodation, R.drawable.ic_category_accommodation),
        ExpenseCategory("activities", R.string.category_activities, R.drawable.ic_category_activities),
        ExpenseCategory("car_rent", R.string.category_car_rent, R.drawable.ic_category_car_rent),
        ExpenseCategory("clothing", R.string.category_clothing, R.drawable.ic_category_clothing),
        ExpenseCategory("drinks", R.string.category_drinks, R.drawable.ic_category_drinks),
        ExpenseCategory("electronics", R.string.category_electronics, R.drawable.ic_category_electronics),
        ExpenseCategory("flights", R.string.category_flights, R.drawable.ic_category_flights),
        ExpenseCategory("food", R.string.category_food, R.drawable.ic_category_food),
        ExpenseCategory("fuel", R.string.category_fuel, R.drawable.ic_category_fuel),
        ExpenseCategory("groceries", R.string.category_groceries, R.drawable.ic_category_groceries),
        ExpenseCategory("other_fees", R.string.category_other_fees, R.drawable.ic_category_other_fees),
        ExpenseCategory("souvenirs", R.string.category_souvenirs, R.drawable.ic_category_souvenirs),
        ExpenseCategory("train", R.string.category_train, R.drawable.ic_category_train),
        ExpenseCategory("transport", R.string.category_transport, R.drawable.ic_category_transport)
    )

    fun getById(id: String): ExpenseCategory? = ALL.find { it.id == id }
}