package com.example.tripapp2.data.repository

import android.util.Log
import com.example.tripapp2.data.model.SuccessDto
import com.example.tripapp2.data.network.GraphQLDataSource

/**
 * ReceiptRepository — zarządza zdjęciami rachunków z hash-based cache.
 *
 * Cache: Map<expenseId, CachedReceipt(base64, hash)>
 *
 * Logika invalidacji:
 * - getReceipt(expenseId, expectedHash): jeśli hash w cache == expectedHash → zwróć z cache
 *   jeśli hash się różni lub brak w cache → fetch z API
 * - Po RECEIPT_CHANGED notyfikacji → następne otwarcie modala pobierze nowy receiptHash
 *   z tripDetails i porówna z cache
 */
class ReceiptRepository private constructor() {

    private val graphQL = GraphQLDataSource.getInstance()

    /**
     * Cache: expenseId → (base64, hash)
     */
    private val receiptCache = mutableMapOf<String, CachedReceipt>()

    companion object {
        private const val TAG = "ReceiptRepository"

        @Volatile
        private var INSTANCE: ReceiptRepository? = null

        fun getInstance(): ReceiptRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReceiptRepository().also { INSTANCE = it }
            }
        }
    }

    // ==========================================
    // QUERY
    // ==========================================

    /**
     * Pobierz zdjęcie rachunku — z cache (jeśli hash się zgadza) lub z API.
     *
     * @param expenseId ID wydatku
     * @param expectedHash hash z tripDetails query (może być null jeśli nie wiadomo)
     * @return base64 string lub null
     */
    suspend fun getReceipt(expenseId: String, expectedHash: String? = null): String? {
        // Sprawdź cache
        val cached = receiptCache[expenseId]
        if (cached != null) {
            if (expectedHash == null || cached.hash == expectedHash) {
                Log.d(TAG, "Receipt for expense $expenseId loaded from cache (hash match)")
                return cached.base64
            } else {
                Log.d(TAG, "Receipt hash mismatch for expense $expenseId: " +
                        "cached=${cached.hash}, expected=$expectedHash → refetching")
            }
        }

        // Fetch from API
        return try {
            val result = graphQL.getExpenseReceipt(expenseId.toInt())
            result.onSuccess { receiptData ->
                receiptData?.let {
                    receiptCache[expenseId] = CachedReceipt(
                        base64 = it.imageData,
                        hash = it.receiptHash ?: ""
                    )
                    Log.d(TAG, "Receipt for expense $expenseId fetched and cached (hash=${it.receiptHash})")
                }
            }
            result.getOrNull()?.imageData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch receipt for expense $expenseId", e)
            null
        }
    }

    /**
     * Invaliduj cache dla danego expense (np. po RECEIPT_CHANGED notyfikacji).
     * Nie kasuje od razu — przy następnym getReceipt hash się nie zgadza i refetchuje.
     */
    fun invalidateCache(expenseId: String) {
        receiptCache.remove(expenseId)
        Log.d(TAG, "Cache invalidated for expense $expenseId")
    }

    /**
     * Invaliduj cały cache (np. po RECEIPT_CHANGED gdy nie wiadomo który expense).
     */
    fun invalidateAll() {
        receiptCache.clear()
        Log.d(TAG, "All receipt cache invalidated")
    }

    // ==========================================
    // MUTATIONS
    // ==========================================

    suspend fun uploadReceipt(
        expenseId: String,
        imageBase64: String
    ): Result<SuccessDto> {
        return try {
            val result = graphQL.uploadReceipt(expenseId.toInt(), imageBase64)
            result.onSuccess { successDto ->
                if (successDto.success) {
                    // Zapisz do cache z nowym hashem (obliczonym lokalnie)
                    val hash = md5(imageBase64)
                    receiptCache[expenseId] = CachedReceipt(base64 = imageBase64, hash = hash)
                    Log.d(TAG, "Receipt uploaded and cached for expense $expenseId (hash=$hash)")
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload receipt for expense $expenseId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReceipt(expenseId: String): Result<SuccessDto> {
        return try {
            val result = graphQL.deleteReceipt(expenseId.toInt())
            result.onSuccess { successDto ->
                if (successDto.success) {
                    receiptCache.remove(expenseId)
                    Log.d(TAG, "Receipt deleted and removed from cache for expense $expenseId")
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete receipt for expense $expenseId", e)
            Result.failure(e)
        }
    }

    fun clearCache() {
        receiptCache.clear()
        Log.d(TAG, "Receipt cache cleared")
    }

    private fun md5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Cache entry: base64 + hash
 */
data class CachedReceipt(
    val base64: String,
    val hash: String
)

/**
 * DTO dla danych rachunku z API.
 */
data class ReceiptDto(
    val expenseId: String,
    val imageData: String,
    val receiptHash: String?,
    val uploadedByNickname: String?,
    val createdAt: Long
)
