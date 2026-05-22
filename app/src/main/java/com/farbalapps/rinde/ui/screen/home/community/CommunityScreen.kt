package com.farbalapps.rinde.ui.screen.home.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.ui.screen.home.community.components.CommunityTabRow
import com.farbalapps.rinde.ui.screen.home.community.components.PostCard
import com.farbalapps.rinde.ui.screen.home.community.components.CommentsBottomSheet
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.RindeTheme
import kotlin.math.roundToInt
import androidx.compose.ui.zIndex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToCreatePost: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel(),
    commentsViewModel: CommentsViewModel = hiltViewModel(),
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var showCommentsSheet by remember { mutableStateOf(false) }
    var activePostId by remember { mutableStateOf<String?>(null) }
    
    val commentsState by commentsViewModel.uiState.collectAsStateWithLifecycle()

    CommunityContent(
        currentTab = currentTab,
        posts = posts,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onTabSelected = { viewModel.setTab(it) },
        onNavigateToCreatePost = onNavigateToCreatePost,
        onLikeClick = { viewModel.toggleLike(it) },
        onSaveClick = { viewModel.toggleSave(it) },
        onLoadMore = { viewModel.loadMore() },
        onCommentClick = { postId ->
            activePostId = postId
            commentsViewModel.loadComments(postId)
            showCommentsSheet = true
        },
        onVoteHot = { postId -> viewModel.toggleVote(postId, 1) },
        onVoteCold = { postId -> viewModel.toggleVote(postId, -1) },
        innerPadding = innerPadding
    )

    if (showCommentsSheet && activePostId != null) {
        CommentsBottomSheet(
            postId = activePostId!!,
            comments = commentsState.comments,
            replies = commentsState.replies,
            onCommentSubmit = { text -> 
                commentsViewModel.onCommentTextChange(text)
                commentsViewModel.submitComment()
            },
            onReplySubmit = { commentId, text ->
                // Handle reply submission logic
            },
            onLikeComment = { commentId -> commentsViewModel.toggleLike(commentId) },
            onLikeReply = { commentId, replyId -> commentsViewModel.toggleReplyLike(commentId, replyId) },
            onLoadReplies = { commentId -> commentsViewModel.loadReplies(commentId) },
            onDismiss = { showCommentsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityContent(
    currentTab: CommunityTab,
    posts: List<CommunityPost>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTabSelected: (CommunityTab) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onLikeClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onVoteHot: (String) -> Unit,
    onVoteCold: (String) -> Unit,
    onLoadMore: () -> Unit,
    innerPadding: PaddingValues
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val lazyListState = rememberLazyListState()

    // Lógica de visibilidad del FAB
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    var lastScrollIndex by remember { mutableIntStateOf(0) }
    var isFabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(lazyListState.firstVisibleItemScrollOffset, lazyListState.firstVisibleItemIndex) {
        val currentIndex = lazyListState.firstVisibleItemIndex
        val currentOffset = lazyListState.firstVisibleItemScrollOffset

        if (currentIndex > lastScrollIndex || (currentIndex == lastScrollIndex && currentOffset > lastScrollOffset)) {
            if (currentOffset > 10) isFabVisible = false
        } else if (currentIndex < lastScrollIndex || (currentIndex == lastScrollIndex && currentOffset < lastScrollOffset)) {
            isFabVisible = true
        }

        lastScrollIndex = currentIndex
        lastScrollOffset = currentOffset
    }

    // Infinite Scroll Logic
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItemIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemsCount = lazyListState.layoutInfo.totalItemsCount
            lastVisibleItemIndex >= totalItemsCount - 2 && totalItemsCount > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    val paddingMedium = dimensionResource(id = R.dimen.padding_medium)
    val spacerHuge = dimensionResource(id = R.dimen.spacer_huge)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Medimos la altura total del header para saber cuánto ocultar
    var headerTotalHeightPx by remember { mutableFloatStateOf(0f) }
    
    SideEffect {
        if (headerTotalHeightPx > 0f && scrollBehavior.state.heightOffsetLimit != -headerTotalHeightPx) {
            scrollBehavior.state.heightOffsetLimit = -headerTotalHeightPx
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        val paddingValues = PaddingValues(0.dp)
        // Calcular el top padding dinámico para la lista
        val dynamicTopPadding = with(density) {
            (statusBarHeight + (headerTotalHeightPx + scrollBehavior.state.heightOffset).toDp())
                .coerceAtLeast(statusBarHeight)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Contenido de la lista (capa inferior)
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = dynamicTopPadding,
                        bottom = paddingMedium + 80.dp
                    )
                ) {
                    // Feed de Publicaciones
                    if (posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = spacerHuge),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(id = R.string.community_empty_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_small)))
                                    Text(
                                        text = stringResource(id = R.string.community_empty_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                isAuthorVerified = post.isAuthorVerified,
                                onLikeClick = { onLikeClick(post.id) },
                                onSaveClick = { onSaveClick(post.id) },
                                onCommentClick = { onCommentClick(post.id) },
                                onVoteHot = { onVoteHot(post.id) },
                                onVoteCold = { onVoteCold(post.id) },
                                modifier = Modifier.padding(horizontal = paddingMedium, vertical = dimensionResource(id = R.dimen.padding_small))
                            )
                        }

                        if (posts.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(paddingMedium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(spacerHuge))
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xxlarge)))
                    }
                }
            }

            // 2. Header (SearchBar + Tabs) — zIndex(1) para estar encima de la lista
            Surface(
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { headerTotalHeightPx = it.size.height.toFloat() }
                    .offset { IntOffset(0, scrollBehavior.state.heightOffset.roundToInt()) }
                    .statusBarsPadding()
                    .clipToBounds(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                    ) {
                            SearchBar(
                                query = "",
                                onQueryChange = {},
                                onSearch = {},
                                active = false,
                                onActiveChange = { },
                                placeholder = { 
                                    Text(
                                        text = stringResource(id = R.string.community_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    ) 
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Search, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp) 
                                    ) 
                                },
                                trailingIcon = { 
                                    IconButton(onClick = { /* TODO */ }) {
                                        Icon(
                                            Icons.Default.MoreVert, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                colors = SearchBarDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    inputFieldColors = TextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) { }
                    }
                    
                    CommunityTabRow(
                        selectedTab = currentTab,
                        onTabSelected = onTabSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. Status Bar Guard — zIndex(2) siempre encima de todo
            // Bloquea visualmente la zona de la barra de estado/notch
            Box(
                modifier = Modifier
                    .zIndex(2f)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CommunityScreenPreview() {
    RindeTheme {
        CommunityContent(
            currentTab = CommunityTab.DISCOVER,
            posts = emptyList(),
            isRefreshing = false,
            onRefresh = {},
            onTabSelected = {},
            onNavigateToCreatePost = {},
            onLikeClick = {},
            onSaveClick = {},
            onCommentClick = {},
            onVoteHot = {},
            onVoteCold = {},
            onLoadMore = {},
            innerPadding = PaddingValues(0.dp)
        )
    }
}
