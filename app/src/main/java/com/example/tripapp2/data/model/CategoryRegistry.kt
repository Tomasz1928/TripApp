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
        "1" to CategoryInfo("#F59E0B", R.drawable.ic_category_transport, R.string.category_transport),
        "2" to CategoryInfo("#FB923C", R.drawable.ic_category_flights, R.string.category_flights),
        "3" to CategoryInfo("#FBBF24", R.drawable.ic_category_train, R.string.category_train),
        "4" to CategoryInfo("#F97316", R.drawable.ic_category_car_rent, R.string.category_car_rent),
        "5" to CategoryInfo("#EA580C", R.drawable.ic_category_fuel, R.string.category_fuel),
        "6" to CategoryInfo("#34D399", R.drawable.ic_category_food, R.string.category_food),
        "7" to CategoryInfo("#10B981", R.drawable.ic_category_groceries, R.string.category_groceries),
        "8" to CategoryInfo("#059669", R.drawable.ic_category_drinks, R.string.category_drinks),
        "9" to CategoryInfo("#136DEC", R.drawable.ic_category_accommodation, R.string.category_accommodation),
        "10" to CategoryInfo("#F43F5E", R.drawable.ic_category_activities, R.string.category_activities),
        "11" to CategoryInfo("#8B5CF6", R.drawable.ic_category_clothing, R.string.category_clothing),
        "12" to CategoryInfo("#C084FC", R.drawable.ic_category_souvenirs, R.string.category_souvenirs),
        "13" to CategoryInfo("#7C3AED", R.drawable.ic_category_electronics, R.string.category_electronics),
        "14" to CategoryInfo("#6B7280", R.drawable.ic_category_other_fees, R.string.category_other_fees)
    )

    private val default = CategoryInfo("#6B7280", R.drawable.ic_category_other_fees, R.string.category_other_fees)

    fun getById(id: String): CategoryInfo = categories[id] ?: default
}