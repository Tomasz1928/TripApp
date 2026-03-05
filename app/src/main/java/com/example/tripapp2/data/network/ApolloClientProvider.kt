package com.example.tripapp2.data.network

import SessionManager
import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApolloClientProvider {

    // ZMIEŃ NA SWÓJ URL
    private const val BASE_URL =
//        "https://tripbe.onrender.com/graphql/"
        "http://10.0.2.2:8000/graphql/"
    private const val WS_URL =
//        "wss://tripbe.onrender.com/graphql/"
    "ws://10.0.2.2:8000/graphql/"



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
            .build()
    }

    fun resetAndRebuild(): ApolloClient {
        synchronized(this) {
            apolloClient?.close()
            apolloClient = null
            return buildClient().also { apolloClient = it }
        }
    }
}