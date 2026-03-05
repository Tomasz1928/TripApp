package com.example.tripapp2.data.network

import SessionManager
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class CookieInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        sessionManager.getSessionId()?.let { sessionId ->
            requestBuilder.addHeader("Cookie", "sessionid=$sessionId")
        }

        val response = chain.proceed(requestBuilder.build())

        response.headers("Set-Cookie").forEach { cookie ->
            if (cookie.contains("sessionid")) {
                val value = cookie.split(";")[0].split("=")[1]
                sessionManager.saveSessionId(value)
            }
        }

        return response
    }
}