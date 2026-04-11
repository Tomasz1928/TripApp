package com.example.tripapp2.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.tripapp2.R

/**
 * Prosty rejestr kategorii
 * Backend zwraca tylko ID - tutaj mapujemy na kolor, ikonę i nazwę
 */
object CategoryRegistry {

    data class CategoryInfo(
        val colorHex: String,
        @DrawableRes val iconResId: Int,
        @StringRes val nameResId: Int
    )

    private val categories = mapOf(
        "10" to CategoryInfo("#FF006E", R.drawable.ic_category_activities, R.string.category_activities),     // Aktywności
        "13" to CategoryInfo("#023E8A", R.drawable.ic_category_electronics, R.string.category_electronics),   // Elektronika
        "14" to CategoryInfo("#6C757D", R.drawable.ic_category_other_fees, R.string.category_other_fees),     // Inne opłaty
        "6" to CategoryInfo("#E9C46A", R.drawable.ic_category_food, R.string.category_food),                   // Jedzenie
        "9" to CategoryInfo("#3A86FF", R.drawable.ic_category_accommodation, R.string.category_accommodation),// Nocleg
        "8" to CategoryInfo("#9D4EDD", R.drawable.ic_category_drinks, R.string.category_drinks),               // Napoje
        "5" to CategoryInfo("#6D597A", R.drawable.ic_category_fuel, R.string.category_fuel),                   // Paliwo
        "12" to CategoryInfo("#FB5607", R.drawable.ic_category_souvenirs, R.string.category_souvenirs),       // Pamiątki
        "3" to CategoryInfo("#2A9D8F", R.drawable.ic_category_train, R.string.category_train),                 // Pociąg
        "2" to CategoryInfo("#1D84B5", R.drawable.ic_category_flights, R.string.category_flights),             // Loty
        "7" to CategoryInfo("#52B788", R.drawable.ic_category_groceries, R.string.category_groceries),         // Zakupy spożywcze
        "4" to CategoryInfo("#F4A261", R.drawable.ic_category_car_rent, R.string.category_car_rent),           // Wynajem auta
        "1" to CategoryInfo("#E63946", R.drawable.ic_category_transport, R.string.category_transport),         // Transport
        "11" to CategoryInfo("#8338EC", R.drawable.ic_category_clothing, R.string.category_clothing)           // Ubrania
    )

    private val default = CategoryInfo("#6B7280", R.drawable.ic_category_other_fees, R.string.category_other_fees)

    fun getById(id: String): CategoryInfo = categories[id] ?: default
}