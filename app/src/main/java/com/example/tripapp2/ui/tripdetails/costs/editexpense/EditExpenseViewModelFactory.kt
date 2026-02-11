package com.example.tripapp2.ui.tripdetails.costs.editexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EditExpenseViewModelFactory(
    private val tripId: String,
    private val expenseId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditExpenseViewModel(tripId, expenseId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}