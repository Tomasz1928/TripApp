package com.example.tripapp2.ui.tripdetails.participants

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.baseModals.ConfirmModalFragment
import com.example.tripapp2.ui.common.baseModals.InputModalFragment
import com.example.tripapp2.ui.common.extension.hide
import com.example.tripapp2.ui.common.extension.show
import com.example.tripapp2.ui.common.widget.AvatarStackHelper
import com.example.tripapp2.ui.dashboard.DashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import android.view.View
import com.example.tripapp2.ui.common.extension.applyStatusBarInsets

/**
 * Fragment listy uczestników — Propozycja C: Floating Sections
 *
 * Zmiany vs oryginał:
 * - Brak header image (top bar z tytułem + "Zarządzaj")
 * - Avatar stack summary pod top barem (kółka + "N uczestników")
 * - Lista uczestników w jednej grouped card (settings-style)
 * - Przycisk "Zarządzaj" toggle'uje pasek akcji (Wszyscy/Dodaj/Odłącz/Usuń)
 * - Logika trybów widoku (ALL/ADD/DETACH/DELETE) — BEZ ZMIAN
 */
class TripParticipantsFragment : BaseFragment<TripParticipantsViewModel>(R.layout.fragment_trip_participants) {

    override val viewModel: TripParticipantsViewModel by viewModels {
        TripParticipantsViewModelFactory(getTripId())
    }

    // ================================
    // VIEWS
    // ================================
    private lateinit var participantsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyStateTitle: TextView
    private lateinit var emptyStateMessage: TextView
    private lateinit var scrollParticipants: View
    private lateinit var actionsScroll: View

    // Nowe views (Propozycja C)
    private lateinit var manageButton: MaterialButton
    private lateinit var avatarStackContainer: LinearLayout
    private lateinit var participantCount: TextView

    // Przyciski akcji
    private lateinit var actionAll: MaterialButton
    private lateinit var actionAdd: MaterialButton
    private lateinit var actionDetach: MaterialButton
    private lateinit var actionDelete: MaterialButton

    override fun setupUI() {
        requireView().findViewById<View>(R.id.topBar).applyStatusBarInsets()
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
                showAddPlaceholderModal()
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

        // Nowe (Propozycja C)
        manageButton = view.findViewById(R.id.manageButton)
        avatarStackContainer = view.findViewById(R.id.avatarStackContainer)
        participantCount = view.findViewById(R.id.participantCount)

        // Przyciski akcji
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

        // Toggle pasek akcji
        manageButton.setOnClickListener {
            val isVisible = actionsScroll.visibility == View.VISIBLE
            actionsScroll.visibility = if (isVisible) View.GONE else View.VISIBLE
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

    // ================================================================
    // STATE HANDLING
    // ================================================================

    private fun handleParticipantsState(state: TripParticipantsState) {
        when (state) {
            is TripParticipantsState.Loading -> {
                participantsContainer.hide()
                emptyState.hide()
            }
            is TripParticipantsState.Success -> {
                emptyState.hide()
                participantsContainer.show()

                // Manage button — widoczny tylko dla właściciela
                manageButton.visibility = if (state.isCurrentUserOwner) View.VISIBLE else View.GONE

                // Avatar stack summary
                participantCount.text = "${state.participants.size} uczestników"
                updateAvatarStack(state.participants)

                // Pokaż pasek akcji TYLKO dla właściciela
                if (state.isCurrentUserOwner) {
                    // Nie pokazuj domyślnie — toggle przez manageButton
                } else {
                    actionsScroll.hide()
                }

                displayParticipants(state.participants, state.isCurrentUserOwner, state.currentMode)
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

    // ================================================================
    // AVATAR STACK
    // ================================================================

    private fun updateAvatarStack(participants: List<ParticipantUiModel>) {
        AvatarStackHelper.buildAvatarStack(
            context = requireContext(),
            container = avatarStackContainer,
            names = participants.map { it.nickname },
            maxVisible = 5,
            sizeDp = 28
        )
    }

    // ================================================================
    // DISPLAY PARTICIPANTS
    // ================================================================

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

    private fun createParticipantView(
        participant: ParticipantUiModel,
        isCurrentUserOwner: Boolean,
        currentMode: ParticipantViewMode
    ): View {
        val view = layoutInflater.inflate(R.layout.item_participant, participantsContainer, false)

        val avatar = view.findViewById<TextView>(R.id.participantAvatar)
        avatar.text = participant.nickname.take(2).uppercase()

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
            participant.isOwner -> ownerBadge.visibility = View.VISIBLE
            participant.isPlaceholder -> placeholderBadge.visibility = View.VISIBLE
            else -> activeBadge.visibility = View.VISIBLE
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

        // Action Buttons
        val actionsContainer = view.findViewById<LinearLayout>(R.id.actionsContainer)
        val detachButton = view.findViewById<MaterialButton>(R.id.detachButton)
        val deleteButton = view.findViewById<MaterialButton>(R.id.deleteButton)

        if (isCurrentUserOwner) {
            when (currentMode) {
                ParticipantViewMode.DETACH -> {
                    if (!participant.isPlaceholder && !participant.isOwner) {
                        actionsContainer.visibility = View.VISIBLE
                        detachButton.visibility = View.VISIBLE
                        detachButton.setOnClickListener {
                            showDetachUserModal(participant)
                        }
                    }
                }
                ParticipantViewMode.DELETE -> {
                    if (participant.isPlaceholder) {
                        actionsContainer.visibility = View.VISIBLE
                        deleteButton.visibility = View.VISIBLE
                        deleteButton.setOnClickListener {
                            showDeletePlaceholderModal(participant)
                        }
                    }
                }
                else -> {
                    actionsContainer.visibility = View.GONE
                }
            }
        }

        return view
    }

    // ================================================================
    // MODALS
    // ================================================================

    private fun showAddPlaceholderModal() {
        InputModalFragment.newInstance(
            title = getString(R.string.participants_dialog_add_title),
            hint = getString(R.string.dialog_add_placeholder_hint),
            confirmText = getString(R.string.dialog_button_add),
            onConfirm = { nickname ->
                viewModel.addPlaceholder(nickname)
            }
        ).show(parentFragmentManager, "add_placeholder")
    }

    private fun showDetachUserModal(participant: ParticipantUiModel) {
        ConfirmModalFragment.newInstance(
            title = getString(R.string.participants_dialog_detach_title),
            message = getString(R.string.participants_dialog_detach_message, participant.nickname),
            confirmText = getString(R.string.dialog_button_detach),
            confirmStyle = ConfirmModalFragment.ConfirmStyle.DANGER,
            onConfirm = { viewModel.detachUser(participant.id) }
        ).show(parentFragmentManager, "detach_user")
    }

    private fun showDeletePlaceholderModal(participant: ParticipantUiModel) {
        ConfirmModalFragment.newInstance(
            title = getString(R.string.participants_dialog_delete_title),
            message = getString(R.string.participants_dialog_delete_message, participant.nickname),
            confirmText = getString(R.string.dialog_button_delete),
            confirmStyle = ConfirmModalFragment.ConfirmStyle.DANGER,
            onConfirm = { viewModel.removePlaceholder(participant.id) }
        ).show(parentFragmentManager, "delete_placeholder")
    }

    // ================================================================
    // HELPERS
    // ================================================================

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

    private fun updateActionButtons(activeMode: ParticipantViewMode) {
        listOf(actionAll, actionAdd, actionDetach, actionDelete).forEach {
            it.alpha = 0.6f
        }
        val activeButton = when (activeMode) {
            ParticipantViewMode.ALL -> actionAll
            ParticipantViewMode.ADD -> actionAdd
            ParticipantViewMode.DETACH -> actionDetach
            ParticipantViewMode.DELETE -> actionDelete
        }
        activeButton.alpha = 1.0f
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.trip_details_access_code), text)
        clipboard.setPrimaryClip(clip)
    }

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
