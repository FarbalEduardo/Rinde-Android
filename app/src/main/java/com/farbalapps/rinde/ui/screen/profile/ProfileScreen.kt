package com.farbalapps.rinde.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.ui.screen.home.community.components.PostCard
import com.farbalapps.rinde.ui.screen.profile.components.ProfileHeader
import com.farbalapps.rinde.ui.theme.RindeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onEditProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToBlocked: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar Snackbar cuando cambie el uploadStatus
    LaunchedEffect(uiState.profile?.uploadStatus) {
        uiState.profile?.uploadStatus?.let { status ->
            if (status.isNotEmpty() && status != "OK") {
                snackbarHostState.showSnackbar(
                    message = status,
                    duration = SnackbarDuration.Short
                )
                // Limpiar el estado después de mostrarlo para evitar que reaparezca al recargar la pantalla
                if (status.contains("completada") || status.contains("Error")) {
                    viewModel.clearUploadStatus()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isCurrentUser) {
                TopAppBar(
                    title = { },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        ProfileContent(
            innerPadding = innerPadding,
            uiState = uiState,
            onEditProfile = onEditProfile,
            toggleFollow = { viewModel.toggleFollow() },
            onRetry = { viewModel.retry() }
        )
    }
}

@Composable
fun ProfileContent(
    innerPadding: PaddingValues,
    uiState: ProfileUiState,
    onEditProfile: () -> Unit,
    toggleFollow: () -> Unit,
    onRetry: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Surface(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isLoading && uiState.profile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Surface
        }

        uiState.error?.let { error ->
            ProfileErrorState(error = error, onRetry = onRetry)
            return@Surface
        }

        val profile = uiState.profile
        
        // Resolve strings here in Composable context
        val emptyMyPostsMsg = stringResource(id = R.string.profile_empty_my_posts)
        val emptyUserPostsMsg = stringResource(id = R.string.profile_empty_user_posts)
        val emptySavedMsg = stringResource(id = R.string.profile_empty_saved)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = dimensionResource(id = R.dimen.padding_large))
        ) {
            // Main Header
            item {
                Box(modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_medium))) {
                    ProfileHeader(
                        uiState = uiState,
                        onEditProfile = onEditProfile,
                        toggleFollow = toggleFollow
                    )
                }
            }

            // Tabs Row
            item {
                ProfileTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    isCurrentUser = uiState.isCurrentUser
                )
            }

            // Posts Content
            ProfilePostsContent(
                selectedTab = selectedTab,
                uiState = uiState,
                profile = profile,
                emptyMyPostsMsg = emptyMyPostsMsg,
                emptyUserPostsMsg = emptyUserPostsMsg,
                emptySavedMsg = emptySavedMsg
            )

            item { Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_huge))) }
        }
    }
}

@Composable
fun ProfileErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.profile_btn_retry))
        }
    }
}

@Composable
fun ProfileTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isCurrentUser: Boolean
) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = MaterialTheme.colorScheme.primary
            )
        },
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.GridOn, null, modifier = Modifier.size(20.dp)) }
        )
        if (isCurrentUser) {
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                icon = { Icon(Icons.Default.BookmarkBorder, null, modifier = Modifier.size(20.dp)) }
            )
        }
    }
}

fun LazyListScope.ProfilePostsContent(
    selectedTab: Int,
    uiState: ProfileUiState,
    profile: Profile?,
    emptyMyPostsMsg: String,
    emptyUserPostsMsg: String,
    emptySavedMsg: String
) {
    if (selectedTab == 0) {
        if (uiState.posts.isEmpty()) {
            item {
                EmptyProfileState(
                    message = if (uiState.isCurrentUser) emptyMyPostsMsg else emptyUserPostsMsg,
                    icon = Icons.Default.PostAdd
                )
            }
        } else {
            items(uiState.posts, key = { it.id }) { post ->
                Box(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_medium), vertical = dimensionResource(id = R.dimen.padding_small))) {
                    PostCard(
                        post = post,
                        isAuthorVerified = false, // TODO: Fetch from profile if needed
                        onLikeClick = { /* TODO */ },
                        onSaveClick = { /* TODO */ },
                        onCommentClick = { /* TODO */ },
                        onVoteHot = { /* TODO */ },
                        onVoteCold = { /* TODO */ }
                    )
                }
            }
        }
    } else {
        if (uiState.savedPosts.isEmpty()) {
            item {
                EmptyProfileState(
                    message = emptySavedMsg,
                    icon = Icons.Default.BookmarkBorder
                )
            }
        } else {
            items(uiState.savedPosts, key = { it.id }) { post ->
                Box(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_medium), vertical = dimensionResource(id = R.dimen.padding_small))) {
                    PostCard(
                        post = post,
                        isAuthorVerified = false,
                        onLikeClick = { /* TODO */ },
                        onSaveClick = { /* TODO */ },
                        onCommentClick = { /* TODO */ },
                        onVoteHot = { /* TODO */ },
                        onVoteCold = { /* TODO */ }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyProfileState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
