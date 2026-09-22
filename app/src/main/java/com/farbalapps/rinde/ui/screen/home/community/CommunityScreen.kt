package com.farbalapps.rinde.ui.screen.home.community

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.VoteOverlay
import com.farbalapps.rinde.ui.screen.home.community.components.CommunityFeedList
import com.farbalapps.rinde.ui.screen.home.community.components.CommunityTopBar
import com.farbalapps.rinde.ui.screen.home.community.components.NotificationsBottomSheet
import com.farbalapps.rinde.ui.theme.RindeTheme
import kotlinx.coroutines.flow.flowOf
import kotlin.math.roundToInt
/** Re-export de EmptyFeedState para retrocompatibilidad con pantallas existentes */
@Composable
fun EmptyFeedState(tab: CommunityTab, modifier: Modifier = Modifier) {
    com.farbalapps.rinde.ui.screen.home.community.components.EmptyFeedState(tab, modifier)
}

/** Re-export de FeedErrorState para retrocompatibilidad con pantallas existentes */
@Composable
fun FeedErrorState(errorMessage: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    com.farbalapps.rinde.ui.screen.home.community.components.FeedErrorState(errorMessage, onRetry, modifier)
}

/**
 * Pantalla principal de Comunidad (Stateful).
 * Inyecta ViewModels, observa ciclos de vida y delega el renderizado al composable puro [CommunityContent].
 *
 * [HU-01] Feed de ofertas, votos y navegación a creación de post.
 */
@Composable
fun CommunityScreen(
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToPostDetail: (postId: String, scrollToComments: Boolean, isExpiredNotice: Boolean) -> Unit = { _, _, _ -> },
    onEditPost: (String) -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    notificationsViewModel: NotificationsViewModel = hiltViewModel(),
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val postStatusOverlay by viewModel.postStatusOverlay.collectAsStateWithLifecycle()
    val savedOverlay by viewModel.savedOverlay.collectAsStateWithLifecycle()
    val voteOverlay by viewModel.voteOverlay.collectAsStateWithLifecycle()
    val discoverItems = viewModel.pagedFeed.collectAsLazyPagingItems()
    val hotItems = viewModel.hotPagedFeed.collectAsLazyPagingItems()

    val searchUiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()

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

    val context = LocalContext.current
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.clearSnackbar()
        }
    }

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
        searchUiState = searchUiState,
        unreadNotificationsCount = notificationsUiState.unreadCount,
        notificationsViewModel = notificationsViewModel,
        onRefresh = { viewModel.refresh() },
        onTabSelected = { viewModel.setTab(it) },
        onSaveClick = { viewModel.toggleSave(it) },
        onShowNewPosts = { viewModel.showPendingPosts() },
        onPostClick = { postId -> onNavigateToPostDetail(postId, false, false) },
        onCommentClick = { postId -> onNavigateToPostDetail(postId, true, false) },
        onDeletePost = { postId, photos -> viewModel.deletePost(postId, photos) },
        onEditPost = onEditPost,
        onMarkExpired = { viewModel.markAsExpired(it) },
        onReportExpired = { postId, title, authorId -> viewModel.reportAsExpired(postId, title, authorId) },
        onMarkAvailable = { viewModel.markAsAvailable(it) },
        onSharePost = { post ->
            val shareText = context.getString(R.string.community_share_subject, post.title, post.id)
            val chooserTitle = context.getString(R.string.community_share_chooser_title)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        },
        onNavigateToUserProfile = onNavigateToUserProfile,
        onQueryChange = { searchViewModel.onQueryChange(it) },
        onSearchTriggered = { searchViewModel.onSearchTriggered(it) },
        onCategorySelect = { searchViewModel.onCategorySelect(it) },
        onRemoveRecentSearch = { searchViewModel.removeRecentSearch(it) },
        onClearRecentSearches = { searchViewModel.clearRecentSearches() },
        onNavigateToPostDetail = onNavigateToPostDetail,
        innerPadding = innerPadding,
        snackbarHostState = snackbarHostState
    )
}

/**
 * Contenido puro y desacoplado de la pantalla de Comunidad (Stateless).
 * 100% testeable con previews sin requerir Hilt ni contextos Android complejos.
 */
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
    newPostsCount: Int,
    currentUserId: String,
    postStatusOverlay: Map<String, VerificationStatus>,
    savedOverlay: Map<String, Boolean>,
    voteOverlay: Map<String, VoteOverlay>,
    searchUiState: SearchUiState,
    unreadNotificationsCount: Int,
    notificationsViewModel: NotificationsViewModel?,
    onRefresh: () -> Unit,
    onTabSelected: (CommunityTab) -> Unit,
    onSaveClick: (String) -> Unit,
    onShowNewPosts: () -> Unit,
    onPostClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onDeletePost: (String, List<String>) -> Unit,
    onEditPost: (String) -> Unit,
    onMarkExpired: (String) -> Unit,
    onReportExpired: (String, String, String) -> Unit,
    onMarkAvailable: (String) -> Unit,
    onSharePost: (CommunityPost) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchTriggered: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onNavigateToPostDetail: (String, Boolean, Boolean) -> Unit,
    innerPadding: PaddingValues,
    snackbarHostState: androidx.compose.material3.SnackbarHostState = remember { androidx.compose.material3.SnackbarHostState() },
    modifier: Modifier = Modifier
) {
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

    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var headerTotalHeightPx by remember { mutableFloatStateOf(0f) }

    SideEffect {
        if (headerTotalHeightPx > 0f && scrollBehavior.state.heightOffsetLimit != -headerTotalHeightPx) {
            scrollBehavior.state.heightOffsetLimit = -headerTotalHeightPx
        }
    }

    val dynamicTopPadding = with(density) {
        (statusBarHeight + (headerTotalHeightPx + scrollBehavior.state.heightOffset).toDp())
            .coerceAtLeast(statusBarHeight)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        // 1. Contenido de la lista
        CommunityFeedList(
            currentTab = currentTab,
            discoverItems = discoverItems,
            hotItems = hotItems,
            savedPosts = savedPosts,
            isRefreshing = isRefreshing,
            isSavedLoading = isSavedLoading,
            lazyListState = lazyListState,
            currentUserId = currentUserId,
            postStatusOverlay = postStatusOverlay,
            savedOverlay = savedOverlay,
            voteOverlay = voteOverlay,
            dynamicTopPadding = dynamicTopPadding,
            innerPadding = innerPadding,
            onRefresh = onRefresh,
            onSaveClick = onSaveClick,
            onPostClick = onPostClick,
            onCommentClick = onCommentClick,
            onDeletePost = onDeletePost,
            onEditPost = onEditPost,
            onMarkExpired = onMarkExpired,
            onReportExpired = onReportExpired,
            onMarkAvailable = onMarkAvailable,
            onSharePost = onSharePost,
            onNavigateToUserProfile = onNavigateToUserProfile
        )

        // 2. Header (SearchBar + Notifications + Tabs)
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
            CommunityTopBar(
                currentTab = currentTab,
                searchUiState = searchUiState,
                unreadNotificationsCount = unreadNotificationsCount,
                newPostsCount = newPostsCount,
                currentUserId = currentUserId,
                onTabSelected = onTabSelected,
                onQueryChange = onQueryChange,
                onSearchTriggered = onSearchTriggered,
                onCategorySelect = onCategorySelect,
                onRemoveRecentSearch = onRemoveRecentSearch,
                onClearRecentSearches = onClearRecentSearches,
                onNotificationsClick = { showNotificationsSheet = true },
                onShowNewPosts = onShowNewPosts,
                onPostClick = onPostClick,
                onSaveClick = onSaveClick,
                onCommentClick = onCommentClick
            )
        }

        // 3. Status Bar Guard
        Box(
            modifier = Modifier
                .zIndex(2f)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.background)
        )

        if (showNotificationsSheet && notificationsViewModel != null) {
            NotificationsBottomSheet(
                viewModel = notificationsViewModel,
                onNotificationClick = { postId, scrollToComments, isExpiredNotice ->
                    onNavigateToPostDetail(postId, scrollToComments, isExpiredNotice)
                },
                onDismiss = { showNotificationsSheet = false }
            )
        }

        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = innerPadding.calculateBottomPadding() + 8.dp)
                .zIndex(3f)
        )
    }
}

@PreviewLightDark
@Composable
private fun CommunityContentPreview() {
    RindeTheme {
        val emptyDiscoverItems = flowOf(PagingData.empty<CommunityPost>()).collectAsLazyPagingItems()
        val emptyHotItems = flowOf(PagingData.empty<CommunityPost>()).collectAsLazyPagingItems()
        CommunityContent(
            currentTab = CommunityTab.DISCOVER,
            discoverItems = emptyDiscoverItems,
            hotItems = emptyHotItems,
            savedPosts = emptyList(),
            isRefreshing = false,
            isLoading = false,
            isSavedLoading = false,
            newPostsCount = 3,
            currentUserId = "user_preview",
            postStatusOverlay = emptyMap(),
            savedOverlay = emptyMap(),
            voteOverlay = emptyMap(),
            searchUiState = SearchUiState(),
            unreadNotificationsCount = 2,
            notificationsViewModel = null,
            onRefresh = {},
            onTabSelected = {},
            onSaveClick = {},
            onShowNewPosts = {},
            onPostClick = {},
            onCommentClick = {},
            onDeletePost = { _, _ -> },
            onEditPost = {},
            onMarkExpired = {},
            onReportExpired = { _, _, _ -> },
            onMarkAvailable = {},
            onSharePost = {},
            onNavigateToUserProfile = {},
            onQueryChange = {},
            onSearchTriggered = {},
            onCategorySelect = {},
            onRemoveRecentSearch = {},
            onClearRecentSearches = {},
            onNavigateToPostDetail = { _, _, _ -> },
            innerPadding = PaddingValues(0.dp)
        )
    }
}
