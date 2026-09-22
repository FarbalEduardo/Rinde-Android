package com.farbalapps.rinde.ui.screen.profile.posts

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.VoteOverlay
import com.farbalapps.rinde.ui.screen.home.community.components.PostCard
import com.farbalapps.rinde.ui.screen.profile.EmptyProfileState
import com.farbalapps.rinde.ui.screen.profile.ProfileUiState
import com.farbalapps.rinde.ui.screen.profile.ProfileViewModel
import com.farbalapps.rinde.ui.theme.RindeTheme
import androidx.compose.ui.tooling.preview.Preview

private fun sharePost(context: Context, post: CommunityPost) {
    val shareText = context.getString(
        R.string.profile_share_offer_format,
        post.title,
        "https://rinde.app/post/${post.id}"
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.profile_share_chooser_title)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPostsScreen(
    userId: String,
    userName: String = "",
    onBack: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit = {},
    onEditPost: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val postStatusOverlay by viewModel.postStatusOverlay.collectAsStateWithLifecycle()
    val savedStatusOverlay by viewModel.savedStatusOverlay.collectAsStateWithLifecycle()
    val voteStatusOverlay by viewModel.voteStatusOverlay.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isCurrentUser) {
                            stringResource(R.string.profile_tab_posts)
                        } else {
                            if (userName.isNotBlank()) {
                                stringResource(R.string.profile_user_posts_title_format, userName)
                            } else {
                                stringResource(R.string.profile_tab_posts)
                            }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        UserPostsContent(
            innerPadding = padding,
            uiState = uiState,
            postStatusOverlay = postStatusOverlay,
            savedStatusOverlay = savedStatusOverlay,
            voteStatusOverlay = voteStatusOverlay,
            onPostClick = onNavigateToPostDetail,
            onEditPost = onEditPost,
            onToggleSave = { viewModel.toggleSave(it) },
            onDeletePost = { post -> viewModel.deletePost(post.id, post.photos) },
            onMarkExpired = { post -> viewModel.markAsExpired(post.id) },
            onReportExpired = { post -> viewModel.reportAsExpired(post.id, post.title, post.authorId) },
            onMarkAvailable = { post -> viewModel.markAsAvailable(post.id) }
        )
    }
}

@Composable
fun UserPostsContent(
    innerPadding: PaddingValues,
    uiState: ProfileUiState,
    postStatusOverlay: Map<String, VerificationStatus> = emptyMap(),
    savedStatusOverlay: Map<String, Boolean> = emptyMap(),
    voteStatusOverlay: Map<String, VoteOverlay> = emptyMap(),
    onPostClick: (String) -> Unit,
    onEditPost: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onDeletePost: (CommunityPost) -> Unit,
    onMarkExpired: (CommunityPost) -> Unit,
    onReportExpired: (CommunityPost) -> Unit,
    onMarkAvailable: (CommunityPost) -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isLoading && uiState.posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Surface
        }

        if (uiState.posts.isEmpty()) {
            val emptyMsg = if (uiState.isCurrentUser) {
                stringResource(id = R.string.profile_empty_my_posts)
            } else {
                stringResource(id = R.string.profile_empty_user_posts)
            }
            EmptyProfileState(
                message = emptyMsg,
                icon = Icons.Default.PostAdd
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = dimensionResource(id = R.dimen.padding_large)),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(uiState.posts, key = { it.id }) { post ->
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
                        currentUserId = uiState.profile?.id ?: "",
                        onSaveClick = { onToggleSave(post.id) },
                        onPostClick = { onPostClick(post.id) },
                        onEditPost = { onEditPost(post.id) },
                        onDeletePost = { onDeletePost(post) },
                        onMarkExpired = { onMarkExpired(post) },
                        onReportExpired = { onReportExpired(post) },
                        onMarkAvailable = { onMarkAvailable(post) },
                        onSharePost = { sharePost(context, post) },
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserPostsContentPreview() {
    RindeTheme {
        UserPostsContent(
            innerPadding = PaddingValues(0.dp),
            uiState = ProfileUiState(
                profile = com.farbalapps.rinde.domain.model.Profile(
                    id = "user1",
                    name = "Eduardo Farbal",
                    photoUrl = null
                ),
                posts = emptyList(),
                isLoading = false
            ),
            onPostClick = {},
            onEditPost = {},
            onToggleSave = {},
            onDeletePost = {},
            onMarkExpired = {},
            onReportExpired = {},
            onMarkAvailable = {}
        )
    }
}

