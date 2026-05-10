package com.farbalapps.rinde.ui.navigation

sealed class HomeRoute(val route: String) {
    object List : HomeRoute("home_list")
    object Community : HomeRoute("home_community")
    object Goals : HomeRoute("home_goals")
    object Assistant : HomeRoute("home_assistant")
    object Profile : HomeRoute("home_profile")
    object EditProfile : HomeRoute("home_edit_profile")
    object SavedPosts : HomeRoute("saved_posts")
    object BlockedUsers : HomeRoute("blocked_users")
    object CreatePost : HomeRoute("create_post")
    object Settings : HomeRoute("home_settings")
}
