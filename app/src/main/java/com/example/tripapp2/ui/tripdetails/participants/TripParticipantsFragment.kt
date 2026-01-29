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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Fragment listy uczestników wycieczki
 *
 * Funkcjonalności:
 * - Wyświetlanie listy uczestników (aktywni + placeholderzy)
 * - Dodawanie placeholderów (tylko właściciel)
 * - Kopiowanie kodów dostępu
 * - Usuwanie placeholderów (tylko właściciel)
 */
class TripParticipantsFragment : BaseFragment<TripParticipantsViewModel>(R.layout.fragment_trip_participants) {

    override val viewModel: TripParticipantsViewModel by viewModels {
        TripParticipantsViewModelFactory(getTripId())
    }

    private lateinit var participantsContainer: LinearLayout
    private lateinit var emptyState: View
    private lateinit var addParticipantFab: FloatingActionButton
    private lateinit var scrollParticipants: View

    override fun setupUI() {
        initializeViews()
        setupFab()
        setupBottomPadding()
    }

    override fun setupCustomObservers() {
        // Stan uczestników
        viewModel.participantsState.observe(viewLifecycleOwner) { state ->
            handleParticipantsState(state)
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

        // Event dodania placeholdera
        viewModel.placeholderAddedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { addedEvent ->
                showPlaceholderAddedDialog(addedEvent)
            }
        }
    }

    private fun initializeViews() {
        val view = requireView()
        participantsContainer = view.findViewById(R.id.participantsContainer)
        emptyState = view.findViewById(R.id.emptyState)
        addParticipantFab = view.findViewById(R.id.addParticipantFab)
        scrollParticipants = view.findViewById(R.id.scrollParticipants)
    }

    private fun setupFab() {
        addParticipantFab.setOnClickListener {
            viewModel.onAddPlaceholderClicked()
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
                displayParticipants(state.participants, state.isCurrentUserOwner)

                // FAB widoczny ZAWSZE dla właściciela
                if (state.isCurrentUserOwner) {
                    addParticipantFab.show()
                } else {
                    addParticipantFab.hide()
                }
            }
            is TripParticipantsState.Empty -> {
                participantsContainer.hide()
                emptyState.show()
                // FAB widoczny w empty state - musi być, żeby dodać pierwszego!
                addParticipantFab.show()
            }
            is TripParticipantsState.Error -> {
                participantsContainer.hide()
                emptyState.hide()
                addParticipantFab.hide()
                showError(state.message)
            }
        }
    }

    /**
     * Wyświetla listę uczestników
     */
    private fun displayParticipants(
        participants: List<ParticipantUiModel>,
        isCurrentUserOwner: Boolean
    ) {
        participantsContainer.removeAllViews()

        participants.forEach { participant ->
            val itemView = createParticipantView(participant, isCurrentUserOwner)
            participantsContainer.addView(itemView)
        }
    }

    /**
     * Tworzy widok pojedynczego uczestnika
     */
    private fun createParticipantView(
        participant: ParticipantUiModel,
        isCurrentUserOwner: Boolean
    ): View {
        val view = layoutInflater.inflate(R.layout.item_participant, participantsContainer, false)

        // Podstawowe info
        view.findViewById<TextView>(R.id.participantNickname).text = participant.nickname
        view.findViewById<TextView>(R.id.participantExpenses).text =
            "Wydatki: ${participant.formattedExpenses}"

        // Badges
        val ownerBadge = view.findViewById<MaterialCardView>(R.id.ownerBadge)
        val activeBadge = view.findViewById<MaterialCardView>(R.id.activeBadge)
        val placeholderBadge = view.findViewById<MaterialCardView>(R.id.placeholderBadge)

        when {
            participant.isOwner -> {
                // Właściciel
                ownerBadge.visibility = View.VISIBLE
            }
            participant.isPlaceholder -> {
                // Placeholder (oczekujący)
                placeholderBadge.visibility = View.VISIBLE
            }
            else -> {
                // Aktywny uczestnik (nie właściciel, nie placeholder)
                activeBadge.visibility = View.VISIBLE
            }
        }

        // Access code section (tylko dla placeholderów)
        val accessCodeSection = view.findViewById<LinearLayout>(R.id.accessCodeSection)
        val accessCodeText = view.findViewById<TextView>(R.id.accessCodeText)
        val copyCodeButton = view.findViewById<ImageView>(R.id.copyCodeButton)

        if (participant.isPlaceholder && participant.accessCode != null) {
            accessCodeSection.visibility = View.VISIBLE
            accessCodeText.text = participant.accessCode

            // Kopiowanie kodu - cała sekcja jest klikalna
            accessCodeSection.setOnClickListener {
                viewModel.onCopyAccessCode(participant)
            }

            copyCodeButton.setOnClickListener {
                viewModel.onCopyAccessCode(participant)
            }
        }

        // Action buttons (tylko dla właściciela)
        val actionsContainer = view.findViewById<LinearLayout>(R.id.actionsContainer)
        val detachButton = view.findViewById<MaterialButton>(R.id.detachButton)
        val deleteButton = view.findViewById<MaterialButton>(R.id.deleteButton)

        if (isCurrentUserOwner && !participant.isOwner) { // Nie można odłączyć/usunąć właściciela
            actionsContainer.visibility = View.VISIBLE

            if (participant.isPlaceholder) {
                // Placeholder - pokaż przycisk DELETE
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener {
                    showDeletePlaceholderDialog(participant)
                }
            } else {
                // Aktywny użytkownik - pokaż przycisk DETACH
                detachButton.visibility = View.VISIBLE
                detachButton.setOnClickListener {
                    showDetachUserDialog(participant)
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
            .setTitle("Dodaj uczestnika")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val nickname = nicknameInput.text.toString()
                viewModel.addPlaceholder(nickname)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    /**
     * Pokazuje dialog z kodem dostępu po dodaniu placeholdera
     */
    private fun showPlaceholderAddedDialog(event: PlaceholderAddedEvent) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_placeholder_added, null)

        dialogView.findViewById<TextView>(R.id.nicknameText).text = event.nickname
        dialogView.findViewById<TextView>(R.id.accessCodeText).text = event.accessCode

        val copyButton = dialogView.findViewById<MaterialButton>(R.id.copyCodeButton)
        copyButton.setOnClickListener {
            copyToClipboard(event.accessCode)
            showMessage("Skopiowano kod dostępu")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Uczestnik dodany")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * Pokazuje dialog potwierdzenia odłączenia użytkownika
     */
    private fun showDetachUserDialog(participant: ParticipantUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Odłącz użytkownika")
            .setMessage(
                "Czy na pewno chcesz odłączyć ${participant.nickname}?\n\n" +
                        "Użytkownik stanie się placeholderem z nowym kodem dostępu. " +
                        "Będzie mógł ponownie dołączyć używając nowego kodu."
            )
            .setPositiveButton("Odłącz") { _, _ ->
                viewModel.detachUser(participant.id, participant.nickname)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    /**
     * Pokazuje dialog potwierdzenia usunięcia placeholdera
     */
    private fun showDeletePlaceholderDialog(participant: ParticipantUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Usuń placeholdera")
            .setMessage(
                "Czy na pewno chcesz usunąć placeholdera ${participant.nickname}?\n\n" +
                        "Ta akcja jest nieodwracalna. Wszystkie dane związane z tym placeholderem zostaną usunięte."
            )
            .setPositiveButton("Usuń") { _, _ ->
                viewModel.removePlaceholder(participant.id, participant.nickname)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    /**
     * Kopiuje tekst do schowka
     */
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Access Code", text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Pobiera ID wycieczki
     */
    private fun getTripId(): String {
        return arguments?.getString(ARG_TRIP_ID) ?: "trip_2"
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