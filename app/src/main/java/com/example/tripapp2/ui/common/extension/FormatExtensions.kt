package com.example.tripapp2.ui.common.extension

import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension functions dla formatowania danych UI
 * Używane w całej aplikacji dla spójności
 */

/**
 * Formatuje kwotę w danej walucie
 * @param currency Kod waluty (PLN, EUR, USD, itp.)
 * @return Sformatowany string, np. "1 234,56 PLN"
 */
fun Float.toCurrency(currency: String): String {
    return "%,.2f %s".format(this, currency)
}

/**
 * Formatuje kwotę bez miejsc po przecinku
 * @param currency Kod waluty
 * @return Sformatowany string, np. "1 235 PLN"
 */
fun Float.toCurrencyShort(currency: String): String {
    return "%,.0f %s".format(this, currency)
}

/**
 * Formatuje parę dat na zakres
 * @return String w formacie "15 Lipca 2025 - 25 Lipca 2025"
 */
fun Pair<Long, Long>.toDateRange(): String {
    val format = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val startStr = format.format(Date(first))
    val endStr = format.format(Date(second))
    return "$startStr - $endStr"
}

/**
 * Formatuje datę
 * @return String w formacie "15 Lipca 2025"
 */
fun Long.toDateString(): String {
    val format = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return format.format(Date(this))
}

/**
 * Formatuje datę w krótkim formacie
 * @return String w formacie "15.07.2025"
 */
fun Long.toShortDateString(): String {
    val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return format.format(Date(this))
}