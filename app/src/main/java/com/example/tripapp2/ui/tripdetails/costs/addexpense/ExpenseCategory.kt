package com.example.tripapp2.ui.tripdetails.costs.addexpense

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
        ExpenseCategory("10", R.string.category_activities, R.drawable.ic_category_activities),   // Aktywności
        ExpenseCategory("13", R.string.category_electronics, R.drawable.ic_category_electronics), // Elektronika
        ExpenseCategory("14", R.string.category_other_fees, R.drawable.ic_category_other_fees),   // Inne opłaty
        ExpenseCategory("6", R.string.category_food, R.drawable.ic_category_food),                // Jedzenie
        ExpenseCategory("9", R.string.category_accommodation, R.drawable.ic_category_accommodation), // Nocleg
        ExpenseCategory("8", R.string.category_drinks, R.drawable.ic_category_drinks),            // Napoje
        ExpenseCategory("5", R.string.category_fuel, R.drawable.ic_category_fuel),                // Paliwo
        ExpenseCategory("12", R.string.category_souvenirs, R.drawable.ic_category_souvenirs),    // Pamiątki
        ExpenseCategory("3", R.string.category_train, R.drawable.ic_category_train),              // Pociąg
        ExpenseCategory("2", R.string.category_flights, R.drawable.ic_category_flights),          // Loty
        ExpenseCategory("7", R.string.category_groceries, R.drawable.ic_category_groceries),     // Zakupy spożywcze
        ExpenseCategory("4", R.string.category_car_rent, R.drawable.ic_category_car_rent),       // Wynajem auta
        ExpenseCategory("1", R.string.category_transport, R.drawable.ic_category_transport),     // Transport
        ExpenseCategory("11", R.string.category_clothing, R.drawable.ic_category_clothing)       // Ubrania
    )

    fun getById(id: String): ExpenseCategory? = ALL.find { it.id == id }
}