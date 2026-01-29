package com.example.tripapp2.ui.common.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.google.android.material.snackbar.Snackbar

abstract class BaseFragment<VM : BaseViewModel>(@LayoutRes layoutId: Int) : Fragment(layoutId) {

    protected abstract val viewModel: VM

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupUI()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            onLoadingStateChanged(isLoading)
        }

        viewModel.error.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorMessage ->
                onError(errorMessage)
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { command ->
                handleNavigation(command)
            }
        }

        setupCustomObservers()
    }

    protected abstract fun setupUI()

    protected open fun setupCustomObservers() {}

    protected open fun onLoadingStateChanged(isLoading: Boolean) {}

    protected open fun onError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    protected open fun handleNavigation(command: NavigationCommand) {}

    // ✅ METODA showError() - używana w handleDashboardState
    protected fun showError(message: String) {
        onError(message)
    }

    protected fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    protected fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}