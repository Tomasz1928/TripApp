package com.example.tripapp2.ui.receipt

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tripapp2.R

/**
 * ReceiptViewerActivity — fullscreen podgląd zdjęcia rachunku.
 *
 * Funkcje:
 * - Ciemne tło, zdjęcie na środku
 * - Płynny pinch-to-zoom (1x–5x) z ograniczeniami
 * - Pan/drag gdy powiększone (z bounds — nie zgubisz zdjęcia)
 * - Double-tap: toggle między 1x a 2.5x
 * - Przycisk ← zamyka
 *
 * Implementacja oparta na Matrix — pełna kontrola nad transformacjami,
 * bez zewnętrznych bibliotek.
 */
class ReceiptViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_BASE64 = "receipt_image_base64"
        const val EXTRA_EXPENSE_NAME = "receipt_expense_name"
        private const val TAG = "ReceiptViewer"
        private const val MIN_SCALE = 1.0f
        private const val MAX_SCALE = 5.0f
        private const val DOUBLE_TAP_SCALE = 2.5f
    }

    private lateinit var imageView: ImageView
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    // Matrix do transformacji
    private val imageMatrix = Matrix()
    private val savedMatrix = Matrix()

    // Stan dotyku
    private enum class TouchMode { NONE, DRAG, ZOOM }
    private var mode = TouchMode.NONE

    // Punkty dotyku
    private val startPoint = PointF()
    private val midPoint = PointF()
    private var oldDist = 1f

    // Wymiary obrazu i widoku
    private var imageWidth = 0f
    private var imageHeight = 0f
    private var viewWidth = 0f
    private var viewHeight = 0f

    // Bazowa matrix (fit-center na starcie)
    private val baseMatrix = Matrix()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_viewer)

        imageView = findViewById(R.id.receiptImage)
        val backButton = findViewById<ImageView>(R.id.backButton)
        val titleView = findViewById<TextView>(R.id.receiptTitle)

        backButton.setOnClickListener { finish() }

        val expenseName = intent.getStringExtra(EXTRA_EXPENSE_NAME) ?: getString(R.string.receipt_viewer_title)
        titleView.text = expenseName

        // Decode and show image
        val base64 = intent.getStringExtra(EXTRA_IMAGE_BASE64)
        if (base64 == null) {
            finish()
            return
        }

        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap == null) {
            finish()
            return
        }

        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.setImageBitmap(bitmap)

        imageWidth = bitmap.width.toFloat()
        imageHeight = bitmap.height.toFloat()

        // Czekamy na layout żeby znać wymiary widoku
        imageView.post {
            viewWidth = imageView.width.toFloat()
            viewHeight = imageView.height.toFloat()

            if (viewWidth > 0 && viewHeight > 0) {
                fitImageToView()
                setupGestures()
            }
        }
    }

    /**
     * Dopasuj obraz do widoku (fit-center) jako bazowy stan.
     */
    private fun fitImageToView() {
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = minOf(scaleX, scaleY)

        val dx = (viewWidth - imageWidth * scale) / 2f
        val dy = (viewHeight - imageHeight * scale) / 2f

        baseMatrix.setScale(scale, scale)
        baseMatrix.postTranslate(dx, dy)

        imageMatrix.set(baseMatrix)
        imageView.imageMatrix = imageMatrix
    }

    /**
     * Aktualny poziom zoom (względem base scale).
     */
    private fun getCurrentScale(): Float {
        val values = FloatArray(9)
        imageMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun getBaseScale(): Float {
        val values = FloatArray(9)
        baseMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun getRelativeScale(): Float {
        val baseScale = getBaseScale()
        return if (baseScale > 0) getCurrentScale() / baseScale else 1f
    }

    // ==========================================
    // GESTURE SETUP
    // ==========================================

    private fun setupGestures() {
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val currentRelativeScale = getRelativeScale()
                val newRelativeScale = currentRelativeScale * scaleFactor

                // Ograniczenie zoom
                val clampedFactor = when {
                    newRelativeScale < MIN_SCALE -> MIN_SCALE / currentRelativeScale
                    newRelativeScale > MAX_SCALE -> MAX_SCALE / currentRelativeScale
                    else -> scaleFactor
                }

                imageMatrix.postScale(clampedFactor, clampedFactor, detector.focusX, detector.focusY)
                constrainTranslation()
                imageView.imageMatrix = imageMatrix
                return true
            }
        })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val relativeScale = getRelativeScale()

                if (relativeScale > 1.5f) {
                    // Zoom out → reset do fit
                    animateToMatrix(baseMatrix)
                } else {
                    // Zoom in → 2.5x na punkcie kliknięcia
                    val targetMatrix = Matrix(baseMatrix)
                    targetMatrix.postScale(DOUBLE_TAP_SCALE, DOUBLE_TAP_SCALE, e.x, e.y)
                    animateToMatrix(targetMatrix)
                }
                return true
            }
        })

        imageView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(imageMatrix)
                    startPoint.set(event.x, event.y)
                    mode = TouchMode.DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount >= 2) {
                        savedMatrix.set(imageMatrix)
                        mode = TouchMode.ZOOM
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == TouchMode.DRAG && !scaleDetector.isInProgress) {
                        val dx = event.x - startPoint.x
                        val dy = event.y - startPoint.y

                        imageMatrix.set(savedMatrix)
                        imageMatrix.postTranslate(dx, dy)
                        constrainTranslation()
                        imageView.imageMatrix = imageMatrix
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = TouchMode.NONE

                    // Snap back jeśli zoom < 1x
                    if (getRelativeScale() < MIN_SCALE) {
                        animateToMatrix(baseMatrix)
                    }
                }
            }
            true
        }
    }

    /**
     * Ogranicza przesuwanie — obraz nie może być przesunięty
     * tak żeby widoczne było puste tło (czarne).
     *
     * Logika:
     * - Jeśli obraz jest mniejszy niż widok → centruj
     * - Jeśli obraz jest większy → nie pozwól na puste krawędzie
     */
    private fun constrainTranslation() {
        val rect = getImageRect()

        var dx = 0f
        var dy = 0f

        if (rect.width() <= viewWidth) {
            // Obraz mniejszy niż widok — centruj X
            dx = (viewWidth - rect.width()) / 2f - rect.left
        } else {
            // Obraz większy — nie pozwól na puste boki
            if (rect.left > 0) dx = -rect.left
            if (rect.right < viewWidth) dx = viewWidth - rect.right
        }

        if (rect.height() <= viewHeight) {
            // Obraz mniejszy niż widok — centruj Y
            dy = (viewHeight - rect.height()) / 2f - rect.top
        } else {
            // Obraz większy — nie pozwól na pusty góra/dół
            if (rect.top > 0) dy = -rect.top
            if (rect.bottom < viewHeight) dy = viewHeight - rect.bottom
        }

        if (dx != 0f || dy != 0f) {
            imageMatrix.postTranslate(dx, dy)
        }
    }

    /**
     * Prostokąt obrazu po aktualnej transformacji.
     */
    private fun getImageRect(): RectF {
        val rect = RectF(0f, 0f, imageWidth, imageHeight)
        imageMatrix.mapRect(rect)
        return rect
    }

    /**
     * Animacja przejścia do docelowej matrix (np. przy double-tap).
     */
    private fun animateToMatrix(targetMatrix: Matrix) {
        val startValues = FloatArray(9)
        val endValues = FloatArray(9)
        imageMatrix.getValues(startValues)
        targetMatrix.getValues(endValues)

        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                val interpolated = FloatArray(9)
                for (i in 0..8) {
                    interpolated[i] = startValues[i] + (endValues[i] - startValues[i]) * fraction
                }
                imageMatrix.setValues(interpolated)
                constrainTranslation()
                imageView.imageMatrix = imageMatrix
            }
        }
        animator.start()
    }
}