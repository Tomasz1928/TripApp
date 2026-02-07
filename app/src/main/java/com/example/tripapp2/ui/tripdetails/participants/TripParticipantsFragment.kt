package com.example.tripapp2.ui.tripdetails.participants

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.extension.hide
import com.example.tripapp2.ui.common.extension.show
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

/**
 * Fragment listy uczestników wycieczki
 *
 * Funkcjonalności:
 * - Wyświetlanie listy uczestników (aktywni + placeholderzy)
 * - Pasek akcji dla właściciela (Wszyscy/Dodaj/Odłącz/Usuń)
 * - Dodawanie placeholderów (tylko właściciel)
 * - Kopiowanie kodów dostępu
 * - Odłączanie użytkowników (tylko właściciel)
 * - Usuwanie placeholderów (tylko właściciel)
 */
class TripParticipantsFragment : BaseFragment<TripParticipantsViewModel>(R.layout.fragment_trip_participants) {

    override val viewModel: TripParticipantsViewModel by viewModels {
        TripParticipantsViewModelFactory(getTripId())
    }

    private lateinit var participantsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyStateTitle: TextView
    private lateinit var emptyStateMessage: TextView
    private lateinit var scrollParticipants: View
    private lateinit var actionsScroll: View

    // Przyciski akcji
    private lateinit var actionAll: MaterialButton
    private lateinit var actionAdd: MaterialButton
    private lateinit var actionDetach: MaterialButton
    private lateinit var actionDelete: MaterialButton

    override fun setupUI() {
        initializeViews()
        setupActions()
        setupBottomPadding()
    }

    override fun setupCustomObservers() {
        // Stan uczestników
        viewModel.participantsState.observe(viewLifecycleOwner) { state ->
            handleParticipantsState(state)
        }

        // Aktualny tryb widoku
        viewModel.currentViewMode.observe(viewLifecycleOwner) { mode ->
            updateActionButtons(mode)
        }

        // Event kopiowania kodu
        viewModel.copyCodeEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { copyEvent ->
                copyToClipboard(copyEvent.code)
                showMessage(copyEvent.message)
            }
        }

        // Event pokazania dialogu dodawania
        viewModel.showAddPlaceholderDialogEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                showAddPlaceholderDialog()
            }
        }
    }

    private fun initializeViews() {
        val view = requireView()
        participantsContainer = view.findViewById(R.id.participantsContainer)
        emptyState = view.findViewById(R.id.emptyState)
        emptyStateTitle = view.findViewById(R.id.emptyStateTitle)
        emptyStateMessage = view.findViewById(R.id.emptyStateMessage)
        scrollParticipants = view.findViewById(R.id.scrollParticipants)
        actionsScroll = view.findViewById(R.id.actionsScroll)

        actionAll = view.findViewById(R.id.actionAll)
        actionAdd = view.findViewById(R.id.actionAdd)
        actionDetach = view.findViewById(R.id.actionDetach)
        actionDelete = view.findViewById(R.id.actionDelete)
    }

    private fun setupActions() {
        actionAll.setOnClickListener {
            viewModel.changeViewMode(ParticipantViewMode.ALL)
        }

        actionAdd.setOnClickListener {
            viewModel.changeViewMode(ParticipantViewMode.ADD)
        }

        actionDetach.setOnClickListener {
            viewModel.changeViewMode(ParticipantViewMode.DETACH)
        }

        actionDelete.setOnClickListener {
            viewModel.changeViewMode(ParticipantViewMode.DELETE)
        }
    }

    private fun setupBottomPadding() {
        val tripBottomNav = (activity as? DashboardActivity)?.tripBottomNav
        tripBottomNav?.viewTreeObserver?.addOnGlobalLayoutListener {
            val navHeight = tripBottomNav.height
            scrollParticipants.setPadding(
                scrollParticipants.paddingLeft,
                scrollParticipants.paddingTop,
                scrollParticipants.paddingRight,
                navHeight
            )
        }
    }

    /**
     * Obsługa różnych stanów ekranu
     */
    private fun handleParticipantsState(state: TripParticipantsState) {
        when (state) {
            is TripParticipantsState.Loading -> {
                participantsContainer.hide()
                emptyState.hide()
            }
            is TripParticipantsState.Success -> {
                emptyState.hide()
                participantsContainer.show()
                displayParticipants(state.participants, state.isCurrentUserOwner, state.currentMode)

                // Pokaż pasek akcji TYLKO dla właściciela
                if (state.isCurrentUserOwner) {
                    actionsScroll.show()
                } else {
                    actionsScroll.hide()
                }
            }
            is TripParticipantsState.Empty -> {
                participantsContainer.hide()
                emptyState.show()
                updateEmptyStateMessage(viewModel.currentViewMode.value ?: ParticipantViewMode.ALL)
            }
            is TripParticipantsState.Error -> {
                participantsContainer.hide()
                emptyState.hide()
                showError(state.message)
            }
        }
    }

    /**
     * Aktualizuje komunikat pustego stanu w zależności od trybu
     */
    private fun updateEmptyStateMessage(mode: ParticipantViewMode) {
        when (mode) {
            ParticipantViewMode.ALL -> {
                emptyStateTitle.text = getString(R.string.participants_empty_state_title)
                emptyStateMessage.text = getString(R.string.participants_empty_state_message)
            }
            ParticipantViewMode.DETACH -> {
                emptyStateTitle.text = "Brak użytkowników do odłączenia"
                emptyStateMessage.text = "Wszyscy uczestnicy są właścicielem lub placeholderami"
            }
            ParticipantViewMode.DELETE -> {
                emptyStateTitle.text = "Brak placeholderów"
                emptyStateMessage.text = "Nie ma placeholderów do usunięcia"
            }
            else -> {}
        }
    }

    /**
     * Wyświetla listę uczestników
     */
    private fun displayParticipants(
        participants: List<ParticipantUiModel>,
        isCurrentUserOwner: Boolean,
        currentMode: ParticipantViewMode
    ) {
        participantsContainer.removeAllViews()

        participants.forEach { participant ->
            val itemView = createParticipantView(participant, isCurrentUserOwner, currentMode)
            participantsContainer.addView(itemView)
        }
    }

    /**
     * Tworzy widok pojedynczego uczestnika
     */
    private fun createParticipantView(
        participant: ParticipantUiModel,
        isCurrentUserOwner: Boolean,
        currentMode: ParticipantViewMode
    ): View {
        val view = layoutInflater.inflate(R.layout.item_participant, participantsContainer, false)

        // Podstawowe info
        view.findViewById<TextView>(R.id.participantNickname).text = participant.nickname
        val expensesLabel = getString(R.string.participants_expenses_label)
        view.findViewById<TextView>(R.id.participantExpenses).text =
            "$expensesLabel ${participant.formattedExpenses}"

        // Badges
        val ownerBadge = view.findViewById<MaterialCardView>(R.id.ownerBadge)
        val activeBadge = view.findViewById<MaterialCardView>(R.id.activeBadge)
        val placeholderBadge = view.findViewById<MaterialCardView>(R.id.placeholderBadge)

        when {
            participant.isOwner -> {
                ownerBadge.visibility = View.VISIBLE
            }
            participant.isPlaceholder -> {
                placeholderBadge.visibility = View.VISIBLE
            }
            else -> {
                activeBadge.visibility = View.VISIBLE
            }
        }

        // Access Code Section (tylko dla placeholderów)
        val accessCodeSection = view.findViewById<LinearLayout>(R.id.accessCodeSection)
        if (participant.isPlaceholder && participant.accessCode != null) {
            accessCodeSection.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.accessCodeText).text = participant.accessCode

            val copyButton = view.findViewById<View>(R.id.copyCodeButton)
            copyButton.setOnClickListener {
                viewModel.onCopyAccessCode(participant)
            }
        }

        // Action Buttons - widoczne w odpowiednich trybach
        val actionsContainer = view.findViewById<LinearLayout>(R.id.actionsContainer)
        val detachButton = view.findViewById<MaterialButton>(R.id.detachButton)
        val deleteButton = view.findViewById<MaterialButton>(R.id.deleteButton)

        if (isCurrentUserOwner) {
            when (currentMode) {
                ParticipantViewMode.DETACH -> {
                    // Pokaż przycisk odłącz dla aktywnych użytkowników (nie właściciel, nie placeholder)
                    if (!participant.isPlaceholder && !participant.isOwner) {
                        actionsContainer.visibility = View.VISIBLE
                        detachButton.visibility = View.VISIBLE
                        detachButton.setOnClickListener {
                            showDetachUserDialog(participant)
                        }
                    }
                }
                ParticipantViewMode.DELETE -> {
                    // Pokaż przycisk usuń dla placeholderów
                    if (participant.isPlaceholder) {
                        actionsContainer.visibility = View.VISIBLE
                        deleteButton.visibility = View.VISIBLE
                        deleteButton.setOnClickListener {
                            showDeletePlaceholderDialog(participant)
                        }
                    }
                }
                else -> {
                    // W trybie ALL nie pokazuj przycisków akcji
                    actionsContainer.visibility = View.GONE
                }
            }
        }

        // Click na całą kartę
        view.setOnClickListener {
            viewModel.onParticipantClicked(participant)
        }

        return view
    }

    /**
     * Pokazuje dialog dodawania placeholdera
     */
    private fun showAddPlaceholderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_placeholder, null)
        val nicknameInput = dialogView.findViewById<TextInputEditText>(R.id.nicknameInput)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.participants_dialog_add_title)
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_button_add) { _, _ ->
                val nickname = nicknameInput.text.toString()
                viewModel.addPlaceholder(nickname)
            }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    /**
     * Pokazuje dialog potwierdzenia odłączenia użytkownika
     */
    private fun showDetachUserDialog(participant: ParticipantUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.participants_dialog_detach_title)
            .setMessage(
                getString(R.string.participants_dialog_detach_message, participant.nickname)
            )
            .setPositiveButton(R.string.dialog_button_detach) { _, _ ->
                viewModel.detachUser(participant.id, participant.nickname)
            }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    /**
     * Pokazuje dialog potwierdzenia usunięcia placeholdera
     */
    private fun showDeletePlaceholderDialog(participant: ParticipantUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.participants_dialog_delete_title)
            .setMessage(
                getString(R.string.participants_dialog_delete_message, participant.nickname)
            )
            .setPositiveButton(R.string.dialog_button_delete) { _, _ ->
                viewModel.removePlaceholder(participant.id, participant.nickname)
            }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    /**
     * Aktualizuje wygląd przycisków akcji
     */
    private fun updateActionButtons(activeMode: ParticipantViewMode) {
        // Reset wszystkich
        listOf(actionAll, actionAdd, actionDetach, actionDelete).forEach {
            it.alpha = 0.6f
        }

        // Podświetl aktywny
        val activeButton = when (activeMode) {
            ParticipantViewMode.ALL -> actionAll
            ParticipantViewMode.ADD -> actionAdd
            ParticipantViewMode.DETACH -> actionDetach
            ParticipantViewMode.DELETE -> actionDelete
        }
        activeButton.alpha = 1.0f
    }

    /**
     * Kopiuje tekst do schowka
     */
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.trip_details_access_code), text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Pobiera ID wycieczki
     */
    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID) ?: ""
    }

    companion object {
        private const val ARG_TRIP_ID = "trip_id"

        fun newInstance(tripId: String) = TripParticipantsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TRIP_ID, tripId)
            }
        }
    }
}