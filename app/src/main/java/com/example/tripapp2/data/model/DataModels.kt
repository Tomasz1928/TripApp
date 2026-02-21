package com.example.tripapp2.data.model


data class TripListDto(
    val trips: List<TripDto>? = null
)

data class TripDto(
    val id: String,
    val title: String,
    val dateStart: Long,
    val dateEnd: Long,
    val currency: String,
    val description: String? = null,
    val totalExpenses: Float,
    val categories: List<CategoryDto>,
    val ownerId: String,
    val imOwner: Boolean,
    val myCost: List<SimpleMoneyValueDto>,
    val expenses: List<ExpenseDto>,
    val participants: List<ParticipantDto>,
    val settlement: SettlementDto?
)

data class CategoryDto(
    val categoryId: String,
    val totalAmount: Float
)

data class ExpenseDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val totalExpense: List<SimpleMoneyValueDto>,
    val amount: Float,
    val currency: String,
    val date: Long,
    val categoryId: String,
    val payerId: String,
    val sharedWith: List<ShareDto>,
    val payerNickname: String
)

data class ShareDto(
    val participantId: String,
    val participantNickname: String,
    val splitValue: List<SimpleMoneyValueDto>,
    val isSettlement: Boolean
)

data class ParticipantDto(
    val id: String,
    val nickname: String,
    val totalExpenses: List<SimpleMoneyValueDto>,
    val isOwner: Boolean,
    val isPlaceholder: Boolean,
    val accessCode: String?,
    val isActive: Boolean
)

// ==========================================
// SETTLEMENT MODELS (REFACTORED)
// ==========================================

data class SettlementDto(
    val relations: List<SettlementRelationDto>?
)

data class SettlementRelationDto(
    val relatedId: String,
    val relatedName: String,
    val leftForSettled: List<SimpleMoneyValueDto>,
    val allRelatedAmount: List<SimpleMoneyValueDto>,
    val prepayment: PrepaymentDetailsDto
)

data class PrepaymentDetailsDto(
    val amountLeft: List<SimpleMoneyValueDto>,
    val history: List<PrepaymentHistoryDto>
)

data class PrepaymentHistoryDto(
    val date: Long,
    val values: SimpleMoneyValueDto
)

data class SimpleMoneyValueDto(
    val isMainCurrency: Boolean,
    val currency: String,
    val amount: Float
)

data class SettlementResultDto(
    val success: SuccessDto,
    val trip: TripDto? = null
)

data class UserInfoDto(
    val id: String,
    val nickname: String
)

data class SuccessDto(
    val success: Boolean,
    val message: String? = null
)

data class CreateTripDto(
    val success: SuccessDto,
    val trip: TripDto? = null
)

data class JoinTripDto(
    val success: SuccessDto,
    val trip: TripDto? = null
)

data class AddExpenseDto(
    val success: SuccessDto,
    val trip: TripDto? = null
)

data class UpdateExpenseDto(
    val success: SuccessDto,
    val trip: TripDto? = null
)

data class DeleteExpenseDto(
    val success: SuccessDto,
    val trip: TripDto? = null
)

data class ParticipantsDto(
    val success: SuccessDto,
    val trip: TripDto? = null
)

data class AddExpenseRequest(
    val tripId: String,
    val name: String,
    val description: String? = null,
    val amount: Float,
    val currency: String,
    val categoryId: String,
    val date: Long,
    val payerId: String,
    val payerNickname: String,
    val sharedWith: List<ShareRequest>
)

data class UpdateExpenseRequest(
    val tripId: String,
    val expenseId: String,
    val name: String,
    val description: String? = null,
    val amount: Float,
    val currency: String,
    val categoryId: String,
    val date: Long,
    val payerId: String,
    val payerNickname: String,
    val sharedWith: List<ShareRequest>
)

// ==========================================
// HELPER EXTENSIONS for SettlementRelationDto
// ==========================================
data class ShareRequest(
    val participantId: String,
    val participantNickname: String,
    val splitValue: List<SimpleMoneyValueDto>
)

/**
 * Czy relacja jest w pełni rozliczona (wszystkie leftForSettled == 0)
 */
val SettlementRelationDto.isSettled: Boolean
    get() = leftForSettled.all { kotlin.math.abs(it.amount) < 0.01f }

/**
 * Balans w głównej walucie (z leftForSettled)
 * + = on mi jest winien, - = ja jestem winien
 */
val SettlementRelationDto.mainCurrencyBalance: Float
    get() = leftForSettled.firstOrNull { it.isMainCurrency }?.amount ?: 0f

/**
 * Czy istnieje jakakolwiek niezerowa kwota do rozliczenia
 */
val SettlementRelationDto.hasOutstandingAmount: Boolean
    get() = leftForSettled.any { kotlin.math.abs(it.amount) > 0.01f }

// ==========================================
// HELPER EXTENSIONS for List<SimpleMoneyValueDto>
// ==========================================


fun List<SimpleMoneyValueDto>.mainCurrencyAmount(): Float =
    firstOrNull { it.isMainCurrency }?.amount ?: 0f

fun List<SimpleMoneyValueDto>.amountFor(currency: String): Float =
    firstOrNull { it.currency == currency }?.amount ?: 0f