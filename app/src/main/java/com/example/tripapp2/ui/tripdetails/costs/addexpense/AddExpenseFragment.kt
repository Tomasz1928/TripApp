package com.example.tripapp2.ui.tripdetails.costs.addexpense

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
import com.example.tripapp2.ui.common.KeyboardAwareFragment
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.tripapp2.ui.common.extension.applyStatusBarInsets

/**
 * Fragment dodawania wydatku
 *
 * ZMIANA vs oryginał:
 * - Usunięto setDefaultCurrency() z hardcoded "PLN"
 * - Dodano obserwer viewModel.tripCurrency w setupCustomObservers()
 *   który ustawia domyślną walutę na trip.currency
 */
class AddExpenseFragment : KeyboardAwareFragment<AddExpenseViewModel>(R.layout.fragment_add_expense) {

    override val viewModel: AddExpenseViewModel by viewModels {
        AddExpenseViewModelFactory(getTripId())
    }

    // ================================
    // TOP BAR
    // ================================
    private lateinit var backButton: ImageView
    private lateinit var createButton: Button

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
        keyboardScrollView = view.findViewById(R.id.scrollViewAddExpense)
        keyboardBottomNav = null
    }

    override fun setupUI() {
        requireView().findViewById<View>(R.id.topBar).applyStatusBarInsets()
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
        createButton = view.findViewById(R.id.createButton)

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
        (activity as? DashboardActivity)?.closeAddExpenseAndShowCosts(getTripId())
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
        createButton.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            viewModel.onAddExpenseClicked()
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

        // ZMIENIONE: Row 4 i Row 5 otwierają ten sam połączony modal
        // Row 4: Kto płacił → PayerSplitModal
        rowPayer.setOnClickListener {
            showPayerSplitModal()
        }

        // Row 5: Podział kosztów → PayerSplitModal
        rowSplit.setOnClickListener {
            showPayerSplitModal()
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
    // CURRENCY DROPDOWN (ujednolicone)
    // ================================================================

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
            currencyDisplay.text = selected
        }
    }

    // USUNIĘTO: setDefaultCurrency() z hardcoded "PLN"
    // Zastąpione obserwatorem tripCurrency w setupCustomObservers()

    // ================================================================
    // OBSERVERS — aktualizacja display views
    // ================================================================

    override fun setupCustomObservers() {
        // --- Błędy walidacji ---

        viewModel.titleError.observe(viewLifecycleOwner) { errorResId ->
            titleLayout.error = errorResId?.let { getString(it) }
            // Podświetl wiersz tytułu na czerwono jeśli błąd
            if (errorResId != null) {
                rowTitle.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.transparent)
                )
            }
        }

        viewModel.amountError.observe(viewLifecycleOwner) { errorResId ->
            amountLayout.error = errorResId?.let { getString(it) }
            // Automatycznie expand inputy kwoty jeśli błąd
            if (errorResId != null && amountInputContainer.visibility == View.GONE) {
                amountInputContainer.visibility = View.VISIBLE
            }
        }

        viewModel.categoryError.observe(viewLifecycleOwner) { errorResId ->
            val errorText = errorResId?.let { getString(it) }
            categoryError.text = errorText
            categoryError.visibility = if (errorText != null) View.VISIBLE else View.GONE
        }

        viewModel.dateError.observe(viewLifecycleOwner) { errorResId ->
            dateLayout.error = errorResId?.let { getString(it) }
            val errorText = errorResId?.let { getString(it) }
            dateError.text = errorText
            dateError.visibility = if (errorText != null) View.VISIBLE else View.GONE
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

                dateDisplay.text = dateStr
                timeDisplay.text = timeStr

                dateInput.setText(dateStr)
                timeInput.setText(timeStr)
            }
        }

        // --- Waluta ---

        viewModel.currency.observe(viewLifecycleOwner) { currency ->
            if (!currency.isNullOrBlank()) {
                currencyDisplay.text = currency
            }
        }

        // --- NOWE: Trip currency → ustaw domyślną walutę ---

        viewModel.tripCurrency.observe(viewLifecycleOwner) { tripCurrency ->
            // Ustaw tylko jeśli użytkownik jeszcze nie wybrał waluty
            if (viewModel.currency.value.isNullOrBlank()) {
                currencyInput.setText(tripCurrency, false)
                currencyDisplay.text = tripCurrency
                viewModel.onCurrencySelected(tripCurrency)
            }
        }

        // --- Payer → aktualizacja wiersza z awatarem ---

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
                payerButton.text = participant?.name ?: getString(R.string.error_payer_required)
            } else {
                payerDisplay.text = null
                payerDisplay.hint = getString(R.string.add_expense_payer_hint)
                payerAvatar.text = "?"
            }
        }

        // --- Split → aktualizacja wiersza ---

        viewModel.expenseSplit.observe(viewLifecycleOwner) { split ->
            val selected = split?.getSelectedParticipants() ?: emptyList()
            if (selected.isNotEmpty()) {
                splitDisplay.text = "${selected.size} osób"
                updateSplitAvatars(selected)
            } else {
                splitDisplay.text = null
                splitDisplay.hint = getString(R.string.add_expense_split_hint)
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
                showPayerSplitModal()
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
                navigateBackToCosts()
            }
        }
    }

    // ================================================================
    // SPLIT AVATARS (mini awatary w wierszu podziału)
    // ================================================================

    private fun updateSplitAvatars(participants: List<SplitParticipant>) {
        splitAvatarStack.removeAllViews()
        val maxAvatars = 4
        val density = resources.displayMetrics.density

        participants.take(maxAvatars).forEachIndexed { index, participant ->
            val avatar = TextView(requireContext()).apply {
                text = participant.name.take(1).uppercase()
                setTextColor(Color.WHITE)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER

                val size = (24 * density).toInt()
                val lp = LinearLayout.LayoutParams(size, size)
                if (index > 0) lp.marginStart = (-6 * density).toInt()
                layoutParams = lp

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    setStroke((1.5 * density).toInt(), Color.WHITE)
                }
                background = bg
            }
            splitAvatarStack.addView(avatar)
        }

        if (participants.size > maxAvatars) {
            val moreLabel = TextView(requireContext()).apply {
                text = "+${participants.size - maxAvatars}"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                textSize = 11f
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.marginStart = (4 * density).toInt()
                layoutParams = lp
            }
            splitAvatarStack.addView(moreLabel)
        }
    }

    // ================================================================
    // DIALOGS / MODALS — ZMIENIONE
    // ================================================================

    private fun showCategoryPicker() {
        CategoryPickerModalFragment.newInstance { category ->
            viewModel.onCategorySelected(category)
        }.show(parentFragmentManager, "category_picker")
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.add_expense_date_hint))
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            viewModel.onDateSelected(selection)
            showTimePicker()
        }

        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker() {
        val now = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(now.get(Calendar.HOUR_OF_DAY))
            .setMinute(now.get(Calendar.MINUTE))
            .setTitleText(getString(R.string.add_expense_time_hint))
            .build()

        picker.addOnPositiveButtonClickListener {
            viewModel.onTimeSelected(picker.hour, picker.minute)
        }

        picker.show(parentFragmentManager, "TIME_PICKER")
    }

    private fun showPayerSplitModal() {
        val split = viewModel.expenseSplit.value ?: return
        val amount = amountInput.text.toString().toFloatOrNull() ?: 0f

        if (amount <= 0) {
            showMessage(getString(R.string.error_amount_required_before_split))
            return
        }

        val participants = viewModel.participants.value ?: emptyList()
        if (participants.isEmpty()) {
            showMessage(getString(R.string.error_no_participants))
            return
        }

        val currentUserId = viewModel.getCurrentUserId()
        val currentPayerId = viewModel.selectedPayer.value

        val modal = PayerSplitModalFragment.newInstance(
            split = split,
            totalAmount = amount,
            currentUserId = currentUserId,
            selectedPayerId = currentPayerId,
            onResult = { payerId, updatedSplit ->
                viewModel.onPayerSelected(payerId)
                viewModel.onExpenseSplitUpdated(updatedSplit)
            }
        )
        modal.show(parentFragmentManager, "PAYER_SPLIT_MODAL")
    }

    // ================================================================
    // HELPERS
    // ================================================================

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