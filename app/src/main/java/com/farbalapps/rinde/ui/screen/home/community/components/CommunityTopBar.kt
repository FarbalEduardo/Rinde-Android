package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.home.community.CommunityTab
import com.farbalapps.rinde.ui.screen.home.community.SearchUiState
import com.farbalapps.rinde.ui.theme.RindeTheme

/**
 * Encabezado de la pantalla de Comunidad, incluyendo barra de búsqueda M3 integrada,
 * botón de notificaciones con badge, tabs de filtrado (Descubrir, Hot, Guardados)
 * y banner de nuevos posts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityTopBar(
    currentTab: CommunityTab,
    searchUiState: SearchUiState,
    unreadNotificationsCount: Int,
    newPostsCount: Int,
    currentUserId: String,
    onTabSelected: (CommunityTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchTriggered: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onNotificationsClick: () -> Unit,
    onShowNewPosts: () -> Unit,
    onPostClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchActive by remember { mutableStateOf(false) }

    val searchBarHorizontalPadding by animateDpAsState(
        targetValue = if (searchActive) 0.dp else 16.dp,
        label = "searchBarHorizontalPadding"
    )
    val searchBarHeight by animateDpAsState(
        targetValue = if (searchActive) 56.dp else 48.dp,
        label = "searchBarHeight"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = searchBarHorizontalPadding,
                    vertical = if (searchActive) 0.dp else 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Suppress("DEPRECATION")
            SearchBar(
                query = searchUiState.query,
                onQueryChange = onQueryChange,
                onSearch = onSearchTriggered,
                active = searchActive,
                onActiveChange = { active ->
                    searchActive = active
                    if (!active) {
                        onQueryChange("")
                    }
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.community_search_hint),
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
                            onQueryChange("")
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.community_close_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = SearchBarDefaults.colors(
                    containerColor = if (searchActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                    inputFieldColors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = searchBarHeight, max = if (searchActive) Dp.Unspecified else 48.dp)
            ) {
                SearchContent(
                    uiState = searchUiState,
                    onQueryChange = onQueryChange,
                    onCategorySelect = onCategorySelect,
                    onSearchTriggered = onSearchTriggered,
                    onRemoveRecentSearch = onRemoveRecentSearch,
                    onClearRecentSearches = onClearRecentSearches,
                    onPostClick = { postId ->
                        searchActive = false
                        onPostClick(postId)
                    },
                    onSaveClick = onSaveClick,
                    onCommentClick = { postId ->
                        searchActive = false
                        onCommentClick(postId)
                    },
                    currentUserId = currentUserId
                )
            }

            AnimatedVisibility(
                visible = !searchActive,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationsCount > 0) {
                            Badge {
                                Text(if (unreadNotificationsCount > 99) "99+" else unreadNotificationsCount.toString())
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.community_notif_title),
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

            NewPostsBanner(
                newPostsCount = newPostsCount,
                onClick = onShowNewPosts
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CommunityTopBarPreview() {
    RindeTheme {
        CommunityTopBar(
            currentTab = CommunityTab.DISCOVER,
            searchUiState = SearchUiState(),
            unreadNotificationsCount = 3,
            newPostsCount = 2,
            currentUserId = "user1",
            onTabSelected = {},
            onQueryChange = {},
            onSearchTriggered = {},
            onCategorySelect = {},
            onRemoveRecentSearch = {},
            onClearRecentSearches = {},
            onNotificationsClick = {},
            onShowNewPosts = {},
            onPostClick = {},
            onSaveClick = {},
            onCommentClick = {}
        )
    }
}
