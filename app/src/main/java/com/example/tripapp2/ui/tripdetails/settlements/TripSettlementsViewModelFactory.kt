package com.example.tripapp2.ui.tripdetails.settlements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TripSettlementsViewModelFactory(
    private val tripId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripSettlementsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripSettlementsViewModel(tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}