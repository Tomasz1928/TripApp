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
        // ✅ ZMIANA: Błędy walidacji - konwertuj Int? na String?
        viewModel.nameError.observe(viewLifecycleOwner) { errorResId ->
            tripNameLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.currencyError.observe(viewLifecycleOwner) { errorResId ->
            tripCurrencyLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.dateError.observe(viewLifecycleOwner) { errorResId ->
            tripDateLayout.error = errorResId?.let { getString(it) }
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

        // ✅ ZMIANA: Parsuj message i konwertuj jeśli potrzeba
        viewModel.tripCreatedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                // Sprawdź czy message zawiera resource ID
                val displayMessage = if (message.startsWith("RES_ID:")) {
                    val resId = message.substringAfter(":").toIntOrNull()
                    resId?.let { getString(it) } ?: message
                } else {
                    message
                }
                showMessage(displayMessage)
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
            .setTitleText(getString(R.string.create_trip_date_hint))
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
        // ✅ ZMIANA: Użyj getString() zamiast .toString()
        createButton.text = if (isLoading) {
            getString(R.string.create_trip_button_loading)
        } else {
            getString(R.string.create_trip_button)
        }
    }
}