package com.example.tripapp2.ui.common.extension

import android.content.res.Resources
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Extension functions dla View'ów
 * Upraszczają operacje na UI
 */

/**
 * Konwersja dp na piksele
 */
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Float.dp: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

/**
 * Show/hide z animacją
 */
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Ustawia widoczność na podstawie warunku
 */
fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

/**
 * Usuwa tint z ikon BottomNavigationView
 * Pozwala używać kolorowych ikon
 */
fun BottomNavigationView.setupIconsInOriginalColor() {
    this.itemIconTintList = null
    this.itemTextColor = null
}