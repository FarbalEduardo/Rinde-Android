package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.VoteOverlay
import com.farbalapps.rinde.ui.screen.home.community.CommunityTab

/**
 * Lista de ofertas paginadas/cacheadas de la comunidad.
 * Maneja estados de carga, error, vacío y soporte de Pull-to-refresh para cada pestaña.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedList(
    currentTab: CommunityTab,
    discoverItems: LazyPagingItems<CommunityPost>,
    hotItems: LazyPagingItems<CommunityPost>,
    savedPosts: List<CommunityPost>,
    isRefreshing: Boolean,
    isSavedLoading: Boolean,
    lazyListState: LazyListState,
    currentUserId: String,
    postStatusOverlay: Map<String, VerificationStatus>,
    savedOverlay: Map<String, Boolean>,
    voteOverlay: Map<String, VoteOverlay>,
    dynamicTopPadding: Dp,
    innerPadding: PaddingValues,
    onRefresh: () -> Unit,
    onSaveClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onDeletePost: (String, List<String>) -> Unit,
    onEditPost: (String) -> Unit,
    onMarkExpired: (String) -> Unit,
    onReportExpired: (String, String, String) -> Unit,
    onMarkAvailable: (String) -> Unit,
    onSharePost: (CommunityPost) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val paddingMedium = dimensionResource(id = R.dimen.padding_medium)
    val pullToRefreshState = rememberPullToRefreshState()

    val isDiscoverRefreshing = currentTab == CommunityTab.DISCOVER &&
            discoverItems.loadState.refresh is LoadState.Loading
    val isHotRefreshing = currentTab == CommunityTab.HOT &&
            hotItems.loadState.refresh is LoadState.Loading

    val effectiveIsRefreshing = when (currentTab) {
        CommunityTab.DISCOVER -> isRefreshing || isDiscoverRefreshing
        CommunityTab.HOT -> isRefreshing || isHotRefreshing
        CommunityTab.SAVED -> isRefreshing
    }

    PullToRefreshBox(
        isRefreshing = effectiveIsRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding()),
        contentAlignment = Alignment.TopCenter,
        indicator = {
            PullToRefreshDefaults.Indicator(
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
                    val isDiscoverLoading = loadState.refresh is LoadState.Loading ||
                            loadState.mediator?.refresh is LoadState.Loading ||
                            loadState.source.refresh is LoadState.Loading
                    val isDiscoverError = (loadState.refresh is LoadState.Error ||
                            loadState.mediator?.refresh is LoadState.Error) && discoverItems.itemCount == 0
                    val endOfPaginationReached = (loadState.refresh as? LoadState.NotLoading)?.endOfPaginationReached == true
                    val isDiscoverTrulyEmpty = discoverItems.itemCount == 0 &&
                            !isDiscoverLoading &&
                            !isDiscoverError &&
                            endOfPaginationReached &&
                            loadState.mediator?.refresh !is LoadState.Loading

                    if (isDiscoverError) {
                        val error = (loadState.refresh as? LoadState.Error)?.error
                            ?: (loadState.mediator?.refresh as? LoadState.Error)?.error
                        item {
                            FeedErrorState(
                                errorMessage = error?.localizedMessage ?: "Error desconocido",
                                onRetry = { discoverItems.retry() }
                            )
                        }
                    } else if (discoverItems.itemCount == 0 && !isDiscoverTrulyEmpty) {
                        items(5) {
                            PostCardSkeleton(modifier = Modifier.padding(horizontal = paddingMedium / 2, vertical = 2.dp))
                        }
                    } else if (isDiscoverTrulyEmpty) {
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
                                    onAuthorClick = { onNavigateToUserProfile(post.authorId) },
                                    modifier = Modifier.padding(
                                        horizontal = paddingMedium / 2,
                                        vertical = 2.dp
                                    )
                                )
                            }
                        }

                        when {
                            loadState.append is LoadState.Loading -> {
                                items(2) {
                                    PostCardSkeleton(
                                        modifier = Modifier.padding(
                                            horizontal = paddingMedium / 2,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                            loadState.append is LoadState.Error -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = stringResource(R.string.community_error_append_failed),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            TextButton(onClick = { discoverItems.retry() }) {
                                                Text(stringResource(R.string.community_btn_retry))
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
                    val isHotLoading = loadState.refresh is LoadState.Loading ||
                            loadState.mediator?.refresh is LoadState.Loading ||
                            loadState.source.refresh is LoadState.Loading
                    val isHotError = (loadState.refresh is LoadState.Error ||
                            loadState.mediator?.refresh is LoadState.Error) && hotItems.itemCount == 0
                    val isHotEndOfPagination = (loadState.refresh as? LoadState.NotLoading)?.endOfPaginationReached == true
                    val isHotTrulyEmpty = hotItems.itemCount == 0 &&
                            !isHotLoading &&
                            !isHotError &&
                            isHotEndOfPagination &&
                            loadState.mediator?.refresh !is LoadState.Loading

                    if (isHotError) {
                        val error = (loadState.refresh as? LoadState.Error)?.error
                            ?: (loadState.mediator?.refresh as? LoadState.Error)?.error
                        item {
                            FeedErrorState(
                                errorMessage = error?.localizedMessage ?: "Error desconocido",
                                onRetry = { hotItems.retry() }
                            )
                        }
                    } else if (hotItems.itemCount == 0 && !isHotTrulyEmpty) {
                        items(5) {
                            PostCardSkeleton(modifier = Modifier.padding(horizontal = paddingMedium / 2, vertical = 2.dp))
                        }
                    } else if (isHotTrulyEmpty) {
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
                                if (overriddenStatus == VerificationStatus.EXPIRED) {
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
                                    onAuthorClick = { onNavigateToUserProfile(post.authorId) },
                                    modifier = Modifier.padding(
                                        horizontal = paddingMedium / 2,
                                        vertical = 2.dp
                                    )
                                )
                            }
                        }

                        when {
                            loadState.append is LoadState.Loading -> {
                                items(2) {
                                    PostCardSkeleton(
                                        modifier = Modifier.padding(
                                            horizontal = paddingMedium / 2,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                            loadState.append is LoadState.Error -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = stringResource(R.string.community_error_append_failed),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            TextButton(onClick = { hotItems.retry() }) {
                                                Text(stringResource(R.string.community_btn_retry))
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
                                onAuthorClick = { onNavigateToUserProfile(post.authorId) },
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
}
