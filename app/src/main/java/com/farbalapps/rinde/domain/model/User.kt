package com.farbalapps.rinde.domain.model

data class User(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null
)
