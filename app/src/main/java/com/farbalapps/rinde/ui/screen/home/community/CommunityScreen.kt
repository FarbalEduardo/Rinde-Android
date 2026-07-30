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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Notifications
import com.farbalapps.rinde.ui.screen.home.community.components.NotificationsBottomSheet
import com.farbalapps.rinde.ui.screen.home.community.components.SearchContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

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
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.farbalapps.rinde.ui.screen.home.community.components.CommunityTabRow
import com.farbalapps.rinde.ui.screen.home.community.components.PostCard
import com.farbalapps.rinde.ui.screen.home.community.components.PostCardSkeleton
import com.farbalapps.rinde.ui.screen.home.community.components.CommentsBottomSheet
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.RindeTheme
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToPostDetail: (postId: String, scrollToComments: Boolean, isExpiredNotice: Boolean) -> Unit = { _, _, _ -> },
    onEditPost: (String) -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    commentsViewModel: CommentsViewModel = hiltViewModel(),
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val postStatusOverlay by viewModel.postStatusOverlay.collectAsStateWithLifecycle()
    val savedOverlay by viewModel.savedOverlay.collectAsStateWithLifecycle()
    val voteOverlay by viewModel.voteOverlay.collectAsStateWithLifecycle()
    val discoverItems = viewModel.pagedFeed.collectAsLazyPagingItems()
    val hotItems = viewModel.hotPagedFeed.collectAsLazyPagingItems()

    // Observar ciclo de vida para verificar nuevas publicaciones al reanudar la app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkForNewPostsOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    CommunityContent(
        currentTab = uiState.currentTab,
        discoverItems = discoverItems,
        hotItems = hotItems,
        savedPosts = uiState.posts,
        isRefreshing = uiState.isRefreshing,
        isLoading = uiState.isLoading,
        isSavedLoading = uiState.isSavedLoading,
        newPostsCount = uiState.newPostsCount,
        currentUserId = uiState.userId,
        postStatusOverlay = postStatusOverlay,
        savedOverlay = savedOverlay,
        voteOverlay = voteOverlay,
        onRefresh = { viewModel.refresh() },
        onTabSelected = { viewModel.setTab(it) },
        onNavigateToCreatePost = onNavigateToCreatePost,
        onLikeClick = { viewModel.toggleVote(it, 1) },
        onSaveClick = { viewModel.toggleSave(it) },
        onShowNewPosts = { viewModel.showPendingPosts() },
        onPostClick = { postId -> onNavigateToPostDetail(postId, false, false) },
        onCommentClick = { postId ->
            onNavigateToPostDetail(postId, true, false)
        },
        onVoteHot = { postId -> viewModel.toggleVote(postId, 1) },
        onVoteCold = { postId -> viewModel.toggleVote(postId, -1) },
        onDeletePost = { postId, photos -> viewModel.deletePost(postId, photos) },
        onEditPost = onEditPost,
        onMarkExpired = { viewModel.markAsExpired(it) },
        onReportExpired = { postId, title, authorId -> viewModel.reportAsExpired(postId, title, authorId) },
        onMarkAvailable = { viewModel.markAsAvailable(it) },
        onSharePost = { post ->
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "¡Mira esta oferta en Rinde!\n${post.title}\nhttps://rinde.app/post/${post.id}")
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir publicación"))
        },
        onNavigateToPostDetail = onNavigateToPostDetail,
        innerPadding = innerPadding
    )

}

@Composable
fun EmptyFeedState(
    tab: CommunityTab,
    modifier: Modifier = Modifier
) {
    val icon = when (tab) {
        CommunityTab.DISCOVER -> Icons.Default.Explore
        CommunityTab.HOT -> Icons.Default.Whatshot
        CommunityTab.SAVED -> Icons.Default.BookmarkBorder
    }
    val title = when (tab) {
        CommunityTab.DISCOVER -> "Aún no hay ofertas"
        CommunityTab.HOT -> "Sin publicaciones destacadas"
        CommunityTab.SAVED -> "Nada guardado aún"
    }
    val subtitle = when (tab) {
        CommunityTab.DISCOVER -> "Sé el primero en publicar una oferta para la comunidad."
        CommunityTab.HOT -> "Las ofertas con más votos de la comunidad aparecerán aquí."
        CommunityTab.SAVED -> "Guarda las mejores ofertas para acceder a ellas más tarde."
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) + 
                androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp, bottom = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon with subtle pulse animation
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
        }
    }
}

@Composable
fun FeedErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanErrorMessage = remember(errorMessage) {
        val errorLower = errorMessage.lowercase()
        if (errorLower.contains("failed_precondition") ||
            errorLower.contains("index") ||
            errorLower.contains("firestore") ||
            errorLower.contains("firebase") ||
            errorLower.contains("query")) {
            "Ocurrió un problema temporal con el servidor de base de datos. Por favor, vuelve a intentarlo más tarde."
        } else {
            errorMessage
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) +
                androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp, bottom = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon with pulse animation (same pattern as EmptyFeedState)
                val infiniteTransition = rememberInfiniteTransition(label = "error_pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "error_scale"
                )

                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = "No se pudieron cargar las publicaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = cleanErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
                TextButton(
                    onClick = onRetry
                ) {
                    Text(
                        text = "Reintentar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityContent(
    currentTab: CommunityTab,
    discoverItems: LazyPagingItems<CommunityPost>,
    hotItems: LazyPagingItems<CommunityPost>,
    savedPosts: List<CommunityPost>,
    isRefreshing: Boolean,
    isLoading: Boolean,
    isSavedLoading: Boolean,
    newPostsCount: Int = 0,
    currentUserId: String = "",
    postStatusOverlay: Map<String, com.farbalapps.rinde.domain.model.VerificationStatus> = emptyMap(),
    savedOverlay: Map<String, Boolean> = emptyMap(),
    voteOverlay: Map<String, com.farbalapps.rinde.domain.repository.VoteOverlay> = emptyMap(),
    onRefresh: () -> Unit,
    onTabSelected: (CommunityTab) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onLikeClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onPostClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit,
    onVoteHot: (String) -> Unit,
    onVoteCold: (String) -> Unit,
    onShowNewPosts: () -> Unit = {},
    onDeletePost: (String, List<String>) -> Unit = { _, _ -> },
    onEditPost: (String) -> Unit = {},
    onMarkExpired: (String) -> Unit = {},
    onReportExpired: (String, String, String) -> Unit = { _, _, _ -> },
    onMarkAvailable: (String) -> Unit = {},
    onSharePost: (CommunityPost) -> Unit = {},
    onNavigateToPostDetail: (String, Boolean, Boolean) -> Unit = { _, _, _ -> },
    searchViewModel: SearchViewModel = hiltViewModel(),
    notificationsViewModel: NotificationsViewModel = hiltViewModel(),
    innerPadding: PaddingValues
) {

    val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val discoverListState = rememberLazyListState()
    val hotListState = rememberLazyListState()
    val savedListState = rememberLazyListState()

    val lazyListState = when (currentTab) {
        CommunityTab.DISCOVER -> discoverListState
        CommunityTab.HOT -> hotListState
        CommunityTab.SAVED -> savedListState
    }

    LaunchedEffect(currentTab) {
        when (currentTab) {
            CommunityTab.DISCOVER -> { /* Conservar scroll de discover */ }
            CommunityTab.HOT -> { hotListState.scrollToItem(0) }
            CommunityTab.SAVED -> { savedListState.scrollToItem(0) }
        }
    }

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

    // Derivar isRefreshing real de Paging 3
    val isDiscoverRefreshing = currentTab == CommunityTab.DISCOVER && 
            discoverItems.loadState.refresh is androidx.paging.LoadState.Loading
    val isHotRefreshing = currentTab == CommunityTab.HOT && 
            hotItems.loadState.refresh is androidx.paging.LoadState.Loading
    
    val effectiveIsRefreshing = when (currentTab) {
        CommunityTab.DISCOVER -> isRefreshing || isDiscoverRefreshing
        CommunityTab.HOT -> isRefreshing || isHotRefreshing
        CommunityTab.SAVED -> isRefreshing
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

        val pullToRefreshState = rememberPullToRefreshState()

        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Contenido de la lista (capa inferior)
            PullToRefreshBox(
                isRefreshing = effectiveIsRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                contentAlignment = Alignment.TopCenter,
                indicator = {
                    androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = effectiveIsRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = dynamicTopPadding + 16.dp)
                    )
                }
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = dynamicTopPadding,
                        bottom = paddingMedium + 80.dp
                    )
                ) {
                    when (currentTab) {
                        CommunityTab.DISCOVER -> {
                            val loadState = discoverItems.loadState
                            val isDiscoverLoading = loadState.refresh is androidx.paging.LoadState.Loading
                            val isDiscoverError = loadState.refresh is androidx.paging.LoadState.Error || 
                                    loadState.mediator?.refresh is androidx.paging.LoadState.Error
                            val endOfPaginationReached = (loadState.refresh as? androidx.paging.LoadState.NotLoading)?.endOfPaginationReached == true

                            if (isDiscoverError && discoverItems.itemCount == 0) {
                                val error = (loadState.refresh as? androidx.paging.LoadState.Error)?.error
                                    ?: (loadState.mediator?.refresh as? androidx.paging.LoadState.Error)?.error
                                item {
                                    FeedErrorState(
                                        errorMessage = error?.localizedMessage ?: "Error desconocido",
                                        onRetry = { discoverItems.retry() }
                                    )
                                }
                            } else if (discoverItems.itemCount == 0 && (isDiscoverLoading || !endOfPaginationReached)) {
                                items(5) {
                                    PostCardSkeleton(modifier = Modifier.padding(horizontal = paddingMedium / 2, vertical = 2.dp))
                                }
                            } else if (discoverItems.itemCount == 0 && endOfPaginationReached) {
                                item {
                                    EmptyFeedState(tab = CommunityTab.DISCOVER)
                                }
                            } else {
                                items(
                                    count = discoverItems.itemCount,
                                    key = discoverItems.itemKey { it.id }
                                ) { index ->
                                    val post = discoverItems[index]
                                    if (post != null) {
                                        val overriddenStatus = postStatusOverlay[post.id] ?: post.verificationStatus
                                        val overriddenSaved = savedOverlay[post.id] ?: post.isSavedByMe
                                        val voteOver = voteOverlay[post.id]
                                        val finalTruth = if (voteOver != null && post.myVoteValue != voteOver.myVote) (voteOver.truthCount ?: post.truthCount) else post.truthCount
                                        val finalFalse = if (voteOver != null && post.myVoteValue != voteOver.myVote) (voteOver.falseCount ?: post.falseCount) else post.falseCount
                                        val finalMyVote = if (voteOver != null && post.myVoteValue != voteOver.myVote) voteOver.myVote else post.myVoteValue
                                        val finalScore = finalTruth - finalFalse
                                        PostCard(
                                            post = post.copy(
                                                verificationStatus = overriddenStatus,
                                                isSavedByMe = overriddenSaved,
                                                truthCount = finalTruth,
                                                falseCount = finalFalse,
                                                myVoteValue = finalMyVote,
                                                votesScore = finalScore
                                            ),
                                            isAuthorVerified = post.isAuthorVerified,
                                            currentUserId = currentUserId,
                                            onSaveClick = { onSaveClick(post.id) },
                                            onPostClick = { onPostClick(post.id) },
                                            onCommentClick = { onCommentClick(post.id) },
                                            onDeletePost = { onDeletePost(post.id, post.photos) },
                                            onEditPost = { onEditPost(post.id) },
                                            onMarkExpired = { onMarkExpired(post.id) },
                                            onReportExpired = { onReportExpired(post.id, post.title, post.authorId) },
                                            onMarkAvailable = { onMarkAvailable(post.id) },
                                            onSharePost = { onSharePost(post) },
                                            modifier = Modifier.padding(
                                                horizontal = paddingMedium / 2,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }

                                // Render append state indicators
                                when {
                                    loadState.append is androidx.paging.LoadState.Loading -> {
                                        items(2) {
                                            PostCardSkeleton(
                                                modifier = Modifier.padding(
                                                    horizontal = paddingMedium / 2,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                    }
                                    loadState.append is androidx.paging.LoadState.Error -> {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "No se pudieron cargar más ofertas.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    TextButton(onClick = { discoverItems.retry() }) {
                                                        Text("Reintentar")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        CommunityTab.HOT -> {
                            val loadState = hotItems.loadState
                            val isHotLoading = loadState.refresh is androidx.paging.LoadState.Loading
                            val isHotError = loadState.refresh is androidx.paging.LoadState.Error || 
                                    loadState.mediator?.refresh is androidx.paging.LoadState.Error
                            val endOfPaginationReached = (loadState.refresh as? androidx.paging.LoadState.NotLoading)?.endOfPaginationReached == true

                            if (isHotError && hotItems.itemCount == 0) {
                                val error = (loadState.refresh as? androidx.paging.LoadState.Error)?.error
                                    ?: (loadState.mediator?.refresh as? androidx.paging.LoadState.Error)?.error
                                item {
                                    FeedErrorState(
                                        errorMessage = error?.localizedMessage ?: "Error desconocido",
                                        onRetry = { hotItems.retry() }
                                    )
                                }
                            } else if (hotItems.itemCount == 0 && (isHotLoading || !endOfPaginationReached)) {
                                items(5) {
                                    PostCardSkeleton(modifier = Modifier.padding(horizontal = paddingMedium / 2, vertical = 2.dp))
                                }
                            } else if (hotItems.itemCount == 0 && endOfPaginationReached) {
                                item {
                                    EmptyFeedState(tab = CommunityTab.HOT)
                                }
                            } else {
                                items(
                                    count = hotItems.itemCount,
                                    key = hotItems.itemKey { it.id }
                                ) { index ->
                                    val post = hotItems[index]
                                    if (post != null) {
                                        val overriddenStatus = postStatusOverlay[post.id] ?: post.verificationStatus
                                        if (overriddenStatus == com.farbalapps.rinde.domain.model.VerificationStatus.EXPIRED) {
                                            return@items
                                        }
                                        val overriddenSaved = savedOverlay[post.id] ?: post.isSavedByMe
                                        val voteOver = voteOverlay[post.id]
                                        val finalTruth = if (voteOver != null && post.myVoteValue != voteOver.myVote) (voteOver.truthCount ?: post.truthCount) else post.truthCount
                                        val finalFalse = if (voteOver != null && post.myVoteValue != voteOver.myVote) (voteOver.falseCount ?: post.falseCount) else post.falseCount
                                        val finalMyVote = if (voteOver != null && post.myVoteValue != voteOver.myVote) voteOver.myVote else post.myVoteValue
                                        val finalScore = finalTruth - finalFalse
                                        PostCard(
                                            post = post.copy(
                                                verificationStatus = overriddenStatus,
                                                isSavedByMe = overriddenSaved,
                                                truthCount = finalTruth,
                                                falseCount = finalFalse,
                                                myVoteValue = finalMyVote,
                                                votesScore = finalScore
                                            ),
                                            isAuthorVerified = post.isAuthorVerified,
                                            currentUserId = currentUserId,
                                            onSaveClick = { onSaveClick(post.id) },
                                            onPostClick = { onPostClick(post.id) },
                                            onCommentClick = { onCommentClick(post.id) },
                                            onDeletePost = { onDeletePost(post.id, post.photos) },
                                            onEditPost = { onEditPost(post.id) },
                                            onMarkExpired = { onMarkExpired(post.id) },
                                            onReportExpired = { onReportExpired(post.id, post.title, post.authorId) },
                                            onMarkAvailable = { onMarkAvailable(post.id) },
                                            onSharePost = { onSharePost(post) },
                                            modifier = Modifier.padding(
                                                horizontal = paddingMedium / 2,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }

                                // Render append state indicators
                                when {
                                    loadState.append is androidx.paging.LoadState.Loading -> {
                                        items(2) {
                                            PostCardSkeleton(
                                                modifier = Modifier.padding(
                                                    horizontal = paddingMedium / 2,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                    }
                                    loadState.append is androidx.paging.LoadState.Error -> {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "No se pudieron cargar más ofertas.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    TextButton(onClick = { hotItems.retry() }) {
                                                        Text("Reintentar")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        CommunityTab.SAVED -> {
                            if (isSavedLoading) {
                                items(5) {
                                    PostCardSkeleton(modifier = Modifier.padding(horizontal = paddingMedium, vertical = 2.dp))
                                }
                            } else if (savedPosts.isEmpty()) {
                                item {
                                    EmptyFeedState(tab = CommunityTab.SAVED)
                                }
                            } else {
                                items(savedPosts, key = { it.id }) { post ->
                                    val overriddenStatus = postStatusOverlay[post.id] ?: post.verificationStatus
                                    val overriddenSaved = savedOverlay[post.id] ?: post.isSavedByMe
                                    val voteOver = voteOverlay[post.id]
                                    val finalTruth = if (voteOver != null && post.myVoteValue != voteOver.myVote) (voteOver.truthCount ?: post.truthCount) else post.truthCount
                                    val finalFalse = if (voteOver != null && post.myVoteValue != voteOver.myVote) (voteOver.falseCount ?: post.falseCount) else post.falseCount
                                    val finalMyVote = if (voteOver != null && post.myVoteValue != voteOver.myVote) voteOver.myVote else post.myVoteValue
                                    val finalScore = finalTruth - finalFalse
                                    PostCard(
                                        post = post.copy(
                                            verificationStatus = overriddenStatus,
                                            isSavedByMe = overriddenSaved,
                                            truthCount = finalTruth,
                                            falseCount = finalFalse,
                                            myVoteValue = finalMyVote,
                                            votesScore = finalScore
                                        ),
                                        isAuthorVerified = post.isAuthorVerified,
                                        currentUserId = currentUserId,
                                        onSaveClick = { onSaveClick(post.id) },
                                        onPostClick = { onPostClick(post.id) },
                                        onCommentClick = { onCommentClick(post.id) },
                                        onDeletePost = { onDeletePost(post.id, post.photos) },
                                        onEditPost = { onEditPost(post.id) },
                                        onMarkExpired = { onMarkExpired(post.id) },
                                        onReportExpired = { onReportExpired(post.id, post.title, post.authorId) },
                                        onMarkAvailable = { onMarkAvailable(post.id) },
                                        onSharePost = { onSharePost(post) },
                                        modifier = Modifier.padding(
                                            horizontal = paddingMedium / 2,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xxlarge)))
                    }
                }
            }

            // 2. Header (SearchBar + Notifications Bell + Tabs) — zIndex(1) para estar encima de la lista
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
                    var searchActive by remember { mutableStateOf(false) }
                    val searchUiState by searchViewModel.uiState.collectAsStateWithLifecycle()
                    val searchBarHorizontalPadding by animateDpAsState(
                        targetValue = if (searchActive) 0.dp else 16.dp,
                        label = "searchBarHorizontalPadding"
                    )
                    val searchBarHeight by animateDpAsState(
                        targetValue = if (searchActive) 56.dp else 48.dp,
                        label = "searchBarHeight"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = searchBarHorizontalPadding,
                                vertical = if (searchActive) 0.dp else 2.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchBar(
                            query = searchUiState.query,
                            onQueryChange = { searchViewModel.onQueryChange(it) },
                            onSearch = { searchViewModel.onSearchTriggered(it) },
                            active = searchActive,
                            onActiveChange = { active ->
                                searchActive = active
                                if (!active) {
                                    searchViewModel.onQueryChange("")
                                }
                            },
                            placeholder = { 
                                Text(
                                    text = "Buscar ofertas o tiendas...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                ) 
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Search, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp) 
                                ) 
                            },
                            trailingIcon = { 
                                if (searchActive) {
                                    IconButton(onClick = {
                                        searchActive = false
                                        searchViewModel.onQueryChange("")
                                    }) {
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = "Cerrar búsqueda", 
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                // TODO: Implementar menú de 3 puntos (MoreVert) con las siguientes funciones futuras:
                                //   1. "Ordenar por: Más recientes / Más votados / Más comentados"
                                //      → Cambiar el criterio de ordenación del feed con opciones de sort.
                                //   2. "Filtrar por categoría"
                                //      → Filtrar el feed mostrando sólo publicaciones de una categoría específica.
                                //   3. "Ocultar ofertas ya vistas"
                                //      → Marcar posts como vistos y no volver a mostrarlos en el feed principal.
                                //   4. "Configuración del feed"
                                //      → Pantalla de preferencias: tipo de publicaciones, zonas de caza preferidas.
                            },
                            colors = SearchBarDefaults.colors(
                                containerColor = if (searchActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                inputFieldColors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = searchBarHeight, max = if (searchActive) androidx.compose.ui.unit.Dp.Unspecified else 48.dp)
                        ) {
                            SearchContent(
                                uiState = searchUiState,
                                onQueryChange = { searchViewModel.onQueryChange(it) },
                                onCategorySelect = { searchViewModel.onCategorySelect(it) },
                                onSearchTriggered = { searchViewModel.onSearchTriggered(it) },
                                onRemoveRecentSearch = { searchViewModel.removeRecentSearch(it) },
                                onClearRecentSearches = { searchViewModel.clearRecentSearches() },
                                onPostClick = { postId ->
                                    searchActive = false
                                    onPostClick(postId)
                                },
                                onSaveClick = { postId -> onSaveClick(postId) },
                                onCommentClick = { postId ->
                                    searchActive = false
                                    onCommentClick(postId)
                                },
                                currentUserId = currentUserId
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = !searchActive,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                        ) {
                            BadgedBox(
                                badge = {
                                    if (notificationsUiState.unreadCount > 0) {
                                        Badge {
                                            Text(if (notificationsUiState.unreadCount > 99) "99+" else notificationsUiState.unreadCount.toString())
                                        }
                                    }
                                },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                IconButton(onClick = { showNotificationsSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Notificaciones",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    if (!searchActive) {
                        CommunityTabRow(
                            selectedTab = currentTab,
                            onTabSelected = onTabSelected,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Banner animado de nuevas ofertas
                        AnimatedVisibility(
                            visible = newPostsCount >= 1,
                            enter = slideInVertically(initialOffsetY = { -it }),
                            exit = slideOutVertically(targetOffsetY = { -it })
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onShowNewPosts() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Hay $newPostsCount nuevas ofertas",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
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

        if (showNotificationsSheet) {
            NotificationsBottomSheet(
                viewModel = notificationsViewModel,
                onNotificationClick = { postId, scrollToComments, isExpiredNotice ->
                    onNavigateToPostDetail(postId, scrollToComments, isExpiredNotice)
                },
                onDismiss = { showNotificationsSheet = false }
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun CommunityScreenPreview() {
    RindeTheme {
        val emptyDiscoverItems = kotlinx.coroutines.flow.flowOf(androidx.paging.PagingData.empty<CommunityPost>()).collectAsLazyPagingItems()
        val emptyHotItems = kotlinx.coroutines.flow.flowOf(androidx.paging.PagingData.empty<CommunityPost>()).collectAsLazyPagingItems()
        CommunityContent(
            currentTab = CommunityTab.DISCOVER,
            discoverItems = emptyDiscoverItems,
            hotItems = emptyHotItems,
            savedPosts = emptyList(),
            isRefreshing = false,
            isLoading = false,
            isSavedLoading = false,
            newPostsCount = 0,
            onRefresh = {},
            onTabSelected = {},
            onNavigateToCreatePost = {},
            onLikeClick = {},
            onSaveClick = {},
            onCommentClick = {},
            onVoteHot = {},
            onVoteCold = {},
            onShowNewPosts = {},
            innerPadding = PaddingValues(0.dp)
        )
    }
}
