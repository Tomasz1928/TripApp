package com.example.tripapp2.ui.tripdetails.settlements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory dla TripSettlementsViewModel
 */
class TripSettlementsViewModelFactory(
    private val tripId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripSettlementsViewModel::class.java)) {
            return TripSettlementsViewModel(tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


