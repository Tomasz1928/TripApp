package com.example.tripapp2.data.repository

import com.example.tripapp2.data.model.*

object MockData {

    fun createTripMock(title:String, dateStart: Long, dateEnd: Long, description: String, currency: String):CreateTripDto{
        return CreateTripDto(
            trip = TripDto(
            id=(1..100).random().toString(),
            title = title,
            dateStart =   dateStart,
            dateEnd = dateEnd,
            description = description,
            currency = currency,
            totalExpenses = 0f,
            accessCode = "1234",
            ownerId = "10",
            imOwner = true,
            myCost = null,
            categories = emptyList(),
            expenses = emptyList(),
            participants =listOf(
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
        ),
            success = SuccessDto(
                success = true,
                message = "Trip created successfully"
            )
        )
    }


    fun joinTripMock(accessCode:String):JoinTripDto{
        return JoinTripDto(
            success = SuccessDto(
                success = true,
                message = "Trip created successfully"
            ),
            trip = TripDto(
                id = "22",
                title = "City Break Barcelona",
                dateStart = 1719532800000, // 28.06.2024
                dateEnd = 1719964800000,   // 03.07.2024
                description = "Zwiedzanie Barcelony i plaża",
                currency = "EUR",
                totalExpenses = 1850f,
                accessCode = "BCN2024",
                ownerId = "20",
                imOwner = true,
                myCost = MoneyValueDto(
                    valueMainCurrency = 462.50f,
                    valueOtherCurrencies = listOf(
                        MoneyValueDetailsDto("PLN", 1990f),
                        MoneyValueDetailsDto("USD", 502f)
                    )
                ),
                categories = listOf(
                    CategoryDto("1", 720f),  // Noclegi
                    CategoryDto("2", 580f),  // Jedzenie
                    CategoryDto("3", 320f),  // Transport
                    CategoryDto("4", 230f)   // Atrakcje
                ),
                expenses = listOf(
                    ExpenseDto(
                        id = "21",
                        name = "Apartament Airbnb",
                        description = "4 noce w Barcelonecie",
                        totalExpense = MoneyValueDto(
                            valueMainCurrency = 720f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 3096f),
                                MoneyValueDetailsDto("USD", 782f)
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
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 180f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "21",
                                participantNickname = "Michał",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 180f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 180f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "22",
                                participantNickname = "Ola",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 180f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 180f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "23",
                                participantNickname = "Tomek",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 180f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 180f)
                                    )
                                )
                            )
                        )
                    ),
                    ExpenseDto(
                        id = "22",
                        name = "Kolacja z owocami morza",
                        description = "Restauracja La Mar Salada",
                        totalExpense = MoneyValueDto(
                            valueMainCurrency = 280f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 1204f),
                                MoneyValueDetailsDto("USD", 304f)
                            )
                        ),
                        amount = 280f,
                        currency = "EUR",
                        date = 1719619200000,
                        categoryId = "2",
                        payerId = "21",
                        payerNickname = "Michał",
                        sharedWith = listOf(
                            ShareDto(
                                participantId = "20",
                                participantNickname = "Kasia",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 70f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 70f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "21",
                                participantNickname = "Michał",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 70f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 70f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "22",
                                participantNickname = "Ola",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 70f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 70f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "23",
                                participantNickname = "Tomek",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 70f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 70f)
                                    )
                                )
                            )
                        )
                    ),
                    ExpenseDto(
                        id = "23",
                        name = "Tapas i wino",
                        description = "Bar El Xampanyet",
                        totalExpense = MoneyValueDto(
                            valueMainCurrency = 150f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 645f),
                                MoneyValueDetailsDto("USD", 163f)
                            )
                        ),
                        amount = 150f,
                        currency = "EUR",
                        date = 1719705600000,
                        categoryId = "2",
                        payerId = "22",
                        payerNickname = "Ola",
                        sharedWith = listOf(
                            ShareDto(
                                participantId = "20",
                                participantNickname = "Kasia",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 37.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 37.50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "21",
                                participantNickname = "Michał",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 37.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 37.50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "22",
                                participantNickname = "Ola",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 37.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 37.50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "23",
                                participantNickname = "Tomek",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 37.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 37.50f)
                                    )
                                )
                            )
                        )
                    ),
                    ExpenseDto(
                        id = "24",
                        name = "Śniadanie La Boqueria",
                        description = "Świeże owoce i soki",
                        totalExpense = MoneyValueDto(
                            valueMainCurrency = 150f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 645f),
                                MoneyValueDetailsDto("USD", 163f)
                            )
                        ),
                        amount = 150f,
                        currency = "EUR",
                        date = 1719792000000,
                        categoryId = "2",
                        payerId = "20",
                        payerNickname = "Kasia",
                        sharedWith = listOf(
                            ShareDto(
                                participantId = "20",
                                participantNickname = "Kasia",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 37.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 37.50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "21",
                                participantNickname = "Michał",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 37.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 37.50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "22",
                                participantNickname = "Ola",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 37.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 37.50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "23",
                                participantNickname = "Tomek",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 37.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 37.50f)
                                    )
                                )
                            )
                        )
                    ),
                    ExpenseDto(
                        id = "25",
                        name = "Wynajem skuterów",
                        description = "2 skutery na cały dzień",
                        totalExpense = MoneyValueDto(
                            valueMainCurrency = 120f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 516f),
                                MoneyValueDetailsDto("USD", 130f)
                            )
                        ),
                        amount = 120f,
                        currency = "EUR",
                        date = 1719705600000,
                        categoryId = "3",
                        payerId = "23",
                        payerNickname = "Tomek",
                        sharedWith = listOf(
                            ShareDto(
                                participantId = "20",
                                participantNickname = "Kasia",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 30f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 30f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "21",
                                participantNickname = "Michał",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 30f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 30f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "22",
                                participantNickname = "Ola",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 30f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 30f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "23",
                                participantNickname = "Tomek",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 30f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 30f)
                                    )
                                )
                            )
                        )
                    ),
                    ExpenseDto(
                        id = "26",
                        name = "Metro - bilety grupowe",
                        description = "T-Casual x4",
                        totalExpense = MoneyValueDto(
                            valueMainCurrency = 200f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 860f),
                                MoneyValueDetailsDto("USD", 217f)
                            )
                        ),
                        amount = 200f,
                        currency = "EUR",
                        date = 1719532800000,
                        categoryId = "3",
                        payerId = "21",
                        payerNickname = "Michał",
                        sharedWith = listOf(
                            ShareDto(
                                participantId = "20",
                                participantNickname = "Kasia",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "21",
                                participantNickname = "Michał",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "22",
                                participantNickname = "Ola",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "23",
                                participantNickname = "Tomek",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 50f)
                                    )
                                )
                            )
                        )
                    ),
                    ExpenseDto(
                        id = "27",
                        name = "Sagrada Familia",
                        description = "Bilety wstępu z przewodnikiem",
                        totalExpense = MoneyValueDto(
                            valueMainCurrency = 140f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 602f),
                                MoneyValueDetailsDto("USD", 152f)
                            )
                        ),
                        amount = 140f,
                        currency = "EUR",
                        date = 1719619200000,
                        categoryId = "4",
                        payerId = "22",
                        payerNickname = "Ola",
                        sharedWith = listOf(
                            ShareDto(
                                participantId = "20",
                                participantNickname = "Kasia",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 35f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 35f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "21",
                                participantNickname = "Michał",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 35f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 35f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "22",
                                participantNickname = "Ola",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 35f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 35f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "23",
                                participantNickname = "Tomek",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 35f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 35f)
                                    )
                                )
                            )
                        )
                    ),
                    ExpenseDto(
                        id = "28",
                        name = "Park Güell",
                        description = "Bilety online",
                        totalExpense = MoneyValueDto(
                            valueMainCurrency = 90f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 387f),
                                MoneyValueDetailsDto("USD", 98f)
                            )
                        ),
                        amount = 90f,
                        currency = "EUR",
                        date = 1719792000000,
                        categoryId = "4",
                        payerId = "23",
                        payerNickname = "Tomek",
                        sharedWith = listOf(
                            ShareDto(
                                participantId = "20",
                                participantNickname = "Kasia",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 22.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 22.50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "21",
                                participantNickname = "Michał",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 22.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 22.50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "22",
                                participantNickname = "Ola",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 22.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 22.50f)
                                    )
                                )
                            ),
                            ShareDto(
                                participantId = "23",
                                participantNickname = "Tomek",
                                splitValue = MoneyValueDto(
                                    valueMainCurrency = 22.50f,
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 22.50f)
                                    )
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
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 1990f)
                            )
                        ),
                        isOwner = true,
                        isPlaceholder = false,
                        accessCode = "BCN2024",
                        isActive = true
                    ),
                    ParticipantDto(
                        id = "21",
                        nickname = "Michał",
                        totalExpenses = MoneyValueDto(
                            valueMainCurrency = 462.50f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 1990f)
                            )
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
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 1990f)
                            )
                        ),
                        isOwner = false,
                        isPlaceholder = true,
                        accessCode = "BCN2024",
                        isActive = false
                    ),
                    ParticipantDto(
                        id = "23",
                        nickname = "Tomek",
                        totalExpenses = MoneyValueDto(
                            valueMainCurrency = 462.50f,
                            valueOtherCurrencies = listOf(
                                MoneyValueDetailsDto("PLN", 1990f)
                            )
                        ),
                        isOwner = false,
                        isPlaceholder = true,
                        accessCode = "BCN2024",
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
                            amount = MoneyValueDto(
                                valueMainCurrency = 17.50f,
                            ),
                            isSettled = true
                        ),
                        SettlementRelationDto(
                            fromUserId = "22",
                            fromUserName = "Ola",
                            toUserId = "21",
                            toUserName = "Michał",
                            amount = MoneyValueDto(
                                valueMainCurrency = 17.50f,
                            ),
                            isSettled = true
                        )
                    )
                )
            )

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
            trips = listOf(
                // ===================== TRIP 1: WSZYSTKIE KOSZTY W TEJ SAMEJ WALUCIE (PLN) =====================
                TripDto(
                    id = "1",
                    title = "Weekend w Zakopanem",
                    dateStart = 1711929600000,
                    dateEnd = 1712534400000,
                    description = "Weekend w górach",
                    currency = "PLN", // Waluta główna wycieczki
                    totalExpenses = 2400f,
                    accessCode = "1234",
                    ownerId = "10",
                    imOwner = true,
                    myCost = MoneyValueDto(
                        valueMainCurrency = 800f, // W PLN (trip currency)
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
                                valueMainCurrency = 1200f, // W PLN (trip currency)
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("EUR", 270f),
                                    MoneyValueDetailsDto("USD", 300f)
                                )
                            ),
                            amount = 1200f, // Faktyczny koszt
                            currency = "PLN", // Koszt w PLN (ta sama co trip)
                            date = 1712016000000,
                            categoryId = "1",
                            payerId = "10",
                            payerNickname = "Adam",
                            sharedWith = listOf(
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 400f, // cost currency = PLN
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 400f) // trip currency = PLN (ta sama!)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 400f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 400f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 400f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 400f)
                                        )
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
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 266.67f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 266.67f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 266.67f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 266.66f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 266.66f)
                                        )
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
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 133.33f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 133.33f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 133.33f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 133.34f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 133.34f)
                                        )
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
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("EUR", 180f)
                                )
                            ),
                            isOwner = true,
                            isPlaceholder = false,
                            accessCode = "1234",
                            isActive = true
                        ),
                        ParticipantDto(
                            id = "11",
                            nickname = "Beata",
                            totalExpenses = MoneyValueDto(
                                valueMainCurrency = 800f,
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("USD", 200f)
                                )
                            ),
                            isOwner = false,
                            isPlaceholder = false,
                            accessCode = "1234",
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
                            accessCode = "1234",
                            isActive = true
                        )
                    ),
                    settlement = SettlementDto(
                        balance = 0f,
                        balanceStatus = BalanceStatus.PLUS,
                        relations = emptyList()
                    )
                ),

                // ===================== TRIP 2: WYMIESZANE WALUTY =====================
                TripDto(
                    id = "2",
                    title = "Wypad do Rzymu",
                    dateStart = 1713139200000,
                    dateEnd = 1713398400000,
                    description = "Zwiedzanie Włoch",
                    currency = "PLN", // Waluta główna wycieczki
                    totalExpenses = 2250f,
                    accessCode = "5678",
                    ownerId = "10",
                    imOwner = true,
                    myCost = MoneyValueDto(
                        valueMainCurrency = 1125f, // W PLN (trip currency)
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
                        // Wydatek 1: Koszt w EUR, wycieczka w PLN (różne waluty!)
                        ExpenseDto(
                            id = "21",
                            name = "Hotel",
                            description = "3 noce w centrum Rzymu",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 900f, // W PLN (trip currency)
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("EUR", 200f),
                                    MoneyValueDetailsDto("USD", 225f)
                                )
                            ),
                            amount = 600f, // Faktyczny koszt
                            currency = "EUR", // Waluta kosztowa (inna niż trip!)
                            date = 1713225600000,
                            categoryId = "1",
                            payerId = "10",
                            payerNickname = "Adam",
                            sharedWith = listOf(
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 200f, // cost currency = EUR
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 900f) // trip currency = PLN
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 200f, // EUR
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 900f) // PLN
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 200f, // EUR
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 900f) // PLN
                                        )
                                    )
                                )
                            )
                        ),
                        // Wydatek 2: Koszt w PLN, wycieczka też w PLN (ta sama waluta!)
                        ExpenseDto(
                            id = "22",
                            name = "Kolacja",
                            description = "Restauracja przy Colosseum",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 900f, // W PLN
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("EUR", 200f),
                                    MoneyValueDetailsDto("USD", 225f)
                                )
                            ),
                            amount = 900f,
                            currency = "PLN", // Ta sama co trip!
                            date = 1713312000000,
                            categoryId = "2",
                            payerId = "11",
                            payerNickname = "Beata",
                            sharedWith = listOf(
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 300f, // cost currency = PLN
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 300f) // trip = PLN (ta sama!)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 300f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 300f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 300f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 300f)
                                        )
                                    )
                                )
                            )
                        ),
                        // Wydatek 3: Koszt w USD, wycieczka w PLN (różne waluty!)
                        ExpenseDto(
                            id = "23",
                            name = "Muzeum Watykańskie",
                            description = "Bilety wstępu",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 450f, // W PLN (trip currency)
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("EUR", 100f),
                                    MoneyValueDetailsDto("USD", 112f)
                                )
                            ),
                            amount = 100f, // Faktyczny koszt
                            currency = "USD", // Waluta kosztowa (inna niż trip!)
                            date = 1713398400000,
                            categoryId = "3",
                            payerId = "12",
                            payerNickname = "Cezary",
                            sharedWith = listOf(
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 33.33f, // cost currency = USD
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 150f) // trip currency = PLN
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 33.33f, // USD
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 150f) // PLN
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 33.34f, // USD
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("PLN", 150f) // PLN
                                        )
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
                            accessCode = "5678",
                            isActive = true
                        ),
                        ParticipantDto(
                            id = "11",
                            nickname = "Beata",
                            totalExpenses = MoneyValueDto(
                                valueMainCurrency = 750f,
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("EUR", 167f)
                                )
                            ),
                            isOwner = false,
                            isPlaceholder = false,
                            accessCode = "5678",
                            isActive = true
                        ),
                        ParticipantDto(
                            id = "12",
                            nickname = "Cezary",
                            totalExpenses = MoneyValueDto(
                                valueMainCurrency = 750f,
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("USD", 188f)
                                )
                            ),
                            isOwner = false,
                            isPlaceholder = false,
                            accessCode = "5678",
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
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("EUR", 12f)
                                    )
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
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("USD", 13f)
                                    )
                                ),
                                isSettled = false
                            )
                        )
                    )
                ),

                // ===================== TRIP 3: KOMPLEKSOWE DANE Z RÓŻNYMI SCENARIUSZAMI =====================
                TripDto(
                    id = "3",
                    title = "Roadtrip po Europie",
                    dateStart = 1714521600000,
                    dateEnd = 1715731200000,
                    description = "Niemcy -> Francja -> Hiszpania",
                    currency = "EUR", // Waluta główna wycieczki
                    totalExpenses = 8500f,
                    accessCode = "9999",
                    ownerId = "10",
                    imOwner = true,
                    myCost = MoneyValueDto(
                        valueMainCurrency = 2125f, // W EUR (trip currency)
                        valueOtherCurrencies = listOf(
                            MoneyValueDetailsDto("PLN", 9500f),
                            MoneyValueDetailsDto("USD", 2350f),
                            MoneyValueDetailsDto("GBP", 1800f)
                        )
                    ),
                    categories = listOf(
                        CategoryDto("1", 3000f), // Zakwaterowanie
                        CategoryDto("2", 2500f), // Jedzenie
                        CategoryDto("5", 1800f), // Transport
                        CategoryDto("3", 1200f)  // Rozrywka
                    ),
                    expenses = listOf(
                        // Wydatek 1: Hotel w Berlinie - EUR (ta sama waluta)
                        ExpenseDto(
                            id = "31",
                            name = "Hotel Berlin",
                            description = "5 nocy w centrum",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 1500f, // EUR (trip currency)
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("PLN", 6700f),
                                    MoneyValueDetailsDto("USD", 1650f),
                                    MoneyValueDetailsDto("GBP", 1270f)
                                )
                            ),
                            amount = 1500f,
                            currency = "EUR", // Ta sama co trip
                            date = 1714608000000,
                            categoryId = "1",
                            payerId = "10",
                            payerNickname = "Adam",
                            sharedWith = listOf(
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 375f, // EUR
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 375f) // Ta sama waluta!
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 375f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 375f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 375f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 375f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "13",
                                    participantNickname = "Diana",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 375f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 375f)
                                        )
                                    )
                                )
                            )
                        ),
                        // Wydatek 2: Wynajem auta - GBP (inna waluta!)
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
                            amount = 1400f, // Faktyczny koszt
                            currency = "GBP", // Inna waluta!
                            date = 1714694400000,
                            categoryId = "5",
                            payerId = "11",
                            payerNickname = "Beata",
                            sharedWith = listOf(
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 350f, // GBP (cost currency)
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 450f) // EUR (trip currency)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 350f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 450f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 350f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 450f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "13",
                                    participantNickname = "Diana",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 350f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 450f)
                                        )
                                    )
                                )
                            )
                        ),
                        // Wydatek 3: Restauracja w Paryżu - EUR (ta sama)
                        ExpenseDto(
                            id = "33",
                            name = "Le Bernardin",
                            description = "Obiad na Montmartre",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 800f, // EUR
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("PLN", 3600f),
                                    MoneyValueDetailsDto("USD", 880f),
                                    MoneyValueDetailsDto("GBP", 680f)
                                )
                            ),
                            amount = 800f,
                            currency = "EUR", // Ta sama
                            date = 1715126400000,
                            categoryId = "2",
                            payerId = "12",
                            payerNickname = "Cezary",
                            sharedWith = listOf(
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 200f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 200f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 200f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 200f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 200f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 200f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "13",
                                    participantNickname = "Diana",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 200f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 200f)
                                        )
                                    )
                                )
                            )
                        ),
                        // Wydatek 4: Bilety do Luwru - USD (inna waluta!)
                        ExpenseDto(
                            id = "34",
                            name = "Muzeum Luwr",
                            description = "Bilety + audioguide",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 600f, // EUR
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("PLN", 2700f),
                                    MoneyValueDetailsDto("USD", 660f),
                                    MoneyValueDetailsDto("GBP", 510f)
                                )
                            ),
                            amount = 600f, // Faktyczny koszt
                            currency = "USD", // Inna waluta!
                            date = 1715212800000,
                            categoryId = "3",
                            payerId = "13",
                            payerNickname = "Diana",
                            sharedWith = listOf(
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 150f, // USD
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 150f) // EUR
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 150f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 150f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 150f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 150f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "13",
                                    participantNickname = "Diana",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 150f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 150f)
                                        )
                                    )
                                )
                            )
                        ),
                        // Wydatek 5: Tapa w Barcelonie - EUR (ta sama)
                        ExpenseDto(
                            id = "35",
                            name = "Bar de Tapas",
                            description = "Kolacja z widokiem na Sagrada Familia",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 700f, // EUR
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
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 175f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 175f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 175f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 175f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 175f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 175f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "13",
                                    participantNickname = "Diana",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 175f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 175f)
                                        )
                                    )
                                )
                            )
                        ),
                        // Wydatek 6: Park Güell - PLN (inna waluta!)
                        ExpenseDto(
                            id = "36",
                            name = "Park Güell",
                            description = "Bilety wstępu i przewodnik",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 600f, // EUR
                                valueOtherCurrencies = listOf(
                                    MoneyValueDetailsDto("PLN", 2700f),
                                    MoneyValueDetailsDto("USD", 660f),
                                    MoneyValueDetailsDto("GBP", 510f)
                                )
                            ),
                            amount = 2400f, // Faktyczny koszt
                            currency = "PLN", // Inna waluta!
                            date = 1715644800000,
                            categoryId = "3",
                            payerId = "11",
                            payerNickname = "Beata",
                            sharedWith = listOf(
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 600f, // PLN
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 150f) // EUR
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 600f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 150f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 600f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 150f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "13",
                                    participantNickname = "Diana",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 600f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 150f)
                                        )
                                    )
                                )
                            )
                        ),
                        // Wydatek 7: Hotel w Paryżu - EUR (ta sama)
                        ExpenseDto(
                            id = "37",
                            name = "Hotel Paris",
                            description = "4 noce blisko Wieży Eiffla",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 1500f, // EUR
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
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 375f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 375f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "11",
                                    participantNickname = "Beata",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 375f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 375f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "12",
                                    participantNickname = "Cezary",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 375f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 375f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "13",
                                    participantNickname = "Diana",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 375f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 375f)
                                        )
                                    )
                                )
                            )
                        ),
                        // Wydatek 8: Jedzenie na stacjach benzynowych - mix (niektórzy nie uczestniczą!)
                        ExpenseDto(
                            id = "38",
                            name = "Zakupy spożywcze",
                            description = "Przekąski na drogę",
                            totalExpense = MoneyValueDto(
                                valueMainCurrency = 400f, // EUR
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
                                ShareDto(
                                    participantId = "10",
                                    participantNickname = "Adam",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 200f, // Adam zapłacił więcej
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 200f)
                                        )
                                    )
                                ),
                                ShareDto(
                                    participantId = "13",
                                    participantNickname = "Diana",
                                    splitValue = MoneyValueDto(
                                        valueMainCurrency = 200f,
                                        valueOtherCurrencies = listOf(
                                            MoneyValueDetailsDto("EUR", 200f)
                                        )
                                    )
                                )
                                // Beata i Cezary nie uczestniczą w tym wydatku!
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
                            accessCode = "9999",
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
                            accessCode = "9999",
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
                            accessCode = "9999",
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
                            accessCode = "9999",
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
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("USD", 220f)
                                    )
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
                                    valueOtherCurrencies = listOf(
                                        MoneyValueDetailsDto("GBP", 85f)
                                    )
                                ),
                                isSettled = true
                            )
                        )
                    )
                )
            )
        )
    }
}