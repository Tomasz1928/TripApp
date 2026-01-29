package com.example.tripapp2.ui.dashboard

import android.content.Context
import android.graphics.Color
import com.example.tripapp2.data.model.CategoryDto
import com.example.tripapp2.data.model.CategoryRegistry
import com.example.tripapp2.data.model.TripDto

/**
 * UI Models dla Dashboard
 * Modele dostosowane do potrzeb UI - zawierają już przeliczone wartości,
 * sformatowane stringi, kolory jako Int, itp.
 */

/**
 * Model wycieczki dla UI
 */
data class TripUiModel(
    val id: String,
    val title: String,
    val dateRange: String,          // Już sformatowany: "10 - 15 Grudnia 2024"
    val totalValue: Float,
    val currency: String,
    val totalFormatted: String,     // Już sformatowany: "4 250 PLN"
    val categories: List<PieCategoryUiModel>
)

/**
 * Kategoria dla wykresu kołowego
 */
data class PieCategoryUiModel(
    val label: String,
    val value: Float,
    val color: Int,                 // Kolor jako Int (Color.parseColor)
    val formattedValue: String      // "1 234 PLN"
)

/**
 * Stan ekranu Dashboard
 */
sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val trips: List<TripDto>) : DashboardState()  // Zostaw TripDto
    object Empty : DashboardState()
    data class Error(val message: String) : DashboardState()
}

// ==========================================
// MAPPERS - Data Layer → UI Layer
// ==========================================

/**
 * Konwertuje TripDto na TripUiModel
 */
fun TripDto.toUiModel(context: Context): TripUiModel {
    return TripUiModel(
        id = id,
        title = title,
        dateRange = formatDateRange(dateStart, dateEnd),
        totalValue = totalExpenses,
        currency = currency,
        totalFormatted = "%,.0f %s".format(totalExpenses, currency),
        categories = categories.map { it.toUiModel(currency, context) }
    )
}

/**
 * Mapuje CategoryDto na PieCategoryUiModel
 */
fun CategoryDto.toUiModel(currency: String, context: Context): PieCategoryUiModel {
    // Pobierz informacje o kategorii z rejestru
    val categoryInfo = CategoryRegistry.getById(categoryId)

    return PieCategoryUiModel(
        label = context.getString(categoryInfo.nameResId),
        value = totalAmount,
        color = Color.parseColor(categoryInfo.colorHex),
        formattedValue = "%,.0f %s".format(totalAmount, currency)
    )
}

/**
 * Formatuje zakres dat
 */
private fun formatDateRange(start: Long, end: Long): String {
    val format = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault())
    val startStr = format.format(java.util.Date(start))
    val endStr = format.format(java.util.Date(end))

    // Jeśli miesiąc i rok się zgadzają, skróć format
    val startParts = startStr.split(" ")
    val endParts = endStr.split(" ")

    return if (startParts[1] == endParts[1] && startParts[2] == endParts[2]) {
        "${startParts[0]} - ${endParts[0]} ${endParts[1]} ${endParts[2]}"
    } else {
        "$startStr - $endStr"
    }
}