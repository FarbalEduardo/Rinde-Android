package com.farbalapps.rinde.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isVerified: Boolean = false,
    val reputationScore: Float = 0f,
    val isPrivate: Boolean = false
)

