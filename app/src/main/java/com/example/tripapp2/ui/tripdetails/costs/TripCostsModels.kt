package com.example.tripapp2.ui.tripdetails.costs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.tripapp2.data.model.CategoryRegistry
import com.example.tripapp2.data.model.ExpenseDto
import com.example.tripapp2.data.model.ShareDto
import com.example.tripapp2.data.model.SettlementBreakdownType
import com.example.tripapp2.data.model.mainCurrencyAmount
import com.example.tripapp2.data.model.notMainCurrencyAmount
import com.example.tripapp2.ui.common.extension.toShortDateString

/**
 * UI Models dla Trip Costs
 */

/**
 * Model szczegółów wydatku (dla modala)
 */
data class ExpenseDetailUiModel(
    val isMine: Boolean,
    val id: String,
    val name: String,
    @StringRes val category: Int,
    @DrawableRes val categoryIconName: Int,
    val description: String,
    val hasReceipt: Boolean = false,
    val receiptHash: String? = null,
    val date: String,
    val payerId: String,
    val payerName: String,
    val amountCostCurrency: Float,
    val currencyCost: String,
    val currencyTrip: String,
    val formattedAmountCostCurrency: String,
    val amountTripCurrency: Float?,
    val formattedAmountTripCurrency: String,
    val sharedWith: List<ShareItemUiModel>
)

/**
 * Model podziału wydatku
 *
 * ZMIENIONE: dodano dominantType i secondaryTypes dla ikon breakdown
 */
data class ShareItemUiModel(
    val personName: String,
    val amountCostCurrency: Float?,
    val formattedAmountCostCurrency: String,
    val amountTripCurrency: Float,
    val formattedAmountTripCurrency: String,
    val isSettlement: Boolean,
    val dominantType: SettlementBreakdownType,           // NOWE: typ do dużej ikony (24dp)
    val secondaryTypes: List<SettlementBreakdownType>    // NOWE: typy do małych ikon (16dp)
)

/**
 * Typ filtra wydatków
 */
enum class ExpenseFilter {
    ALL,                // Wszystkie
    MINE,               // Dotyczące mnie
    PAID_BY_ME,         // Płaciłem ja
    PAID_BY_OTHERS      // Płacili inni
}

/**
 * Stan ekranu Trip Costs
 */
sealed class TripCostsState {
    object Loading : TripCostsState()
    data class Success(
        val expenses: List<ExpenseDetailUiModel>,
        val currentFilter: ExpenseFilter
    ) : TripCostsState()
    object Empty : TripCostsState()
    data class Error(val message: String) : TripCostsState()
}

/**
 * Konwertuje ExpenseDto na ExpenseDetailUiModel
 *
 * totalExpense i splitValue są teraz List<SimpleMoneyValueDto>
 *
 * ZMIENIONE: mapowanie ShareDto → ShareItemUiModel teraz wywołuje resolveBreakdownIcons()
 */
fun ExpenseDto.toDetailUiModel(
    currencyParticipantId: String,
    mainCurrency: String,
): ExpenseDetailUiModel {

    // totalExpense: główna waluta = trip currency
    val totalExpenseMainAmount = totalExpense.mainCurrencyAmount()

    return ExpenseDetailUiModel(
        id = id,
        name = name,
        category = CategoryRegistry.getById(categoryId).nameResId,
        categoryIconName = CategoryRegistry.getById(categoryId).iconResId,
        description = description ?: "",
        date = date.toShortDateString(),
        payerId = payerId,
        payerName = payerNickname,
        hasReceipt = this.hasReceipt,
        receiptHash = this.receiptHash,

        // Cost currency (waluta wydatku)
        amountCostCurrency = amount,
        currencyCost = currency,
        formattedAmountCostCurrency = "%.2f %s".format(amount, currency),

        // Trip currency (główna waluta wycieczki)
        currencyTrip = mainCurrency,
        amountTripCurrency = totalExpenseMainAmount,
        formattedAmountTripCurrency = "%.2f %s".format(totalExpenseMainAmount, mainCurrency),


        isMine = sharedWith.any { it.participantId == currencyParticipantId },
        sharedWith = sharedWith.map { share ->
            // splitValue jest teraz List<SimpleMoneyValueDto>
            // Główna waluta (isMainCurrency=true) = kwota w cost currency
            // Dodatkowe waluty = przeliczenia (np. trip currency)

            val shareTripCurrencyValue = share.splitValue.mainCurrencyAmount()
            val shareCostCurrencyAmount = share.splitValue.notMainCurrencyAmount()

            // NOWE: oblicz ikony breakdown
            val (dominant, secondary) = resolveShareBreakdownIcons(share)

            ShareItemUiModel(
                isSettlement = share.isSettlement,
                personName = share.participantNickname,

                amountCostCurrency = shareCostCurrencyAmount,
                formattedAmountCostCurrency ="%.2f".format(shareCostCurrencyAmount),

                amountTripCurrency = shareTripCurrencyValue,
                formattedAmountTripCurrency = "%.2f".format(shareTripCurrencyValue),

                // NOWE: breakdown icons
                dominantType = dominant,
                secondaryTypes = secondary
            )
        }
    )
}

// ==========================================
// BREAKDOWN ICONS LOGIC
// ==========================================

/**
 * Logika doboru ikon — identyczna jak w SettlementDetailsModels.resolveBreakdownIcons()
 *
 * 1. SELF → dominant = SELF, brak secondary
 * 2. UNSETTLED w breakdown → dominant = UNSETTLED, secondary = reszta typów
 * 3. W pełni rozliczone → dominant = typ z największą amountTrip
 * 4. Fallback (brak breakdown) → isSettlement ? MANUAL_BY_AMOUNT : UNSETTLED
 */
private fun resolveShareBreakdownIcons(
    share: ShareDto
): Pair<SettlementBreakdownType, List<SettlementBreakdownType>> {

    val breakdown = share.settlementBreakdown

    // Fallback: brak danych breakdown (backward compatibility)
    if (breakdown.isEmpty()) {
        return if (share.isSettlement) {
            SettlementBreakdownType.MANUAL_BY_AMOUNT to emptyList()
        } else {
            SettlementBreakdownType.UNSETTLED to emptyList()
        }
    }

    val types = breakdown.map { it.type }.distinct()

    // Przypadek 1: SELF
    if (types.contains(SettlementBreakdownType.SELF)) {
        return SettlementBreakdownType.SELF to emptyList()
    }

    // Przypadek 2: jest UNSETTLED (częściowo rozliczone)
    if (types.contains(SettlementBreakdownType.UNSETTLED)) {
        val secondary = types.filter { it != SettlementBreakdownType.UNSETTLED }
        return SettlementBreakdownType.UNSETTLED to secondary
    }

    // Przypadek 3: w pełni rozliczone → dominant = typ z największą kwotą
    val dominant = breakdown
        .filter { it.type != SettlementBreakdownType.SELF }
        .maxByOrNull { it.amountTrip }
        ?.type ?: SettlementBreakdownType.MANUAL_BY_AMOUNT

    return dominant to emptyList()
}