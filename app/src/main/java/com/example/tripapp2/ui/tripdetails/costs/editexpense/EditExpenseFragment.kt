package com.example.tripapp2.ui.tripdetails.costs.editexpense

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.tripdetails.costs.addexpense.CategoryPickerModalFragment
import com.example.tripapp2.ui.tripdetails.costs.addexpense.ExpenseSplit
import com.example.tripapp2.ui.tripdetails.costs.addexpense.SplitExpenseModalFragment
import com.example.tripapp2.ui.tripdetails.costs.addexpense.SplitParticipant
import com.example.tripapp2.ui.tripdetails.costs.addexpense.SplitType
import com.example.tripapp2.ui.common.KeyboardAwareFragment
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.example.tripapp2.data.repository.CurrencyRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment edycji wydatku
 *
 * Struktura identyczna jak AddExpenseFragment, różnice:
 * - Tytuł: "Edytuj wydatek"
 * - Przycisk: "Zaktualizuj" (w top barze)
 * - Pre-populacja pól z istniejącego wydatku
 * - Payer i Split row-y są NIEEDYTOWALNE (tylko wyświetlają dane)
 * - Wywołuje updateExpense zamiast addExpense
 */
class EditExpenseFragment : KeyboardAwareFragment<EditExpenseViewModel>(R.layout.fragment_edit_expense) {

    override val viewModel: EditExpenseViewModel by viewModels {
        EditExpenseViewModelFactory(getTripId(), getExpenseId())
    }

    // ================================
    // TOP BAR
    // ================================
    private lateinit var backButton: ImageView
    private lateinit var updateButton: Button

    // ================================
    // HERO AMOUNT
    // ================================
    private lateinit var heroAmountContainer: LinearLayout
    private lateinit var amountDisplay: TextView
    private lateinit var currencyDisplay: TextView
    private lateinit var amountHint: TextView
    private lateinit var amountInputContainer: LinearLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountLayout: TextInputLayout
    private lateinit var currencyInput: AutoCompleteTextView
    private lateinit var currencyLayout: TextInputLayout

    // ================================
    // SECTION ROWS (klikalne wiersze)
    // ================================
    private lateinit var rowTitle: LinearLayout
    private lateinit var rowCategory: LinearLayout
    private lateinit var rowDate: LinearLayout
    private lateinit var rowPayer: LinearLayout
    private lateinit var rowSplit: LinearLayout

    // ================================
    // DISPLAY VIEWS (widoczne teksty w wierszach)
    // ================================
    private lateinit var titleDisplay: TextView
    private lateinit var descriptionDisplay: TextView
    private lateinit var categoryInput: TextView
    private lateinit var categoryIcon: ImageView
    private lateinit var dateDisplay: TextView
    private lateinit var timeDisplay: TextView
    private lateinit var payerDisplay: TextView
    private lateinit var payerAvatar: TextView
    private lateinit var splitDisplay: TextView
    private lateinit var splitAvatarStack: LinearLayout

    // ================================
    // ERROR VIEWS
    // ================================
    private lateinit var categoryError: TextView
    private lateinit var dateError: TextView
    private lateinit var payerError: TextView
    private lateinit var splitError: TextView

    // ================================
    // HIDDEN VIEWS (kompatybilność z ViewModel)
    // ================================
    private lateinit var titleLayout: TextInputLayout
    private lateinit var titleInput: TextInputEditText
    private lateinit var titleCounter: TextView
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var descriptionCounter: TextView
    private lateinit var categoryCard: MaterialCardView
    private lateinit var dateInput: TextInputEditText
    private lateinit var dateLayout: TextInputLayout
    private lateinit var timeInput: TextInputEditText
    private lateinit var timeLayout: TextInputLayout
    private lateinit var payerButton: Button
    private lateinit var splitButton: Button

    override fun initKeyboardViews(view: View) {
        keyboardScrollView = view.findViewById(R.id.scrollViewEditExpense)
        keyboardBottomNav = null
    }

    override fun setupUI() {
        initializeViews()
        setupCurrencyDropdown()
        setupRowClickListeners()
        setupHeroAmountListeners()
        setupBackButton()
    }

    // ================================================================
    // INITIALIZE VIEWS
    // ================================================================

    private fun initializeViews() {
        val view = requireView()

        // Top bar
        backButton = view.findViewById(R.id.backButton)
        updateButton = view.findViewById(R.id.updateButton)

        // Hero amount
        heroAmountContainer = view.findViewById(R.id.heroAmountContainer)
        amountDisplay = view.findViewById(R.id.amountDisplay)
        currencyDisplay = view.findViewById(R.id.currencyDisplay)
        amountHint = view.findViewById(R.id.amountHint)
        amountInputContainer = view.findViewById(R.id.amountInputContainer)
        amountInput = view.findViewById(R.id.amountInput)
        amountLayout = view.findViewById(R.id.amountLayout)
        currencyInput = view.findViewById(R.id.currencyInput)
        currencyLayout = view.findViewById(R.id.currencyLayout)

        // Section rows
        rowTitle = view.findViewById(R.id.rowTitle)
        rowCategory = view.findViewById(R.id.rowCategory)
        rowDate = view.findViewById(R.id.rowDate)
        rowPayer = view.findViewById(R.id.rowPayer)
        rowSplit = view.findViewById(R.id.rowSplit)

        // Display views
        titleDisplay = view.findViewById(R.id.titleDisplay)
        descriptionDisplay = view.findViewById(R.id.descriptionDisplay)
        categoryInput = view.findViewById(R.id.categoryInput)
        categoryIcon = view.findViewById(R.id.categoryIcon)
        dateDisplay = view.findViewById(R.id.dateDisplay)
        timeDisplay = view.findViewById(R.id.timeDisplay)
        payerDisplay = view.findViewById(R.id.payerDisplay)
        payerAvatar = view.findViewById(R.id.payerAvatar)
        splitDisplay = view.findViewById(R.id.splitDisplay)
        splitAvatarStack = view.findViewById(R.id.splitAvatarStack)

        // Error views
        categoryError = view.findViewById(R.id.categoryError)
        dateError = view.findViewById(R.id.dateError)
        payerError = view.findViewById(R.id.payerError)
        splitError = view.findViewById(R.id.splitError)

        // Hidden views (kompatybilność z ViewModel)
        titleLayout = view.findViewById(R.id.titleLayout)
        titleInput = view.findViewById(R.id.titleInput)
        titleCounter = view.findViewById(R.id.titleCounter)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        descriptionCounter = view.findViewById(R.id.descriptionCounter)
        categoryCard = view.findViewById(R.id.categoryCard)
        dateInput = view.findViewById(R.id.dateInput)
        dateLayout = view.findViewById(R.id.dateLayout)
        timeInput = view.findViewById(R.id.timeInput)
        timeLayout = view.findViewById(R.id.timeLayout)
        payerButton = view.findViewById(R.id.payerButton)
        splitButton = view.findViewById(R.id.splitButton)
    }

    // ================================================================
    // BACK BUTTON
    // ================================================================

    private fun setupBackButton() {
        backButton.setOnClickListener {
            navigateBackToCosts()
        }
    }

    private fun navigateBackToCosts() {
        (activity as? DashboardActivity)?.closeEditExpenseAndShowCosts(getTripId())
    }

    // ================================================================
    // HERO AMOUNT — expand/collapse + live update
    // ================================================================

    private fun setupHeroAmountListeners() {
        // Kliknięcie hero → toggle expand inputów kwoty
        heroAmountContainer.setOnClickListener {
            toggleAmountInput()
        }

        // Live update display podczas wpisywania
        amountInput.addTextChangedListener { text ->
            val amountText = text?.toString() ?: ""
            viewModel.onAmountChanged(amountText)
            updateAmountDisplay(amountText)
        }

        // Przycisk zapisz
        updateButton.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            viewModel.onUpdateExpenseClicked()
        }
    }

    private fun toggleAmountInput() {
        val isExpanded = amountInputContainer.visibility == View.VISIBLE
        if (isExpanded) {
            // Collapse — schowaj inputy
            amountInputContainer.visibility = View.GONE
            hideKeyboard()
        } else {
            // Expand — pokaż inputy i ustaw focus
            amountInputContainer.visibility = View.VISIBLE
            amountInput.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(amountInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun updateAmountDisplay(amountText: String) {
        if (amountText.isNotEmpty()) {
            amountDisplay.text = amountText
            amountDisplay.visibility = View.VISIBLE
            amountHint.text = "Kliknij aby zmienić kwotę"
        } else {
            amountDisplay.text = "0.00"
            amountHint.text = "Kliknij aby wpisać kwotę"
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    // ================================================================
    // ROW CLICK LISTENERS
    // ================================================================

    private fun setupRowClickListeners() {
        // Row 1: Tytuł + Opis → BottomSheet z inputami
        rowTitle.setOnClickListener {
            showTitleInputBottomSheet()
        }

        // Row 2: Kategoria → CategoryPicker
        rowCategory.setOnClickListener {
            viewModel.onCategoryFieldClicked()
        }

        // Row 3: Data → DatePicker, potem TimePicker
        rowDate.setOnClickListener {
            viewModel.onDateFieldClicked()
        }

        // Row 4: Kto płacił → NIEEDYTOWALNY (nie podpinamy click listenera)
        // rowPayer jest clickable=false w XML

        // Row 5: Podział → edytowalny (kwota może się zmienić, więc podział też)
        rowSplit.isClickable = true
        rowSplit.isFocusable = true
        rowSplit.setOnClickListener {
            viewModel.onSplitFieldClicked()
        }
    }

    // ================================================================
    // TITLE/DESCRIPTION BOTTOM SHEET
    // ================================================================

    private fun showTitleInputBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.Theme_TripApp_BottomSheet)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_title_input, null)
        bottomSheet.setContentView(sheetView)

        val sheetTitleInput = sheetView.findViewById<TextInputEditText>(R.id.sheetTitleInput)
        val sheetTitleCounter = sheetView.findViewById<TextView>(R.id.sheetTitleCounter)
        val sheetDescriptionInput = sheetView.findViewById<TextInputEditText>(R.id.sheetDescriptionInput)
        val sheetDescriptionCounter = sheetView.findViewById<TextView>(R.id.sheetDescriptionCounter)
        val sheetConfirmButton = sheetView.findViewById<Button>(R.id.sheetConfirmButton)

        // Pre-populate z aktualnych wartości
        val currentTitle = viewModel.title.value ?: ""
        val currentDesc = viewModel.description.value ?: ""
        sheetTitleInput.setText(currentTitle)
        sheetDescriptionInput.setText(currentDesc)
        sheetTitleCounter.text = "${currentTitle.length}/40"
        sheetDescriptionCounter.text = "${currentDesc.length}/200"

        // Live counters
        sheetTitleInput.addTextChangedListener { text ->
            sheetTitleCounter.text = "${text?.length ?: 0}/40"
        }
        sheetDescriptionInput.addTextChangedListener { text ->
            sheetDescriptionCounter.text = "${text?.length ?: 0}/200"
        }

        // Potwierdź
        sheetConfirmButton.setOnClickListener {
            val newTitle = sheetTitleInput.text.toString()
            val newDesc = sheetDescriptionInput.text.toString()

            // Zaktualizuj ViewModel
            viewModel.onTitleChanged(newTitle)
            viewModel.onDescriptionChanged(newDesc)

            // Zaktualizuj ukryte inputy (kompatybilność)
            titleInput.setText(newTitle)
            descriptionInput.setText(newDesc)

            // Zaktualizuj display views
            titleDisplay.text = newTitle.ifEmpty { null }
            titleDisplay.hint = if (newTitle.isEmpty()) getString(R.string.add_expense_title_hint) else null
            descriptionDisplay.text = newDesc.ifEmpty { null }
            descriptionDisplay.hint = if (newDesc.isEmpty()) getString(R.string.add_expense_description_hint) else null

            bottomSheet.dismiss()
        }

        // Focus na tytuł i pokaż klawiaturę
        bottomSheet.setOnShowListener {
            sheetTitleInput.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(sheetTitleInput, InputMethodManager.SHOW_IMPLICIT)
        }

        bottomSheet.show()
    }

    // ================================================================
    // CURRENCY DROPDOWN
    // ================================================================

    private fun setupCurrencyDropdown() {
        val currencies = CurrencyRepository.getInstance().getCurrencies()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        currencyInput.setAdapter(adapter)

        currencyInput.setOnItemClickListener { _, _, position, _ ->
            viewModel.onCurrencySelected(currencies[position])
        }
    }

    // ================================================================
    // OBSERVERS
    // ================================================================

    override fun setupCustomObservers() {
        // Flaga załadowania danych — wypełnij pola gdy dane są gotowe
        viewModel.dataLoaded.observe(viewLifecycleOwner) { loaded ->
            if (loaded) {
                populateFieldsFromViewModel()
            }
        }

        // --- Błędy walidacji ---

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

        // --- Wybrana kategoria → aktualizacja wiersza ---

        viewModel.selectedCategory.observe(viewLifecycleOwner) { category ->
            category?.let {
                categoryInput.text = getString(it.nameResId)
                categoryIcon.setImageResource(it.iconResId)
                categoryIcon.visibility = View.VISIBLE
            }
        }

        // --- Data i czas → aktualizacja wiersza ---

        viewModel.dateTime.observe(viewLifecycleOwner) { dateTime ->
            dateTime?.let { (date, time) ->
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val dateStr = dateFormat.format(Date(date))
                val timeStr = timeFormat.format(Date(time))

                // Aktualizuj display views
                dateDisplay.text = dateStr
                timeDisplay.text = timeStr

                // Aktualizuj ukryte inputy (kompatybilność)
                dateInput.setText(dateStr)
                timeInput.setText(timeStr)
            }
        }

        // --- Waluta ---

        viewModel.currency.observe(viewLifecycleOwner) { currency ->
            if (!currency.isNullOrBlank()) {
                currencyDisplay.text = currency
            }
            if (currency.isNullOrBlank()) {
                currencyLayout.error = null
            }
        }

        // --- Payer → aktualizacja wiersza z awatarem (tylko wyświetlanie) ---

        viewModel.selectedPayer.observe(viewLifecycleOwner) { payerId ->
            if (payerId != null) {
                val participants = viewModel.participants.value ?: emptyList()
                val participant = participants.find { it.id == payerId }
                participant?.let {
                    payerDisplay.text = it.name
                    payerDisplay.hint = null
                    payerAvatar.text = it.name.take(2).uppercase()
                    payerAvatar.visibility = View.VISIBLE
                }
                // Aktualizuj ukryty przycisk (kompatybilność)
                payerButton.text = participant?.name ?: getString(R.string.error_payer_required)
            } else {
                payerDisplay.text = null
                payerDisplay.hint = getString(R.string.add_expense_payer_hint)
                payerAvatar.text = "?"
                payerButton.text = getString(R.string.add_expense_payer_hint)
            }
        }

        // --- Podział → aktualizacja wiersza + avatar stack (tylko wyświetlanie) ---

        viewModel.expenseSplit.observe(viewLifecycleOwner) { split ->
            val selectedParticipants = split.getSelectedParticipants()
            val selectedCount = selectedParticipants.size

            if (selectedCount > 0) {
                val typeLabel = when (split.splitType) {
                    SplitType.EQUAL -> "Po równo"
                    SplitType.MANUAL -> "Ręcznie"
                }
                splitDisplay.text = "$typeLabel · $selectedCount os."
                splitDisplay.hint = null
                splitButton.text = "$selectedCount os."

                // Buduj avatar stack
                updateSplitAvatarStack(selectedParticipants)
            } else {
                splitDisplay.text = null
                splitDisplay.hint = "Podział kosztów"
                splitButton.text = getString(R.string.add_expense_split_hint)
                splitAvatarStack.removeAllViews()
            }
        }

        // --- Eventy ---

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

    // ================================================================
    // AVATAR STACK
    // ================================================================

    private fun updateSplitAvatarStack(participants: List<SplitParticipant>) {
        splitAvatarStack.removeAllViews()

        val avatarColors = intArrayOf(
            ContextCompat.getColor(requireContext(), R.color.primary),
            ContextCompat.getColor(requireContext(), R.color.category_food),
            ContextCompat.getColor(requireContext(), R.color.category_transport),
            ContextCompat.getColor(requireContext(), R.color.category_shopping),
            ContextCompat.getColor(requireContext(), R.color.category_entertainment),
            ContextCompat.getColor(requireContext(), R.color.category_other)
        )

        val density = resources.displayMetrics.density
        val sizePx = (22 * density).toInt()
        val overlapPx = (-6 * density).toInt()
        val borderPx = (2 * density).toInt()

        participants.take(5).forEachIndexed { index, participant ->
            val avatar = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    if (index > 0) marginStart = overlapPx
                }
                gravity = Gravity.CENTER
                textSize = 7f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                text = participant.name.take(2).uppercase()

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(avatarColors[index % avatarColors.size])
                    setStroke(borderPx, Color.WHITE)
                }
            }
            splitAvatarStack.addView(avatar)
        }

        // Jeśli więcej niż 5 → pokaż "+N"
        if (participants.size > 5) {
            val moreLabel = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginStart = overlapPx
                }
                gravity = Gravity.CENTER
                textSize = 7f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                typeface = Typeface.DEFAULT_BOLD
                text = "+${participants.size - 5}"

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(requireContext(), R.color.background))
                    setStroke(borderPx, ContextCompat.getColor(requireContext(), R.color.divider))
                }
            }
            splitAvatarStack.addView(moreLabel)
        }
    }

    // ================================================================
    // POPULATE FIELDS — wypełnia display views po załadowaniu danych
    // ================================================================

    private fun populateFieldsFromViewModel() {
        // Tytuł i opis → display views
        val title = viewModel.title.value ?: ""
        val desc = viewModel.description.value ?: ""

        titleDisplay.text = title.ifEmpty { null }
        titleDisplay.hint = if (title.isEmpty()) getString(R.string.add_expense_title_hint) else null
        descriptionDisplay.text = desc.ifEmpty { null }
        descriptionDisplay.hint = if (desc.isEmpty()) getString(R.string.add_expense_description_hint) else null

        // Ukryte inputy (kompatybilność)
        titleInput.setText(title)
        descriptionInput.setText(desc)

        // Kwota → hero amount display + input
        val amount = viewModel.amount.value ?: ""
        amountInput.setText(amount)
        updateAmountDisplay(amount)

        // Waluta → display + input
        val currency = viewModel.currency.value ?: ""
        currencyDisplay.text = currency
        currencyInput.setText(currency, false)
    }

    // ================================================================
    // DIALOGS / MODALS
    // ================================================================

    private fun showCategoryPicker() {
        CategoryPickerModalFragment.newInstance { category ->
            viewModel.onCategorySelected(category)
        }.show(parentFragmentManager, "category_picker")
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

    // ================================================================
    // HELPERS
    // ================================================================

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