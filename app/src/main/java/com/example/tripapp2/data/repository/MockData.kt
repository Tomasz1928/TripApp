package com.example.tripapp2.data.repository

import com.example.tripapp2.data.model.*

object MockData {

    // ==================== MUTOWALNY STAN ====================
    private val tripsMap = mutableMapOf<String, TripDto>()
    private var expenseIdCounter = 100

    init {
        initializeTrips()
    }

    private fun initializeTrips() {
        val initialTrips = listOf(
            createZakopaneTrip(),
            createRzymTrip(),
            createRoadtripTrip()
        )
        initialTrips.forEach { trip ->
            tripsMap[trip.id] = trip
        }
    }

    // Reset do stanu początkowego (przydatne do testów)
    fun resetMockData() {
        tripsMap.clear()
        expenseIdCounter = 100
        initializeTrips()
    }

    // ==================== GŁÓWNE OPERACJE ====================

    fun getTripById(tripId: String): TripDto? {
        return tripsMap[tripId]
    }

    fun addExpense(
        tripId: String,
        name: String,
        description: String,
        amount: Float,
        currency: String,
        date: Long,
        categoryId: String,
        payerId: String,
        payerNickname: String,
        sharedWith: List<ShareDto>
    ): AddExpenseResponseDto? {
        val trip = tripsMap[tripId] ?: return null

        val newExpenseId = (++expenseIdCounter).toString()

        // Oblicz totalExpense w walucie wycieczki
        val totalExpenseInTripCurrency = if (currency == trip.currency) {
            amount
        } else {
            // Prosty mock konwersji - w realnej aplikacji użyłbyś API kursów
            convertCurrency(amount, currency, trip.currency)
        }

        val newExpense = ExpenseDto(
            id = newExpenseId,
            name = name,
            description = description,
            totalExpense = MoneyValueDto(
                valueMainCurrency = totalExpenseInTripCurrency,
                valueOtherCurrencies = generateOtherCurrencies(totalExpenseInTripCurrency, trip.currency)
            ),
            amount = amount,
            currency = currency,
            date = date,
            categoryId = categoryId,
            payerId = payerId,
            payerNickname = payerNickname,
            sharedWith = sharedWith
        )

        // Zaktualizuj trip z nowym wydatkiem
        val updatedExpenses = trip.expenses + newExpense
        val updatedTotalExpenses = trip.totalExpenses + totalExpenseInTripCurrency

        // Zaktualizuj kategorie
        val updatedCategories = updateCategories(trip.categories, categoryId, totalExpenseInTripCurrency)

        // Zaktualizuj myCost jeśli użytkownik jest w sharedWith
        val updatedMyCost = updateMyCost(trip, sharedWith)

        val updatedTrip = trip.copy(
            expenses = updatedExpenses,
            totalExpenses = updatedTotalExpenses,
            categories = updatedCategories,
            myCost = updatedMyCost
        )

        tripsMap[tripId] = updatedTrip

        return AddExpenseResponseDto(
            success = SuccessDto(
                success = true,
                message = "Expense added successfully"
            ),
            updatedTrip = updatedTrip
        )
    }

    fun deleteExpense(tripId: String, expenseId: String): DeleteExpenseResponseDto? {
        val trip = tripsMap[tripId] ?: return null
        val expenseToDelete = trip.expenses.find { it.id == expenseId } ?: return null

        val updatedExpenses = trip.expenses.filter { it.id != expenseId }
        val totalExpenseValue = expenseToDelete.totalExpense?.valueMainCurrency ?: 0f
        val updatedTotalExpenses = trip.totalExpenses - totalExpenseValue

        val updatedCategories = updateCategories(
            trip.categories,
            expenseToDelete.categoryId,
            -totalExpenseValue
        )

        val updatedTrip = trip.copy(
            expenses = updatedExpenses,
            totalExpenses = updatedTotalExpenses,
            categories = updatedCategories
        )

        tripsMap[tripId] = updatedTrip

        return DeleteExpenseResponseDto(
            success = SuccessDto(
                success = true,
                message = "Expense deleted successfully"
            ),
            updatedTrip = updatedTrip
        )
    }

    fun updateExpense(
        tripId: String,
        expenseId: String,
        name: String? = null,
        description: String? = null,
        amount: Float? = null,
        currency: String? = null,
        date: Long? = null,
        categoryId: String? = null,
        sharedWith: List<ShareDto>? = null
    ): UpdateExpenseResponseDto? {
        val trip = tripsMap[tripId] ?: return null
        val existingExpense = trip.expenses.find { it.id == expenseId } ?: return null

        val updatedExpense = existingExpense.copy(
            name = name ?: existingExpense.name,
            description = description ?: existingExpense.description,
            amount = amount ?: existingExpense.amount,
            currency = currency ?: existingExpense.currency,
            date = date ?: existingExpense.date,
            categoryId = categoryId ?: existingExpense.categoryId,
            sharedWith = sharedWith ?: existingExpense.sharedWith
        )

        val updatedExpenses = trip.expenses.map {
            if (it.id == expenseId) updatedExpense else it
        }

        val updatedTrip = trip.copy(expenses = updatedExpenses)
        tripsMap[tripId] = updatedTrip

        return UpdateExpenseResponseDto(
            success = SuccessDto(
                success = true,
                message = "Expense updated successfully"
            ),
            updatedTrip = updatedTrip
        )
    }

    fun createTripMock(
        title: String,
        dateStart: Long,
        dateEnd: Long,
        description: String,
        currency: String
    ): CreateTripDto {
        val newTripId = (1..100).random().toString()

        val newTrip = TripDto(
            id = newTripId,
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
                    accessCode = "1234",
                    isActive = true
                )
            ),
            settlement = null
        )

        tripsMap[newTripId] = newTrip

        return CreateTripDto(
            trip = newTrip,
            success = SuccessDto(
                success = true,
                message = "Trip created successfully"
            )
        )
    }

    fun joinTripMock(accessCode: String): JoinTripDto {
        // Szukaj tripu po accessCode lub zwróć domyślny
        val existingTrip = tripsMap.values.find { it.accessCode == accessCode }

        return JoinTripDto(
            success = SuccessDto(
                success = true,
                message = "Joined trip successfully"
            ),
            trip = existingTrip ?: createDefaultJoinTrip()
        )
    }

    fun addParticipant(
        tripId: String,
        nickname: String,
        isPlaceholder: Boolean = true
    ): AddParticipantResponseDto? {
        val trip = tripsMap[tripId] ?: return null

        val newParticipantId = ((trip.participants.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1).toString()

        val newParticipant = ParticipantDto(
            id = newParticipantId,
            nickname = nickname,
            totalExpenses = null,
            isOwner = false,
            isPlaceholder = isPlaceholder,
            accessCode = if (isPlaceholder) trip.accessCode else null,
            isActive = !isPlaceholder
        )

        val updatedTrip = trip.copy(
            participants = trip.participants + newParticipant
        )

        tripsMap[tripId] = updatedTrip

        return AddParticipantResponseDto(
            success = SuccessDto(
                success = true,
                message = "Participant added successfully"
            ),
            updatedTrip = updatedTrip
        )
    }

    fun getUsrInfo(): UserInfoDto {
        return UserInfoDto(
            id = "10",
            nickname = "Adam"
        )
    }

    fun getTripList(): TripListDto {
        return TripListDto(
            trips = tripsMap.values.toList()
        )
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun generateAccessCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun convertCurrency(amount: Float, from: String, to: String): Float {
        // Mock kursy walut
        val rates = mapOf(
            "PLN" to 1.0f,
            "EUR" to 4.30f,
            "USD" to 3.95f,
            "GBP" to 5.05f
        )

        val fromRate = rates[from] ?: 1.0f
        val toRate = rates[to] ?: 1.0f

        return amount * fromRate / toRate
    }

    private fun generateOtherCurrencies(amountInTripCurrency: Float, tripCurrency: String): List<MoneyValueDetailsDto> {
        val currencies = listOf("PLN", "EUR", "USD", "GBP").filter { it != tripCurrency }
        return currencies.map { currency ->
            MoneyValueDetailsDto(
                currency = currency,
                value = convertCurrency(amountInTripCurrency, tripCurrency, currency)
            )
        }
    }

    private fun updateCategories(
        categories: List<CategoryDto>,
        categoryId: String,
        amountDelta: Float
    ): List<CategoryDto> {
        val categoryExists = categories.any { it.categoryId == categoryId }

        return if (categoryExists) {
            categories.map { category ->
                if (category.categoryId == categoryId) {
                    category.copy(totalAmount = category.totalAmount + amountDelta)
                } else {
                    category
                }
            }
        } else {
            categories + CategoryDto(categoryId, amountDelta)
        }
    }

    private fun updateMyCost(trip: TripDto, newSharedWith: List<ShareDto>): MoneyValueDto? {
        val myShare = newSharedWith.find { it.participantId == "10" } // Current user ID

        return if (myShare != null) {
            val currentMyCost = trip.myCost
            val newValue = myShare.splitValue?.valueMainCurrency ?: 0f

            MoneyValueDto(
                valueMainCurrency = (currentMyCost?.valueMainCurrency ?: 0f) + newValue,
                valueOtherCurrencies = currentMyCost?.valueOtherCurrencies ?: emptyList()
            )
        } else {
            trip.myCost
        }
    }

    private fun createDefaultJoinTrip(): TripDto {
        return TripDto(
            id = "99",
            title = "Nowa wycieczka",
            dateStart = System.currentTimeMillis(),
            dateEnd = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000,
            description = "Wycieczka do której dołączyłeś",
            currency = "PLN",
            totalExpenses = 0f,
            accessCode = "NEW123",
            ownerId = "99",
            imOwner = false,
            myCost = null,
            categories = emptyList(),
            expenses = emptyList(),
            participants = listOf(
                ParticipantDto(
                    id = "99",
                    nickname = "Organizator",
                    totalExpenses = null,
                    isOwner = true,
                    isPlaceholder = false,
                    accessCode = "NEW123",
                    isActive = true
                ),
                ParticipantDto(
                    id = "10",
                    nickname = "Adam",
                    totalExpenses = null,
                    isOwner = false,
                    isPlaceholder = false,
                    accessCode = null,
                    isActive = true
                )
            ),
            settlement = null
        )
    }

    // ==================== INITIAL TRIPS DATA ====================

    private fun createZakopaneTrip(): TripDto {
        return TripDto(
            id = "1",
            title = "Weekend w Zakopanem",
            dateStart = 1711929600000,
            dateEnd = 1712534400000,
            description = "Weekend w górach",
            currency = "PLN",
            totalExpenses = 2400f,
            accessCode = "ZAK123",
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
            expenses = listOf(
                ExpenseDto(
                    id = "11",
                    name = "Hotel",
                    description = "2 noce w centrum",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 1200f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("EUR", 270f),
                            MoneyValueDetailsDto("USD", 300f)
                        )
                    ),
                    amount = 1200f,
                    currency = "PLN",
                    date = 1712016000000,
                    categoryId = "1",
                    payerId = "10",
                    payerNickname = "Adam",
                    sharedWith = listOf(
                        ShareDto(
                            participantId = "10",
                            participantNickname = "Adam",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 400f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 400f))
                            )
                        ),
                        ShareDto(
                            participantId = "11",
                            participantNickname = "Beata",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 400f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 400f))
                            )
                        ),
                        ShareDto(
                            participantId = "12",
                            participantNickname = "Cezary",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 400f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 400f))
                            )
                        )
                    )
                ),
                ExpenseDto(
                    id = "12",
                    name = "Kolacja w Karczmie",
                    description = "Góralskie jedzenie",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 800f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("EUR", 180f),
                            MoneyValueDetailsDto("USD", 200f)
                        )
                    ),
                    amount = 800f,
                    currency = "PLN",
                    date = 1712102400000,
                    categoryId = "2",
                    payerId = "11",
                    payerNickname = "Beata",
                    sharedWith = listOf(
                        ShareDto(
                            participantId = "10",
                            participantNickname = "Adam",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 266.67f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 266.67f))
                            )
                        ),
                        ShareDto(
                            participantId = "11",
                            participantNickname = "Beata",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 266.67f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 266.67f))
                            )
                        ),
                        ShareDto(
                            participantId = "12",
                            participantNickname = "Cezary",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 266.66f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 266.66f))
                            )
                        )
                    )
                ),
                ExpenseDto(
                    id = "13",
                    name = "Wyciąg narciarski",
                    description = "Bilety dzienne",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 400f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("EUR", 90f),
                            MoneyValueDetailsDto("USD", 100f)
                        )
                    ),
                    amount = 400f,
                    currency = "PLN",
                    date = 1712188800000,
                    categoryId = "3",
                    payerId = "12",
                    payerNickname = "Cezary",
                    sharedWith = listOf(
                        ShareDto(
                            participantId = "10",
                            participantNickname = "Adam",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 133.33f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 133.33f))
                            )
                        ),
                        ShareDto(
                            participantId = "11",
                            participantNickname = "Beata",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 133.33f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 133.33f))
                            )
                        ),
                        ShareDto(
                            participantId = "12",
                            participantNickname = "Cezary",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 133.34f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 133.34f))
                            )
                        )
                    )
                )
            ),
            participants = listOf(
                ParticipantDto(
                    id = "10",
                    nickname = "Adam",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 800f,
                        valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 180f))
                    ),
                    isOwner = true,
                    isPlaceholder = false,
                    accessCode = "ZAK123",
                    isActive = true
                ),
                ParticipantDto(
                    id = "11",
                    nickname = "Beata",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 800f,
                        valueOtherCurrencies = listOf(MoneyValueDetailsDto("USD", 200f))
                    ),
                    isOwner = false,
                    isPlaceholder = false,
                    accessCode = "ZAK123",
                    isActive = true
                ),
                ParticipantDto(
                    id = "12",
                    nickname = "Cezary",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 800f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("EUR", 180f),
                            MoneyValueDetailsDto("USD", 200f)
                        )
                    ),
                    isOwner = false,
                    isPlaceholder = false,
                    accessCode = "ZAK123",
                    isActive = true
                )
            ),
            settlement = SettlementDto(
                balance = 0f,
                balanceStatus = BalanceStatus.PLUS,
                relations = emptyList()
            )
        )
    }

    private fun createRzymTrip(): TripDto {
        return TripDto(
            id = "2",
            title = "Wypad do Rzymu",
            dateStart = 1713139200000,
            dateEnd = 1713398400000,
            description = "Zwiedzanie Włoch",
            currency = "PLN",
            totalExpenses = 2250f,
            accessCode = "ROM456",
            ownerId = "10",
            imOwner = true,
            myCost = MoneyValueDto(
                valueMainCurrency = 1125f,
                valueOtherCurrencies = listOf(
                    MoneyValueDetailsDto("EUR", 250f),
                    MoneyValueDetailsDto("USD", 280f)
                )
            ),
            categories = listOf(
                CategoryDto("1", 900f),
                CategoryDto("2", 900f),
                CategoryDto("3", 450f)
            ),
            expenses = listOf(
                ExpenseDto(
                    id = "21",
                    name = "Hotel",
                    description = "3 noce w centrum Rzymu",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 900f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("EUR", 200f),
                            MoneyValueDetailsDto("USD", 225f)
                        )
                    ),
                    amount = 600f,
                    currency = "EUR",
                    date = 1713225600000,
                    categoryId = "1",
                    payerId = "10",
                    payerNickname = "Adam",
                    sharedWith = listOf(
                        ShareDto(
                            participantId = "10",
                            participantNickname = "Adam",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 200f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 900f))
                            )
                        ),
                        ShareDto(
                            participantId = "11",
                            participantNickname = "Beata",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 200f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 900f))
                            )
                        ),
                        ShareDto(
                            participantId = "12",
                            participantNickname = "Cezary",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 200f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 900f))
                            )
                        )
                    )
                ),
                ExpenseDto(
                    id = "22",
                    name = "Kolacja",
                    description = "Restauracja przy Colosseum",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 900f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("EUR", 200f),
                            MoneyValueDetailsDto("USD", 225f)
                        )
                    ),
                    amount = 900f,
                    currency = "PLN",
                    date = 1713312000000,
                    categoryId = "2",
                    payerId = "11",
                    payerNickname = "Beata",
                    sharedWith = listOf(
                        ShareDto(
                            participantId = "10",
                            participantNickname = "Adam",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 300f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 300f))
                            )
                        ),
                        ShareDto(
                            participantId = "11",
                            participantNickname = "Beata",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 300f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 300f))
                            )
                        ),
                        ShareDto(
                            participantId = "12",
                            participantNickname = "Cezary",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 300f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 300f))
                            )
                        )
                    )
                ),
                ExpenseDto(
                    id = "23",
                    name = "Muzeum Watykańskie",
                    description = "Bilety wstępu",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 450f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("EUR", 100f),
                            MoneyValueDetailsDto("USD", 112f)
                        )
                    ),
                    amount = 100f,
                    currency = "USD",
                    date = 1713398400000,
                    categoryId = "3",
                    payerId = "12",
                    payerNickname = "Cezary",
                    sharedWith = listOf(
                        ShareDto(
                            participantId = "10",
                            participantNickname = "Adam",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 33.33f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 150f))
                            )
                        ),
                        ShareDto(
                            participantId = "11",
                            participantNickname = "Beata",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 33.33f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 150f))
                            )
                        ),
                        ShareDto(
                            participantId = "12",
                            participantNickname = "Cezary",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 33.34f,
                                valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 150f))
                            )
                        )
                    )
                )
            ),
            participants = listOf(
                ParticipantDto(
                    id = "10",
                    nickname = "Adam",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 750f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("EUR", 167f),
                            MoneyValueDetailsDto("USD", 188f)
                        )
                    ),
                    isOwner = true,
                    isPlaceholder = false,
                    accessCode = "ROM456",
                    isActive = true
                ),
                ParticipantDto(
                    id = "11",
                    nickname = "Beata",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 750f,
                        valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 167f))
                    ),
                    isOwner = false,
                    isPlaceholder = false,
                    accessCode = "ROM456",
                    isActive = true
                ),
                ParticipantDto(
                    id = "12",
                    nickname = "Cezary",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 750f,
                        valueOtherCurrencies = listOf(MoneyValueDetailsDto("USD", 188f))
                    ),
                    isOwner = false,
                    isPlaceholder = false,
                    accessCode = "ROM456",
                    isActive = true
                )
            ),
            settlement = SettlementDto(
                balance = 110.5f,
                balanceStatus = BalanceStatus.PLUS,
                relations = listOf(
                    SettlementRelationDto(
                        fromUserId = "11",
                        toUserId = "10",
                        fromUserName = "Beata",
                        toUserName = "Adam",
                        amount = MoneyValueDto(
                            valueMainCurrency = 55.25f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 12f))
                        ),
                        isSettled = false
                    ),
                    SettlementRelationDto(
                        fromUserId = "12",
                        toUserId = "10",
                        fromUserName = "Cezary",
                        toUserName = "Adam",
                        amount = MoneyValueDto(
                            valueMainCurrency = 55.25f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("USD", 13f))
                        ),
                        isSettled = false
                    )
                )
            )
        )
    }

    private fun createRoadtripTrip(): TripDto {
        return TripDto(
            id = "3",
            title = "Roadtrip po Europie",
            dateStart = 1714521600000,
            dateEnd = 1715731200000,
            description = "Niemcy -> Francja -> Hiszpania",
            currency = "EUR",
            totalExpenses = 8500f,
            accessCode = "EUR789",
            ownerId = "10",
            imOwner = true,
            myCost = MoneyValueDto(
                valueMainCurrency = 2125f,
                valueOtherCurrencies = listOf(
                    MoneyValueDetailsDto("PLN", 9500f),
                    MoneyValueDetailsDto("USD", 2350f),
                    MoneyValueDetailsDto("GBP", 1800f)
                )
            ),
            categories = listOf(
                CategoryDto("1", 3000f),
                CategoryDto("2", 2500f),
                CategoryDto("5", 1800f),
                CategoryDto("3", 1200f)
            ),
            expenses = listOf(
                ExpenseDto(
                    id = "31",
                    name = "Hotel Berlin",
                    description = "5 nocy w centrum",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 1500f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 6700f),
                            MoneyValueDetailsDto("USD", 1650f),
                            MoneyValueDetailsDto("GBP", 1270f)
                        )
                    ),
                    amount = 1500f,
                    currency = "EUR",
                    date = 1714608000000,
                    categoryId = "1",
                    payerId = "10",
                    payerNickname = "Adam",
                    sharedWith = listOf(
                        ShareDto(participantId = "10", participantNickname = "Adam", splitValue = MoneyValueDto(valueMainCurrency = 375f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f)))),
                        ShareDto(participantId = "11", participantNickname = "Beata", splitValue = MoneyValueDto(valueMainCurrency = 375f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f)))),
                        ShareDto(participantId = "12", participantNickname = "Cezary", splitValue = MoneyValueDto(valueMainCurrency = 375f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f)))),
                        ShareDto(participantId = "13", participantNickname = "Diana", splitValue = MoneyValueDto(valueMainCurrency = 375f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f))))
                    )
                ),
                ExpenseDto(
                    id = "32",
                    name = "Wynajem auta",
                    description = "14 dni z pełnym ubezpieczeniem",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 1800f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 8000f),
                            MoneyValueDetailsDto("USD", 1980f),
                            MoneyValueDetailsDto("GBP", 1525f)
                        )
                    ),
                    amount = 1400f,
                    currency = "GBP",
                    date = 1714694400000,
                    categoryId = "5",
                    payerId = "11",
                    payerNickname = "Beata",
                    sharedWith = listOf(
                        ShareDto(participantId = "10", participantNickname = "Adam", splitValue = MoneyValueDto(valueMainCurrency = 350f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 450f)))),
                        ShareDto(participantId = "11", participantNickname = "Beata", splitValue = MoneyValueDto(valueMainCurrency = 350f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 450f)))),
                        ShareDto(participantId = "12", participantNickname = "Cezary", splitValue = MoneyValueDto(valueMainCurrency = 350f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 450f)))),
                        ShareDto(participantId = "13", participantNickname = "Diana", splitValue = MoneyValueDto(valueMainCurrency = 350f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 450f))))
                    )
                ),
                ExpenseDto(
                    id = "33",
                    name = "Le Bernardin",
                    description = "Obiad na Montmartre",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 800f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 3600f),
                            MoneyValueDetailsDto("USD", 880f),
                            MoneyValueDetailsDto("GBP", 680f)
                        )
                    ),
                    amount = 800f,
                    currency = "EUR",
                    date = 1715126400000,
                    categoryId = "2",
                    payerId = "12",
                    payerNickname = "Cezary",
                    sharedWith = listOf(
                        ShareDto(participantId = "10", participantNickname = "Adam", splitValue = MoneyValueDto(valueMainCurrency = 200f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 200f)))),
                        ShareDto(participantId = "11", participantNickname = "Beata", splitValue = MoneyValueDto(valueMainCurrency = 200f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 200f)))),
                        ShareDto(participantId = "12", participantNickname = "Cezary", splitValue = MoneyValueDto(valueMainCurrency = 200f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 200f)))),
                        ShareDto(participantId = "13", participantNickname = "Diana", splitValue = MoneyValueDto(valueMainCurrency = 200f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 200f))))
                    )
                ),
                ExpenseDto(
                    id = "34",
                    name = "Muzeum Luwr",
                    description = "Bilety + audioguide",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 600f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 2700f),
                            MoneyValueDetailsDto("USD", 660f),
                            MoneyValueDetailsDto("GBP", 510f)
                        )
                    ),
                    amount = 600f,
                    currency = "USD",
                    date = 1715212800000,
                    categoryId = "3",
                    payerId = "13",
                    payerNickname = "Diana",
                    sharedWith = listOf(
                        ShareDto(participantId = "10", participantNickname = "Adam", splitValue = MoneyValueDto(valueMainCurrency = 150f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 150f)))),
                        ShareDto(participantId = "11", participantNickname = "Beata", splitValue = MoneyValueDto(valueMainCurrency = 150f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 150f)))),
                        ShareDto(participantId = "12", participantNickname = "Cezary", splitValue = MoneyValueDto(valueMainCurrency = 150f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 150f)))),
                        ShareDto(participantId = "13", participantNickname = "Diana", splitValue = MoneyValueDto(valueMainCurrency = 150f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 150f))))
                    )
                ),
                ExpenseDto(
                    id = "35",
                    name = "Bar de Tapas",
                    description = "Kolacja z widokiem na Sagrada Familia",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 700f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 3150f),
                            MoneyValueDetailsDto("USD", 770f),
                            MoneyValueDetailsDto("GBP", 595f)
                        )
                    ),
                    amount = 700f,
                    currency = "EUR",
                    date = 1715558400000,
                    categoryId = "2",
                    payerId = "10",
                    payerNickname = "Adam",
                    sharedWith = listOf(
                        ShareDto(participantId = "10", participantNickname = "Adam", splitValue = MoneyValueDto(valueMainCurrency = 175f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 175f)))),
                        ShareDto(participantId = "11", participantNickname = "Beata", splitValue = MoneyValueDto(valueMainCurrency = 175f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 175f)))),
                        ShareDto(participantId = "12", participantNickname = "Cezary", splitValue = MoneyValueDto(valueMainCurrency = 175f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 175f)))),
                        ShareDto(participantId = "13", participantNickname = "Diana", splitValue = MoneyValueDto(valueMainCurrency = 175f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 175f))))
                    )
                ),
                ExpenseDto(
                    id = "36",
                    name = "Park Güell",
                    description = "Bilety wstępu i przewodnik",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 600f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 2700f),
                            MoneyValueDetailsDto("USD", 660f),
                            MoneyValueDetailsDto("GBP", 510f)
                        )
                    ),
                    amount = 2400f,
                    currency = "PLN",
                    date = 1715644800000,
                    categoryId = "3",
                    payerId = "11",
                    payerNickname = "Beata",
                    sharedWith = listOf(
                        ShareDto(participantId = "10", participantNickname = "Adam", splitValue = MoneyValueDto(valueMainCurrency = 600f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 150f)))),
                        ShareDto(participantId = "11", participantNickname = "Beata", splitValue = MoneyValueDto(valueMainCurrency = 600f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 150f)))),
                        ShareDto(participantId = "12", participantNickname = "Cezary", splitValue = MoneyValueDto(valueMainCurrency = 600f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 150f)))),
                        ShareDto(participantId = "13", participantNickname = "Diana", splitValue = MoneyValueDto(valueMainCurrency = 600f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 150f))))
                    )
                ),
                ExpenseDto(
                    id = "37",
                    name = "Hotel Paris",
                    description = "4 noce blisko Wieży Eiffla",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 1500f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 6700f),
                            MoneyValueDetailsDto("USD", 1650f),
                            MoneyValueDetailsDto("GBP", 1270f)
                        )
                    ),
                    amount = 1500f,
                    currency = "EUR",
                    date = 1715040000000,
                    categoryId = "1",
                    payerId = "12",
                    payerNickname = "Cezary",
                    sharedWith = listOf(
                        ShareDto(participantId = "10", participantNickname = "Adam", splitValue = MoneyValueDto(valueMainCurrency = 375f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f)))),
                        ShareDto(participantId = "11", participantNickname = "Beata", splitValue = MoneyValueDto(valueMainCurrency = 375f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f)))),
                        ShareDto(participantId = "12", participantNickname = "Cezary", splitValue = MoneyValueDto(valueMainCurrency = 375f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f)))),
                        ShareDto(participantId = "13", participantNickname = "Diana", splitValue = MoneyValueDto(valueMainCurrency = 375f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f))))
                    )
                ),
                ExpenseDto(
                    id = "38",
                    name = "Zakupy spożywcze",
                    description = "Przekąski na drogę",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 400f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 1800f),
                            MoneyValueDetailsDto("USD", 440f),
                            MoneyValueDetailsDto("GBP", 340f)
                        )
                    ),
                    amount = 400f,
                    currency = "EUR",
                    date = 1714953600000,
                    categoryId = "2",
                    payerId = "13",
                    payerNickname = "Diana",
                    sharedWith = listOf(
                        ShareDto(participantId = "10", participantNickname = "Adam", splitValue = MoneyValueDto(valueMainCurrency = 200f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 200f)))),
                        ShareDto(participantId = "13", participantNickname = "Diana", splitValue = MoneyValueDto(valueMainCurrency = 200f, valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 200f))))
                    )
                )
            ),
            participants = listOf(
                ParticipantDto(
                    id = "10",
                    nickname = "Adam",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 2325f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 10400f),
                            MoneyValueDetailsDto("USD", 2555f)
                        )
                    ),
                    isOwner = true,
                    isPlaceholder = false,
                    accessCode = "EUR789",
                    isActive = true
                ),
                ParticipantDto(
                    id = "11",
                    nickname = "Beata",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 2125f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("GBP", 1800f),
                            MoneyValueDetailsDto("USD", 2335f)
                        )
                    ),
                    isOwner = false,
                    isPlaceholder = false,
                    accessCode = "EUR789",
                    isActive = true
                ),
                ParticipantDto(
                    id = "12",
                    nickname = "Cezary",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 2125f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 9500f),
                            MoneyValueDetailsDto("GBP", 1800f)
                        )
                    ),
                    isOwner = false,
                    isPlaceholder = false,
                    accessCode = "EUR789",
                    isActive = true
                ),
                ParticipantDto(
                    id = "13",
                    nickname = "Diana",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 1925f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("USD", 2115f),
                            MoneyValueDetailsDto("GBP", 1635f)
                        )
                    ),
                    isOwner = false,
                    isPlaceholder = false,
                    accessCode = "EUR789",
                    isActive = true
                )
            ),
            settlement = SettlementDto(
                balance = 200f,
                balanceStatus = BalanceStatus.PLUS,
                relations = listOf(
                    SettlementRelationDto(
                        fromUserId = "13",
                        toUserId = "10",
                        fromUserName = "Diana",
                        toUserName = "Adam",
                        amount = MoneyValueDto(
                            valueMainCurrency = 200f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("USD", 220f))
                        ),
                        isSettled = false
                    ),
                    SettlementRelationDto(
                        fromUserId = "11",
                        toUserId = "12",
                        fromUserName = "Beata",
                        toUserName = "Cezary",
                        amount = MoneyValueDto(
                            valueMainCurrency = 100f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("GBP", 85f))
                        ),
                        isSettled = true
                    )
                )
            )
        )
    }
}

