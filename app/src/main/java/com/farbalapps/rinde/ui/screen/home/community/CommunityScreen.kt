package com.farbalapps.rinde.ui.screen.home.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.farbalapps.rinde.ui.screen.home.community.components.FilterChipRow
import com.farbalapps.rinde.ui.screen.home.community.components.PostCard
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.RindeTheme
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.tooling.preview.Preview
import com.farbalapps.rinde.domain.model.CommunityPost
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToCreatePost: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel(),
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val posts by viewModel.posts.collectAsState()

    CommunityContent(
        currentTab = currentTab,
        posts = posts,
        onTabSelected = { viewModel.setTab(it) },
        onNavigateToCreatePost = onNavigateToCreatePost,
        onLikeClick = { viewModel.toggleLike(it) },
        onSaveClick = { viewModel.toggleSave(it) },
        onLoadMore = { viewModel.loadMore() },
        innerPadding = innerPadding
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityContent(
    currentTab: CommunityTab,
    posts: List<CommunityPost>,
    onTabSelected: (CommunityTab) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onLikeClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    innerPadding: PaddingValues
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val lazyListState = rememberLazyListState()
    
    // FAB visibility logic: hide when scrolling down, show when scrolling up
    val isScrollingDown = remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    
    // We'll use a more sophisticated way to detect direction for the FAB
    var lastScrollOffset by remember { mutableStateOf(0) }
    var lastScrollIndex by remember { mutableStateOf(0) }
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()

            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .offset { IntOffset(0, scrollBehavior.state.heightOffset.roundToInt()) }
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Comunidad",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
                // Filter chips that now move in sync with the top bar
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = if (scrollBehavior.state.contentOffset < -1f) 2.dp else 0.dp
                ) {
                    FilterChipRow(
                        selectedTab = currentTab,
                        onTabSelected = onTabSelected,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = slideInVertically(initialOffsetY = { it * 2 }),
                exit = slideOutVertically(targetOffsetY = { it * 2 })
            ) {
                FloatingActionButton(
                    onClick = onNavigateToCreatePost,
                    containerColor = RindePrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear publicación")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feed Content
            if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay publicaciones en esta sección.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "¡Sé el primero en compartir una oferta!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(posts) { post ->
                    PostCard(
                        username = post.authorName,
                        timeLocation = post.location.name,
                        imageUrl = post.photos.firstOrNull(),
                        title = post.title,
                        description = post.descriptionShort,
                        descriptionLong = post.descriptionLong,
                        isRecommended = post.isRecommended,
                        votes = post.votes,
                        likes = post.likes,
                        commentsCount = post.commentsCount,
                        onLikeClick = { onLikeClick(post.id) },
                        onSaveClick = { onSaveClick(post.id) }
                    )
                }
            }

            // Bottom Spacing and Infinite Scroll trigger
            item {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CommunityScreenPreview() {
    RindeTheme {
        CommunityContent(
            currentTab = CommunityTab.RECENT,
            posts = emptyList(),
            onTabSelected = {},
            onNavigateToCreatePost = {},
            onLikeClick = {},
            onSaveClick = {},
            onLoadMore = {},
            innerPadding = PaddingValues(0.dp)
        )
    }
}
