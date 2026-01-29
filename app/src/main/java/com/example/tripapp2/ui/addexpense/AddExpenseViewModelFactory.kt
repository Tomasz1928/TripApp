package com.example.tripapp2.ui.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AddExpenseViewModelFactory(
    private val tripId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddExpenseViewModel(tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}