package com.example.tripapp2.ui.tripdetails.costs.editexpense

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.tripdetails.costs.addexpense.CategoryPickerDialog
import com.example.tripapp2.ui.tripdetails.costs.addexpense.ExpenseSplit
import com.example.tripapp2.ui.tripdetails.costs.addexpense.SplitExpenseModalFragment
import com.example.tripapp2.ui.common.KeyboardAwareFragment
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment edycji wydatku
 *
 * Struktura identyczna jak AddExpenseFragment, różnice:
 * - Tytuł: "Edytuj wydatek"
 * - Przycisk: "Zaktualizuj"
 * - Pre-populacja pól z istniejącego wydatku
 * - Wywołuje updateExpense zamiast addExpense
 */
class EditExpenseFragment : KeyboardAwareFragment<EditExpenseViewModel>(R.layout.fragment_edit_expense) {

    override val viewModel: EditExpenseViewModel by viewModels {
        EditExpenseViewModelFactory(getTripId(), getExpenseId())
    }

    private lateinit var backButton: ImageView

    private lateinit var titleLayout: TextInputLayout
    private lateinit var titleInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var categoryCard: MaterialCardView
    private lateinit var categoryInput: TextView
    private lateinit var categoryError: TextView
    private lateinit var categoryIcon: ImageView
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountLayout: TextInputLayout
    private lateinit var currencyInput: AutoCompleteTextView
    private lateinit var currencyLayout: TextInputLayout
    private lateinit var dateInput: TextInputEditText
    private lateinit var dateLayout: TextInputLayout
    private lateinit var timeInput: TextInputEditText
    private lateinit var timeLayout: TextInputLayout
    private lateinit var payerButton: Button
    private lateinit var payerError: TextView
    private lateinit var splitButton: Button
    private lateinit var splitError: TextView
    private lateinit var updateButton: Button
    private lateinit var titleCounter: TextView
    private lateinit var descriptionCounter: TextView

    override fun initKeyboardViews(view: View) {
        keyboardScrollView = view.findViewById(R.id.scrollViewEditExpense)
        keyboardBottomNav = null
    }

    override fun setupUI() {
        initializeViews()
        setupCurrencyDropdown()
        setupInputListeners()
        setupBackButton()
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            navigateBackToCosts()
        }
    }

    private fun navigateBackToCosts() {
        (activity as? DashboardActivity)?.closeEditExpenseAndShowCosts(getTripId())
    }

    override fun setupCustomObservers() {
        // Flaga załadowania danych - wypełnij pola gdy dane są gotowe
        viewModel.dataLoaded.observe(viewLifecycleOwner) { loaded ->
            if (loaded) {
                populateFieldsFromViewModel()
            }
        }

        // Błędy walidacji
        viewModel.titleError.observe(viewLifecycleOwner) { errorResId ->
            titleLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.amountError.observe(viewLifecycleOwner) { errorResId ->
            amountLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.categoryError.observe(viewLifecycleOwner) { errorResId ->
            val errorText = errorResId?.let { getString(it) }
            categoryError.text = errorText
            categoryError.visibility = if (errorText != null) View.VISIBLE else View.GONE

            if (errorText != null) {
                categoryCard.strokeColor = resources.getColor(R.color.error, null)
            } else {
                categoryCard.strokeColor = resources.getColor(R.color.divider, null)
            }
        }

        viewModel.dateError.observe(viewLifecycleOwner) { errorResId ->
            dateLayout.error = errorResId?.let { getString(it) }
        }

        viewModel.payerError.observe(viewLifecycleOwner) { errorResId ->
            val errorText = errorResId?.let { getString(it) }
            payerError.text = errorText
            payerError.visibility = if (errorText != null) View.VISIBLE else View.GONE
        }

        viewModel.splitError.observe(viewLifecycleOwner) { errorResId ->
            val errorText = errorResId?.let { getString(it) }
            splitError.text = errorText
            splitError.visibility = if (errorText != null) View.VISIBLE else View.GONE
        }

        // Wybrana kategoria
        viewModel.selectedCategory.observe(viewLifecycleOwner) { category ->
            category?.let {
                categoryInput.text = getString(it.nameResId)
                categoryIcon.setImageResource(it.iconResId)
                categoryIcon.visibility = View.VISIBLE
            }
        }

        // Data i czas
        viewModel.dateTime.observe(viewLifecycleOwner) { dateTime ->
            dateTime?.let { (date, time) ->
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                dateInput.setText(dateFormat.format(Date(date)))

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                timeInput.setText(timeFormat.format(Date(time)))
            }
        }

        // Payer - aktualizacja tekstu przycisku
        viewModel.selectedPayer.observe(viewLifecycleOwner) { payerId ->
            if (payerId != null) {
                val participants = viewModel.participants.value ?: emptyList()
                val participant = participants.find { it.id == payerId }
                payerButton.text = participant?.name ?: getString(R.string.error_payer_required)
            } else {
                payerButton.text = getString(R.string.add_expense_payer_hint)
            }
        }

        // Podział
        viewModel.expenseSplit.observe(viewLifecycleOwner) { split ->
            val selectedCount = split.getSelectedParticipants().size
            if (selectedCount > 0) {
                splitButton.text = "$selectedCount os."
            } else {
                splitButton.text = getString(R.string.add_expense_split_hint)
            }
        }

        // Eventy
        viewModel.showCategoryPickerEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                showCategoryPicker()
            }
        }

        viewModel.showDatePickerEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                showDatePicker()
            }
        }

        viewModel.showTimePickerEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                showTimePicker()
            }
        }

        viewModel.showSplitModalEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { split ->
                showSplitModal(split)
            }
        }

        // Event aktualizacji wydatku
        viewModel.expenseUpdatedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                val displayMessage = if (message.startsWith("EXPENSE_UPDATED_SUCCESS_RES_ID:")) {
                    val resId = message.substringAfter(":").toIntOrNull()
                    resId?.let { getString(it) } ?: message
                } else {
                    message
                }

                Toast.makeText(requireContext(), displayMessage, Toast.LENGTH_SHORT).show()

                if (message.startsWith("EXPENSE_UPDATED_SUCCESS_RES_ID:")) {
                    navigateBackToCosts()
                }
            }
        }
    }

    /**
     * Wypełnia pola formularza danymi z ViewModel po załadowaniu
     */
    private fun populateFieldsFromViewModel() {
        viewModel.title.value?.let { titleInput.setText(it) }
        viewModel.description.value?.let { descriptionInput.setText(it) }
        viewModel.amount.value?.let { amountInput.setText(it) }
        viewModel.currency.value?.let { currencyInput.setText(it, false) }
    }

    private fun initializeViews() {
        val view = requireView()

        backButton = view.findViewById(R.id.backButton)

        titleLayout = view.findViewById(R.id.titleLayout)
        titleInput = view.findViewById(R.id.titleInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        categoryCard = view.findViewById(R.id.categoryCard)
        categoryInput = view.findViewById(R.id.categoryInput)
        categoryError = view.findViewById(R.id.categoryError)
        categoryIcon = view.findViewById(R.id.categoryIcon)
        amountInput = view.findViewById(R.id.amountInput)
        amountLayout = view.findViewById(R.id.amountLayout)
        currencyInput = view.findViewById(R.id.currencyInput)
        currencyLayout = view.findViewById(R.id.currencyLayout)
        dateInput = view.findViewById(R.id.dateInput)
        dateLayout = view.findViewById(R.id.dateLayout)
        timeInput = view.findViewById(R.id.timeInput)
        timeLayout = view.findViewById(R.id.timeLayout)
        payerButton = view.findViewById(R.id.payerButton)
        payerError = view.findViewById(R.id.payerError)
        splitButton = view.findViewById(R.id.splitButton)
        splitError = view.findViewById(R.id.splitError)
        updateButton = view.findViewById(R.id.updateButton)
        titleCounter = view.findViewById(R.id.titleCounter)
        descriptionCounter = view.findViewById(R.id.descriptionCounter)
    }

    private fun setupCurrencyDropdown() {
        val currencies = listOf("PLN", "EUR", "USD", "GBP", "CHF", "CZK", "NOK", "SEK", "DKK")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        currencyInput.setAdapter(adapter)

        currencyInput.setOnItemClickListener { _, _, position, _ ->
            viewModel.onCurrencySelected(currencies[position])
        }
    }

    private fun setupInputListeners() {
        // Tytuł
        titleInput.addTextChangedListener { text ->
            viewModel.onTitleChanged(text.toString())
            titleCounter.text = "${text?.length ?: 0}/40"
        }

        // Opis
        descriptionInput.addTextChangedListener { text ->
            viewModel.onDescriptionChanged(text.toString())
            descriptionCounter.text = "${text?.length ?: 0}/200"
        }

        // Kwota
        amountInput.addTextChangedListener { text ->
            viewModel.onAmountChanged(text.toString())
        }

        // Waluta
        currencyInput.addTextChangedListener { text ->
            viewModel.onCurrencySelected(text.toString())
        }

        // Kategoria
        categoryCard.setOnClickListener {
            viewModel.onCategoryFieldClicked()
        }

        // Data
        dateInput.setOnClickListener {
            viewModel.onDateFieldClicked()
        }
        dateInput.isFocusable = false

        // Czas
        timeInput.setOnClickListener {
            viewModel.onTimeFieldClicked()
        }
        timeInput.isFocusable = false

        // Płatnik
        payerButton.setOnClickListener {
            showPayerDialog()
        }

        // Podział
        splitButton.setOnClickListener {
            viewModel.onSplitFieldClicked()
        }

        // Przycisk aktualizacji
        updateButton.setOnClickListener {
            viewModel.onUpdateExpenseClicked()
        }
    }

    private fun showCategoryPicker() {
        CategoryPickerDialog(requireContext()) { category ->
            viewModel.onCategorySelected(category)
        }.show()
    }

    private fun showDatePicker() {
        val currentDate = viewModel.dateTime.value?.first ?: System.currentTimeMillis()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.add_expense_date_hint))
            .setSelection(currentDate)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            viewModel.onDateSelected(selection)
        }

        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        viewModel.dateTime.value?.second?.let {
            calendar.timeInMillis = it
        }

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText(getString(R.string.add_expense_time_hint))
            .build()

        picker.addOnPositiveButtonClickListener {
            viewModel.onTimeSelected(picker.hour, picker.minute)
        }

        picker.show(parentFragmentManager, "TIME_PICKER")
    }

    private fun showSplitModal(split: ExpenseSplit) {
        val amount = viewModel.amount.value?.toFloatOrNull() ?: 0f

        if (amount <= 0) {
            showMessage(getString(R.string.error_amount_required_before_split))
            return
        }

        val modal = SplitExpenseModalFragment.newInstance(split, amount) { updatedSplit ->
            viewModel.onExpenseSplitUpdated(updatedSplit)
        }
        modal.show(parentFragmentManager, "SPLIT_MODAL")
    }

    private fun showPayerDialog() {
        val participants = viewModel.participants.value ?: emptyList()
        if (participants.isEmpty()) {
            showMessage(getString(R.string.error_no_participants))
            return
        }

        val names = participants.map { it.name }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_expense_payer_hint))
            .setItems(names) { _, which ->
                val selected = participants[which]
                viewModel.onPayerSelected(selected.id)
            }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID) ?: ""
    }

    private fun getExpenseId(): String {
        return arguments?.getString(ARG_EXPENSE_ID) ?: ""
    }

    override fun onLoadingStateChanged(isLoading: Boolean) {
        updateButton.isEnabled = !isLoading
        updateButton.text = if (isLoading) getString(R.string.edit_expense_button_loading)
        else getString(R.string.edit_expense_button)
    }

    companion object {
        private const val ARG_TRIP_ID = "trip_id"
        private const val ARG_EXPENSE_ID = "expense_id"

        fun newInstance(tripId: String, expenseId: String) = EditExpenseFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TRIP_ID, tripId)
                putString(ARG_EXPENSE_ID, expenseId)
            }
        }
    }
}