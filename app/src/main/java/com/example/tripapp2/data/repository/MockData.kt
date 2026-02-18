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
            accessCode = generateAccessCode(),
            ownerId = "10",
            imOwner = true,
            myCost = null,
            categories = emptyList(),
            expenses = emptyList(),
            participants = listOf(
                ParticipantDto(
                    id = "10",
                    nickname = "Adam",
                    totalExpenses = null,
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

        val existingTrip = tripsStorage.values.find { it.accessCode == accessCode }

        if (existingTrip != null) {
            return JoinTripDto(
                success = SuccessDto(success = true, message = "Successfully joined trip"),
                trip = existingTrip
            )
        }

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

        val sharedWith = request.sharedWith.map {share -> ShareDto(
            participantNickname = share.participantNickname,
            participantId = share.participantId,
            splitValue = share.splitValue,
            isSettlement = request.payerId == share.participantId
        )  }


        val newExpense = ExpenseDto(
            id = newExpenseId,
            name = request.name,
            description = request.description,
            totalExpense = MoneyValueDto(
                valueMainCurrency = request.amount,
                valueOtherCurrencies = emptyList()
            ),
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

        val sharedWith = request.sharedWith.map {share -> ShareDto(
            participantNickname = share.participantNickname,
            participantId = share.participantId,
            splitValue = share.splitValue,
            isSettlement = request.payerId == share.participantId
        )  }

        val updatedExpense = ExpenseDto(
            id = request.expenseId,
            name = request.name,
            description = request.description,
            totalExpense = MoneyValueDto(
                valueMainCurrency = request.amount,
                valueOtherCurrencies = emptyList()
            ),
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
            totalExpenses = MoneyValueDto(valueMainCurrency = 0f, valueOtherCurrencies = emptyList()),
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
                success = SuccessDto(success = false, message = "Cannot detach trip owner"),
                trip = null
            )
        }

        if (participant.isPlaceholder) {
            return ParticipantsDto(
                success = SuccessDto(success = false, message = "Participant is already a placeholder"),
                trip = null
            )
        }

        val updatedParticipant = participant.copy(
            isPlaceholder = true,
            accessCode = generateAccessCode(),
            isActive = false
        )

        val updatedParticipants = trip.participants.map { p ->
            if (p.id == participantId) updatedParticipant else p
        }

        val updatedTrip = trip.copy(participants = updatedParticipants)
        tripsStorage[tripId] = updatedTrip

        return ParticipantsDto(
            success = SuccessDto(success = true, message = "User detached successfully"),
            trip = updatedTrip
        )
    }

    /**
     * Usuwa placeholder uczestnika z tripu
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
                success = SuccessDto(success = false, message = "Can only remove placeholder participants"),
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

    // Import potrzebny na górze pliku:
    // import com.example.tripapp2.ui.tripdetails.settlements.SettleByCostsItem

    /**
     * Rozlicza wybrane koszty - oznacza odpowiednie sharedWith jako isSettlement = true
     *
     * Dla każdego elementu:
     * 1. Znajdź wydatek po expenseId
     * 2. Znajdź wpis sharedWith po participantId
     * 3. Ustaw isSettlement = true
     *
     * @param tripId ID wycieczki
     * @param items Lista kosztów do rozliczenia (expenseId + payerId + participantId)
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
                    // Znajdź wpis sharedWith dla participanta (osoba która NIE płaciła)
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
    // PUBLIC API - SETTLEMENTS
    // ==========================================

    /**
     * Dodaje zaliczkę między użytkownikami
     *
     * Logika:
     * 1. Znajdź relację między mną a uczestnikiem (lub utwórz nową)
     * 2. Kierunek:
     *    - TO_ME = uczestnik daje mi pieniądze → relacja: participant -> me
     *    - FROM_ME = ja daję uczestnikowi → relacja: me -> participant
     * 3. Waluta:
     *    - Jeśli currency = trip.currency → dodaj do valueMainCurrency
     *    - Jeśli currency ≠ trip.currency → dodaj do valueOtherCurrencies
     * 4. Aktualizuj SettlementDto.balance
     *
     * @param tripId ID wycieczki
     * @param participantId ID uczestnika (nie aktualny user)
     * @param amount Kwota zaliczki
     * @param currency Waluta zaliczki
     * @param direction "TO_ME" (uczestnik daje mi) lub "FROM_ME" (ja daję uczestnikowi)
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

        // Oblicz kwotę zaliczki ze znakiem
        // TO_ME: participant daje mi pieniądze → zwiększa mój balans → +amount w prepayment
        // FROM_ME: ja daję participantowi → zmniejsza mój balans → -amount w prepayment
        val signedAmount = if (direction == "TO_ME") amount else -amount

        val currentRelations = trip.settlement?.relations?.toMutableList() ?: mutableListOf()

        // Znajdź lub utwórz relację
        val existingIndex = currentRelations.indexOfFirst { it.relatedId == participantId }

        if (existingIndex != -1) {
            val existing = currentRelations[existingIndex]

            // Dodaj do prepayment
            val updatedPrepayment = addToMoneyList(existing.prepayment, isMainCurrency, currency, signedAmount)

            // Przelicz leftForSettled: allRelatedAmount - (prepayment - leftFromPrepayment)
            // Uproszczenie: leftForSettled zmniejszamy o kwotę zaliczki
            val updatedLeftForSettled = addToMoneyList(existing.leftForSettled, isMainCurrency, currency, -signedAmount)

            currentRelations[existingIndex] = existing.copy(
                prepayment = updatedPrepayment,
                leftForSettled = updatedLeftForSettled
            )
        } else {
            // Nowa relacja
            val moneyEntry = SimpleMoneyValueDto(isMainCurrency, currency, -signedAmount)
            val prepaymentEntry = SimpleMoneyValueDto(isMainCurrency, currency, signedAmount)

            currentRelations.add(
                SettlementRelationDto(
                    relatedId = participantId,
                    relatedName = participant.nickname,
                    leftForSettled = listOf(moneyEntry),
                    allRelatedAmount = listOf(
                        SimpleMoneyValueDto(isMainCurrency, currency, 0f)
                    ),
                    prepayment = listOf(prepaymentEntry),
                    leftFromPrepayment = emptyList()
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



    /**
     * Oznacza rozliczenie jako spłacone
     *
     * @param tripId ID wycieczki
     * @param fromUserId ID dłużnika
     * @param toUserId ID wierzyciela
     * @param amount Kwota do rozliczenia
     * @param currency Waluta
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

        // Znajdź relację - relatedId to ten "drugi" uczestnik
        // fromUserId lub toUserId — jeden z nich to "ja", drugi to related
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

        // Znajdź walutę w leftForSettled
        val currencyEntry = relation.leftForSettled.find { it.currency == currency }
            ?: return SettlementResultDto(
                success = SuccessDto(success = false, message = "Currency not found in relation"),
                trip = null
            )

        // Zmniejsz leftForSettled o rozliczoną kwotę (zachowując znak)
        val currentAmount = currencyEntry.amount
        val newAmount = if (currentAmount > 0) {
            // On mi jest winien → zmniejszam kwotę
            maxOf(0f, currentAmount - amount)
        } else {
            // Ja jestem winien → zwiększam (w kierunku 0)
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
            accessCode = "ZAKO-2024",
            ownerId = "10",
            imOwner = true,
            myCost = MoneyValueDto(
                valueMainCurrency = 800f,
                valueOtherCurrencies = listOf(
                    MoneyValueDetailsDto("EUR", 180f),
                    MoneyValueDetailsDto("USD", 200f)
                )
            ),
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
                        prepayment = emptyList(),
                        leftFromPrepayment = emptyList()
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
                totalExpense = MoneyValueDto(valueMainCurrency = 1200f),
                amount = 900f,
                currency = "EUR",
                date = 1711929600000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 300f,valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 200f))),true),
                    ShareDto("11", "Beata", MoneyValueDto(valueMainCurrency = 600f,valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 200f))),false)
                )
            ),
            ExpenseDto(
                id = "12",
                name = "Kolacja w Karczmie",
                description = "Tradycyjna kuchnia góralska",
                totalExpense = MoneyValueDto(valueMainCurrency = 400f),
                amount = 400f,
                currency = "PLN",
                date = 1711969200000,
                categoryId = "2",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 200f),false),
                    ShareDto("11", "Beata", MoneyValueDto(valueMainCurrency = 200f),false)
                )
            ),
            ExpenseDto(
                id = "13",
                name = "Paliwo",
                description = "Dojazd do Zakopanego",
                totalExpense = MoneyValueDto(valueMainCurrency = 400f),
                amount = 400f,
                currency = "PLN",
                date = 1711922400000,
                categoryId = "3",
                payerId = "11",
                payerNickname = "Beata",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 200f),false),
                    ShareDto("11", "Beata", MoneyValueDto(valueMainCurrency = 200f),false)
                )
            ),
            ExpenseDto(
                id = "14",
                name = "Śniadanie w górach",
                description = "Schronisko",
                totalExpense = MoneyValueDto(valueMainCurrency = 400f),
                amount = 400f,
                currency = "PLN",
                date = 1712012400000,
                categoryId = "2",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 200f),false),
                    ShareDto("11", "Beata", MoneyValueDto(valueMainCurrency = 200f),false)
                )
            )
        )
    }

    private fun createZakopaneParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = MoneyValueDto(valueMainCurrency = 2000f),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "11",
                nickname = "Beata",
                totalExpenses = MoneyValueDto(valueMainCurrency = 400f),
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
            accessCode = "EURO-2024",
            ownerId = "11",
            imOwner = false,
            myCost = MoneyValueDto(
                valueMainCurrency = 2325f,
                valueOtherCurrencies = listOf(
                    MoneyValueDetailsDto("PLN", 10400f),
                    MoneyValueDetailsDto("USD", 2555f)
                )
            ),
            categories = listOf(
                CategoryDto("1", 1500f),
                CategoryDto("2", 1200f),
                CategoryDto("3", 1800f)
            ),
            expenses = createEurotripExpenses(),
            participants = createEurotripParticipants(),
            settlement = SettlementDto(
                relations = listOf(
                    // Beata (11) jest winna Adamowi (10) 75 EUR
                    SettlementRelationDto(
                        relatedId = "11",
                        relatedName = "Beata",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 75f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 75f)
                        ),
                        prepayment = emptyList(),
                        leftFromPrepayment = emptyList()
                    ),
                    // Diana (13) jest winna Adamowi (10) 75 EUR
                    SettlementRelationDto(
                        relatedId = "13",
                        relatedName = "Diana",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 75f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "EUR", amount = 75f)
                        ),
                        prepayment = emptyList(),
                        leftFromPrepayment = emptyList()
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
                totalExpense = MoneyValueDto(valueMainCurrency = 900f),
                amount = 900f,
                currency = "EUR",
                date = 1720137600000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 225f),false),
                    ShareDto("11", "Beata", MoneyValueDto(valueMainCurrency = 225f),false),
                    ShareDto("12", "Cezary", MoneyValueDto(valueMainCurrency = 225f),false),
                    ShareDto("13", "Diana", MoneyValueDto(valueMainCurrency = 225f),false)
                )
            ),
            ExpenseDto(
                id = "22",
                name = "Wynajem samochodu",
                description = "VW Passat na 2 tygodnie",
                totalExpense = MoneyValueDto(valueMainCurrency = 1800f),
                amount = 1800f,
                currency = "EUR",
                date = 1720137600000,
                categoryId = "3",
                payerId = "11",
                payerNickname = "Beata",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 450f),false),
                    ShareDto("11", "Beata", MoneyValueDto(valueMainCurrency = 450f),false),
                    ShareDto("12", "Cezary", MoneyValueDto(valueMainCurrency = 450f),false),
                    ShareDto("13", "Diana", MoneyValueDto(valueMainCurrency = 450f),false)
                )
            ),
            ExpenseDto(
                id = "23",
                name = "Restauracja Amsterdam",
                description = "Wspólna kolacja",
                totalExpense = MoneyValueDto(valueMainCurrency = 400f),
                amount = 400f,
                currency = "EUR",
                date = 1720483200000,
                categoryId = "2",
                payerId = "12",
                payerNickname = "Cezary",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 100f),false),
                    ShareDto("11", "Beata", MoneyValueDto(valueMainCurrency = 100f),false),
                    ShareDto("12", "Cezary", MoneyValueDto(valueMainCurrency = 100f),false),
                    ShareDto("13", "Diana", MoneyValueDto(valueMainCurrency = 100f),false)
                )
            )
        )
    }

    private fun createEurotripParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = MoneyValueDto(valueMainCurrency = 900f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "11",
                nickname = "Beata",
                totalExpenses = MoneyValueDto(valueMainCurrency = 1800f),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "12",
                nickname = "Cezary",
                totalExpenses = MoneyValueDto(valueMainCurrency = 400f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "13",
                nickname = "Diana",
                totalExpenses = MoneyValueDto(valueMainCurrency = 0f),
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
            accessCode = "SOPOT-24",
            ownerId = "10",
            imOwner = true,
            myCost = MoneyValueDto(valueMainCurrency = 1733.33f),
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
                        prepayment = emptyList(),
                        leftFromPrepayment = emptyList()
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
                totalExpense = MoneyValueDto(valueMainCurrency = 3500f),
                amount = 3500f,
                currency = "PLN",
                date = 1723161600000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 1166.67f),false),
                    ShareDto("14", "Ewa", MoneyValueDto(valueMainCurrency = 1166.67f),false),
                    ShareDto("15", "Filip", MoneyValueDto(valueMainCurrency = 1166.66f),false)
                )
            ),
            ExpenseDto(
                id = "32",
                name = "Restauracja nad morzem",
                description = "Kolacja z owocami morza",
                totalExpense = MoneyValueDto(valueMainCurrency = 600f),
                amount = 600f,
                currency = "PLN",
                date = 1723248000000,
                categoryId = "2",
                payerId = "14",
                payerNickname = "Ewa",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 200f),false),
                    ShareDto("14", "Ewa", MoneyValueDto(valueMainCurrency = 200f),false),
                    ShareDto("15", "Filip", MoneyValueDto(valueMainCurrency = 200f),false)
                )
            ),
            ExpenseDto(
                id = "33",
                name = "Rejs statkiem",
                description = "Wycieczka po Zatoce Gdańskiej",
                totalExpense = MoneyValueDto(valueMainCurrency = 500f),
                amount = 500f,
                currency = "PLN",
                date = 1723420800000,
                categoryId = "4",
                payerId = "15",
                payerNickname = "Filip",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 166.67f),false),
                    ShareDto("14", "Ewa", MoneyValueDto(valueMainCurrency = 166.67f),false),
                    ShareDto("15", "Filip", MoneyValueDto(valueMainCurrency = 166.66f),false)
                )
            )
        )
    }

    private fun createWakacjeParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = MoneyValueDto(valueMainCurrency = 3500f),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "14",
                nickname = "Ewa",
                totalExpenses = MoneyValueDto(valueMainCurrency = 600f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "15",
                nickname = "Filip",
                totalExpenses = MoneyValueDto(valueMainCurrency = 500f),
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
            accessCode = "BCN-2024",
            ownerId = "20",
            imOwner = false,
            myCost = MoneyValueDto(valueMainCurrency = 462.50f),
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
                totalExpense = MoneyValueDto(valueMainCurrency = 800f),
                amount = 800f,
                currency = "EUR",
                date = 1725148800000,
                categoryId = "1",
                payerId = "20",
                payerNickname = "Kasia",
                sharedWith = listOf(
                    ShareDto("20", "Kasia", MoneyValueDto(valueMainCurrency = 200f),false),
                    ShareDto("21", "Michał", MoneyValueDto(valueMainCurrency = 200f),false),
                    ShareDto("22", "Ola", MoneyValueDto(valueMainCurrency = 200f),false),
                    ShareDto("23", "Tomek", MoneyValueDto(valueMainCurrency = 200f),false)
                )
            ),
            ExpenseDto(
                id = "42",
                name = "Tapas Bar",
                description = "Wieczór z tapas",
                totalExpense = MoneyValueDto(valueMainCurrency = 650f),
                amount = 650f,
                currency = "EUR",
                date = 1725235200000,
                categoryId = "2",
                payerId = "21",
                payerNickname = "Michał",
                sharedWith = listOf(
                    ShareDto("20", "Kasia", MoneyValueDto(valueMainCurrency = 162.50f),false),
                    ShareDto("21", "Michał", MoneyValueDto(valueMainCurrency = 162.50f),false),
                    ShareDto("22", "Ola", MoneyValueDto(valueMainCurrency = 162.50f),false),
                    ShareDto("23", "Tomek", MoneyValueDto(valueMainCurrency = 162.50f),false)
                )
            ),
            ExpenseDto(
                id = "43",
                name = "Transfer z lotniska",
                description = "Taxi dla grupy",
                totalExpense = MoneyValueDto(valueMainCurrency = 400f),
                amount = 400f,
                currency = "EUR",
                date = 1725148800000,
                categoryId = "3",
                payerId = "22",
                payerNickname = "Ola",
                sharedWith = listOf(
                    ShareDto("20", "Kasia", MoneyValueDto(valueMainCurrency = 100f),false),
                    ShareDto("21", "Michał", MoneyValueDto(valueMainCurrency = 100f),false),
                    ShareDto("22", "Ola", MoneyValueDto(valueMainCurrency = 100f),false),
                    ShareDto("23", "Tomek", MoneyValueDto(valueMainCurrency = 100f),false)
                )
            )
        )
    }

    private fun createBarcelonaParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "20",
                nickname = "Kasia",
                totalExpenses = MoneyValueDto(valueMainCurrency = 800f),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "21",
                nickname = "Michał",
                totalExpenses = MoneyValueDto(valueMainCurrency = 650f),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "22",
                nickname = "Ola",
                totalExpenses = MoneyValueDto(valueMainCurrency = 400f),
                isOwner = false,
                isPlaceholder = true,
                accessCode = "BCN-2024",
                isActive = false
            ),
            ParticipantDto(
                id = "23",
                nickname = "Tomek",
                totalExpenses = MoneyValueDto(valueMainCurrency = 0f),
                isOwner = false,
                isPlaceholder = true,
                accessCode = "BCN-2024",
                isActive = false
            )
        )
    }

    private fun createTripMultiCurrency(): TripDto {
        return TripDto(
            id = "5",
            title = "Azja 2024",
            dateStart = 1727740800000,  // 1 października 2024
            dateEnd = 1729555200000,    // 22 października 2024
            description = "Podróż po Azji - Japonia, Tajlandia, Wietnam",
            currency = "PLN",
            totalExpenses = 15000f,
            accessCode = "AZJA-2024",
            ownerId = "10",
            imOwner = true,
            myCost = MoneyValueDto(
                valueMainCurrency = 7500f,
                valueOtherCurrencies = listOf(
                    MoneyValueDetailsDto("EUR", 300f),
                    MoneyValueDetailsDto("USD", 400f),
                    MoneyValueDetailsDto("JPY", 10000f)
                )
            ),
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
                    SettlementRelationDto(
                        relatedId = "16",
                        relatedName = "Gosia",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 500f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = 150f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "USD", amount = 200f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "JPY", amount = 5000f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = 500f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = 150f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "USD", amount = 200f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "JPY", amount = 5000f)
                        ),
                        prepayment = emptyList(),
                        leftFromPrepayment = emptyList()
                    ),
                    // Hubert (17) - Adam jest winien Hubertowi 300 PLN i 50 EUR
                    SettlementRelationDto(
                        relatedId = "17",
                        relatedName = "Hubert",
                        leftForSettled = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = -300f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = -50f)
                        ),
                        allRelatedAmount = listOf(
                            SimpleMoneyValueDto(isMainCurrency = true, currency = "PLN", amount = -300f),
                            SimpleMoneyValueDto(isMainCurrency = false, currency = "EUR", amount = -50f)
                        ),
                        prepayment = emptyList(),
                        leftFromPrepayment = emptyList()
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
                totalExpense = MoneyValueDto(valueMainCurrency = 6000f),
                amount = 6000f,
                currency = "PLN",
                date = 1727740800000,
                categoryId = "3",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 3000f),false),
                    ShareDto("16", "Gosia", MoneyValueDto(valueMainCurrency = 3000f),false)
                )
            ),
            // Wydatek w EUR
            ExpenseDto(
                id = "52",
                name = "Hotel Tokio",
                description = "5 nocy w Shinjuku",
                totalExpense = MoneyValueDto(valueMainCurrency = 600f),
                amount = 600f,
                currency = "EUR",
                date = 1727827200000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 300f),false),
                    ShareDto("16", "Gosia", MoneyValueDto(valueMainCurrency = 300f),false)
                )
            ),
            // Wydatek w USD
            ExpenseDto(
                id = "53",
                name = "Wycieczka Mount Fuji",
                description = "Całodniowa wycieczka z przewodnikiem",
                totalExpense = MoneyValueDto(valueMainCurrency = 400f),
                amount = 400f,
                currency = "USD",
                date = 1728086400000,
                categoryId = "3",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 200f),false),
                    ShareDto("16", "Gosia", MoneyValueDto(valueMainCurrency = 200f),false)
                )
            ),
            // Wydatek w JPY
            ExpenseDto(
                id = "54",
                name = "Kolacja Omakase",
                description = "Ekskluzywna kolacja sushi",
                totalExpense = MoneyValueDto(valueMainCurrency = 10000f),
                amount = 10000f,
                currency = "JPY",
                date = 1728172800000,
                categoryId = "2",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 5000f),false),
                    ShareDto("16", "Gosia", MoneyValueDto(valueMainCurrency = 5000f),false)
                )
            ),
            // Kolejny wydatek w PLN
            ExpenseDto(
                id = "55",
                name = "Ubezpieczenie podróżne",
                description = "Pełne ubezpieczenie na 3 tygodnie",
                totalExpense = MoneyValueDto(valueMainCurrency = 1000f),
                amount = 1000f,
                currency = "PLN",
                date = 1727654400000,
                categoryId = "3",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto("10", "Adam", MoneyValueDto(valueMainCurrency = 500f),false),
                    ShareDto("16", "Gosia", MoneyValueDto(valueMainCurrency = 500f),false)
                )
            )
        )
    }

    private fun createMultiCurrencyParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 7000f,
                    valueOtherCurrencies = listOf(
                        MoneyValueDetailsDto("EUR", 600f),
                        MoneyValueDetailsDto("USD", -400f),
                        MoneyValueDetailsDto("JPY", 10000f)
                    )
                ),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "16",
                nickname = "Gosia",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 0f,
                    valueOtherCurrencies = emptyList()
                ),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            )
        )
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


}