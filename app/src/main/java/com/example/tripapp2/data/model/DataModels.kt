package com.example.tripapp2.data.model

// ==========================================
// TRIP LIST (lightweight, z query tripList)
// ==========================================

data class TripListItemDto(
    val id: String,
    val title: String,
    val dateStart: Long,
    val dateEnd: Long,
    val currency: String,
    val description: String? = null,
    val totalExpenses: Float,
    val imOwner: Boolean,
)

// ==========================================
// TRIP DETAIL (full)
// ==========================================

data class TripListDto(
    val trips: List<TripIdDto>? = null
)

data class TripIdDto(
    val id: String
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
    val myParticipantId: Int,
    val myCost: List<SimpleMoneyValueDto>,
    val expenses: List<ExpenseDto>,
    val participants: List<ParticipantDto>,
    val settlement: SettlementDto?,
)

data class MoneyValueDetailsDto(
    val currency: String,
    val value: Float
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
    val leftForSettled: List<SimpleMoneyValueDto>,
    val isSettlement: Boolean,
    val settlementBreakdown: List<SettlementBreakdownEntryDto> = emptyList()
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

data class SettlementBreakdownEntryDto(
    val type: SettlementBreakdownType,
    val amountCost: Float,
    val amountTrip: Float
)

enum class SettlementBreakdownType {
    SELF,
    MANUAL_BY_AMOUNT,
    MANUAL_BY_COSTS,
    AUTO_PREPAYMENT,
    AUTO_CROSS_SETTLE,
    UNSETTLED
}

/**
 * Typ zdarzenia w historii rozliczeń
 */
enum class SettlementHistoryEventType {
    MANUAL_BY_AMOUNT,
    MANUAL_BY_COSTS,
    MANUAL_BY_PREPAYMENT,
    AUTO_PREPAYMENT,
    AUTO_CROSS_SETTLE
}

data class RelationSettlementHistoryDto(
    val id: Int,
    val settlementType: SettlementHistoryEventType,
    val actorNickname: String?,
    val amountInSettlementCurrency: Float,
    val settlementCurrency: String,
    val amountInTripCurrency: Float,
    val relatedExpenseNames: List<String>,
    val createdAt: Long
)


// ==========================================
// SETTLEMENT MODELS
// ==========================================

data class SettlementDto(
    val relations: List<SettlementRelationDto>?
)

data class SettlementRelationDto(
    val relatedId: String,
    val relatedName: String,
    val leftForSettled: List<SimpleMoneyValueDto>,
    val allRelatedAmount: List<SimpleMoneyValueDto>,
    val prepayment: PrepaymentDetailsDto,
    val settlementHistory: List<RelationSettlementHistoryDto> = emptyList()  // ← DODANE
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

// ==========================================
// RESPONSE DTOs
// ==========================================

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
    val success: Boolean,
    val message: String? = null,
    val trip: Int? = null
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

// ==========================================
// REQUEST DTOs
// ==========================================

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

data class ShareRequest(
    val participantId: String,
    val participantNickname: String,
    val splitValue: List<SimpleMoneyValueDto>
)


/**
 * Request do settle by costs — używany przez TripSettlementsViewModel
 */
data class SettleByCostsRequest(
    val expenseId: String,
    val payerId: String,
    val participantId: String
)

// ==========================================
// HELPER EXTENSIONS for SettlementRelationDto
// ==========================================

val SettlementRelationDto.isSettled: Boolean
    get() = leftForSettled.all { kotlin.math.abs(it.amount) < 0.01f }

val SettlementRelationDto.mainCurrencyBalance: Float
    get() = leftForSettled.firstOrNull { it.isMainCurrency }?.amount ?: 0f

val SettlementRelationDto.notMainCurrencyBalance: Float
    get() = leftForSettled.firstOrNull { !it.isMainCurrency }?.amount ?: 0f

val SettlementRelationDto.hasOutstandingAmount: Boolean
    get() = leftForSettled.any { kotlin.math.abs(it.amount) > 0.01f }

// ==========================================
// HELPER EXTENSIONS for List<SimpleMoneyValueDto>
// ==========================================


fun List<SimpleMoneyValueDto>.mainCurrencyAmount(): Float =
    firstOrNull { it.isMainCurrency }?.amount ?: 0f

fun List<SimpleMoneyValueDto>.notMainCurrencyAmount(): Float =
    firstOrNull { !it.isMainCurrency }?.amount ?: 0f

fun List<SimpleMoneyValueDto>.amountFor(currency: String): Float =
    firstOrNull { it.currency == currency }?.amount ?: 0f