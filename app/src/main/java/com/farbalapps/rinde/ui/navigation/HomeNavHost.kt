package com.farbalapps.rinde.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.farbalapps.rinde.ui.screen.home.list.ListScreen
import com.farbalapps.rinde.ui.screen.home.list.ListViewModel
import com.farbalapps.rinde.ui.screen.home.community.CommunityScreen
import com.farbalapps.rinde.ui.screen.home.goals.GoalsScreen
import com.farbalapps.rinde.ui.screen.home.assistant.AssistantScreen
import com.farbalapps.rinde.ui.screen.profile.ProfileScreen

@Composable
fun HomeNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    listViewModel: ListViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute.List.route,
        modifier = modifier
    ) {
        composable(HomeRoute.List.route) {
            ListScreen(innerPadding = innerPadding, viewModel = listViewModel)
        }
        composable(HomeRoute.Community.route) {
            CommunityScreen(innerPadding = innerPadding)
        }
        composable(HomeRoute.Goals.route) {
            GoalsScreen(innerPadding = innerPadding)
        }
        composable(HomeRoute.Assistant.route) {
            AssistantScreen(innerPadding = innerPadding)
        }
        composable(HomeRoute.Profile.route) {
            ProfileScreen(innerPadding = innerPadding)
        }
    }
}
