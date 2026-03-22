package com.farbalapps.rinde.ui.navigation

sealed class HomeRoute(val route: String) {
    object List : HomeRoute("home_list")
    object Community : HomeRoute("home_community")
    object Goals : HomeRoute("home_goals")
    object Assistant : HomeRoute("home_assistant")
}
