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
    val accessCode: String,
    val ownerId: String,
    val imOwner: Boolean,
    val myCost: MoneyValueDto?,
    val expenses: List<ExpenseDto>,
    val participants: List<ParticipantDto>,
    val settlement: SettlementDto?
)

data class MoneyValueDto(
    val valueMainCurrency: Float,
    val valueOtherCurrencies: List<MoneyValueDetailsDto> = emptyList()
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
    val totalExpense: MoneyValueDto,
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
    val splitValue: MoneyValueDto
)

data class ParticipantDto(
    val id: String,
    val nickname: String,
    val totalExpenses: MoneyValueDto?,
    val isOwner: Boolean,
    val isPlaceholder: Boolean,
    val accessCode: String?,
    val isActive: Boolean
)

data class SettlementDto(
    val balance: Float,
    val balanceStatus: BalanceStatus,
    val relations: List<SettlementRelationDto>?
)

data class SettlementRelationDto(
    val fromUserId: String,
    val toUserId: String,
    val fromUserName: String,
    val toUserName: String,
    val amount: MoneyValueDto,
    val isSettled: Boolean
)

enum class BalanceStatus {
    PLUS,
    MINUS
}

data class UserInfoDto (
    val id: String,
    val nickname: String
)

data class SuccessDto(
    val success: Boolean,
    val message: String?=null
)


data class CreateTripDto(
    val success: SuccessDto,
    val trip: TripDto?=null
)

data class JoinTripDto(
    val success: SuccessDto,
    val trip: TripDto?=null
)

data class AddExpenseDto(
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
    val sharedWith: List<ShareDto>
)

data class ParticipantsDto(
    val success: SuccessDto,
    val trip: TripDto? = null

)