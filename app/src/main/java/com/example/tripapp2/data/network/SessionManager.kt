package com.example.tripapp2.data.network

import android.content.Context
import android.util.Log

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    fun saveSessionId(sessionId: String) {
        prefs.edit().putString("session_id", sessionId).apply()
        Log.d("SessionManager", "Session ID saved: $sessionId")
    }

    fun getSessionId(): String? = prefs.getString("session_id", null)

    fun clearSession() {
        prefs.edit().remove("session_id").apply()
    }

    fun isLoggedIn(): Boolean = getSessionId() != null
}