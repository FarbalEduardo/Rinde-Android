package com.farbalapps.rinde.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.farbalapps.rinde.ui.screen.home.list.ListScreen
import com.farbalapps.rinde.ui.screen.home.list.ListViewModel
import com.farbalapps.rinde.ui.screen.home.community.CommunityScreen
import com.farbalapps.rinde.ui.screen.home.community.CreatePostScreen
import com.farbalapps.rinde.ui.screen.home.community.EditPostScreen
import com.farbalapps.rinde.ui.screen.home.community.PostDetailScreen
import com.farbalapps.rinde.ui.screen.home.goals.GoalsScreen
import com.farbalapps.rinde.ui.screen.home.goals.detail.GoalDetailScreen
import com.farbalapps.rinde.ui.screen.home.assistant.AssistantScreen
import com.farbalapps.rinde.ui.screen.profile.ProfileScreen
import com.farbalapps.rinde.ui.screen.profile.SettingsScreen
import com.farbalapps.rinde.ui.screen.profile.edit.EditProfileScreen
import com.farbalapps.rinde.ui.screen.profile.extras.SavedPostsScreen
import com.farbalapps.rinde.ui.screen.profile.extras.BlockedUsersScreen

import androidx.navigation.toRoute

@Composable
fun HomeNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    listViewModel: ListViewModel,
    onLogout: () -> Unit,
    // Contador que se incrementa cada vez que el FAB de "Agregar Meta" es pulsado.
    // Usando un Int en lugar de un callback registrador evita la complejidad de tipos de orden superior.
    addGoalTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute.Community,
        modifier = modifier
    ) {
        addListScreen(innerPadding, listViewModel)
        addCommunityScreen(navController, innerPadding)
        addGoalsScreen(navController, innerPadding, addGoalTrigger)
        addAssistantScreen(innerPadding)
        addProfileScreens(navController, innerPadding, onLogout)
        addPostScreens(navController)
    }
}

private fun androidx.navigation.NavGraphBuilder.addListScreen(
    innerPadding: PaddingValues, 
    listViewModel: ListViewModel
) {
    composable<HomeRoute.List> {
        ListScreen(innerPadding = innerPadding, viewModel = listViewModel)
    }
}

private fun androidx.navigation.NavGraphBuilder.addCommunityScreen(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    composable<HomeRoute.Community> {
        CommunityScreen(
            innerPadding = innerPadding,
            onNavigateToCreatePost = { navController.navigate(HomeRoute.CreatePost) },
            onNavigateToUserProfile = { userId -> navController.navigate(HomeRoute.UserProfile(userId)) },
            onNavigateToPostDetail = { postId, scrollToComments, isExpiredNotice ->
                navController.navigate(HomeRoute.PostDetail(postId, scrollToComments, isExpiredNotice))
            },
            onEditPost = { postId -> navController.navigate(HomeRoute.EditPost(postId)) }
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addGoalsScreen(
    navController: NavHostController,
    innerPadding: PaddingValues,
    addGoalTrigger: Int
) {
    composable<HomeRoute.Goals> {
        var showSheet by remember { mutableStateOf(false) }
        LaunchedEffect(addGoalTrigger) {
            if (addGoalTrigger > 0) showSheet = true
        }
        GoalsScreen(
            innerPadding = innerPadding,
            showCreateBottomSheetExternal = showSheet,
            onDismissCreateBottomSheet = { showSheet = false },
            onGoalClick = { goalId -> navController.navigate(HomeRoute.GoalDetail(goalId)) }
        )
    }
    composable<HomeRoute.GoalDetail> {
        GoalDetailScreen(onBack = { navController.popBackStack() })
    }
}

private fun androidx.navigation.NavGraphBuilder.addAssistantScreen(innerPadding: PaddingValues) {
    composable<HomeRoute.Assistant> {
        AssistantScreen(innerPadding = innerPadding)
    }
}

private fun androidx.navigation.NavGraphBuilder.addProfileScreens(
    navController: NavHostController,
    innerPadding: PaddingValues,
    onLogout: () -> Unit
) {
    composable<HomeRoute.Profile> {
        ProfileScreen(
            innerPadding = innerPadding,
            onEditProfile = { navController.navigate(HomeRoute.EditProfile) },
            onNavigateToSettings = { navController.navigate(HomeRoute.Settings) },
            onNavigateToPostDetail = { postId -> navController.navigate(HomeRoute.PostDetail(postId)) },
            onEditPost = { postId -> navController.navigate(HomeRoute.EditPost(postId)) }
        )
    }
    composable<HomeRoute.Settings> {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onLogout = onLogout,
            onNavigateToSaved = { navController.navigate(HomeRoute.SavedPosts) },
            onNavigateToBlocked = { navController.navigate(HomeRoute.BlockedUsers) }
        )
    }
    composable<HomeRoute.EditProfile> {
        EditProfileScreen(onBack = { navController.popBackStack() })
    }
    composable<HomeRoute.SavedPosts> {
        SavedPostsScreen(onBack = { navController.popBackStack() })
    }
    composable<HomeRoute.BlockedUsers> {
        BlockedUsersScreen(onBack = { navController.popBackStack() })
    }
    composable<HomeRoute.UserProfile> { backStackEntry ->
        val args = backStackEntry.toRoute<HomeRoute.UserProfile>()
        ProfileScreen(
            innerPadding = innerPadding,
            targetUserId = args.userId,
            onBack = { navController.popBackStack() },
            onNavigateToPostDetail = { postId -> navController.navigate(HomeRoute.PostDetail(postId)) },
            onEditPost = { postId -> navController.navigate(HomeRoute.EditPost(postId)) }
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addPostScreens(
    navController: NavHostController
) {
    composable<HomeRoute.CreatePost> {
        CreatePostScreen(onBack = { navController.popBackStack() })
    }
    composable<HomeRoute.PostDetail> { backStackEntry ->
        val args = backStackEntry.toRoute<HomeRoute.PostDetail>()
        PostDetailScreen(
            postId = args.postId,
            scrollToComments = args.scrollToComments,
            isExpiredNotice = args.isExpiredNotice,
            onBack = { navController.popBackStack() },
            onAuthorClick = { userId -> navController.navigate(HomeRoute.UserProfile(userId)) },
            onEditPost = { postId -> navController.navigate(HomeRoute.EditPost(postId)) }
        )
    }
    composable<HomeRoute.EditPost> { backStackEntry ->
        val args = backStackEntry.toRoute<HomeRoute.EditPost>()
        EditPostScreen(
            postId = args.postId,
            onBack = { navController.popBackStack() }
        )
    }
}
