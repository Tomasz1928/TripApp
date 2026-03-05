package com.example.tripapp2.data.cache

import android.content.Context
import android.util.Log
import com.example.tripapp2.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * TripCacheManager — persystentny cache tripów w SharedPreferences.
 *
 * Analogicznie do SessionManager zapisuje dane na dysku telefonu,
 * dzięki czemu tripy przetrwają zamknięcie aplikacji.
 *
 * Strategia:
 * - Tripy serializowane do JSON przez Gson
 * - Każdy trip jako osobny klucz: "trip_{id}"
 * - Lista ID tripów w osobnym kluczu: "trip_ids"
 * - UserInfo w kluczu: "cached_user_info"
 *
 * Przy starcie aplikacji dane wczytują się natychmiast z pamięci,
 * a potem są nadpisywane świeżymi danymi z API.
 */
class TripCacheManager private constructor(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "trip_cache_prefs"
        private const val KEY_TRIP_IDS = "trip_ids"
        private const val KEY_TRIP_PREFIX = "trip_"
        private const val KEY_USER_INFO = "cached_user_info"
        private const val TAG = "TripCacheManager"


        @Volatile
        private var INSTANCE: TripCacheManager? = null

        fun getInstance(context: Context): TripCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TripCacheManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getInstance(): TripCacheManager {
            return INSTANCE ?: throw IllegalStateException(
                "TripCacheManager not initialized. Call getInstance(context) first."
            )
        }
    }

    // ==========================================
    // TRIP OPERATIONS
    // ==========================================

    /**
     * Zapisuje pojedynczy trip do pamięci telefonu.
     */
    fun saveTrip(trip: TripDto) {
        try {
            val json = gson.toJson(trip)
            prefs.edit()
                .putString("$KEY_TRIP_PREFIX${trip.id}", json)
                .apply()

            // Dodaj ID do listy jeśli jeszcze nie ma
            val ids = getTripIds().toMutableSet()
            ids.add(trip.id)
            saveTripIds(ids)

            Log.d(TAG, "Trip saved: ${trip.id} (${trip.title})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save trip ${trip.id}", e)
        }
    }

    /**
     * Zapisuje wiele tripów naraz (batch) — wydajniejsze niż pojedyncze zapisy.
     */
    fun saveAllTrips(trips: Map<String, TripDto>) {
        try {
            val editor = prefs.edit()

            trips.forEach { (id, trip) ->
                val json = gson.toJson(trip)
                editor.putString("$KEY_TRIP_PREFIX$id", json)
            }

            // Zapisz listę ID
            val idsJson = gson.toJson(trips.keys.toList())
            editor.putString(KEY_TRIP_IDS, idsJson)

            editor.apply()
            Log.d(TAG, "All trips saved: ${trips.size} trips")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save all trips", e)
        }
    }

    /**
     * Pobiera trip z pamięci telefonu.
     */
    fun getTrip(tripId: String): TripDto? {
        return try {
            val json = prefs.getString("$KEY_TRIP_PREFIX$tripId", null) ?: return null
            gson.fromJson(json, TripDto::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read trip $tripId", e)
            null
        }
    }

    /**
     * Pobiera wszystkie tripy z pamięci telefonu.
     */
    fun getAllTrips(): Map<String, TripDto> {
        val result = mutableMapOf<String, TripDto>()
        try {
            val ids = getTripIds()
            for (id in ids) {
                val trip = getTrip(id)
                if (trip != null) {
                    result[id] = trip
                }
            }
            Log.d(TAG, "Loaded ${result.size} trips from persistent cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load all trips", e)
        }
        return result
    }

    /**
     * Usuwa trip z pamięci.
     */
    fun removeTrip(tripId: String) {
        prefs.edit()
            .remove("$KEY_TRIP_PREFIX$tripId")
            .apply()

        val ids = getTripIds().toMutableSet()
        ids.remove(tripId)
        saveTripIds(ids)

        Log.d(TAG, "Trip removed: $tripId")
    }

    // ==========================================
    // USER INFO
    // ==========================================

    fun saveUserInfo(userInfo: UserInfoDto) {
        try {
            val json = gson.toJson(userInfo)
            prefs.edit().putString(KEY_USER_INFO, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user info", e)
        }
    }

    fun getUserInfo(): UserInfoDto? {
        return try {
            val json = prefs.getString(KEY_USER_INFO, null) ?: return null
            gson.fromJson(json, UserInfoDto::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read user info", e)
            null
        }
    }

    // ==========================================
    // CLEAR
    // ==========================================

    /**
     * Czyści cały cache (przy wylogowaniu).
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "All cache cleared")
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private fun getTripIds(): Set<String> {
        return try {
            val json = prefs.getString(KEY_TRIP_IDS, null) ?: return emptySet()
            val type = object : TypeToken<List<String>>() {}.type
            val list: List<String> = gson.fromJson(json, type)
            list.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read trip IDs", e)
            emptySet()
        }
    }

    private fun saveTripIds(ids: Set<String>) {
        val json = gson.toJson(ids.toList())
        prefs.edit().putString(KEY_TRIP_IDS, json).apply()
    }

    /**
     * Sprawdza czy cache zawiera jakiekolwiek dane.
     */
    fun hasCachedData(): Boolean {
        return getTripIds().isNotEmpty()
    }
}