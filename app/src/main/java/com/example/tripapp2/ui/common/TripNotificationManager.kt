package com.example.tripapp2.ui.common

import android.app.Activity
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.tripapp2.R
import com.example.tripapp2.data.model.TripNotificationDto
import com.example.tripapp2.data.repository.TripRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TripNotificationManager(
    private val activity: Activity,
    private val rootView: View,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val repository: TripRepository,
) {
    companion object {
        private const val TAG = "TripNotificationManager"
        private const val ANIM_DURATION = 250L
    }

    private var notificationView: MaterialCardView? = null
    private var notificationText: TextView? = null
    private var btnRefresh: ImageButton? = null
    private var btnDismiss: ImageButton? = null

    private var refreshJob: Job? = null
    private var collectionJob: Job? = null

    fun start() {
        ensureViewAttached()
        collectionJob = lifecycleScope.launch {
            repository.notificationFlow.collect { notification ->
                Log.d(TAG, "Showing notification: ${notification.eventType} by ${notification.actorNickname}")
                showNotification(notification)
            }
        }
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
        dismiss()
    }

    private fun ensureViewAttached() {
        if (notificationView != null) return

        val parent = rootView as? ViewGroup ?: return

        val view = LayoutInflater.from(activity)
            .inflate(R.layout.item_trip_notification, parent, false)

        val params = view.layoutParams as? ViewGroup.MarginLayoutParams
        params?.topMargin = getStatusBarHeight() + dpToPx(8)

        if (params is FrameLayout.LayoutParams) {
            params.gravity = android.view.Gravity.TOP
        } else if (params is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
            params.gravity = android.view.Gravity.TOP
        }

        parent.addView(view)

        notificationView = view.findViewById(R.id.notificationCard) ?: view as MaterialCardView
        notificationText = view.findViewById(R.id.notificationText)
        btnRefresh = view.findViewById(R.id.notificationRefresh)
        btnDismiss = view.findViewById(R.id.notificationDismiss)

        btnDismiss?.setOnClickListener { dismiss() }
    }

    private fun showNotification(notification: TripNotificationDto) {
        refreshJob?.cancel()

        val card = notificationView ?: return
        val text = notificationText ?: return

        text.text = formatMessage(notification)

        // Reset stanu przycisków
        btnRefresh?.visibility = View.VISIBLE
        btnRefresh?.isEnabled = true
        btnRefresh?.setOnClickListener { onRefreshClicked(notification) }
        btnDismiss?.visibility = View.VISIBLE

        if (card.visibility != View.VISIBLE) {
            card.visibility = View.VISIBLE
            card.translationY = -dpToPx(80).toFloat()
            card.alpha = 0f
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(card, "translationY", 0f),
                    ObjectAnimator.ofFloat(card, "alpha", 1f)
                )
                duration = ANIM_DURATION
                start()
            }
        }
    }

    private fun dismiss() {
        val card = notificationView ?: return
        if (card.visibility != View.VISIBLE) return

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(card, "translationY", -dpToPx(80).toFloat()),
                ObjectAnimator.ofFloat(card, "alpha", 0f)
            )
            duration = ANIM_DURATION
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    card.visibility = View.GONE
                }
            })
            start()
        }
    }

    private fun onRefreshClicked(notification: TripNotificationDto) {
        val text = notificationText ?: return

        text.text = activity.getString(R.string.notification_loading)
        btnRefresh?.isEnabled = false

        refreshJob = lifecycleScope.launch {
            try {
                val result = repository.refreshTripDetails(notification.tripId)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Trip ${notification.tripId} refreshed successfully")
                        dismiss()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Failed to refresh trip ${notification.tripId}", e)
                        showError()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception refreshing trip", e)
                showError()
            }
        }
    }

    private fun showError() {
        notificationText?.text = activity.getString(R.string.notification_refresh_error)
        btnRefresh?.visibility = View.GONE
        // X zostaje — user klika żeby zamknąć
    }

    private fun formatMessage(notification: TripNotificationDto): String {
        val actor = notification.actorNickname
        val trip = notification.tripName

        return when (notification.eventType) {
            "EXPENSE_ADDED" -> activity.getString(R.string.notification_expense_added, actor, trip)
            "EXPENSE_UPDATED" -> activity.getString(R.string.notification_expense_updated, actor, trip)
            "EXPENSE_DELETED" -> activity.getString(R.string.notification_expense_deleted, actor, trip)
            "PREPAYMENT_ADDED" -> activity.getString(R.string.notification_prepayment_added, actor, trip)
            "SETTLEMENT_CHANGED" -> activity.getString(R.string.notification_settlement_changed, actor, trip)
            "PARTICIPANT_ADDED" -> activity.getString(R.string.notification_participant_added, actor, trip)
            "PARTICIPANT_UPDATED" -> activity.getString(R.string.notification_participant_updated, actor, trip)
            "PARTICIPANT_REMOVED" -> activity.getString(R.string.notification_participant_removed, actor, trip)
            else -> activity.getString(R.string.notification_unknown, trip)
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) activity.resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * activity.resources.displayMetrics.density).toInt()
    }
}