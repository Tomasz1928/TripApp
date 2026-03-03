package com.example.tripapp2.ui.common

import android.app.Activity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.tripapp2.R
import com.example.tripapp2.data.model.TripNotificationDto
import com.example.tripapp2.data.repository.TripRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Manages Snackbar notifications from trip subscriptions.
 *
 * Attached to DashboardActivity — survives fragment changes.
 * Shows notifications at the TOP of the screen.
 * Replaces previous notification if a new one arrives.
 */
class TripNotificationManager(
    private val activity: Activity,
    private val rootView: View,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val repository: TripRepository,
) {
    companion object {
        private const val TAG = "TripNotificationManager"
    }

    private var currentSnackbar: Snackbar? = null
    private var refreshJob: Job? = null
    private var collectionJob: Job? = null

    /**
     * Start collecting notifications from repository.
     */
    fun start() {
        collectionJob = lifecycleScope.launch {
            repository.notificationFlow.collect { notification ->
                Log.d(TAG, "Showing notification: ${notification.eventType} by ${notification.actorNickname}")
                showNotification(notification)
            }
        }
    }

    /**
     * Stop collecting notifications.
     */
    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
        currentSnackbar?.dismiss()
        currentSnackbar = null
    }

    private fun showNotification(notification: TripNotificationDto) {
        // Dismiss previous (zastępowanie)
        currentSnackbar?.dismiss()
        refreshJob?.cancel()

        val message = formatMessage(notification)

        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE)

        // Position at TOP of screen
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams
        if (params is CoordinatorLayout.LayoutParams) {
            params.gravity = Gravity.TOP
            params.topMargin = getStatusBarHeight() + dpToPx(8)
            snackbarView.layoutParams = params
        } else if (params is FrameLayout.LayoutParams) {
            params.gravity = Gravity.TOP
            params.topMargin = getStatusBarHeight() + dpToPx(8)
            snackbarView.layoutParams = params
        }

        // Style matching app theme
        snackbarView.setBackgroundResource(R.drawable.bg_snackbar_notification)
        snackbar.setTextColor(activity.getColor(R.color.text_primary))
        snackbar.setActionTextColor(activity.getColor(R.color.primary))

        // Marginy boczne
        val sideMargin = dpToPx(16)
        if (params is CoordinatorLayout.LayoutParams) {
            params.marginStart = sideMargin
            params.marginEnd = sideMargin
        } else if (params is FrameLayout.LayoutParams) {
            params.marginStart = sideMargin
            params.marginEnd = sideMargin
        }

        // "Odśwież" action
        snackbar.setAction(activity.getString(R.string.notification_action_refresh)) {
            onRefreshClicked(notification, snackbar)
        }

        snackbar.show()
        currentSnackbar = snackbar
    }

    private fun onRefreshClicked(notification: TripNotificationDto, snackbar: Snackbar) {
        // Zmień tekst na "Ładowanie..."
        snackbar.setText(activity.getString(R.string.notification_loading))
        // Ukryj przycisk akcji podczas ładowania
        snackbar.setAction("") {}

        refreshJob = lifecycleScope.launch {
            try {
                val result = repository.refreshTripDetails(notification.tripId)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Trip ${notification.tripId} refreshed successfully")
                        snackbar.dismiss()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Failed to refresh trip ${notification.tripId}", e)
                        snackbar.setText(activity.getString(R.string.notification_refresh_error))
                        snackbar.setAction("OK") { snackbar.dismiss() }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception refreshing trip", e)
                snackbar.setText(activity.getString(R.string.notification_refresh_error))
                snackbar.setAction("OK") { snackbar.dismiss() }
            }
        }
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