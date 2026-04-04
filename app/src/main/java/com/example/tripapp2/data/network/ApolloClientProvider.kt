package com.example.tripapp2.data.network

import android.util.Log
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApolloClientProvider {

    // ZMIEŃ NA SWÓJ URL
    private const val BASE_URL =
//        "https://tripbe.onrender.com/graphql/"
      //  "http://10.0.2.2:8000/graphql/"
        "http://192.168.18.122:8000/graphql/"
    private const val WS_URL =
//        "wss://tripbe.onrender.com/graphql/"
//        "ws://10.0.2.2:8000/graphql/"
    "ws://192.168.18.122:8000/graphql/"

    private const val TAG = "ApolloClientProvider"

    @Volatile
    private var apolloClient: ApolloClient? = null
    private val sessionManager by lazy { SessionManager.getInstance() }

    fun getClient(): ApolloClient {
        return apolloClient ?: synchronized(this) {
            apolloClient ?: buildClient().also { apolloClient = it }
        }
    }

    private fun buildClient(): ApolloClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(CookieInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return ApolloClient.Builder()
            .serverUrl(BASE_URL)
            .okHttpClient(okHttpClient)
            // WebSocket transport dla subscriptions
            .webSocketServerUrl(WS_URL)
            .wsProtocol(
                GraphQLWsProtocol.Factory(
                    connectionPayload = {
                        // Dorzucamy cookie do WebSocket handshake
                        val sid = sessionManager.getSessionId()
                        if (sid != null) mapOf("cookie" to "sessionid=$sid")
                        else emptyMap()
                    }
                )
            )
            // NOWE: automatyczny reconnect WS po utracie połączenia
            // Backoff: 3s, 6s, 9s, ... do max 10 prób
            .webSocketReopenWhen { throwable, attempt ->
                Log.w(TAG, "WS connection lost (attempt $attempt)", throwable)
                if (attempt < 10) {
                    delay(attempt * 3000L)
                    true  // retry
                } else {
                    Log.e(TAG, "WS reconnect failed after $attempt attempts, giving up")
                    false // give up
                }
            }
            .build()
    }

    /**
     * Zamyka aktualny klient i buduje nowy z najnowszym sessionId.
     *
     * WAŻNE: Po wywołaniu tej metody wszystkie aktywne subskrypcje WS
     * na starym kliencie zostaną przerwane. Trzeba je ponownie uruchomić.
     *
     * Wywoływane po:
     * - Udanym logowaniu (nowy sessionId)
     * - Udanej rejestracji (nowy sessionId)
     */
    fun resetAndRebuild(): ApolloClient {
        synchronized(this) {
            apolloClient?.close()
            apolloClient = null
            Log.d(TAG, "Apollo client reset and rebuilt with new session")
            return buildClient().also { apolloClient = it }
        }
    }
}