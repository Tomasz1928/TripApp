package com.example.tripapp2.ui.addexpense

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
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

class AddExpenseFragment : KeyboardAwareFragment<AddExpenseViewModel>(R.layout.fragment_add_expense) {

    override val viewModel: AddExpenseViewModel by viewModels {
        AddExpenseViewModelFactory(getTripId())
    }

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
    private lateinit var createButton: Button
    private lateinit var titleCounter: TextView
    private lateinit var descriptionCounter: TextView

    override fun initKeyboardViews(view: View) {
        keyboardScrollView = view.findViewById(R.id.scrollViewAddExpense)
        keyboardBottomNav = (activity as? DashboardActivity)?.tripBottomNav
    }

    override fun setupUI() {
        initializeViews()
        setupCurrencyDropdown()
        setupInputListeners()
        setDefaultCurrency()
    }

    override fun setupCustomObservers() {
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

        // Waluta
        viewModel.currency.observe(viewLifecycleOwner) { currency ->
            // Walidacja waluty
            if (currency.isNullOrBlank()) {
                currencyLayout.error = null
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

        viewModel.expenseAddedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                val displayMessage = if (message.startsWith("EXPENSE_ADDED_SUCCESS_RES_ID:")) {
                    val resId = message.substringAfter(":").toIntOrNull()
                    resId?.let { getString(it) } ?: message
                } else {
                    message
                }

                showMessage(displayMessage)
                // Nawigacja powrotna po dodaniu wydatku
                (activity as? DashboardActivity)?.apply {
                    tripBottomNav.selectedItemId = R.id.menu_costs
                }
            }
        }
    }

    private fun initializeViews() {
        val view = requireView()

        titleLayout = view.findViewById(R.id.titleLayout)
        titleInput = view.findViewById(R.id.titleInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        categoryCard = view.findViewById(R.id.categoryCard)
        categoryInput = view.findViewById(R.id.categoryInput)
        categoryError = view.findViewById(R.id.categoryError)
        categoryIcon = view.findViewById(R.id.categoryIcon)
        amountLayout = view.findViewById(R.id.amountLayout)
        amountInput = view.findViewById(R.id.amountInput)
        currencyLayout = view.findViewById(R.id.currencyLayout)
        currencyInput = view.findViewById(R.id.currencyInput)
        dateLayout = view.findViewById(R.id.dateLayout)
        dateInput = view.findViewById(R.id.dateInput)
        timeLayout = view.findViewById(R.id.timeLayout)
        timeInput = view.findViewById(R.id.timeInput)
        payerButton = view.findViewById(R.id.payerButton)
        payerError = view.findViewById(R.id.payerError)
        splitButton = view.findViewById(R.id.splitButton)
        splitError = view.findViewById(R.id.splitError)
        createButton = view.findViewById(R.id.createButton)
        titleCounter = view.findViewById(R.id.titleCounter)
        descriptionCounter = view.findViewById(R.id.descriptionCounter)
    }

    private fun setupCurrencyDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            viewModel.getCurrencies()
        )
        currencyInput.setAdapter(adapter)
        currencyInput.threshold = 1

        currencyInput.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as String
            viewModel.onCurrencySelected(selected)
        }
    }

    private fun setDefaultCurrency() {
        // Ustaw domyślną walutę PLN
        currencyInput.setText("PLN", false)
        viewModel.onCurrencySelected("PLN")
    }

    private fun setupInputListeners() {
        titleInput.addTextChangedListener { text ->
            viewModel.onTitleChanged(text.toString())
            titleCounter.text = "${text?.length ?: 0}/40"
        }

        descriptionInput.addTextChangedListener { text ->
            viewModel.onDescriptionChanged(text.toString())
            descriptionCounter.text = "${text?.length ?: 0}/200"
        }

        amountInput.addTextChangedListener { text ->
            viewModel.onAmountChanged(text.toString())
        }

        // Kliknięcie w całą kartę kategorii
        categoryCard.setOnClickListener {
            viewModel.onCategoryFieldClicked()
        }

        dateInput.setOnClickListener {
            viewModel.onDateFieldClicked()
        }

        timeInput.setOnClickListener {
            viewModel.onTimeFieldClicked()
        }

        payerButton.setOnClickListener {
            showPayerDialog()
        }

        splitButton.setOnClickListener {
            viewModel.onSplitFieldClicked()
        }

        createButton.setOnClickListener {
            // Schowaj klawiaturę
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)

            viewModel.onAddExpenseClicked()
        }
    }

    private fun showCategoryPicker() {
        val dialog = CategoryPickerDialog(requireContext()) { category ->
            viewModel.onCategorySelected(category)
        }
        dialog.show()
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.add_expense_date_hint))
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.show(parentFragmentManager, "DATE_PICKER")

        picker.addOnPositiveButtonClickListener { selection ->
            viewModel.onDateSelected(selection)
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText(getString(R.string.add_expense_time_hint))
            .build()

        picker.show(parentFragmentManager, "TIME_PICKER")

        picker.addOnPositiveButtonClickListener {
            viewModel.onTimeSelected(picker.hour, picker.minute)
        }
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
        return arguments?.getString(ARG_TRIP_ID) ?: "trip_2"
    }

    override fun onLoadingStateChanged(isLoading: Boolean) {
        createButton.isEnabled = !isLoading
        createButton.text = if (isLoading) getString(R.string.add_expense_button_loading)
        else getString(R.string.add_expense_button)
    }

    companion object {
        private const val ARG_TRIP_ID = "trip_id"

        fun newInstance(tripId: String) = AddExpenseFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TRIP_ID, tripId)
            }
        }
    }
}