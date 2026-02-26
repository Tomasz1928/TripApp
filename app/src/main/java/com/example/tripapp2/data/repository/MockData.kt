package com.example.tripapp2.data.repository

import com.example.tripapp2.data.model.*
import com.example.tripapp2.ui.tripdetails.settlements.SettleByCostsItem

/**
 * MockData - Dane testowe dla aplikacji
 *
 * Struktura:
 * - Mutable storage dla tripów (pozwala na modyfikacje)
 * - Osobne funkcje dla każdego tripu
 * - getTripList() zwraca aktualny stan wszystkich tripów
 * - Metody dla settlements: addPrepayment(), markSettlementAsPaid()
 */
object MockData {

    // ==========================================
    // MUTABLE STORAGE - przechowuje aktualny stan tripów
    // ==========================================

    private val tripsStorage: MutableMap<String, TripDto> = mutableMapOf()
    private var isInitialized = false

    /**
     * Inicjalizuje storage jeśli pusty
     */
    private fun initializeIfNeeded() {
        if (!isInitialized) {
            tripsStorage["1"] = createTripZakopane()
            tripsStorage["2"] = createTripEurotrip()
            tripsStorage["3"] = createTripWakacjeNadMorzem()
            tripsStorage["4"] = createTripBarcelona()
            tripsStorage["5"] = createTripMultiCurrency()
            isInitialized = true
        }
    }

    /**
     * Resetuje storage do stanu początkowego (przydatne do testów)
     */
    fun resetStorage() {
        tripsStorage.clear()
        isInitialized = false
    }

    // ==========================================
    // HELPER: Skrót do tworzenia List<SimpleMoneyValueDto>
    // ==========================================

    /** Tworzy listę z jedną walutą (główną) */
    private fun money(mainCurrency: String, amount: Float): List<SimpleMoneyValueDto> =
        listOf(SimpleMoneyValueDto(isMainCurrency = true, currency = mainCurrency, amount = amount))

    /** Tworzy listę z główną walutą + innymi walutami */
    private fun money(
        mainCurrency: String,
        mainAmount: Float,
        vararg others: Pair<String, Float>
    ): List<SimpleMoneyValueDto> {
        val list = mutableListOf(
            SimpleMoneyValueDto(isMainCurrency = true, currency = mainCurrency, amount = mainAmount)
        )
        others.forEach { (cur, amt) ->
            list.add(SimpleMoneyValueDto(isMainCurrency = false, currency = cur, amount = amt))
        }
        return list
    }

    // ==========================================
    // PUBLIC API - USER INFO
    // ==========================================

    fun getUsrInfo(): UserInfoDto {
        return UserInfoDto(
            id = "10",
            nickname = "Adam"
        )
    }

    // ==========================================
    // PUBLIC API - TRIPS
    // ==========================================

    /**
     * Zwraca listę wszystkich tripów (aktualny stan)
     */
    fun getTripList(): TripListDto {
        initializeIfNeeded()
        return TripListDto(
            trips = tripsStorage.values.toList()
        )
    }

    /**
     * Zwraca konkretny trip po ID
     */
    fun getTripById(tripId: String): TripDto? {
        initializeIfNeeded()
        return tripsStorage[tripId]
    }

    /**
     * Tworzy nową wycieczkę
     */
    fun createTripMock(
        title: String,
        dateStart: Long,
        dateEnd: Long,
        description: String,
        currency: String
    ): CreateTripDto {
        initializeIfNeeded()

        val newId = (100..999).random().toString()
        val newTrip = TripDto(
            id = newId,
            title = title,
            dateStart = dateStart,
            dateEnd = dateEnd,
            description = description,
            currency = currency,
            totalExpenses = 0f,
            ownerId = "10",
            imOwner = true,
            myCost = emptyList(),
            categories = emptyList(),
            expenses = emptyList(),
            participants = listOf(
                ParticipantDto(
                    id = "10",
                    nickname = "Adam",
                    totalExpenses = emptyList(),
                    isOwner = true,
                    isPlaceholder = false,
                    accessCode = null,
                    isActive = true
                )
            ),
            settlement = null
        )

        tripsStorage[newId] = newTrip

        return CreateTripDto(
            success = SuccessDto(success = true, message = "Trip created successfully"),
            trip = newTrip
        )
    }

    /**
     * Dołącza do wycieczki po kodzie dostępu
     */
    fun joinTripMock(accessCode: String): JoinTripDto {
        initializeIfNeeded()

        return JoinTripDto(
            success = SuccessDto(success = false, message = "Trip not found"),
            trip = null
        )
    }

    // ==========================================
    // PUBLIC API - EXPENSES
    // ==========================================

    /**
     * Dodaje wydatek do istniejącego tripu
     */
    fun addExpenseMock(request: AddExpenseRequest): AddExpenseDto {
        initializeIfNeeded()

        val trip = tripsStorage[request.tripId]
            ?: return AddExpenseDto(
                success = SuccessDto(success = false, message = "Trip not found"),
                trip = null
            )

        val newExpenseId = "${request.tripId}${(100..999).random()}"

        val sharedWith = request.sharedWith.map { share ->
            ShareDto(
                participantNickname = share.participantNickname,
                participantId = share.participantId,
                splitValue = share.splitValue,
                isSettlement = request.payerId == share.participantId
            )
        }

        val newExpense = ExpenseDto(
            id = newExpenseId,
            name = request.name,
            description = request.description,
            totalExpense = money(request.currency, request.amount),
            amount = request.amount,
            currency = request.currency,
            date = request.date,
            categoryId = request.categoryId,
            payerId = request.payerId,
            payerNickname = request.payerNickname,
            sharedWith = sharedWith
        )

        val updatedExpenses = trip.expenses + newExpense
        val updatedTotalExpenses = trip.totalExpenses + request.amount
        val updatedCategories = updateCategories(trip.categories, request.categoryId, request.amount)

        val updatedTrip = trip.copy(
            expenses = updatedExpenses,
            totalExpenses = updatedTotalExpenses,
            categories = updatedCategories
        )

        tripsStorage[request.tripId] = updatedTrip

        return AddExpenseDto(
            success = SuccessDto(success = true, message = "Expense added successfully"),
            trip = updatedTrip
        )
    }

    /**
     * Aktualizuje wydatek w wycieczce
     */
    fun updateExpenseMock(request: UpdateExpenseRequest): UpdateExpenseDto {
        initializeIfNeeded()

        val trip = tripsStorage[request.tripId]
            ?: return UpdateExpenseDto(
                success = SuccessDto(success = false, message = "Trip not found"),
                trip = null
            )

        val expenseIndex = trip.expenses.indexOfFirst { it.id == request.expenseId }

        if (expenseIndex == -1) {
            return UpdateExpenseDto(
                success = SuccessDto(success = false, message = "Expense not found"),
                trip = null
            )
        }

        val oldExpense = trip.expenses[expenseIndex]

        val sharedWith = request.sharedWith.map { share ->
            ShareDto(
                participantNickname = share.participantNickname,
                participantId = share.participantId,
                splitValue = share.splitValue,
                isSettlement = request.payerId == share.participantId
            )
        }

        val updatedExpense = ExpenseDto(
            id = request.expenseId,
            name = request.name,
            description = request.description,
            totalExpense = money(request.currency, request.amount),
            amount = request.amount,
            currency = request.currency,
            date = request.date,
            categoryId = request.categoryId,
            payerId = request.payerId,
            payerNickname = request.payerNickname,
            sharedWith = sharedWith
        )

        val updatedExpenses = trip.expenses.toMutableList().apply {
            set(expenseIndex, updatedExpense)
        }

        val updatedTotalExpenses = trip.totalExpenses - oldExpense.amount + request.amount
        var updatedCategories = updateCategoriesRemove(trip.categories, oldExpense.categoryId, oldExpense.amount)
        updatedCategories = updateCategories(updatedCategories, request.categoryId, request.amount)

        val updatedTrip = trip.copy(
            expenses = updatedExpenses,
            totalExpenses = updatedTotalExpenses,
            categories = updatedCategories
        )

        tripsStorage[request.tripId] = updatedTrip

        return UpdateExpenseDto(
            success = SuccessDto(success = true, message = "Expense updated successfully"),
            trip = updatedTrip
        )
    }

    /**
     * Usuwa wydatek z wycieczki
     */
    fun deleteExpense(tripId: String, expenseId: String): DeleteExpenseDto {
        initializeIfNeeded()

        val trip = tripsStorage[tripId]
            ?: return DeleteExpenseDto(
                success = SuccessDto(success = false, message = "Trip not found"),
                trip = null
            )

        val expense = trip.expenses.find { it.id == expenseId }
            ?: return DeleteExpenseDto(
                success = SuccessDto(success = false, message = "Expense not found"),
                trip = null
            )

        val updatedExpenses = trip.expenses.filter { it.id != expenseId }
        val updatedTotalExpenses = trip.totalExpenses - expense.amount
        val updatedCategories = updateCategoriesRemove(trip.categories, expense.categoryId, expense.amount)

        val updatedTrip = trip.copy(
            expenses = updatedExpenses,
            totalExpenses = updatedTotalExpenses,
            categories = updatedCategories
        )

        tripsStorage[tripId] = updatedTrip

        return DeleteExpenseDto(
            success = SuccessDto(success = true, message = "Expense deleted successfully"),
            trip = updatedTrip
        )
    }

    // ==========================================
    // PUBLIC API - PARTICIPANTS
    // ==========================================

    /**
     * Dodaje placeholder uczestnika do tripu
     */
    fun addPlaceholder(tripId: String, nickname: String): ParticipantsDto {
        initializeIfNeeded()

        val trip = tripsStorage[tripId]
            ?: return ParticipantsDto(
                success = SuccessDto(success = false, message = "Trip not found"),
                trip = null
            )

        val existingParticipant = trip.participants.find {
            it.nickname.equals(nickname, ignoreCase = true)
        }

        if (existingParticipant != null) {
            return ParticipantsDto(
                success = SuccessDto(success = false, message = "Participant with this nickname already exists"),
                trip = null
            )
        }

        val newParticipantId = "${tripId}${(100..999).random()}"
        val accessCode = generateAccessCode()

        val newParticipant = ParticipantDto(
            id = newParticipantId,
            nickname = nickname,
            totalExpenses = emptyList(),
            isOwner = false,
            isPlaceholder = true,
            accessCode = accessCode,
            isActive = false
        )

        val updatedParticipants = trip.participants + newParticipant
        val updatedTrip = trip.copy(participants = updatedParticipants)

        tripsStorage[tripId] = updatedTrip

        return ParticipantsDto(
            success = SuccessDto(success = true, message = "Placeholder added successfully"),
            trip = updatedTrip
        )
    }

    /**
     * Odłącza użytkownika i zamienia go na placeholder
     */
    fun detachUser(tripId: String, participantId: String): ParticipantsDto {
        initializeIfNeeded()

        val trip = tripsStorage[tripId]
            ?: return ParticipantsDto(
                success = SuccessDto(success = false, message = "Trip not found"),
                trip = null
            )

        val participant = trip.participants.find { it.id == participantId }
            ?: return ParticipantsDto(
                success = SuccessDto(success = false, message = "Participant not found"),
                trip = null
            )

        if (participant.isOwner) {
            return ParticipantsDto(
                success = SuccessDto(success = false, message = "Cannot detach owner"),
                trip = null
            )
        }

        val accessCode = generateAccessCode()
        val updatedParticipant = participant.copy(
            isPlaceholder = true,
            accessCode = accessCode,
            isActive = false
        )

        val updatedParticipants = trip.participants.map {
            if (it.id == participantId) updatedParticipant else it
        }

        val updatedTrip = trip.copy(participants = updatedParticipants)
        tripsStorage[tripId] = updatedTrip

        return ParticipantsDto(
            success = SuccessDto(success = true, message = "User detached successfully"),
            trip = updatedTrip
        )
    }

    /**
     * Usuwa placeholder uczestnika
     */
    fun removePlaceholder(tripId: String, participantId: String): ParticipantsDto {
        initializeIfNeeded()

        val trip = tripsStorage[tripId]
            ?: return ParticipantsDto(
                success = SuccessDto(success = false, message = "Trip not found"),
                trip = null
            )

        val participant = trip.participants.find { it.id == participantId }
            ?: return ParticipantsDto(
                success = SuccessDto(success = false, message = "Participant not found"),
                trip = null
            )

        if (!participant.isPlaceholder) {
            return ParticipantsDto(
                success = SuccessDto(success = false, message = "Cannot remove active participant"),
                trip = null
            )
        }

        val updatedParticipants = trip.participants.filter { it.id != participantId }
        val updatedTrip = trip.copy(participants = updatedParticipants)
        tripsStorage[tripId] = updatedTrip

        return ParticipantsDto(
            success = SuccessDto(success = true, message = "Placeholder removed successfully"),
            trip = updatedTrip
        )
    }

    // ==========================================
    // PUBLIC API - SETTLEMENTS: settleByCosts
    // ==========================================

    /**
     * Rozlicza wybrane koszty
     *
     * Logika:
     * 1. Znajdź wydatek po expenseId
     * 2. Znajdź wpis sharedWith po participantId
     * 3. Ustaw isSettlement = true
     */
    fun settleByCosts(
        tripId: String,
        items: List<SettleByCostsItem>
    ): SettlementResultDto {
        initializeIfNeeded()

        val trip = tripsStorage[tripId]
            ?: return SettlementResultDto(
                success = SuccessDto(success = false, message = "Trip not found"),
                trip = null
            )

        var updatedExpenses = trip.expenses.toList()

        for (item in items) {
            updatedExpenses = updatedExpenses.map { expense ->
                if (expense.id == item.expenseId) {
                    val targetParticipantId = if (expense.payerId == item.participantId) {
                        item.participantId
                    } else {
                        item.participantId
                    }

                    val updatedSharedWith = expense.sharedWith.map { share ->
                        if (share.participantId == targetParticipantId) {
                            share.copy(isSettlement = true)
                        } else {
                            share
                        }
                    }
                    expense.copy(sharedWith = updatedSharedWith)
                } else {
                    expense
                }
            }
        }

        val updatedTrip = trip.copy(expenses = updatedExpenses)
        tripsStorage[tripId] = updatedTrip

        return SettlementResultDto(
            success = SuccessDto(success = true, message = "Koszty rozliczone pomyślnie"),
            trip = updatedTrip
        )
    }

    // ==========================================
    // PUBLIC API - SETTLEMENTS: addPrepayment
    // ==========================================

    /**
     * Dodaje zaliczkę między użytkownikami
     */
    fun addPrepayment(
        tripId: String,
        participantId: String,
        amount: Float,
        currency: String,
        direction: String  // "TO_ME" lub "FROM_ME"
    ): SettlementResultDto {
        initializeIfNeeded()

        val trip = tripsStorage[tripId]
            ?: return SettlementResultDto(
                success = SuccessDto(success = false, message = "Trip not found"),
                trip = null
            )

        val participant = trip.participants.find { it.id == participantId }
            ?: return SettlementResultDto(
                success = SuccessDto(success = false, message = "Participant not found"),
                trip = null
            )

        val tripCurrency = trip.currency
        val isMainCurrency = (currency == tripCurrency)

        // TO_ME: participant daje mi pieniądze → +amount
        // FROM_ME: ja daję participantowi → -amount
        val signedAmount = if (direction == "TO_ME") amount else -amount

        val currentRelations = trip.settlement?.relations?.toMutableList()
            ?: mutableListOf()

        val existingIndex = currentRelations.indexOfFirst { it.relatedId == participantId }

        if (existingIndex != -1) {
            val existing = currentRelations[existingIndex]

            val updatedAmountLeft = addToMoneyList(existing.prepayment.amountLeft, isMainCurrency, currency, signedAmount)

            val newHistoryEntry = PrepaymentHistoryDto(
                date = System.currentTimeMillis(),
                values = SimpleMoneyValueDto(isMainCurrency, currency, signedAmount)
            )
            val updatedHistory = existing.prepayment.history + newHistoryEntry

            val updatedPrepayment = PrepaymentDetailsDto(
                amountLeft = updatedAmountLeft,
                history = updatedHistory
            )

            val updatedLeftForSettled = addToMoneyList(existing.leftForSettled, isMainCurrency, currency, -signedAmount)

            currentRelations[existingIndex] = existing.copy(
                prepayment = updatedPrepayment,
                leftForSettled = updatedLeftForSettled
            )
        } else {
            val moneyEntry = SimpleMoneyValueDto(isMainCurrency, currency, -signedAmount)
            val prepaymentEntry = SimpleMoneyValueDto(isMainCurrency, currency, signedAmount)

            val newHistoryEntry = PrepaymentHistoryDto(
                date = System.currentTimeMillis(),
                values = SimpleMoneyValueDto(isMainCurrency, currency, signedAmount)
            )

            currentRelations.add(
                SettlementRelationDto(
                    relatedId = participantId,
                    relatedName = participant.nickname,
                    leftForSettled = listOf(moneyEntry),
                    allRelatedAmount = listOf(
                        SimpleMoneyValueDto(isMainCurrency, currency, 0f)
                    ),
                    prepayment = PrepaymentDetailsDto(
                        amountLeft = listOf(prepaymentEntry),
                        history = listOf(newHistoryEntry)
                    )
                )
            )
        }

        val newSettlement = SettlementDto(relations = currentRelations)
        val updatedTrip = trip.copy(settlement = newSettlement)
        tripsStorage[tripId] = updatedTrip

        return SettlementResultDto(
            success = SuccessDto(success = true, message = "Prepayment added successfully"),
            trip = updatedTrip
        )
    }

    // ==========================================
    // PUBLIC API - SETTLEMENTS: markSettlementAsPaid
    // ==========================================

    /**
     * Oznacza rozliczenie jako spłacone
     */
    fun markSettlementAsPaid(
        tripId: String,
        fromUserId: String,
        toUserId: String,
        amount: Float,
        currency: String,
        isMainCurrency: Boolean
    ): SettlementResultDto {
        initializeIfNeeded()

        val trip = tripsStorage[tripId]
            ?: return SettlementResultDto(
                success = SuccessDto(success = false, message = "Trip not found"),
                trip = null
            )

        val currentRelations = trip.settlement?.relations?.toMutableList()
            ?: return SettlementResultDto(
                success = SuccessDto(success = false, message = "No settlement data found"),
                trip = null
            )

        val currentUserId = getUsrInfo().id
        val relatedUserId = if (fromUserId == currentUserId) toUserId else fromUserId
        val relationIndex = currentRelations.indexOfFirst { it.relatedId == relatedUserId }

        if (relationIndex == -1) {
            return SettlementResultDto(
                success = SuccessDto(success = false, message = "Settlement relation not found"),
                trip = null
            )
        }

        val relation = currentRelations[relationIndex]

        val currencyEntry = relation.leftForSettled.find { it.currency == currency }
            ?: return SettlementResultDto(
                success = SuccessDto(success = false, message = "Currency not found in relation"),
                trip = null
            )

        val currentAmount = currencyEntry.amount
        val newAmount = if (currentAmount > 0) {
            maxOf(0f, currentAmount - amount)
        } else {
            minOf(0f, currentAmount + amount)
        }

        val updatedLeftForSettled = relation.leftForSettled.map {
            if (it.currency == currency) it.copy(amount = newAmount) else it
        }

        currentRelations[relationIndex] = relation.copy(
            leftForSettled = updatedLeftForSettled
        )

        val newSettlement = SettlementDto(relations = currentRelations)
        val updatedTrip = trip.copy(settlement = newSettlement)
        tripsStorage[tripId] = updatedTrip

        return SettlementResultDto(
            success = SuccessDto(success = true, message = "Settlement marked as paid"),
            trip = updatedTrip
        )
    }

    // ==========================================
    // HELPER FUNCTIONS
    // ==========================================

    private fun generateAccessCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val part1 = (1..4).map { chars.random() }.joinToString("")
        val part2 = (1..4).map { chars.random() }.joinToString("")
        return "$part1-$part2"
    }

    private fun updateCategories(
        existingCategories: List<CategoryDto>,
        categoryId: String,
        amount: Float
    ): List<CategoryDto> {
        val categoryExists = existingCategories.any { it.categoryId == categoryId }

        return if (categoryExists) {
            existingCategories.map { category ->
                if (category.categoryId == categoryId) {
                    category.copy(totalAmount = category.totalAmount + amount)
                } else {
                    category
                }
            }
        } else {
            existingCategories + CategoryDto(categoryId = categoryId, totalAmount = amount)
        }
    }

    private fun updateCategoriesRemove(
        existingCategories: List<CategoryDto>,
        categoryId: String,
        amount: Float
    ): List<CategoryDto> {
        return existingCategories.mapNotNull { category ->
            if (category.categoryId == categoryId) {
                val newAmount = category.totalAmount - amount
                if (newAmount > 0) category.copy(totalAmount = newAmount) else null
            } else {
                category
            }
        }
    }

    private fun addToMoneyList(
        list: List<SimpleMoneyValueDto>,
        isMainCurrency: Boolean,
        currency: String,
        amount: Float
    ): List<SimpleMoneyValueDto> {
        val mutableList = list.toMutableList()
        val existingIndex = mutableList.indexOfFirst { it.currency == currency }

        if (existingIndex != -1) {
            val existing = mutableList[existingIndex]
            mutableList[existingIndex] = existing.copy(amount = existing.amount + amount)
        } else {
            mutableList.add(SimpleMoneyValueDto(isMainCurrency, currency, amount))
        }

        return mutableList
    }

    // ==========================================
    // TRIP 1: Weekend w Zakopanem (PLN)
    // Uczestnicy: Adam (10, owner), Beata (11)
    // Settlement: Beata jest winna Adamowi 200 PLN
    // ==========================================

    private fun createTripZakopane(): TripDto {
        return TripDto(
            id = "1",
            title = "Weekend w Zakopanem",
            dateStart = 1711929600000,
            dateEnd = 1712534400000,
            description = "Weekend w górach",
            currency = "PLN",
            totalExpenses = 2400f,
            ownerId = "10",
            imOwner = true,
            myCost = money("PLN", 800f, "EUR" to 180f, "USD" to 200f),
            categories = listOf(
                CategoryDto("1", 1200f),
                CategoryDto("2", 800f),
                CategoryDto("3", 400f)
            ),
            expenses = createZakopaneExpenses(),
            participants = createZakopaneParticipants(),
            settlement = SettlementDto(
                relations = listOf(
                    SettlementRelationDto(
                        relatedId = "11",
                        relatedName = "Beata",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 200f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 200f)
                        ),
                        prepayment = PrepaymentDetailsDto(
                            amountLeft = emptyList(),
                            history = emptyList()
                        )
                    )
                )
            )
        )
    }

    private fun createZakopaneExpenses(): List<ExpenseDto> {
        return listOf(
            ExpenseDto(
                id = "11",
                name = "Nocleg - Pensjonat Górski",
                description = "2 noce dla 2 osób",
                totalExpense = money("EUR", 900f, "PLN" to 1200f),
                amount = 900f,
                currency = "EUR",
                date = 1711929600000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("EUR", 300f, "PLN" to 200f), true),
                    ShareDto("11", "Beata", money("EUR", 600f, "PLN" to 200f), false)
                )
            ),
            ExpenseDto(
                id = "12",
                name = "Kolacja w Karczmie",
                description = "Tradycyjna kuchnia góralska",
                totalExpense = money("PLN", 400f),
                amount = 400f,
                currency = "PLN",
                date = 1711969200000,
                categoryId = "2",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("PLN", 200f), false),
                    ShareDto("11", "Beata", money("PLN", 200f), false)
                )
            ),
            ExpenseDto(
                id = "13",
                name = "Paliwo",
                description = "Dojazd do Zakopanego",
                totalExpense = money("PLN", 400f),
                amount = 400f,
                currency = "PLN",
                date = 1711922400000,
                categoryId = "3",
                payerId = "11",
                payerNickname = "Beata",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("PLN", 200f), false),
                    ShareDto("11", "Beata", money("PLN", 200f), false)
                )
            ),
            ExpenseDto(
                id = "14",
                name = "Śniadanie w górach",
                description = "Schronisko",
                totalExpense = money("PLN", 400f),
                amount = 400f,
                currency = "PLN",
                date = 1712012400000,
                categoryId = "2",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("PLN", 200f), false),
                    ShareDto("11", "Beata", money("PLN", 200f), false)
                )
            )
        )
    }

    private fun createZakopaneParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = money("PLN", 2000f),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "11",
                nickname = "Beata",
                totalExpenses = money("PLN", 400f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            )
        )
    }

    // ==========================================
    // TRIP 2: Eurotrip (EUR)
    // Uczestnicy: Adam (10), Beata (11-owner), Cezary (12), Diana (13)
    // Settlement: Beata i Diana są winni Adamowi po 75 EUR
    // ==========================================

    private fun createTripEurotrip(): TripDto {
        return TripDto(
            id = "2",
            title = "Eurotrip 2024",
            dateStart = 1720137600000,
            dateEnd = 1721347200000,
            description = "Podróż po Europie samochodem",
            currency = "EUR",
            totalExpenses = 4500f,
            ownerId = "11",
            imOwner = false,
            myCost = money("EUR", 2325f, "PLN" to 10400f, "USD" to 2555f),
            categories = listOf(
                CategoryDto("1", 1500f),
                CategoryDto("2", 1200f),
                CategoryDto("3", 1800f)
            ),
            expenses = createEurotripExpenses(),
            participants = createEurotripParticipants(),
            settlement = SettlementDto(
                relations = listOf(
                    // Beata (11) jest winna Adamowi (10) — oryginalnie 75 EUR, po zaliczce 50 EUR
                    SettlementRelationDto(
                        relatedId = "11",
                        relatedName = "Beata",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 50f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 75f)
                        ),
                        prepayment = PrepaymentDetailsDto(
                            amountLeft = listOf(
                                SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 25f)
                            ),
                            history = listOf(
                                PrepaymentHistoryDto(
                                    date = 1720224000000,  // 6 lip 2024
                                    values = SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 25f)
                                )
                            )
                        )
                    ),
                    // Diana (13) jest winna Adamowi (10) 75 EUR — bez zaliczek
                    SettlementRelationDto(
                        relatedId = "13",
                        relatedName = "Diana",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 75f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 75f)
                        ),
                        prepayment = PrepaymentDetailsDto(
                            amountLeft = emptyList(),
                            history = emptyList()
                        )
                    )
                )
            )
        )
    }

    private fun createEurotripExpenses(): List<ExpenseDto> {
        return listOf(
            ExpenseDto(
                id = "21",
                name = "Hotel Berlin",
                description = "3 noce dla 4 osób",
                totalExpense = money("EUR", 900f),
                amount = 900f,
                currency = "EUR",
                date = 1720137600000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("EUR", 225f), false),
                    ShareDto("11", "Beata", money("EUR", 225f), false),
                    ShareDto("12", "Cezary", money("EUR", 225f), false),
                    ShareDto("13", "Diana", money("EUR", 225f), false)
                )
            ),
            ExpenseDto(
                id = "22",
                name = "Wynajem samochodu",
                description = "VW Passat na 2 tygodnie",
                totalExpense = money("EUR", 1800f),
                amount = 1800f,
                currency = "EUR",
                date = 1720137600000,
                categoryId = "3",
                payerId = "11",
                payerNickname = "Beata",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("EUR", 450f), false),
                    ShareDto("11", "Beata", money("EUR", 450f), false),
                    ShareDto("12", "Cezary", money("EUR", 450f), false),
                    ShareDto("13", "Diana", money("EUR", 450f), false)
                )
            ),
            ExpenseDto(
                id = "23",
                name = "Restauracja Amsterdam",
                description = "Wspólna kolacja",
                totalExpense = money("EUR", 400f),
                amount = 400f,
                currency = "EUR",
                date = 1720483200000,
                categoryId = "2",
                payerId = "12",
                payerNickname = "Cezary",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("EUR", 100f), false),
                    ShareDto("11", "Beata", money("EUR", 100f), false),
                    ShareDto("12", "Cezary", money("EUR", 100f), false),
                    ShareDto("13", "Diana", money("EUR", 100f), false)
                )
            )
        )
    }

    private fun createEurotripParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = money("EUR", 900f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "11",
                nickname = "Beata",
                totalExpenses = money("EUR", 1800f),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "12",
                nickname = "Cezary",
                totalExpenses = money("EUR", 400f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "13",
                nickname = "Diana",
                totalExpenses = money("EUR", 0f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            )
        )
    }

    // ==========================================
    // TRIP 3: Wakacje nad morzem (PLN)
    // Uczestnicy: Adam (10-owner), Ewa (14), Filip (15)
    // Settlement: Ewa była winna Adamowi 166.67 PLN - JUŻ ROZLICZONE
    // Filip - brak relacji z Adamem
    // ==========================================

    private fun createTripWakacjeNadMorzem(): TripDto {
        return TripDto(
            id = "3",
            title = "Wakacje nad morzem",
            dateStart = 1723161600000,
            dateEnd = 1723766400000,
            description = "Tydzień w Sopocie",
            currency = "PLN",
            totalExpenses = 5200f,
            ownerId = "10",
            imOwner = true,
            myCost = money("PLN", 1733.33f),
            categories = listOf(
                CategoryDto("1", 3500f),
                CategoryDto("2", 1200f),
                CategoryDto("4", 500f)
            ),
            expenses = createWakacjeExpenses(),
            participants = createWakacjeParticipants(),
            settlement = SettlementDto(
                relations = listOf(
                    // Ewa (14) była winna Adamowi - JUŻ ROZLICZONE (leftForSettled = 0)
                    SettlementRelationDto(
                        relatedId = "14",
                        relatedName = "Ewa",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 0f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 166.67f)
                        ),
                        prepayment = PrepaymentDetailsDto(
                            amountLeft = emptyList(),
                            history = emptyList()
                        )
                    )
                    // Filip (15) - brak relacji z Adamem = brak wpisu
                )
            )
        )
    }

    private fun createWakacjeExpenses(): List<ExpenseDto> {
        return listOf(
            ExpenseDto(
                id = "31",
                name = "Apartament Sopot",
                description = "7 nocy z widokiem na morze",
                totalExpense = money("PLN", 3500f),
                amount = 3500f,
                currency = "PLN",
                date = 1723161600000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("PLN", 1166.67f), false),
                    ShareDto("14", "Ewa", money("PLN", 1166.67f), false),
                    ShareDto("15", "Filip", money("PLN", 1166.66f), false)
                )
            ),
            ExpenseDto(
                id = "32",
                name = "Restauracja nad morzem",
                description = "Kolacja z owocami morza",
                totalExpense = money("PLN", 600f),
                amount = 600f,
                currency = "PLN",
                date = 1723248000000,
                categoryId = "2",
                payerId = "14",
                payerNickname = "Ewa",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("PLN", 200f), false),
                    ShareDto("14", "Ewa", money("PLN", 200f), false),
                    ShareDto("15", "Filip", money("PLN", 200f), false)
                )
            ),
            ExpenseDto(
                id = "33",
                name = "Rejs statkiem",
                description = "Wycieczka po Zatoce Gdańskiej",
                totalExpense = money("PLN", 500f),
                amount = 500f,
                currency = "PLN",
                date = 1723420800000,
                categoryId = "4",
                payerId = "15",
                payerNickname = "Filip",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("PLN", 166.67f), false),
                    ShareDto("14", "Ewa", money("PLN", 166.67f), false),
                    ShareDto("15", "Filip", money("PLN", 166.66f), false)
                )
            )
        )
    }

    private fun createWakacjeParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = money("PLN", 3500f),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "14",
                nickname = "Ewa",
                totalExpenses = money("PLN", 600f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "15",
                nickname = "Filip",
                totalExpenses = money("PLN", 500f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            )
        )
    }

    // ==========================================
    // TRIP 4: Barcelona (EUR)
    // Uczestnicy: Kasia (20-owner), Michał (21), Ola (22-placeholder), Tomek (23-placeholder)
    // Settlement: Wszystko rozliczone
    // Test case: Adam NIE jest uczestnikiem - nie powinien widzieć rozliczeń
    // ==========================================

    private fun createTripBarcelona(): TripDto {
        return TripDto(
            id = "4",
            title = "Barcelona Weekend",
            dateStart = 1725148800000,
            dateEnd = 1725494400000,
            description = "Weekend w Barcelonie",
            currency = "EUR",
            totalExpenses = 1850f,
            ownerId = "20",
            imOwner = false,
            myCost = money("EUR", 462.50f),
            categories = listOf(
                CategoryDto("1", 800f),
                CategoryDto("2", 650f),
                CategoryDto("3", 400f)
            ),
            expenses = createBarcelonaExpenses(),
            participants = createBarcelonaParticipants(),
            settlement = SettlementDto(
                relations = emptyList()
            )
        )
    }

    private fun createBarcelonaExpenses(): List<ExpenseDto> {
        return listOf(
            ExpenseDto(
                id = "41",
                name = "Hostel Barcelona",
                description = "2 noce w centrum",
                totalExpense = money("EUR", 800f),
                amount = 800f,
                currency = "EUR",
                date = 1725148800000,
                categoryId = "1",
                payerId = "20",
                payerNickname = "Kasia",
                sharedWith = listOf(
                    ShareDto("20", "Kasia", money("EUR", 200f), false),
                    ShareDto("21", "Michał", money("EUR", 200f), false),
                    ShareDto("22", "Ola", money("EUR", 200f), false),
                    ShareDto("23", "Tomek", money("EUR", 200f), false)
                )
            ),
            ExpenseDto(
                id = "42",
                name = "Tapas Bar",
                description = "Wieczór z tapas",
                totalExpense = money("EUR", 650f),
                amount = 650f,
                currency = "EUR",
                date = 1725235200000,
                categoryId = "2",
                payerId = "21",
                payerNickname = "Michał",
                sharedWith = listOf(
                    ShareDto("20", "Kasia", money("EUR", 162.50f), false),
                    ShareDto("21", "Michał", money("EUR", 162.50f), false),
                    ShareDto("22", "Ola", money("EUR", 162.50f), false),
                    ShareDto("23", "Tomek", money("EUR", 162.50f), false)
                )
            ),
            ExpenseDto(
                id = "43",
                name = "Transfer z lotniska",
                description = "Taxi dla grupy",
                totalExpense = money("EUR", 400f),
                amount = 400f,
                currency = "EUR",
                date = 1725148800000,
                categoryId = "3",
                payerId = "22",
                payerNickname = "Ola",
                sharedWith = listOf(
                    ShareDto("20", "Kasia", money("EUR", 100f), false),
                    ShareDto("21", "Michał", money("EUR", 100f), false),
                    ShareDto("22", "Ola", money("EUR", 100f), false),
                    ShareDto("23", "Tomek", money("EUR", 100f), false)
                )
            )
        )
    }

    private fun createBarcelonaParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "20",
                nickname = "Kasia",
                totalExpenses = money("EUR", 800f),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "21",
                nickname = "Michał",
                totalExpenses = money("EUR", 650f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "22",
                nickname = "Ola",
                totalExpenses = money("EUR", 400f),
                isOwner = false,
                isPlaceholder = true,
                accessCode = "BCN-2024",
                isActive = false
            ),
            ParticipantDto(
                id = "23",
                nickname = "Tomek",
                totalExpenses = money("EUR", 0f),
                isOwner = false,
                isPlaceholder = true,
                accessCode = "BCN-2024",
                isActive = false
            )
        )
    }

    // ==========================================
    // TRIP 5: Azja 2024 (PLN) - Multi Currency
    // Uczestnicy: Adam (10-owner), Gosia (16)
    // Settlement: Gosia winna Adamowi w wielu walutach + Hubert (17)
    // ==========================================

    private fun createTripMultiCurrency(): TripDto {
        return TripDto(
            id = "5",
            title = "Azja 2024",
            dateStart = 1727740800000,  // 1 października 2024
            dateEnd = 1729555200000,    // 22 października 2024
            description = "Podróż po Azji - Japonia, Tajlandia, Wietnam",
            currency = "PLN",
            totalExpenses = 15000f,
            ownerId = "10",
            imOwner = true,
            myCost = money("PLN", 7500f, "EUR" to 300f, "USD" to 400f, "JPY" to 10000f),
            categories = listOf(
                CategoryDto("1", 8000f),   // Noclegi
                CategoryDto("2", 4000f),   // Jedzenie
                CategoryDto("3", 3000f)    // Transport
            ),
            expenses = createMultiCurrencyExpenses(),
            participants = createMultiCurrencyParticipants(),
            settlement = SettlementDto(
                relations = listOf(
                    // Gosia (16) jest winna Adamowi w wielu walutach
                    // Oryginał: 500 PLN, 150 EUR, 200 USD, 5000 JPY
                    // Po zaliczkach: 400 PLN, 100 EUR, 200 USD, 5000 JPY
                    SettlementRelationDto(
                        relatedId = "16",
                        relatedName = "Gosia",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 400f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = 100f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "USD", amount = 200f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "JPY", amount = 5000f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 500f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = 150f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "USD", amount = 200f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "JPY", amount = 5000f)
                        ),
                        prepayment = PrepaymentDetailsDto(
                            amountLeft = listOf(
                                SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 100f),
                                SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = 50f)
                            ),
                            history = listOf(
                                PrepaymentHistoryDto(
                                    date = 1728000000000,  // 4 paź 2024
                                    values = SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 100f)
                                ),
                                PrepaymentHistoryDto(
                                    date = 1727740800000,  // 1 paź 2024
                                    values = SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = 50f)
                                )
                            )
                        )
                    ),
                    // Hubert (17) - Adam jest winien Hubertowi 300 PLN i 50 EUR
                    // Adam dał Hubertowi zaliczkę 100 PLN → leftForSettled zmniejszone do -200 PLN
                    SettlementRelationDto(
                        relatedId = "17",
                        relatedName = "Hubert",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = -200f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = -50f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = -300f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = -50f)
                        ),
                        prepayment = PrepaymentDetailsDto(
                            amountLeft = listOf(
                                SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = -100f)
                            ),
                            history = listOf(
                                PrepaymentHistoryDto(
                                    date = 1727913600000,  // 3 paź 2024
                                    values = SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = -100f)
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    private fun createMultiCurrencyExpenses(): List<ExpenseDto> {
        return listOf(
            // Wydatek w PLN
            ExpenseDto(
                id = "51",
                name = "Loty międzynarodowe",
                description = "Warszawa - Tokio - Warszawa",
                totalExpense = money("PLN", 6000f),
                amount = 6000f,
                currency = "PLN",
                date = 1727740800000,
                categoryId = "3",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("PLN", 3000f), false),
                    ShareDto("16", "Gosia", money("PLN", 3000f), false)
                )
            ),
            // Wydatek w EUR
            ExpenseDto(
                id = "52",
                name = "Hotel Tokio",
                description = "5 nocy w Shinjuku",
                totalExpense = money("EUR", 600f),
                amount = 600f,
                currency = "EUR",
                date = 1727827200000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("EUR", 300f), false),
                    ShareDto("16", "Gosia", money("EUR", 300f), false)
                )
            ),
            // Wydatek w USD
            ExpenseDto(
                id = "53",
                name = "Wycieczka Mount Fuji",
                description = "Całodniowa wycieczka z przewodnikiem",
                totalExpense = money("USD", 400f),
                amount = 400f,
                currency = "USD",
                date = 1728086400000,
                categoryId = "3",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("USD", 200f), false),
                    ShareDto("16", "Gosia", money("USD", 200f), false)
                )
            ),
            // Wydatek w JPY
            ExpenseDto(
                id = "54",
                name = "Kolacja Omakase",
                description = "Ekskluzywna kolacja sushi",
                totalExpense = money("JPY", 10000f),
                amount = 10000f,
                currency = "JPY",
                date = 1728172800000,
                categoryId = "2",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("JPY", 5000f), false),
                    ShareDto("16", "Gosia", money("JPY", 5000f), false)
                )
            ),
            // Kolejny wydatek w PLN
            ExpenseDto(
                id = "55",
                name = "Ubezpieczenie podróżne",
                description = "Pełne ubezpieczenie na 3 tygodnie",
                totalExpense = money("PLN", 1000f),
                amount = 1000f,
                currency = "PLN",
                date = 1727654400000,
                categoryId = "3",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", money("PLN", 500f), false),
                    ShareDto("16", "Gosia", money("PLN", 500f), false)
                )
            )
        )
    }

    private fun createMultiCurrencyParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = money("PLN", 7000f, "EUR" to 600f, "USD" to -400f, "JPY" to 10000f),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "16",
                nickname = "Gosia",
                totalExpenses = emptyList(),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            )
        )
    }
}