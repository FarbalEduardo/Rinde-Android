package com.farbalapps.rinde.domain.model

data class Profile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val commentsCount: Int = 0,
    val rating: Float = 0f,
    val reviewsCount: Int = 0,
    val isVerified: Boolean = false,
    val isPrivate: Boolean = false,
    val isDummy: Boolean = true,
    val uploadStatus: String? = null,
    val interests: List<String> = emptyList(),
    val zonasDeCaza: List<String> = emptyList(),
    val trustScore: Float = 0f,
    val trustLevel: String = "NEW"
)
