package com.farbalapps.rinde.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface HomeRoute {
    @Serializable data object List : HomeRoute
    @Serializable data object Community : HomeRoute
    @Serializable data object Goals : HomeRoute
    @Serializable data object Assistant : HomeRoute
    @Serializable data object Profile : HomeRoute
    @Serializable data object EditProfile : HomeRoute
    @Serializable data object SavedPosts : HomeRoute
    @Serializable data object BlockedUsers : HomeRoute
    @Serializable data object CreatePost : HomeRoute
    @Serializable data object Settings : HomeRoute

    @Serializable
    data class UserProfile(val userId: String) : HomeRoute

    @Serializable
    data class PostDetail(
        val postId: String,
        val scrollToComments: Boolean = false,
        val isExpiredNotice: Boolean = false
    ) : HomeRoute

    @Serializable
    data class EditPost(val postId: String) : HomeRoute
}
