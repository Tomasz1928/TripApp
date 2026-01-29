package com.example.tripapp2.ui.dashboard.create

import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.KeyboardAwareFragment
import com.example.tripapp2.ui.common.base.NavigationCommand
import com.example.tripapp2.ui.common.extension.toDateRange
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CreateTripFragment : KeyboardAwareFragment<CreateTripViewModel>(R.layout.fragment_create_trip) {

    override val viewModel: CreateTripViewModel by viewModels()

    private lateinit var tripNameLayout: TextInputLayout
    private lateinit var tripNameInput: TextInputEditText
    private lateinit var tripCurrencyLayout: TextInputLayout
    private lateinit var tripCurrencyInput: AutoCompleteTextView
    private lateinit var tripDateLayout: TextInputLayout
    private lateinit var tripDateInput: TextInputEditText
    private lateinit var tripDescriptionInput: TextInputEditText
    private lateinit var createButton: Button

    companion object {
        private const val TAG_DATE_PICKER = "DATE_RANGE_PICKER"
    }

    override fun initKeyboardViews(view: View) {
        keyboardScrollView = view.findViewById(R.id.scrollViewCreateTrip)
        keyboardBottomNav = (activity as? DashboardActivity)?.dashboardBottomNav
    }

    override fun setupUI() {
        initializeViews()
        setupCurrencyDropdown()
        setupInputListeners()
    }

    override fun setupCustomObservers() {
        viewModel.nameError.observe(viewLifecycleOwner) { error ->
            tripNameLayout.error = error
        }

        viewModel.currencyError.observe(viewLifecycleOwner) { error ->
            tripCurrencyLayout.error = error
        }

        viewModel.dateError.observe(viewLifecycleOwner) { error ->
            tripDateLayout.error = error
        }

        viewModel.showDatePickerEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                showDateRangePicker()
            }
        }

        viewModel.dateRange.observe(viewLifecycleOwner) { dateRange ->
            dateRange?.let {
                tripDateInput.setText(it.toDateRange())
            }
        }

        viewModel.tripCreatedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                showMessage(message)
            }
        }
    }

    private fun initializeViews() {
        val view = requireView()

        tripNameLayout = view.findViewById(R.id.TripCreateLayout)
        tripNameInput = view.findViewById(R.id.TripNameInput)
        tripCurrencyLayout = view.findViewById(R.id.TripCurrencyLayout)
        tripCurrencyInput = view.findViewById(R.id.TripCurrencyInput)
        tripDateLayout = view.findViewById(R.id.TripDateLayout)
        tripDateInput = view.findViewById(R.id.TripDateInput)
        tripDescriptionInput = view.findViewById(R.id.TripDescriptionInput)
        createButton = view.findViewById(R.id.TripCreateBtn)
    }

    private fun setupCurrencyDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            viewModel.getCurrencies()
        )
        tripCurrencyInput.setAdapter(adapter)
        tripCurrencyInput.threshold = 1

        tripCurrencyInput.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as String
            viewModel.onCurrencySelected(selected)
        }
    }

    private fun setupInputListeners() {
        tripNameInput.addTextChangedListener { text ->
            viewModel.onTripNameChanged(text.toString())
        }

        tripDescriptionInput.addTextChangedListener { text ->
            viewModel.onDescriptionChanged(text.toString())
        }

        tripDateInput.setOnClickListener {
            viewModel.onDateFieldClicked()
        }

        createButton.setOnClickListener {
            viewModel.onCreateTripClicked()
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Wybierz zakres dat")
            .build()

        picker.show(parentFragmentManager, TAG_DATE_PICKER)

        picker.addOnPositiveButtonClickListener { selection ->
            viewModel.onDateRangeSelected(selection.first to selection.second)
        }
    }

    override fun handleNavigation(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.ToDashboard -> {
                (activity as? DashboardActivity)?.apply {
                    showDashboardFragment(R.id.menu_dashboard)
                    dashboardBottomNav.selectedItemId = R.id.menu_dashboard
                }
            }
            else -> super.handleNavigation(command)
        }
    }

    override fun onLoadingStateChanged(isLoading: Boolean) {
        createButton.isEnabled = !isLoading
        createButton.text = if (isLoading) "Tworzenie..." else "Utwórz"
    }
}