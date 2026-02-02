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
        ExpenseCategory("1", R.string.category_transport, R.drawable.ic_category_transport),
        ExpenseCategory("2", R.string.category_flights, R.drawable.ic_category_flights),
        ExpenseCategory("3", R.string.category_train, R.drawable.ic_category_train),
        ExpenseCategory("4", R.string.category_car_rent, R.drawable.ic_category_car_rent),
        ExpenseCategory("5", R.string.category_fuel, R.drawable.ic_category_fuel),
        ExpenseCategory("6", R.string.category_food, R.drawable.ic_category_food),
        ExpenseCategory("7", R.string.category_groceries, R.drawable.ic_category_groceries),
        ExpenseCategory("8", R.string.category_drinks, R.drawable.ic_category_drinks),
        ExpenseCategory("9", R.string.category_accommodation, R.drawable.ic_category_accommodation),
        ExpenseCategory("10", R.string.category_activities, R.drawable.ic_category_activities),
        ExpenseCategory("11", R.string.category_clothing, R.drawable.ic_category_clothing),
        ExpenseCategory("12", R.string.category_souvenirs, R.drawable.ic_category_souvenirs),
        ExpenseCategory("13", R.string.category_electronics, R.drawable.ic_category_electronics),
        ExpenseCategory("14", R.string.category_other_fees, R.drawable.ic_category_other_fees)
    )

    fun getById(id: String): ExpenseCategory? = ALL.find { it.id == id }
}