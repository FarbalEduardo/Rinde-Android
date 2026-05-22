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
import com.farbalapps.rinde.ui.screen.home.community.CreatePostScreen
import com.farbalapps.rinde.ui.screen.home.goals.GoalsScreen
import com.farbalapps.rinde.ui.screen.home.assistant.AssistantScreen
import com.farbalapps.rinde.ui.screen.profile.ProfileScreen
import com.farbalapps.rinde.ui.screen.profile.SettingsScreen
import com.farbalapps.rinde.ui.screen.profile.edit.EditProfileScreen
import com.farbalapps.rinde.ui.screen.profile.extras.SavedPostsScreen
import com.farbalapps.rinde.ui.screen.profile.extras.BlockedUsersScreen

@Composable
fun HomeNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    listViewModel: ListViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute.Community.route,
        modifier = modifier
    ) {
        composable(HomeRoute.List.route) {
            ListScreen(innerPadding = innerPadding, viewModel = listViewModel)
        }
        composable(HomeRoute.Community.route) {
            CommunityScreen(
                innerPadding = innerPadding,
                onNavigateToCreatePost = { navController.navigate(HomeRoute.CreatePost.route) }
            )
        }
        composable(HomeRoute.Goals.route) {
            GoalsScreen(innerPadding = innerPadding)
        }
        composable(HomeRoute.Assistant.route) {
            AssistantScreen(innerPadding = innerPadding)
        }
        composable(HomeRoute.Profile.route) {
            ProfileScreen(
                innerPadding = innerPadding,
                onEditProfile = { navController.navigate(HomeRoute.EditProfile.route) },
                onNavigateToSettings = { navController.navigate(HomeRoute.Settings.route) }
            )
        }
        composable(HomeRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = onLogout,
                onNavigateToSaved = { navController.navigate(HomeRoute.SavedPosts.route) },
                onNavigateToBlocked = { navController.navigate(HomeRoute.BlockedUsers.route) }
            )
        }
        composable(HomeRoute.EditProfile.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(HomeRoute.SavedPosts.route) {
            SavedPostsScreen(onBack = { navController.popBackStack() })
        }
        composable(HomeRoute.BlockedUsers.route) {
            BlockedUsersScreen(onBack = { navController.popBackStack() })
        }
        composable(HomeRoute.CreatePost.route) {
            CreatePostScreen(onBack = { navController.popBackStack() })
        }
    }
}
