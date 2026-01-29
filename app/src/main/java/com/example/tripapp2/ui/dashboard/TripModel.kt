package com.example.tripapp2.ui.dashboard

// DODANE: Walidacja w data class
data class PieCategory(
    val label: String,
    val value: Float,
    val color: Int,
    val currency: String = "zł"
) {
    init {
        require(label.isNotBlank()) { "Label cannot be blank" }
        require(value >= 0) { "Value cannot be negative" }
    }
}

data class TripModel(
    val title: String,
    val date: String,
    val totalValue: Float,
    val currency: String = "zł",
    val categories: List<PieCategory>
) {
    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(totalValue >= 0) { "Total value cannot be negative" }
    }
}

data class CostModel(
    val name: String,
    val payer: String,
    val amount: Float,
    val currency: String,
    val isMine: Boolean // czy dotyczy mnie
) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(amount >= 0) { "Amount cannot be negative" }
    }
}

data class MyExpensesModel(
    val totalInBaseCurrency: Float,          // Suma w bazowej walucie
    val baseCurrency: String = "EUR",        // Kod waluty bazowej
    val amountsInOtherCurrencies: List<CurrencyAmount>  // Lista kwot w innych walutach
) {
    init {
        require(totalInBaseCurrency >= 0) { "Total cannot be negative" }
    }
}

data class CurrencyAmount(
    val amount: Float,       // Kwota
    val currency: String     // Kod waluty (PLN, USD, itp.)
) {
    init {
        require(amount >= 0) { "Amount cannot be negative" }
        require(currency.isNotBlank()) { "Currency cannot be blank" }
    }
}

data class ExpenseDetail(
    val name: String,
    val category: String,
    val description: String,
    val date: String,
    val payer: String,
    val amountMain: Float,          // kwota w walucie głównej
    val mainCurrency: String,       // np. PLN
    val amountSecondary: Float?,    // kwota w dodatkowej walucie (jeśli jest)
    val secondaryCurrency: String?, // np. EUR
    val sharedWith: List<ShareItem> // lista osób i kwot
) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(amountMain >= 0) { "Amount cannot be negative" }
        require(amountSecondary == null || amountSecondary >= 0) { "Secondary amount cannot be negative" }
    }
}

// Osoba i kwota, zamiast procentów
data class ShareItem(
    val person: String,
    val amountMain: Float,
    val amountSecondary: Float
) {
    init {
        require(person.isNotBlank()) { "Person name cannot be blank" }
        require(amountMain >= 0) { "Amount cannot be negative" }
        require(amountSecondary >= 0) { "Secondary amount cannot be negative" }
    }
}