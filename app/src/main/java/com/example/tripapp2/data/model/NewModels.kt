package com.example.tripapp2.data.model

/**
 * Nowe modele DTO wymagane do integracji z GraphQL backendem.
 * Dodaj te klasy do istniejącego DataModels.kt lub jako osobny plik.
 */

// ==========================================
// AUTH MODELS
// ==========================================

data class AuthResultDto(
    val success: Boolean,
    val message: String,
    val user: UserInfoDto?
)

data class SessionDto(
    val isAuthenticated: Boolean,
    val user: UserInfoDto?
)

// ==========================================
// SUBSCRIPTION DELTA MODEL
// ==========================================

data class TripNotificationDto(
    val tripId: String,
    val tripName: String,
    val eventType: String,
    val actorNickname: String,
    val actorParticipantId: Int
)

// ==========================================
// SETTLE BY COSTS REQUEST (jeśli jeszcze nie istnieje)
// ==========================================
// Sprawdź czy SettleByCostsRequest już jest zdefiniowany w settlements ViewModel.
// Jeśli nie, dodaj:

// data class SettleByCostsRequest(
//     val expenseId: String,
//     val payerId: String,
//     val participantId: String
// )
