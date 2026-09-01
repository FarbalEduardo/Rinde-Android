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
import com.farbalapps.rinde.ui.screen.profile.posts.UserPostsScreen

import androidx.navigation.toRoute

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

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
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(280)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(280)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) }
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
    composable<HomeRoute.GoalDetail>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) {
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
            onNavigateToPosts = { userId, userName -> navController.navigate(HomeRoute.UserPosts(userId, userName)) },
            onNavigateToSaved = { navController.navigate(HomeRoute.SavedPosts) },
            onNavigateToBlocked = { navController.navigate(HomeRoute.BlockedUsers) },
            onLogout = onLogout
        )
    }
    composable<HomeRoute.UserPosts>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) { backStackEntry ->
        val args = backStackEntry.toRoute<HomeRoute.UserPosts>()
        UserPostsScreen(
            userId = args.userId,
            userName = args.userName,
            onBack = { navController.popBackStack() },
            onNavigateToPostDetail = { postId -> navController.navigate(HomeRoute.PostDetail(postId)) },
            onEditPost = { postId -> navController.navigate(HomeRoute.EditPost(postId)) }
        )
    }
    composable<HomeRoute.Settings>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onLogout = onLogout,
            onNavigateToSaved = { navController.navigate(HomeRoute.SavedPosts) },
            onNavigateToBlocked = { navController.navigate(HomeRoute.BlockedUsers) }
        )
    }
    composable<HomeRoute.EditProfile>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) {
        EditProfileScreen(onBack = { navController.popBackStack() })
    }
    composable<HomeRoute.SavedPosts>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) {
        SavedPostsScreen(onBack = { navController.popBackStack() })
    }
    composable<HomeRoute.BlockedUsers>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) {
        BlockedUsersScreen(onBack = { navController.popBackStack() })
    }
    composable<HomeRoute.UserProfile>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) { backStackEntry ->
        val args = backStackEntry.toRoute<HomeRoute.UserProfile>()
        ProfileScreen(
            innerPadding = innerPadding,
            targetUserId = args.userId,
            onBack = { navController.popBackStack() },
            onNavigateToPosts = { userId, userName -> navController.navigate(HomeRoute.UserPosts(userId, userName)) },
            onNavigateToSaved = { navController.navigate(HomeRoute.SavedPosts) },
            onNavigateToBlocked = { navController.navigate(HomeRoute.BlockedUsers) }
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addPostScreens(
    navController: NavHostController
) {
    composable<HomeRoute.CreatePost>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) {
        CreatePostScreen(onBack = { navController.popBackStack() })
    }
    composable<HomeRoute.PostDetail>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) { backStackEntry ->
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
    composable<HomeRoute.EditPost>(
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
    ) { backStackEntry ->
        val args = backStackEntry.toRoute<HomeRoute.EditPost>()
        EditPostScreen(
            postId = args.postId,
            onBack = { navController.popBackStack() }
        )
    }
}
