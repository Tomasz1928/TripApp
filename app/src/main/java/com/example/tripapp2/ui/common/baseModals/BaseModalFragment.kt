package com.example.tripapp2.ui.common.baseModals

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R

/**
 * BaseModalFragment — bazowy komponent dla WSZYSTKICH modali w aplikacji.
 *
 * Funkcje:
 * - Spójny layout: Header (tytuł + opcjonalny subtitle + X) → Separator → Body → Footer
 * - Globalny padding @dimen/padding_default (16dp)
 * - Zamykanie: X button, kliknięcie w overlay, back button
 * - Animacja fade in / fade out
 * - Szerokość 90% ekranu
 * - Obsługa scrollowalnej listy z max ~5 widocznych elementów
 *
 * Użycie:
 * 1. Rozszerz tę klasę
 * 2. Override onCreateBodyView() — zwróć View z treścią modala
 * 3. Opcjonalnie override onCreateFooterView() — zwróć View z przyciskami
 * 4. Wywołaj setModalTitle() / setModalSubtitle() w onViewCreated()
 *
 * Prosty modal (np. confirm):
 *   BaseModalFragment.newConfirmInstance(title, message, onConfirm)
 */
open class BaseModalFragment : DialogFragment() {

    // Views
    protected var modalOverlay: FrameLayout? = null
    protected var modalCard: View? = null
    protected var modalTitle: TextView? = null
    protected var modalSubtitle: TextView? = null
    protected var modalCloseButton: ImageView? = null
    protected var modalBodyContainer: FrameLayout? = null
    protected var modalFooter: LinearLayout? = null
    protected var modalSeparator: View? = null

    // Config
    private var dismissOnOverlayClick: Boolean = true
    private var animationDurationMs: Long = 200L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_TripApp_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_base_modal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBaseViews(view)
        setupCloseHandlers()
        injectBody()
        injectFooter()
        playEnterAnimation()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            // Pozwól na overlay click przez MATCH_PARENT
            setDimAmount(0f) // Mamy własny overlay
        }
    }

    // ==========================================
    // INITIALIZATION
    // ==========================================

    private fun initBaseViews(view: View) {
        modalOverlay = view.findViewById(R.id.baseModalOverlay)
        modalCard = view.findViewById(R.id.baseModalCard)
        modalTitle = view.findViewById(R.id.baseModalTitle)
        modalSubtitle = view.findViewById(R.id.baseModalSubtitle)
        modalCloseButton = view.findViewById(R.id.baseModalCloseButton)
        modalBodyContainer = view.findViewById(R.id.baseModalBodyContainer)
        modalFooter = view.findViewById(R.id.baseModalFooter)
        modalSeparator = view.findViewById(R.id.baseModalSeparator)
    }

    private fun setupCloseHandlers() {
        // X button
        modalCloseButton?.setOnClickListener { dismissAnimated() }

        // Overlay click (outside card)
        modalOverlay?.setOnClickListener {
            if (dismissOnOverlayClick) {
                dismissAnimated()
            }
        }

        // Prevent card clicks from propagating to overlay
        modalCard?.setOnClickListener { /* consume */ }
    }

    private fun injectBody() {
        val bodyView = onCreateBodyView(layoutInflater, modalBodyContainer)
        if (bodyView != null) {
            // Usuń z poprzedniego parenta jeśli istnieje
            (bodyView.parent as? ViewGroup)?.removeView(bodyView)
            modalBodyContainer?.addView(bodyView)
        }
    }

    private fun injectFooter() {
        val footerView = onCreateFooterView(layoutInflater, modalFooter)
        if (footerView != null) {
            (footerView.parent as? ViewGroup)?.removeView(footerView)
            modalFooter?.removeAllViews()
            modalFooter?.addView(footerView)
            modalFooter?.visibility = View.VISIBLE
        }
    }

    // ==========================================
    // ANIMATION
    // ==========================================

    private fun playEnterAnimation() {
        modalOverlay?.alpha = 0f
        modalCard?.alpha = 0f
        modalCard?.scaleX = 0.95f
        modalCard?.scaleY = 0.95f

        val overlayFade = ObjectAnimator.ofFloat(modalOverlay, "alpha", 0f, 1f)
        val cardFade = ObjectAnimator.ofFloat(modalCard, "alpha", 0f, 1f)
        val cardScaleX = ObjectAnimator.ofFloat(modalCard, "scaleX", 0.95f, 1f)
        val cardScaleY = ObjectAnimator.ofFloat(modalCard, "scaleY", 0.95f, 1f)

        AnimatorSet().apply {
            playTogether(overlayFade, cardFade, cardScaleX, cardScaleY)
            duration = animationDurationMs
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    protected fun dismissAnimated() {
        val overlayFade = ObjectAnimator.ofFloat(modalOverlay, "alpha", 1f, 0f)
        val cardFade = ObjectAnimator.ofFloat(modalCard, "alpha", 1f, 0f)
        val cardScaleX = ObjectAnimator.ofFloat(modalCard, "scaleX", 1f, 0.95f)
        val cardScaleY = ObjectAnimator.ofFloat(modalCard, "scaleY", 1f, 0.95f)

        AnimatorSet().apply {
            playTogether(overlayFade, cardFade, cardScaleX, cardScaleY)
            duration = animationDurationMs
            interpolator = DecelerateInterpolator()
            start()

            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (isAdded) {
                        dismiss()
                    }
                }
            })
        }
    }

    // ==========================================
    // PUBLIC API — do override przez dzieci
    // ==========================================

    /**
     * Override w podklasie — zwróć View z treścią modala (body).
     * Będzie dodany do FrameLayout pod separatorem.
     */
    protected open fun onCreateBodyView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View? = null

    /**
     * Override w podklasie — zwróć View z przyciskami (footer).
     * Opcjonalny. Jeśli zwrócisz null, footer będzie ukryty.
     */
    protected open fun onCreateFooterView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View? = null

    // ==========================================
    // HELPERS — do wywołania przez dzieci
    // ==========================================

    protected fun setModalTitle(title: String) {
        modalTitle?.text = title
    }

    protected fun setModalTitle(titleResId: Int) {
        modalTitle?.setText(titleResId)
    }

    protected fun setModalSubtitle(subtitle: String) {
        modalSubtitle?.text = subtitle
        modalSubtitle?.visibility = if (subtitle.isNotEmpty()) View.VISIBLE else View.GONE
    }

    protected fun setModalSubtitle(subtitleResId: Int) {
        modalSubtitle?.setText(subtitleResId)
        modalSubtitle?.visibility = View.VISIBLE
    }

    protected fun hideSeparator() {
        modalSeparator?.visibility = View.GONE
    }

    protected fun setDismissOnOverlayClick(enabled: Boolean) {
        dismissOnOverlayClick = enabled
    }

    /**
     * Helper: ogranicza wysokość ScrollView / RecyclerView do ~5 elementów.
     * Wywołaj po dodaniu elementów do listy.
     *
     * @param listView ScrollView lub RecyclerView
     * @param itemHeightDp Wysokość jednego elementu w dp (domyślnie 56dp)
     * @param maxItems Ile elementów widocznych bez scrolla (domyślnie 5)
     */
    protected fun limitListHeight(listView: View, itemHeightDp: Int = 56, maxItems: Int = 5) {
        val density = resources.displayMetrics.density
        val maxHeightPx = (itemHeightDp * maxItems * density).toInt()
        listView.layoutParams = listView.layoutParams?.apply {
            height = maxHeightPx
        } ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeightPx)
    }

    /**
     * Helper: dynamicznie ogranicza wysokość na podstawie liczby elementów.
     * Jeśli mniej niż maxItems — wrap_content. Jeśli więcej — stała wysokość.
     */
    protected fun limitListHeightIfNeeded(
        listView: View,
        itemCount: Int,
        itemHeightDp: Int = 56,
        maxItems: Int = 5
    ) {
        if (itemCount > maxItems) {
            limitListHeight(listView, itemHeightDp, maxItems)
        } else {
            listView.layoutParams = listView.layoutParams?.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }
}