package com.example.tripapp2

import android.app.Application
import com.example.tripapp2.data.cache.TripCacheManager
import com.example.tripapp2.data.network.SessionManager
import com.example.tripapp2.data.repository.CurrencyRepository
import com.example.tripapp2.data.repository.TripRepository


/**
 * Application class — inicjalizuje singletony wymagające Context.
 *
 * KOLEJNOŚĆ INICJALIZACJI JEST WAŻNA:
 * 1. SessionManager — wymagany przez ApolloClientProvider (CookieInterceptor + WS payload)
 * 2. TripCacheManager — wymagany przez TripRepository (persystentny cache)
 * 3. TripRepository — wymaga SessionManager + TripCacheManager + tworzy GraphQLDataSource
 *
 * WAŻNE: Dodaj android:name=".TripApp" do AndroidManifest.xml:
 *
 * <application
 *     android:name=".TripApp"
 *     ...>
 */
class TripApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // === KOLEJNOŚĆ JEST WAŻNA — patrz komentarz klasy ===

        // 1. SessionManager — SharedPreferences z sessionId
        //    Wymagany przez: ApolloClientProvider (CookieInterceptor, WS connectionPayload)
        SessionManager.getInstance(this)

        // 2. TripCacheManager — SharedPreferences z cache tripów
        //    Wymagany przez: TripRepository (persystentny cache)
        TripCacheManager.getInstance(this)

        // 3. TripRepository — centralny punkt zarządzania danymi
        //    Tworzy wewnętrznie: GraphQLDataSource → ApolloClientProvider
        //    Wymaga: SessionManager (już zainicjalizowany), TripCacheManager (już zainicjalizowany)
        TripRepository.getInstance(this)

        // 4. CurrencyRepository — cache walut z backendu
        //    Singleton bez Context — inicjalizujemy żeby był gotowy
        CurrencyRepository.getInstance()
    }
}