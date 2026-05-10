package com.farbalapps.rinde.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.farbalapps.rinde.ui.screen.home.community.components.PostCard
import com.farbalapps.rinde.ui.screen.profile.components.ProfileHeader
import com.farbalapps.rinde.ui.theme.RindeTheme
import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.model.Profile

@Composable
fun ProfileScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onEditProfile: () -> Unit = {},
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
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize().padding(innerPadding)
    ) { padding ->
        ProfileContent(
            innerPadding = padding,
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
                    Text("Reintentar carga")
                }
            }
            return@Surface
        }

        val profile = uiState.profile

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Main Header
            item {
                Box(modifier = Modifier.padding(20.dp)) {
                    ProfileHeader(
                        uiState = uiState,
                        onEditProfile = onEditProfile,
                        toggleFollow = toggleFollow
                    )
                }
            }

            // Tabs Row
            item {
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
                        onClick = { selectedTab = 0 },
                        text = { Text("Publicaciones", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.GridOn, null, modifier = Modifier.size(20.dp)) }
                    )
                    if (uiState.isCurrentUser) {
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Guardados", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.BookmarkBorder, null, modifier = Modifier.size(20.dp)) }
                        )
                    }
                }
            }

            // Posts Content
            if (selectedTab == 0) {
                if (uiState.posts.isEmpty()) {
                    item {
                        EmptyProfileState(
                            message = if (uiState.isCurrentUser) "Aún no has publicado nada" else "Este usuario no tiene publicaciones",
                            icon = Icons.Default.PostAdd
                        )
                    }
                } else {
                    items(uiState.posts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            PostCard(
                                username = profile?.name ?: "Usuario",
                                profileImageUrl = profile?.photoUrl,
                                timeLocation = post.timeLocation,
                                imageUrl = post.imageUrl,
                                title = post.title,
                                description = post.description,
                                isRecommended = post.isRecommended,
                                votes = post.votes,
                                likes = post.likes,
                                commentsCount = post.commentsCount
                            )
                        }
                    }
                }
            } else {
                // Future: Implement Saved Posts list
                item {
                    EmptyProfileState(
                        message = "Aquí aparecerán las ofertas que guardes",
                        icon = Icons.Default.BookmarkBorder
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    RindeTheme {
        ProfileContent(
            innerPadding = PaddingValues(0.dp),
            uiState = ProfileUiState(
                profile = Profile(id = "1", name = "Eduardo Farbal", email = "test@test.com"),
                isCurrentUser = true
            ),
            onEditProfile = {},
            toggleFollow = {},
            onRetry = {}
        )
    }
}
