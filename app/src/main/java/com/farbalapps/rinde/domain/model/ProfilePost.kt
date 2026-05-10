package com.farbalapps.rinde.domain.model

data class ProfilePost(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val timeLocation: String = "",
    val imageUrl: String? = null,
    val isRecommended: Boolean = false,
    val votes: Int = 0,
    val likes: Int = 0,
    val commentsCount: Int = 0
)
