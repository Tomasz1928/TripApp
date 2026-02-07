package com.example.tripapp2.ui.tripdetails.participants

import com.example.tripapp2.data.model.ParticipantDto

/**
 * UI Models dla Trip Participants
 */

/**
 * Model uczestnika dla listy
 */
data class ParticipantUiModel(
    val id: String,
    val nickname: String,
    val isPlaceholder: Boolean,
    val accessCode: String?,           // Tylko dla placeholderów
    val isOwner: Boolean,              // Czy to właściciel wycieczki
    val totalExpenses: Float,          // Suma wydatków uczestnika
    val formattedExpenses: String      // "1 234,56 PLN"
)

/**
 * Typ uczestnika (do sortowania i filtrowania)
 */
enum class ParticipantType {
    OWNER,          // Właściciel wycieczki
    ACTIVE,         // Aktywny użytkownik
    PLACEHOLDER     // Placeholder
}

/**
 * Tryb widoku uczestników (dla właściciela)
 */
enum class ParticipantViewMode {
    ALL,            // Wszyscy uczestnicy
    ADD,            // Dodawanie uczestnika (pokazuje modal)
    DETACH,         // Odłączanie kont (pokazuje aktywnych bez właściciela)
    DELETE          // Usuwanie placeholderów (pokazuje tylko placeholdery)
}

/**
 * Stan ekranu Participants
 */
sealed class TripParticipantsState {
    object Loading : TripParticipantsState()
    data class Success(
        val participants: List<ParticipantUiModel>,
        val isCurrentUserOwner: Boolean,    // Czy aktualny użytkownik jest właścicielem
        val tripCurrency: String,
        val currentMode: ParticipantViewMode // Aktualny tryb widoku
    ) : TripParticipantsState()
    object Empty : TripParticipantsState()
    data class Error(val message: String) : TripParticipantsState()
}

/**
 * Event kopiowania kodu dostępu
 */
data class CopyAccessCodeEvent(
    val code: String,
    val participantName: String,
    val message: String = "Skopiowano kod dostępu dla $participantName"
)

// ==========================================
// MAPPERS
// ==========================================

/**
 * Konwertuje ParticipantDto na ParticipantUiModel
 */
fun ParticipantDto.toUiModel(
    ownerId: String,
    currentUserId: String,
    currency: String,
    isPlaceholder: Boolean = false,
    accessCode: String? = null
): ParticipantUiModel {
    return ParticipantUiModel(
        id = id,
        nickname = nickname,
        isPlaceholder = isPlaceholder,
        accessCode = accessCode,
        isOwner = id == ownerId,
        totalExpenses = totalExpenses?.valueMainCurrency ?: 0f,
        formattedExpenses = "%.2f %s".format(totalExpenses?.valueMainCurrency ?: 0f, currency)
    )
}

/**
 * Sortuje uczestników: właściciel → aktywni → placeholderzy
 */
fun List<ParticipantUiModel>.sortByType(): List<ParticipantUiModel> {
    return sortedWith(compareBy(
        { participant ->
            when {
                participant.isOwner -> 0
                !participant.isPlaceholder -> 1
                else -> 2
            }
        },
        { it.nickname }  // Alfabetycznie w ramach grupy
    ))
}

/**
 * Filtruje uczestników według trybu
 */
fun List<ParticipantUiModel>.filterByMode(
    mode: ParticipantViewMode,
    currentUserId: String
): List<ParticipantUiModel> {
    return when (mode) {
        ParticipantViewMode.ALL -> this
        ParticipantViewMode.ADD -> emptyList() // Modal pokazuje się zamiast listy
        ParticipantViewMode.DETACH -> this.filter {
            !it.isPlaceholder && !it.isOwner
        }
        ParticipantViewMode.DELETE -> this.filter {
            it.isPlaceholder
        }
    }
}