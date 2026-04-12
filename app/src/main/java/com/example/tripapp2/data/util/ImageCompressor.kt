package com.example.tripapp2.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * ImageCompressor — kompresuje zdjęcia przed uploadem jako base64.
 *
 * Strategia:
 * - Skalowanie do maxWidth (domyślnie 1200px) — dobra czytelność paragonu
 * - JPEG quality 75 — czytelny tekst na paragonie, ~80-150 kB
 * - Automatyczna rotacja na podstawie EXIF
 * - Wynik: base64 string gotowy do wysłania przez GraphQL
 *
 * Użycie:
 *   val base64 = ImageCompressor.compressToBase64(uri, context)
 *   if (base64 != null) { /* upload */ }
 */
object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val DEFAULT_MAX_WIDTH = 1200
    private const val DEFAULT_QUALITY = 75
    private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB

    /**
     * Kompresuje obraz z Uri do base64 JPEG string.
     *
     * @param uri Uri zdjęcia (z galerii lub aparatu)
     * @param context Context do otwierania ContentResolver
     * @param maxWidth Maksymalna szerokość w pikselach (default: 1200)
     * @param quality Jakość JPEG 0-100 (default: 75)
     * @return base64 string lub null jeśli błąd
     */
    fun compressToBase64(
        uri: Uri,
        context: Context,
        maxWidth: Int = DEFAULT_MAX_WIDTH,
        quality: Int = DEFAULT_QUALITY
    ): String? {
        return try {
            // 1. Dekoduj wymiary bez ładowania do pamięci
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e(TAG, "Invalid image dimensions: ${originalWidth}x${originalHeight}")
                return null
            }

            // 2. Oblicz sample size (przyspieszenie dekodowania dużych zdjęć)
            val sampleSize = calculateSampleSize(originalWidth, originalHeight, maxWidth)

            // 3. Dekoduj z sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: run {
                Log.e(TAG, "Failed to decode bitmap from Uri")
                return null
            }

            // 4. Zastosuj rotację EXIF
            val rotatedBitmap = applyExifRotation(context, uri, sampledBitmap)

            // 5. Skaluj do docelowej szerokości (po rotacji, bo wymiary mogły się zamienić)
            val scaledBitmap = scaleBitmap(rotatedBitmap, maxWidth)

            // 6. Kompresuj do JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            // 7. Zwolnij bitmapy
            if (scaledBitmap !== rotatedBitmap) scaledBitmap.recycle()
            if (rotatedBitmap !== sampledBitmap) rotatedBitmap.recycle()
            sampledBitmap.recycle()

            val bytes = outputStream.toByteArray()

            // 8. Sprawdź rozmiar
            if (bytes.size > MAX_FILE_SIZE_BYTES) {
                Log.w(TAG, "Compressed image still too large: ${bytes.size} bytes, retrying with lower quality")
                // Retry z niższą jakością
                return retryWithLowerQuality(uri, context, maxWidth)
            }

            Log.d(TAG, "Compressed: ${originalWidth}x${originalHeight} → " +
                    "${scaledBitmap.width}x${scaledBitmap.height}, " +
                    "quality=$quality, size: ${bytes.size / 1024} kB")

            // 9. Encode do base64
            Base64.encodeToString(bytes, Base64.NO_WRAP)

        } catch (e: Exception) {
            Log.e(TAG, "Compression failed", e)
            null
        }
    }

    /**
     * Retry z niższą jakością jeśli pierwszy pass był za duży.
     */
    private fun retryWithLowerQuality(uri: Uri, context: Context, maxWidth: Int): String? {
        Log.d(TAG, "Retrying compression with quality=50, maxWidth=1000")
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSizeFromUri(context, uri, 1000)
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            val rotated = applyExifRotation(context, uri, bitmap)
            val scaled = scaleBitmap(rotated, 1000)

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)

            if (scaled !== rotated) scaled.recycle()
            if (rotated !== bitmap) rotated.recycle()
            bitmap.recycle()

            val bytes = outputStream.toByteArray()
            Log.d(TAG, "Retry result: ${bytes.size / 1024} kB")

            if (bytes.size > MAX_FILE_SIZE_BYTES) {
                Log.e(TAG, "Still too large after retry: ${bytes.size} bytes")
                return null
            }

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Retry compression failed", e)
            null
        }
    }

    private fun calculateSampleSizeFromUri(context: Context, uri: Uri, targetWidth: Int): Int {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        return calculateSampleSize(opts.outWidth, opts.outHeight, targetWidth)
    }

    private fun calculateSampleSize(
        width: Int, height: Int, targetWidth: Int
    ): Int {
        var sampleSize = 1
        var w = width
        while (w / 2 >= targetWidth) {
            sampleSize *= 2
            w /= 2
        }
        return sampleSize
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap

        val ratio = maxWidth.toFloat() / bitmap.width
        val newWidth = maxWidth
        val newHeight = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun applyExifRotation(
        context: Context, uri: Uri, bitmap: Bitmap
    ): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotation == 0f) return bitmap

            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.w(TAG, "EXIF rotation failed, using original", e)
            bitmap
        }
    }
}