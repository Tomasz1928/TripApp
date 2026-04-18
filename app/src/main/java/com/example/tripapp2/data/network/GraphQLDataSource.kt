package com.example.tripapp2.data.network

import android.util.Log
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import com.example.tripapp2.data.model.*
import com.example.tripapp2.graphql.*
import com.example.tripapp2.graphql.type.AddExpenseInput
import com.example.tripapp2.graphql.type.UpdateExpenseInput
import com.example.tripapp2.graphql.type.ShareInput
import com.example.tripapp2.graphql.type.SimpleMoneyValueInput
import com.example.tripapp2.graphql.type.SettleByCostsItem
import com.example.tripapp2.ui.tripdetails.settlements.SettleByCostsItemInput
import com.example.tripapp2.ui.tripdetails.settlements.SettleByCostsRequest
import com.example.tripapp2.graphql.AvailableCurrenciesQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * GraphQLDataSource — jedyny punkt kontaktu z Apollo Client.
 *
 * Odpowiada za:
 * 1. Wykonywanie queries/mutations/subscriptions przez Apollo
 * 2. Mapowanie Apollo-generated types → istniejące DataModels (DTO)
 * 3. Konwersję ID: backend Int → app String
 *
 * WAŻNE: Apollo generuje klasy z plików .graphql w package com.example.tripapp2.graphql.*
 * Nazwy klas = nazwy operacji: TripListQuery, TripDetailsQuery, LoginUserMutation, etc.
 *
 * KRYTYCZNE: `client` jest property z getterem (nie val), dzięki czemu
 * po ApolloClientProvider.resetAndRebuild() automatycznie używa nowego klienta.
 */
class GraphQLDataSource() {

    // POPRAWKA: Było `private val client: ApolloClient = ApolloClientProvider.getClient()`
    // To powodowało, że po resetAndRebuild() GraphQLDataSource ciągle używał
    // starego, zamkniętego klienta. Teraz dynamicznie pobiera aktualny klient.
    private val client: ApolloClient
        get() = ApolloClientProvider.getClient()

    companion object {
        private const val TAG = "GraphQLDataSource"

        @Volatile
        private var INSTANCE: GraphQLDataSource? = null

        fun getInstance(): GraphQLDataSource {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GraphQLDataSource().also { INSTANCE = it }
            }
        }
    }

    // ==========================================
    // AUTH
    // ==========================================

    suspend fun login(username: String, password: String): Result<AuthResultDto> {
        return try {
            val response = client.mutation(LoginUserMutation(username, password)).execute()
            val data = response.data?.loginUser
                ?: return Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Login failed"))

            Result.success(AuthResultDto(
                success = data.success,
                message = data.message,
                user = data.user?.let {
                    UserInfoDto(
                        id = it.id.toString(),
                        nickname = it.username,
                        email = it.email
                    )
                }
            ))
        } catch (e: ApolloException) {
            Log.e(TAG, "Login error", e)
            Result.failure(e)
        }
    }

    suspend fun register(username: String, password: String, email: String): Result<AuthResultDto> {
        return try {
            val response = client.mutation(RegisterUserMutation(username, password, email)).execute()
            val data = response.data?.registerUser
                ?: return Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Register failed"))

            Result.success(AuthResultDto(
                success = data.success,
                message = data.message,
                user = data.user?.let {
                    UserInfoDto(
                        id = it.id.toString(),
                        nickname = it.username,
                        email = it.email
                    )
                }
            ))
        } catch (e: ApolloException) {
            Log.e(TAG, "Register error", e)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(username: String, email: String): Result<AuthResultDto> {
        return try {
            val response = client.mutation(ResetPasswordMutation(username, email)).execute()
            val data = response.data?.resetPassword
                ?: return Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Reset failed"))

            Result.success(AuthResultDto(
                success = data.success,
                message = data.message,
                user = null
            ))
        } catch (e: ApolloException) {
            Log.e(TAG, "ResetPassword error", e)
            Result.failure(e)
        }
    }

    suspend fun changeEmail(newEmail: String): Result<AuthResultDto> {
        return try {
            val response = client.mutation(ChangeEmailMutation(newEmail)).execute()
            val data = response.data?.changeEmail
                ?: return Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Change email failed"))

            Result.success(AuthResultDto(
                success = data.success,
                message = data.message,
                user = null
            ))
        } catch (e: ApolloException) {
            Log.e(TAG, "ChangeEmail error", e)
            Result.failure(e)
        }
    }

    suspend fun changePassword(newPassword: String, newPasswordConfirm: String): Result<AuthResultDto> {
        return try {
            val response = client.mutation(ChangePasswordMutation(newPassword, newPasswordConfirm)).execute()
            val data = response.data?.changePassword
                ?: return Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Change password failed"))

            Result.success(AuthResultDto(
                success = data.success,
                message = data.message,
                user = null
            ))
        } catch (e: ApolloException) {
            Log.e(TAG, "ChangePassword error", e)
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<AuthResultDto> {
        return try {
            val response = client.mutation(LogoutUserMutation()).execute()
            val data = response.data?.logoutUser
                ?: return Result.failure(Exception("Logout failed"))

            Result.success(AuthResultDto(
                success = data.success,
                message = data.message,
                user = null
            ))
        } catch (e: ApolloException) {
            Log.e(TAG, "Logout error", e)
            Result.failure(e)
        }
    }

    suspend fun getSession(): Result<SessionDto> {
        return try {
            val response = client.query(SessionQuery()).execute()
            val data = response.data?.session
                ?: return Result.failure(Exception("Session query failed"))

            Result.success(SessionDto(
                isAuthenticated = data.isAuthenticated,
                user = data.user?.let { UserInfoDto(id = it.id.toString(), nickname = it.username, email = it.email) }
            ))
        } catch (e: ApolloException) {
            Log.e(TAG, "Session error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // TRIP LIST
    // ==========================================

    suspend fun getTripList(): Result<TripListDto> {
        return try {
            val response = client.query(TripListQuery()).execute()
            val data = response.data?.tripList
                ?: return Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Failed to load trips"))

            val trips = data.trips.map { t ->
                TripIdDto(id = t.id.toString())
            }

            Result.success(TripListDto(trips = trips))
        } catch (e: ApolloException) {
            Log.e(TAG, "TripList error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // TRIP DETAILS
    // ==========================================

    suspend fun getTripDetails(tripId: Int): Result<TripDto> {
        return try {
            val response = client.query(TripDetailsQuery(tripId)).execute()
            val data = response.data?.tripDetails
                ?: return Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Failed to load trip details"))

            Result.success(mapTripDetail(data))
        } catch (e: ApolloException) {
            Log.e(TAG, "TripDetails error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // TRIP CRUD
    // ==========================================

    suspend fun createTrip(
        title: String,
        dateStart: Long,
        dateEnd: Long,
        description: String,
        currency: String
    ): Result<CreateTripDto> {
        return try {
            val response = client.mutation(
                CreateTripMutation(
                    title = title,
                    dateStart = dateStart.toDouble(),
                    dateEnd = dateEnd.toDouble(),
                    description = Optional.present(description),
                    currency = Optional.present(currency)
                )
            ).execute()

            val data = response.data?.createTrip
                ?: return Result.failure(Exception("Create trip failed"))

            Result.success(CreateTripDto(
                success = data.success,
                message = data.message,
                trip = data.tripId
            ))
        } catch (e: ApolloException) {
            Log.e(TAG, "CreateTrip error", e)
            Result.failure(e)
        }
    }

    suspend fun joinTrip(accessCode: String): Result<SuccessDto> {
        return try {
            val response = client.mutation(JoinTripMutation(accessCode)).execute()
            val data = response.data?.joinTrip
                ?: return Result.failure(Exception("Join trip failed"))

            Result.success(SuccessDto(
                success = data.success,
                message = data.message),
            )
        } catch (e: ApolloException) {
            Log.e(TAG, "JoinTrip error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // EXPENSES
    // ==========================================

    suspend fun addExpense(request: AddExpenseRequest): Result<SuccessDto> {
        return try {
            val input = AddExpenseInput(
                tripId = request.tripId.toInt(),
                name = request.name,
                description = Optional.present(request.description ?: ""),
                amount = request.amount.toDouble(),
                currency = request.currency,
                categoryId = request.categoryId.toInt(),
                date = request.date.toDouble(),
                payerId = request.payerId.toInt(),
                sharedWith = request.sharedWith.map { share ->
                    ShareInput(
                        participantId = share.participantId.toInt(),
                        splitValue = share.splitValue.map { mv ->
                            SimpleMoneyValueInput(
                                currency = mv.currency,
                                amount = mv.amount.toDouble()
                            )
                        }
                    )
                }
            )

            val response = client.mutation(AddExpenseMutation(input)).execute()
            val data = response.data?.addExpense
                ?: return Result.failure(Exception("Add expense failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "AddExpense error", e)
            Result.failure(e)
        }
    }

    suspend fun updateExpense(request: UpdateExpenseRequest): Result<SuccessDto> {
        return try {
            val input = UpdateExpenseInput(
                expenseId = request.expenseId.toInt(),
                tripId = request.tripId.toInt(),
                name = request.name,
                description = Optional.present(request.description ?: ""),
                amount = request.amount.toDouble(),
                currency = request.currency,
                categoryId = request.categoryId.toInt(),
                date = request.date.toDouble(),
                payerId = request.payerId.toInt(),
                sharedWith = request.sharedWith.map { share ->
                    ShareInput(
                        participantId = share.participantId.toInt(),
                        splitValue = share.splitValue.map { mv ->
                            SimpleMoneyValueInput(
                                currency = mv.currency,
                                amount = mv.amount.toDouble()
                            )
                        }
                    )
                }
            )

            val response = client.mutation(UpdateExpenseMutation(input)).execute()
            val data = response.data?.updateExpense
                ?: return Result.failure(Exception("Update expense failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "UpdateExpense error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(tripId: String, expenseId: String): Result<SuccessDto> {
        return try {
            val response = client.mutation(
                DeleteExpenseMutation(tripId.toInt(), expenseId.toInt())
            ).execute()

            val data = response.data?.deleteExpense
                ?: return Result.failure(Exception("Delete expense failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "DeleteExpense error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // PARTICIPANTS
    // ==========================================

    suspend fun addPlaceholder(tripId: String, nickname: String): Result<SuccessDto> {
        return try {
            val response = client.mutation(
                AddPlaceholderMutation(tripId.toInt(), nickname)
            ).execute()

            val data = response.data?.addPlaceholder
                ?: return Result.failure(Exception("Add placeholder failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "AddPlaceholder error", e)
            Result.failure(e)
        }
    }

    suspend fun detachUser(tripId: String, participantId: String): Result<SuccessDto> {
        return try {
            val response = client.mutation(
                DetachUserMutation(tripId.toInt(), participantId.toInt())
            ).execute()

            val data = response.data?.detachUser
                ?: return Result.failure(Exception("Detach user failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "DetachUser error", e)
            Result.failure(e)
        }
    }

    suspend fun removePlaceholder(tripId: String, participantId: String): Result<SuccessDto> {
        return try {
            val response = client.mutation(
                RemovePlaceholderMutation(tripId.toInt(), participantId.toInt())
            ).execute()

            val data = response.data?.removePlaceholder
                ?: return Result.failure(Exception("Remove placeholder failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "RemovePlaceholder error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // SETTLEMENTS
    // ==========================================

    suspend fun addPrepayment(
        tripId: String,
        participantId: String,
        amount: Float,
        currency: String,
        direction: String
    ): Result<SuccessDto> {
        return try {
            val response = client.mutation(
                AddPrepaymentMutation(
                    tripId = tripId.toInt(),
                    participantId = participantId.toInt(),
                    amount = amount.toDouble(),
                    currency = currency,
                    direction = direction
                )
            ).execute()

            val data = response.data?.addPrepayment
                ?: return Result.failure(Exception("Add prepayment failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "AddPrepayment error", e)
            Result.failure(e)
        }
    }

    suspend fun settleByAmount(
        tripId: String,
        fromUserId: String,
        toUserId: String,
        amount: Float,
        currency: String,
        isMainCurrency: Boolean
    ): Result<SuccessDto> {
        return try {
            val response = client.mutation(
                SettleByAmountMutation(
                    tripId = tripId.toInt(),
                    fromUserId = fromUserId.toInt(),
                    toUserId = toUserId.toInt(),
                    amount = amount.toDouble(),
                    currency = currency,
                    isMainCurrency = isMainCurrency
                )
            ).execute()

            val data = response.data?.settleByAmount
                ?: return Result.failure(Exception("Settle by amount failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "SettleByAmount error", e)
            Result.failure(e)
        }
    }

    suspend fun settleByCosts(
        tripId: String,
        items: List<SettleByCostsItemInput>
    ): Result<SuccessDto> {
        return try {
            val inputItems = items.map { item ->
                SettleByCostsItem(
                    expenseId = item.expenseId.toInt(),
                    participantId = item.participantId.toInt()
                )
            }

            val response = client.mutation(
                SettleByCostsMutation(tripId.toInt(), inputItems)
            ).execute()

            val data = response.data?.settleByCosts
                ?: return Result.failure(Exception("Settle by costs failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "SettleByCosts error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // SUBSCRIPTION
    // ==========================================

    /**
     * Zwraca Flow z TripNotificationDto — UI subskrybuje to by dostawać real-time updates.
     * Wymaga zaimplementowanej subscription tripUpdates na backendzie.
     */
    fun subscribeTripUpdates(tripId: Int): Flow<TripNotificationDto> {
        return client.subscription(TripUpdatesSubscription(tripId))
            .toFlow()
            .map { response ->
                val data = response.data?.tripUpdates
                    ?: throw Exception("Empty subscription data")
                TripNotificationDto(
                    tripId = data.tripId.toString(),
                    tripName = data.tripName,
                    eventType = data.eventType.name,
                    actorNickname = data.actorNickname,
                    actorParticipantId = data.actorParticipantId
                )
            }
    }

    // ==========================================
// CURRENCIES
// ==========================================

    suspend fun getAvailableCurrencies(): Result<List<String>> {
        return try {
            val response = client.query(AvailableCurrenciesQuery()).execute()
            val data = response.data?.availableCurrencies
                ?: return Result.failure(Exception("Failed to load currencies"))

            Result.success(data)
        } catch (e: ApolloException) {
            Log.e(TAG, "AvailableCurrencies error", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // MAPPERS — Apollo types → App DTOs
    // ==========================================

    private fun mapTripDetail(data: TripDetailsQuery.TripDetails): TripDto {
        return TripDto(
            id = data.id.toString(),
            title = data.title,
            dateStart = data.dateStart.toLong(),
            dateEnd = data.dateEnd.toLong(),
            currency = data.currency,
            description = data.description,
            totalExpenses = data.totalExpenses.toFloat(),
            categories = data.categories.map { c ->
                CategoryDto(
                    categoryId = c.categoryId.toString(),
                    totalAmount = c.totalAmount.toFloat()
                )
            },
            ownerId = data.ownerId.toString(),
            imOwner = data.imOwner,
            myCost = data.myCost.map { mapMoney(it) },
            totalTripCost = data.totalTripCost.map { mapMoney(it) },
            expenses = data.expenses.map { mapExpense(it) },
            participants = data.participants.map { mapParticipant(it) },
            settlement = data.settlement?.let { mapSettlement(it) },
            myParticipantId = data.myParticipantId
        )
    }
    private fun mapHistoryEventType(
        type: com.example.tripapp2.graphql.type.SettlementHistoryEventType
    ): SettlementHistoryEventType {
        return when (type) {
            com.example.tripapp2.graphql.type.SettlementHistoryEventType.MANUAL_BY_AMOUNT ->
                SettlementHistoryEventType.MANUAL_BY_AMOUNT
            com.example.tripapp2.graphql.type.SettlementHistoryEventType.MANUAL_BY_COSTS ->
                SettlementHistoryEventType.MANUAL_BY_COSTS
            com.example.tripapp2.graphql.type.SettlementHistoryEventType.MANUAL_BY_PREPAYMENT ->
                SettlementHistoryEventType.MANUAL_BY_PREPAYMENT
            com.example.tripapp2.graphql.type.SettlementHistoryEventType.AUTO_PREPAYMENT ->
                SettlementHistoryEventType.AUTO_PREPAYMENT
            com.example.tripapp2.graphql.type.SettlementHistoryEventType.AUTO_CROSS_SETTLE ->
                SettlementHistoryEventType.AUTO_CROSS_SETTLE
            else -> SettlementHistoryEventType.MANUAL_BY_AMOUNT
        }
    }

    private fun mapExpense(e: TripDetailsQuery.Expense): ExpenseDto {
        return ExpenseDto(
            id = e.id.toString(),
            name = e.name,
            description = e.description,
            totalExpense = e.totalExpense.map { mapMoney(it) },
            amount = e.amount.toFloat(),
            currency = e.currency,
            date = e.date.toLong(),
            categoryId = e.categoryId.toString(),
            payerId = e.payerId.toString(),
            payerNickname = e.payerNickname,
            hasReceipt = e.hasReceipt,
            receiptHash = e.receiptHash,
            sharedWith = e.sharedWith.map { s ->
                ShareDto(
                    participantId = s.participantId.toString(),
                    participantNickname = s.participantNickname,
                    splitValue = s.splitValue.map { mapMoney(it) },
                    isSettlement = s.isSettlement,
                    leftForSettled = s.leftForSettlement.map {
                        SimpleMoneyValueDto(it.isMainCurrency, it.currency, it.amount.toFloat())
                    },
                    settlementBreakdown = s.settlementBreakdown.map { b ->
                        SettlementBreakdownEntryDto(
                            type = mapBreakdownType(b.type),
                            amountCost = b.amountCost.toFloat(),
                            amountTrip = b.amountTrip.toFloat()
                        )
                    }
                )
            }
        )
    }

    private fun mapParticipant(p: TripDetailsQuery.Participant): ParticipantDto {
        return ParticipantDto(
            id = p.id.toString(),
            nickname = p.nickname,
            totalExpenses = p.totalExpenses.map { mapMoney(it) },
            isOwner = p.isOwner,
            isPlaceholder = p.isPlaceholder,
            accessCode = p.accessCode,
            isActive = p.isActive
        )
    }

    private fun mapSettlement(s: TripDetailsQuery.Settlement): SettlementDto {
        return SettlementDto(
            relations = s.relations.map { r ->
                SettlementRelationDto(
                    relatedId = r.relatedId.toString(),
                    relatedName = r.relatedName,
                    leftForSettled = r.leftForSettled.map { mapMoney(it) },
                    allRelatedAmount = r.allRelatedAmount.map { mapMoney(it) },
                    prepayment = PrepaymentDetailsDto(
                        amountLeft = r.prepayment.amountLeft.map { mapMoney(it) },
                        history = r.prepayment.history.map { h ->
                            PrepaymentHistoryDto(
                                date = h.date.toLong(),
                                values = mapMoney(h.values)
                            )
                        }
                    ),
                    settlementHistory = r.settlementHistory.map { h ->
                        RelationSettlementHistoryDto(
                            id = h.id,
                            settlementType = mapHistoryEventType(h.settlementType),
                            actorNickname = h.actorNickname,
                            amountInSettlementCurrency = h.amountInSettlementCurrency.toFloat(),
                            settlementCurrency = h.settlementCurrency,
                            amountInTripCurrency = h.amountInTripCurrency.toFloat(),
                            relatedExpenseNames = h.relatedExpenseNames,
                            createdAt = h.createdAt.toLong()
                        )
                    }
                )
            }
        )
    }

    // ==========================================
// RECEIPT OPERATIONS
// ==========================================

    suspend fun getExpenseReceipt(expenseId: Int): Result<ReceiptDto?> {
        return try {
            val response = client.query(ExpenseReceiptQuery(expenseId)).execute()
            val data = response.data?.expenseReceipt

            if (data == null) {
                Result.success(null)
            } else {
                Result.success(
                    ReceiptDto(
                        expenseId = data.expenseId.toString(),
                        imageData = data.imageData,
                        receiptHash = data.receiptHash,
                        uploadedByNickname = data.uploadedByNickname?:"",
                        createdAt = data.createdAt.toLong()
                    )
                )
            }
        } catch (e: ApolloException) {
            Log.e(TAG, "GetExpenseReceipt error", e)
            Result.failure(e)
        }
    }

    suspend fun uploadReceipt(expenseId: Int, imageData: String): Result<SuccessDto> {
        return try {
            val response = client.mutation(
                UploadReceiptMutation(expenseId, imageData)
            ).execute()

            val data = response.data?.uploadReceipt
                ?: return Result.failure(Exception("Upload receipt failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "UploadReceipt error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReceipt(expenseId: Int): Result<SuccessDto> {
        return try {
            val response = client.mutation(
                DeleteReceiptMutation(expenseId)
            ).execute()

            val data = response.data?.deleteReceipt
                ?: return Result.failure(Exception("Delete receipt failed"))

            Result.success(SuccessDto(success = data.success, message = data.message))
        } catch (e: ApolloException) {
            Log.e(TAG, "DeleteReceipt error", e)
            Result.failure(e)
        }
    }

    private fun mapBreakdownType(
        type: com.example.tripapp2.graphql.type.SettlementBreakdownType
    ): SettlementBreakdownType {
        return when (type) {
            com.example.tripapp2.graphql.type.SettlementBreakdownType.SELF ->
                SettlementBreakdownType.SELF
            com.example.tripapp2.graphql.type.SettlementBreakdownType.MANUAL_BY_AMOUNT ->
                SettlementBreakdownType.MANUAL_BY_AMOUNT
            com.example.tripapp2.graphql.type.SettlementBreakdownType.MANUAL_BY_COSTS ->
                SettlementBreakdownType.MANUAL_BY_COSTS
            com.example.tripapp2.graphql.type.SettlementBreakdownType.AUTO_PREPAYMENT ->
                SettlementBreakdownType.AUTO_PREPAYMENT
            com.example.tripapp2.graphql.type.SettlementBreakdownType.AUTO_CROSS_SETTLE ->
                SettlementBreakdownType.AUTO_CROSS_SETTLE
            com.example.tripapp2.graphql.type.SettlementBreakdownType.UNSETTLED ->
                SettlementBreakdownType.UNSETTLED
            else -> SettlementBreakdownType.UNSETTLED
        }
    }

    // Overloaded mapMoney for different Apollo generated types
    // (Apollo generates separate classes for each query field with same structure)

    private fun mapMoney(m: TripDetailsQuery.MyCost): SimpleMoneyValueDto {
        return SimpleMoneyValueDto(
            isMainCurrency = m.isMainCurrency,
            currency = m.currency,
            amount = m.amount.toFloat()
        )
    }

    private fun mapMoney(m: TripDetailsQuery.TotalExpense): SimpleMoneyValueDto {
        return SimpleMoneyValueDto(
            isMainCurrency = m.isMainCurrency,
            currency = m.currency,
            amount = m.amount.toFloat()
        )
    }

    private fun mapMoney(m: TripDetailsQuery.SplitValue): SimpleMoneyValueDto {
        return SimpleMoneyValueDto(
            isMainCurrency = m.isMainCurrency,
            currency = m.currency,
            amount = m.amount.toFloat()
        )
    }

    private fun mapMoney(m: TripDetailsQuery.TotalExpense1): SimpleMoneyValueDto {
        return SimpleMoneyValueDto(
            isMainCurrency = m.isMainCurrency,
            currency = m.currency,
            amount = m.amount.toFloat()
        )
    }

    private fun mapMoney(m: TripDetailsQuery.LeftForSettled): SimpleMoneyValueDto {
        return SimpleMoneyValueDto(
            isMainCurrency = m.isMainCurrency,
            currency = m.currency,
            amount = m.amount.toFloat()
        )
    }

    private fun mapMoney(m: TripDetailsQuery.AllRelatedAmount): SimpleMoneyValueDto {
        return SimpleMoneyValueDto(
            isMainCurrency = m.isMainCurrency,
            currency = m.currency,
            amount = m.amount.toFloat()
        )
    }

    private fun mapMoney(m: TripDetailsQuery.AmountLeft): SimpleMoneyValueDto {
        return SimpleMoneyValueDto(
            isMainCurrency = m.isMainCurrency,
            currency = m.currency,
            amount = m.amount.toFloat()
        )
    }

    private fun mapMoney(m: TripDetailsQuery.Values): SimpleMoneyValueDto {
        return SimpleMoneyValueDto(
            isMainCurrency = m.isMainCurrency,
            currency = m.currency,
            amount = m.amount.toFloat()
        )
    }

    private fun mapMoney(m: TripDetailsQuery.TotalTripCost): SimpleMoneyValueDto {
        return SimpleMoneyValueDto(
            isMainCurrency = m.isMainCurrency,
            currency = m.currency,
            amount = m.amount.toFloat()
        )
    }
}