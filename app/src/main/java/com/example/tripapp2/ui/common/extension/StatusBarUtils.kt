package com.example.tripapp2.ui.common.extension

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Dodaje padding-top równy wysokości status bara.
 * Tło widoku rozciąga się pod status bar, ale treść jest poniżej.
 *
 * Używać na top barach (LinearLayout z id=topBar, topSection itp.)
 */
fun View.applyStatusBarInsets() {
    // Zachowaj oryginalny padding-top (z XML)
    val originalPaddingTop = paddingTop

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        v.setPadding(
            v.paddingLeft,
            originalPaddingTop + statusBarTop,
            v.paddingRight,
            v.paddingBottom
        )
        insets
    }
    // Wymusz zastosowanie insets
    ViewCompat.requestApplyInsets(this)
}