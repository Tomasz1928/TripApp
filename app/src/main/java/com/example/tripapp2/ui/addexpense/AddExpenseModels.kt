package com.example.tripapp2.ui.addexpense

/**
 * Model uczestnika w podziale kosztów
 */
data class SplitParticipant(
    val id: String,
    val name: String,
    var isSelected: Boolean = false,
    var amount: Float = 0f
)

/**
 * Typ podziału kosztów
 */
enum class SplitType {
    EQUAL,      // Po równo
    MANUAL      // Ręczny podział
}

/**
 * Model podziału kosztów
 */
data class ExpenseSplit(
    val splitType: SplitType = SplitType.EQUAL,
    val participants: List<SplitParticipant> = emptyList()
) {
    fun getSelectedParticipants() = participants.filter { it.isSelected }

    fun getManualTotal() = participants.filter { it.isSelected }.sumOf { it.amount.toDouble() }.toFloat()

    fun isValid(totalAmount: Float): Boolean {
        val selected = getSelectedParticipants()
        if (selected.isEmpty()) return false

        return when (splitType) {
            SplitType.EQUAL -> true
            SplitType.MANUAL -> {
                val manualTotal = getManualTotal()
                kotlin.math.abs(manualTotal - totalAmount) < 0.01f
            }
        }
    }

    /**
     * Oblicza podział po równo dla wybranych uczestników
     * Reszta z dzielenia trafia do ostatniej osoby
     */
    fun calculateEqualSplit(totalAmount: Float): ExpenseSplit {
        val selected = getSelectedParticipants()
        if (selected.isEmpty()) return this

        val baseAmount = (totalAmount / selected.size * 100).toInt() / 100f // Zaokrąglij do 2 miejsc
        val remainder = totalAmount - (baseAmount * selected.size)

        val updatedParticipants = participants.mapIndexed { index, p ->
            if (p.isSelected) {
                val isLast = index == participants.indexOfLast { it.isSelected }
                val amount = if (isLast) baseAmount + remainder else baseAmount
                p.copy(amount = amount)
            } else {
                p.copy(amount = 0f)
            }
        }

        return copy(participants = updatedParticipants)
    }
}