package com.example.tripapp2.data.repository

import com.example.tripapp2.data.model.*

/**
 * MockData - Dane testowe dla aplikacji
 *
 * Struktura:
 * - Mutable storage dla tripów (pozwala na modyfikacje)
 * - Osobne funkcje dla każdego tripu
 * - getTripList() zwraca aktualny stan wszystkich tripów
 * - addExpenseMock() dodaje wydatek do istniejącego tripu
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
    // PUBLIC API
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

        // Zapisz do storage
        tripsStorage[newId] = newTrip

        return CreateTripDto(
            success = SuccessDto(
                success = true,
                message = "Trip created successfully"
            ),
            trip = newTrip
        )
    }

    /**
     * Dołącza do wycieczki po kodzie dostępu
     */
    fun joinTripMock(accessCode: String): JoinTripDto {
        initializeIfNeeded()

        // Szukaj tripu po kodzie dostępu
        val existingTrip = tripsStorage.values.find { it.accessCode == accessCode }

        if (existingTrip != null) {
            return JoinTripDto(
                success = SuccessDto(
                    success = true,
                    message = "Successfully joined trip"
                ),
                trip = existingTrip
            )
        }

        // Jeśli nie znaleziono - zwróć mock trip (dla kompatybilności)
        return JoinTripDto(
            success = SuccessDto(
                success = true,
                message = "Successfully joined trip"
            ),
            trip = createJoinTripBarcelona()
        )
    }

    /**
     * Dodaje wydatek do istniejącego tripu
     * Zwraca AddExpenseDto z success i zaktualizowanym tripem
     */
    fun addExpenseMock(request: AddExpenseRequest): AddExpenseDto {
        initializeIfNeeded()

        val trip = tripsStorage[request.tripId]
            ?: return AddExpenseDto(
                success = SuccessDto(
                    success = false,
                    message = "Trip not found"
                ),
                trip = null
            )

        val newExpenseId = "${request.tripId}${(100..999).random()}"

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
            sharedWith = request.sharedWith
        )

        // Zaktualizuj trip
        val updatedExpenses = trip.expenses + newExpense
        val updatedTotalExpenses = trip.totalExpenses + request.amount
        val updatedCategories = updateCategories(trip.categories, request.categoryId, request.amount)

        val updatedTrip = trip.copy(
            expenses = updatedExpenses,
            totalExpenses = updatedTotalExpenses,
            categories = updatedCategories
        )

        // Zapisz zaktualizowany trip
        tripsStorage[request.tripId] = updatedTrip

        return AddExpenseDto(
            success = SuccessDto(
                success = true,
                message = "Expense added successfully"
            ),
            trip = updatedTrip
        )
    }

    fun getUsrInfo(): UserInfoDto {
        return UserInfoDto(
            id = "10",
            nickname = "Adam"
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

    // ==========================================
    // TRIP 1: Weekend w Zakopanem (PLN)
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
                CategoryDto("1", 1200f),  // Noclegi
                CategoryDto("2", 800f),   // Jedzenie
                CategoryDto("3", 400f)    // Transport
            ),
            expenses = createZakopaneExpenses(),
            participants = createZakopaneParticipants(),
            settlement = SettlementDto(
                balance = 200f,
                balanceStatus = BalanceStatus.PLUS,
                relations = listOf(
                    SettlementRelationDto(
                        fromUserId = "11",
                        fromUserName = "Beata",
                        toUserId = "10",
                        toUserName = "Adam",
                        amount = MoneyValueDto(valueMainCurrency = 200f),
                        isSettled = false
                    )
                )
            )
        )
    }

    private fun createZakopaneExpenses(): List<ExpenseDto> {
        return listOf(
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
                name = "Restauracja Góralska",
                description = "Obiad dla wszystkich",
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
                name = "Paliwo",
                description = "Dojazd do Zakopanego",
                totalExpense = MoneyValueDto(
                    valueMainCurrency = 400f,
                    valueOtherCurrencies = listOf(
                        MoneyValueDetailsDto("EUR", 90f),
                        MoneyValueDetailsDto("USD", 100f)
                    )
                ),
                amount = 400f,
                currency = "PLN",
                date = 1711929600000,
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
        )
    }

    private fun createZakopaneParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 800f,
                    valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 800f))
                ),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "11",
                nickname = "Beata",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 800f,
                    valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 800f))
                ),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "12",
                nickname = "Cezary",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 800f,
                    valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 800f))
                ),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            )
        )
    }

    // ==========================================
    // TRIP 2: Eurotrip (EUR) - różne waluty wydatków
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
            ownerId = "10",
            imOwner = true,
            myCost = MoneyValueDto(
                valueMainCurrency = 2325f,
                valueOtherCurrencies = listOf(
                    MoneyValueDetailsDto("PLN", 10400f),
                    MoneyValueDetailsDto("USD", 2555f)
                )
            ),
            categories = listOf(
                CategoryDto("1", 1500f),  // Noclegi
                CategoryDto("2", 1200f),  // Jedzenie
                CategoryDto("3", 1800f)   // Transport
            ),
            expenses = createEurotripExpenses(),
            participants = createEurotripParticipants(),
            settlement = SettlementDto(
                balance = 150f,
                balanceStatus = BalanceStatus.PLUS,
                relations = listOf(
                    SettlementRelationDto(
                        fromUserId = "11",
                        fromUserName = "Beata",
                        toUserId = "10",
                        toUserName = "Adam",
                        amount = MoneyValueDto(valueMainCurrency = 75f),
                        isSettled = false
                    ),
                    SettlementRelationDto(
                        fromUserId = "13",
                        fromUserName = "Diana",
                        toUserId = "10",
                        toUserName = "Adam",
                        amount = MoneyValueDto(valueMainCurrency = 75f),
                        isSettled = false
                    )
                )
            )
        )
    }

    private fun createEurotripExpenses(): List<ExpenseDto> {
        return listOf(
            // Wydatek w EUR
            ExpenseDto(
                id = "31",
                name = "Hotel w Paryżu",
                description = "3 noce blisko wieży Eiffla",
                totalExpense = MoneyValueDto(
                    valueMainCurrency = 1500f,
                    valueOtherCurrencies = listOf(
                        MoneyValueDetailsDto("PLN", 6750f),
                        MoneyValueDetailsDto("USD", 1650f)
                    )
                ),
                amount = 1500f,
                currency = "EUR",
                date = 1720224000000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto(
                        participantId = "10",
                        participantNickname = "Adam",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 375f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f))
                        )
                    ),
                    ShareDto(
                        participantId = "11",
                        participantNickname = "Beata",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 375f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f))
                        )
                    ),
                    ShareDto(
                        participantId = "12",
                        participantNickname = "Cezary",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 375f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f))
                        )
                    ),
                    ShareDto(
                        participantId = "13",
                        participantNickname = "Diana",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 375f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 375f))
                        )
                    )
                )
            ),
            // Wydatek w GBP (inna waluta!)
            ExpenseDto(
                id = "32",
                name = "Wynajem auta",
                description = "14 dni z pełnym ubezpieczeniem",
                totalExpense = MoneyValueDto(
                    valueMainCurrency = 1800f, // EUR (trip currency)
                    valueOtherCurrencies = listOf(
                        MoneyValueDetailsDto("PLN", 8000f),
                        MoneyValueDetailsDto("USD", 1980f),
                        MoneyValueDetailsDto("GBP", 1525f)
                    )
                ),
                amount = 1400f, // Faktyczny koszt w GBP
                currency = "GBP",
                date = 1720137600000,
                categoryId = "3",
                payerId = "11",
                payerNickname = "Beata",
                sharedWith = listOf(
                    ShareDto(
                        participantId = "10",
                        participantNickname = "Adam",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 450f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("GBP", 350f))
                        )
                    ),
                    ShareDto(
                        participantId = "11",
                        participantNickname = "Beata",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 450f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("GBP", 350f))
                        )
                    ),
                    ShareDto(
                        participantId = "12",
                        participantNickname = "Cezary",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 450f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("GBP", 350f))
                        )
                    ),
                    ShareDto(
                        participantId = "13",
                        participantNickname = "Diana",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 450f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("GBP", 350f))
                        )
                    )
                )
            ),
            // Wydatek w EUR
            ExpenseDto(
                id = "33",
                name = "Kolacja w Rzymie",
                description = "Restauracja przy Koloseum",
                totalExpense = MoneyValueDto(
                    valueMainCurrency = 1200f,
                    valueOtherCurrencies = listOf(
                        MoneyValueDetailsDto("PLN", 5400f),
                        MoneyValueDetailsDto("USD", 1320f)
                    )
                ),
                amount = 1200f,
                currency = "EUR",
                date = 1720742400000,
                categoryId = "2",
                payerId = "12",
                payerNickname = "Cezary",
                sharedWith = listOf(
                    ShareDto(
                        participantId = "10",
                        participantNickname = "Adam",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 300f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 300f))
                        )
                    ),
                    ShareDto(
                        participantId = "11",
                        participantNickname = "Beata",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 300f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 300f))
                        )
                    ),
                    ShareDto(
                        participantId = "12",
                        participantNickname = "Cezary",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 300f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 300f))
                        )
                    ),
                    ShareDto(
                        participantId = "13",
                        participantNickname = "Diana",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 300f,
                            valueOtherCurrencies = listOf(MoneyValueDetailsDto("EUR", 300f))
                        )
                    )
                )
            )
        )
    }

    private fun createEurotripParticipants(): List<ParticipantDto> {
        return listOf(
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
                accessCode = null,
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
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "12",
                nickname = "Cezary",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 1125f,
                    valueOtherCurrencies = listOf(
                        MoneyValueDetailsDto("PLN", 5000f),
                        MoneyValueDetailsDto("USD", 1235f)
                    )
                ),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "13",
                nickname = "Diana",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 1125f,
                    valueOtherCurrencies = listOf(
                        MoneyValueDetailsDto("PLN", 5000f),
                        MoneyValueDetailsDto("USD", 1235f)
                    )
                ),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            )
        )
    }

    // ==========================================
    // TRIP 3: Wakacje nad morzem (PLN)
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
            myCost = MoneyValueDto(
                valueMainCurrency = 1733.33f,
                valueOtherCurrencies = emptyList()
            ),
            categories = listOf(
                CategoryDto("1", 3500f),  // Noclegi
                CategoryDto("2", 1200f),  // Jedzenie
                CategoryDto("4", 500f)    // Atrakcje
            ),
            expenses = createWakacjeExpenses(),
            participants = createWakacjeParticipants(),
            settlement = SettlementDto(
                balance = 0f,
                balanceStatus = BalanceStatus.PLUS,
                relations = listOf(
                    SettlementRelationDto(
                        fromUserId = "14",
                        fromUserName = "Ewa",
                        toUserId = "10",
                        toUserName = "Adam",
                        amount = MoneyValueDto(valueMainCurrency = 166.67f),
                        isSettled = true
                    )
                )
            )
        )
    }

    private fun createWakacjeExpenses(): List<ExpenseDto> {
        return listOf(
            ExpenseDto(
                id = "41",
                name = "Apartament Sopot",
                description = "7 nocy z widokiem na morze",
                totalExpense = MoneyValueDto(
                    valueMainCurrency = 3500f,
                    valueOtherCurrencies = emptyList()
                ),
                amount = 3500f,
                currency = "PLN",
                date = 1723161600000,
                categoryId = "1",
                payerId = "10",
                payerNickname = "Adam",
                sharedWith = listOf(
                    ShareDto(
                        participantId = "10",
                        participantNickname = "Adam",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 1166.67f,
                            valueOtherCurrencies = emptyList()
                        )
                    ),
                    ShareDto(
                        participantId = "14",
                        participantNickname = "Ewa",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 1166.67f,
                            valueOtherCurrencies = emptyList()
                        )
                    ),
                    ShareDto(
                        participantId = "15",
                        participantNickname = "Filip",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 1166.66f,
                            valueOtherCurrencies = emptyList()
                        )
                    )
                )
            ),
            ExpenseDto(
                id = "42",
                name = "Restauracja rybna",
                description = "Kolacja przy molo",
                totalExpense = MoneyValueDto(
                    valueMainCurrency = 1200f,
                    valueOtherCurrencies = emptyList()
                ),
                amount = 1200f,
                currency = "PLN",
                date = 1723420800000,
                categoryId = "2",
                payerId = "14",
                payerNickname = "Ewa",
                sharedWith = listOf(
                    ShareDto(
                        participantId = "10",
                        participantNickname = "Adam",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 400f,
                            valueOtherCurrencies = emptyList()
                        )
                    ),
                    ShareDto(
                        participantId = "14",
                        participantNickname = "Ewa",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 400f,
                            valueOtherCurrencies = emptyList()
                        )
                    ),
                    ShareDto(
                        participantId = "15",
                        participantNickname = "Filip",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 400f,
                            valueOtherCurrencies = emptyList()
                        )
                    )
                )
            ),
            ExpenseDto(
                id = "43",
                name = "Rejs statkiem",
                description = "Wycieczka po zatoce",
                totalExpense = MoneyValueDto(
                    valueMainCurrency = 500f,
                    valueOtherCurrencies = emptyList()
                ),
                amount = 500f,
                currency = "PLN",
                date = 1723593600000,
                categoryId = "4",
                payerId = "15",
                payerNickname = "Filip",
                sharedWith = listOf(
                    ShareDto(
                        participantId = "10",
                        participantNickname = "Adam",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 166.67f,
                            valueOtherCurrencies = emptyList()
                        )
                    ),
                    ShareDto(
                        participantId = "14",
                        participantNickname = "Ewa",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 166.67f,
                            valueOtherCurrencies = emptyList()
                        )
                    ),
                    ShareDto(
                        participantId = "15",
                        participantNickname = "Filip",
                        splitValue = MoneyValueDto(
                            valueMainCurrency = 166.66f,
                            valueOtherCurrencies = emptyList()
                        )
                    )
                )
            )
        )
    }

    private fun createWakacjeParticipants(): List<ParticipantDto> {
        return listOf(
            ParticipantDto(
                id = "10",
                nickname = "Adam",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 1733.33f,
                    valueOtherCurrencies = emptyList()
                ),
                isOwner = true,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "14",
                nickname = "Ewa",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 1733.33f,
                    valueOtherCurrencies = emptyList()
                ),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            ),
            ParticipantDto(
                id = "15",
                nickname = "Filip",
                totalExpenses = MoneyValueDto(
                    valueMainCurrency = 1733.34f,
                    valueOtherCurrencies = emptyList()
                ),
                isOwner = false,
                isPlaceholder = false,
                accessCode = null,
                isActive = true
            )
        )
    }

    // ==========================================
    // JOIN TRIP - Barcelona (dla kompatybilności)
    // ==========================================

    private fun createJoinTripBarcelona(): TripDto {
        return TripDto(
            id = "22",
            title = "City Break Barcelona",
            dateStart = 1719532800000,
            dateEnd = 1719964800000,
            description = "Zwiedzanie Barcelony i plaża",
            currency = "EUR",
            totalExpenses = 1850f,
            accessCode = "BCN-2024",
            ownerId = "20",
            imOwner = false,
            myCost = MoneyValueDto(
                valueMainCurrency = 462.50f,
                valueOtherCurrencies = listOf(
                    MoneyValueDetailsDto("PLN", 1990f),
                    MoneyValueDetailsDto("USD", 502f)
                )
            ),
            categories = listOf(
                CategoryDto("1", 720f),
                CategoryDto("2", 580f),
                CategoryDto("3", 320f),
                CategoryDto("4", 230f)
            ),
            expenses = listOf(
                ExpenseDto(
                    id = "21",
                    name = "Apartament Airbnb",
                    description = "4 noce w Barcelonecie",
                    totalExpense = MoneyValueDto(
                        valueMainCurrency = 720f,
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 3100f),
                            MoneyValueDetailsDto("USD", 780f)
                        )
                    ),
                    amount = 720f,
                    currency = "EUR",
                    date = 1719532800000,
                    categoryId = "1",
                    payerId = "20",
                    payerNickname = "Kasia",
                    sharedWith = listOf(
                        ShareDto(
                            participantId = "20",
                            participantNickname = "Kasia",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 180f,
                                valueOtherCurrencies = emptyList()
                            )
                        ),
                        ShareDto(
                            participantId = "21",
                            participantNickname = "Michał",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 180f,
                                valueOtherCurrencies = emptyList()
                            )
                        ),
                        ShareDto(
                            participantId = "22",
                            participantNickname = "Ola",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 180f,
                                valueOtherCurrencies = emptyList()
                            )
                        ),
                        ShareDto(
                            participantId = "23",
                            participantNickname = "Tomek",
                            splitValue = MoneyValueDto(
                                valueMainCurrency = 180f,
                                valueOtherCurrencies = emptyList()
                            )
                        )
                    )
                )
            ),
            participants = listOf(
                ParticipantDto(
                    id = "20",
                    nickname = "Kasia",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 462.50f,
                        valueOtherCurrencies = emptyList()
                    ),
                    isOwner = true,
                    isPlaceholder = false,
                    accessCode = null,
                    isActive = true
                ),
                ParticipantDto(
                    id = "21",
                    nickname = "Michał",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 462.50f,
                        valueOtherCurrencies = emptyList()
                    ),
                    isOwner = false,
                    isPlaceholder = false,
                    accessCode = null,
                    isActive = true
                ),
                ParticipantDto(
                    id = "22",
                    nickname = "Ola",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 462.50f,
                        valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 1990f))
                    ),
                    isOwner = false,
                    isPlaceholder = true,
                    accessCode = "BCN-2024",
                    isActive = false
                ),
                ParticipantDto(
                    id = "23",
                    nickname = "Tomek",
                    totalExpenses = MoneyValueDto(
                        valueMainCurrency = 462.50f,
                        valueOtherCurrencies = listOf(MoneyValueDetailsDto("PLN", 1990f))
                    ),
                    isOwner = false,
                    isPlaceholder = true,
                    accessCode = "BCN-2024",
                    isActive = false
                )
            ),
            settlement = SettlementDto(
                balance = 0f,
                balanceStatus = BalanceStatus.PLUS,
                relations = listOf(
                    SettlementRelationDto(
                        fromUserId = "23",
                        fromUserName = "Tomek",
                        toUserId = "20",
                        toUserName = "Kasia",
                        amount = MoneyValueDto(valueMainCurrency = 17.50f),
                        isSettled = true
                    ),
                    SettlementRelationDto(
                        fromUserId = "22",
                        fromUserName = "Ola",
                        toUserId = "21",
                        toUserName = "Michał",
                        amount = MoneyValueDto(valueMainCurrency = 17.50f),
                        isSettled = true
                    )
                )
            )
        )
    }
}