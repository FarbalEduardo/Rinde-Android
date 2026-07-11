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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.ui.screen.home.community.components.PostCard
import com.farbalapps.rinde.ui.screen.profile.components.ProfileHeader
import com.farbalapps.rinde.ui.theme.RindeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    targetUserId: String? = null,
    onBack: (() -> Unit)? = null,
    onEditProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToBlocked: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    onEditPost: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val postStatusOverlay by viewModel.postStatusOverlay.collectAsStateWithLifecycle()
    val savedStatusOverlay by viewModel.savedStatusOverlay.collectAsStateWithLifecycle()
    val voteStatusOverlay by viewModel.voteStatusOverlay.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(targetUserId) {
        viewModel.loadProfile(targetUserId)
    }

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

    // Snackbar para otras acciones (eliminar, reportar)
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    ProfileScreenContent(
        uiState = uiState,
        postStatusOverlay = postStatusOverlay,
        savedStatusOverlay = savedStatusOverlay,
        voteStatusOverlay = voteStatusOverlay,
        snackbarHostState = snackbarHostState,
        onBack = { onBack?.invoke() },
        onEditProfile = onEditProfile,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToSaved = onNavigateToSaved,
        onNavigateToBlocked = onNavigateToBlocked,
        onLogout = onLogout,
        onTabSelected = { /* noop */ },
        onVote = { postId, vote -> viewModel.toggleVote(postId, vote) },
        onToggleSave = { viewModel.toggleSave(it) },
        onPostClick = onNavigateToPostDetail,
        onEditPost = onEditPost,
        onDeletePost = { post -> viewModel.deletePost(post.id, post.photos) },
        onMarkExpired = { post -> viewModel.markAsExpired(post.id) },
        onReportExpired = { post -> viewModel.reportAsExpired(post.id, post.title, post.authorId) },
        onMarkAvailable = { post -> viewModel.markAsAvailable(post.id) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    postStatusOverlay: Map<String, VerificationStatus>,
    savedStatusOverlay: Map<String, Boolean>,
    voteStatusOverlay: Map<String, com.farbalapps.rinde.domain.repository.VoteOverlay>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onLogout: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onVote: (String, Int) -> Unit,
    onToggleSave: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onEditPost: (String) -> Unit,
    onDeletePost: (CommunityPost) -> Unit,
    onMarkExpired: (CommunityPost) -> Unit,
    onReportExpired: (CommunityPost) -> Unit,
    onMarkAvailable: (CommunityPost) -> Unit
) {
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
            } else {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            innerPadding = padding,
            uiState = uiState,
            postStatusOverlay = postStatusOverlay,
            savedStatusOverlay = savedStatusOverlay,
            voteStatusOverlay = voteStatusOverlay,
            onEditProfile = onEditProfile,
            onRetry = { },
            onToggleSave = onToggleSave,
            onToggleVote = onVote,
            onPostClick = onPostClick,
            onEditPost = onEditPost,
            onDeletePost = onDeletePost,
            onMarkExpired = onMarkExpired,
            onReportExpired = onReportExpired,
            onMarkAvailable = onMarkAvailable
        )
    }
}

@Composable
fun ProfileContent(
    innerPadding: PaddingValues,
    uiState: ProfileUiState,
    postStatusOverlay: Map<String, com.farbalapps.rinde.domain.model.VerificationStatus> = emptyMap(),
    savedStatusOverlay: Map<String, Boolean> = emptyMap(),
    voteStatusOverlay: Map<String, com.farbalapps.rinde.domain.repository.VoteOverlay> = emptyMap(),
    onEditProfile: () -> Unit,
    onRetry: () -> Unit,
    onToggleSave: (String) -> Unit = {},
    onToggleVote: (String, Int) -> Unit = { _, _ -> },
    onPostClick: (String) -> Unit = {},
    onEditPost: (String) -> Unit = {},
    onDeletePost: (com.farbalapps.rinde.domain.model.CommunityPost) -> Unit = {},
    onMarkExpired: (com.farbalapps.rinde.domain.model.CommunityPost) -> Unit = {},
    onReportExpired: (com.farbalapps.rinde.domain.model.CommunityPost) -> Unit = {},
    onMarkAvailable: (com.farbalapps.rinde.domain.model.CommunityPost) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
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
                        onShareProfile = {
                            profile?.let { p ->
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "¡Mira el perfil de ${p.name} en Rinde! Mi usuario: @${p.name}"
                                    )
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir perfil"))
                            }
                        }
                    )
                }
            }

            // Posts Content
            ProfilePostsContent(
                uiState = uiState,
                postStatusOverlay = postStatusOverlay,
                savedStatusOverlay = savedStatusOverlay,
                voteStatusOverlay = voteStatusOverlay,
                profile = profile,
                emptyMyPostsMsg = emptyMyPostsMsg,
                emptyUserPostsMsg = emptyUserPostsMsg,
                onToggleSave = onToggleSave,
                onToggleVote = onToggleVote,
                onPostClick = onPostClick,
                onEditPost = onEditPost,
                onDeletePost = onDeletePost,
                onMarkExpired = onMarkExpired,
                onReportExpired = onReportExpired,
                onMarkAvailable = onMarkAvailable
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

fun LazyListScope.ProfilePostsContent(
    uiState: ProfileUiState,
    postStatusOverlay: Map<String, com.farbalapps.rinde.domain.model.VerificationStatus> = emptyMap(),
    savedStatusOverlay: Map<String, Boolean> = emptyMap(),
    voteStatusOverlay: Map<String, com.farbalapps.rinde.domain.repository.VoteOverlay> = emptyMap(),
    profile: Profile?,
    emptyMyPostsMsg: String,
    emptyUserPostsMsg: String,
    onToggleSave: (String) -> Unit = {},
    onToggleVote: (String, Int) -> Unit = { _, _ -> },
    onPostClick: (String) -> Unit = {},
    onEditPost: (String) -> Unit = {},
    onDeletePost: (com.farbalapps.rinde.domain.model.CommunityPost) -> Unit = {},
    onMarkExpired: (com.farbalapps.rinde.domain.model.CommunityPost) -> Unit = {},
    onReportExpired: (com.farbalapps.rinde.domain.model.CommunityPost) -> Unit = {},
    onMarkAvailable: (com.farbalapps.rinde.domain.model.CommunityPost) -> Unit = {}
) {
    if (uiState.posts.isEmpty()) {
        item {
            EmptyProfileState(
                message = if (uiState.isCurrentUser) emptyMyPostsMsg else emptyUserPostsMsg,
                icon = Icons.Default.PostAdd
            )
        }
    } else {
        items(uiState.posts, key = { it.id }) { post ->
            val context = androidx.compose.ui.platform.LocalContext.current
            val overriddenStatus = postStatusOverlay[post.id] ?: post.verificationStatus
            val overriddenSaved = savedStatusOverlay[post.id] ?: post.isSavedByMe
            val voteOver = voteStatusOverlay[post.id]
            val finalTruth = voteOver?.truthCount ?: post.truthCount
            val finalFalse = voteOver?.falseCount ?: post.falseCount
            val finalMyVote = voteOver?.myVote ?: post.myVoteValue
            val finalScore = if (voteOver != null) (finalTruth - finalFalse) else post.votesScore
            PostCard(
                post = post.copy(
                    verificationStatus = overriddenStatus,
                    isSavedByMe = overriddenSaved,
                    truthCount = finalTruth,
                    falseCount = finalFalse,
                    myVoteValue = finalMyVote,
                    votesScore = finalScore
                ),
                isAuthorVerified = false,
                currentUserId = profile?.id ?: "",
                onSaveClick = { onToggleSave(post.id) },
                onPostClick = { onPostClick(post.id) },
                onEditPost = { onEditPost(post.id) },
                onDeletePost = { onDeletePost(post) },
                onMarkExpired = { onMarkExpired(post) },
                onReportExpired = { onReportExpired(post) },
                onMarkAvailable = { onMarkAvailable(post) },
                onSharePost = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "¡Mira esta oferta en Rinde!\n${post.title}\nhttps://rinde.app/post/${post.id}")
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir publicación"))
                },
                modifier = Modifier.padding(vertical = 1.dp)
            )
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

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val dummyProfile = Profile(
        id = "dummy_user",
        name = "Eduardo Farbal",
        email = "eduardo@example.com",
        photoUrl = null,
        followersCount = 142,
        followingCount = 98,
        postsCount = 12,
        rating = 4.7f,
        reviewsCount = 23,
        isPrivate = false,
        isDummy = false
    )
    RindeTheme {
        Surface {
            ProfileScreenContent(
                uiState = ProfileUiState(
                    profile = dummyProfile,
                    posts = emptyList(),
                    isCurrentUser = true
                ),
                postStatusOverlay = emptyMap(),
                savedStatusOverlay = emptyMap(),
                voteStatusOverlay = emptyMap(),
                snackbarHostState = remember { SnackbarHostState() },
                onBack = {},
                onEditProfile = {},
                onNavigateToSettings = {},
                onNavigateToSaved = {},
                onNavigateToBlocked = {},
                onLogout = {},
                onTabSelected = {},
                onVote = { _, _ -> },
                onToggleSave = {},
                onPostClick = {},
                onEditPost = {},
                onDeletePost = {},
                onMarkExpired = {},
                onReportExpired = {},
                onMarkAvailable = {}
            )
        }
    }
}
