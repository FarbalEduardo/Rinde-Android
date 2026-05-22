package com.farbalapps.rinde.ui.screen.profile.extras

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.home.community.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    onBack: () -> Unit,
    viewModel: ProfileExtrasViewModel = hiltViewModel()
) {
    val savedPosts by viewModel.savedPosts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_tab_saved)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (savedPosts.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Bookmark,
                text = "No tienes publicaciones guardadas"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(savedPosts) { post ->
                    PostCard(
                        post = post,
                        onSaveClick = { viewModel.unsavePost(post.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onBack: () -> Unit,
    viewModel: ProfileExtrasViewModel = hiltViewModel()
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_item_blocked)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (blockedUsers.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Block,
                text = "No tienes usuarios bloqueados",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                items(blockedUsers) { user ->
                    ListItem(
                        headlineContent = { Text(user.name) },
                        leadingContent = {
                            if (user.photoUrl != null) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null)
                                }
                            }
                        },
                        trailingContent = {
                            TextButton(onClick = { viewModel.unblockUser(user.id) }) {
                                Text("Desbloquear")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = color.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
