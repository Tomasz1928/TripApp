import android.content.Context
import android.util.Log

class SessionManager private constructor(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    fun saveSessionId(sessionId: String) {
        prefs.edit().putString("session_id", sessionId).apply()
        Log.d("SessionManager", "Session ID saved: $sessionId")
    }

    fun getSessionId(): String? = prefs.getString("session_id", null)

    fun clearSession() {
        prefs.edit().remove("session_id").apply()
    }

    fun isLoggedIn(): Boolean = getSessionId() != null

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getInstance(): SessionManager {
            return INSTANCE ?: throw IllegalStateException(
                "SessionManager not initialized. Call getInstance(context) first."
            )
        }
    }
}