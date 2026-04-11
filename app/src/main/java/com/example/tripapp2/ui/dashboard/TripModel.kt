package com.example.tripapp2.ui.dashboard

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