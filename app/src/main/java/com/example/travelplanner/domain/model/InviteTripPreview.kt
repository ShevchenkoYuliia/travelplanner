package com.example.travelplanner.domain.model

data class InviteTripPreview(
    val token: String,
    val title: String,
    val destination: String,
    val description: String,
    val invitedBy: String
)
