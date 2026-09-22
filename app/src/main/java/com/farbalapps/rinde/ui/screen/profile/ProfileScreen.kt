package com.farbalapps.rinde.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.data.local.ThemeMode
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.ui.screen.profile.components.*
import com.farbalapps.rinde.ui.theme.RindeTheme

data class ProfileActions(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onEditProfile: () -> Unit = {},
    val onNavigateToPosts: (userId: String, userName: String) -> Unit = { _, _ -> },
    val onNavigateToSaved: () -> Unit = {},
    val onNavigateToBlocked: () -> Unit = {},
    val onNavigateToAbout: () -> Unit = {},
    val onNavigateToLegal: (initialTab: Int) -> Unit = {},
    val onLogout: () -> Unit = {},
    val onSetTheme: (ThemeMode) -> Unit = {},
    val onTogglePrivacy: (Boolean) -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    targetUserId: String? = null,
    onBack: (() -> Unit)? = null,
    onEditProfile: () -> Unit = {},
    onNavigateToPosts: (userId: String, userName: String) -> Unit = { _, _ -> },
    onNavigateToSaved: () -> Unit = {},
    onNavigateToBlocked: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToLegal: (initialTab: Int) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isBunkerMode by viewModel.isBunkerMode.collectAsStateWithLifecycle()
    val isProfilePrivate by viewModel.isProfilePrivate.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(targetUserId) {
        viewModel.loadProfile(targetUserId)
    }

    LaunchedEffect(uiState.profile?.uploadStatus) {
        uiState.profile?.uploadStatus?.let { status ->
            if (status.isNotEmpty() && status != "OK") {
                snackbarHostState.showSnackbar(
                    message = status,
                    duration = SnackbarDuration.Short
                )
                if (status.contains("completada") || status.contains("Error")) {
                    viewModel.clearUploadStatus()
                }
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.clearSnackbar()
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showThemeSheet) {
        ThemeSelectorSheet(
            currentTheme = themeMode,
            onThemeSelected = { viewModel.setTheme(it) },
            onDismiss = { showThemeSheet = false }
        )
    }

    val actions = remember(viewModel, onBack, onEditProfile, onNavigateToPosts, onNavigateToSaved, onNavigateToBlocked, onNavigateToAbout, onNavigateToLegal, onLogout) {
        ProfileActions(
            onBack = { onBack?.invoke() },
            onRefresh = { viewModel.refreshProfile() },
            onEditProfile = onEditProfile,
            onNavigateToPosts = onNavigateToPosts,
            onNavigateToSaved = onNavigateToSaved,
            onNavigateToBlocked = onNavigateToBlocked,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToLegal = onNavigateToLegal,
            onLogout = { showLogoutDialog = true },
            onSetTheme = { viewModel.setTheme(it) },
            onTogglePrivacy = { viewModel.togglePrivacy(it) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!uiState.isCurrentUser) {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.profile?.name ?: stringResource(R.string.home_tab_profile),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = actions.onBack) {
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
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val calculatedTopPadding = if (!uiState.isCurrentUser) {
            padding.calculateTopPadding()
        } else {
            statusBarTop + 8.dp
        }
        ProfileContent(
            innerPadding = PaddingValues(
                top = calculatedTopPadding,
                bottom = innerPadding.calculateBottomPadding() 
            ),
            uiState = uiState,
            currentTheme = themeMode,
            isBunkerMode = isBunkerMode,
            isPrivate = isProfilePrivate,
            onShowThemeSheet = { showThemeSheet = true },
            onToggleBunkerMode = { viewModel.toggleBunkerMode(it) },
            onShareApp = {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, context.getString(R.string.settings_share_text))
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
            },
            onRateApp = {
                runCatching {
                    val uri = android.net.Uri.parse("market://details?id=${context.packageName}")
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                }.onFailure {
                    val uri = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                }
            },
            actions = actions
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    innerPadding: PaddingValues,
    uiState: ProfileUiState,
    currentTheme: ThemeMode,
    isBunkerMode: Boolean,
    isPrivate: Boolean,
    onShowThemeSheet: () -> Unit,
    onToggleBunkerMode: (Boolean) -> Unit,
    onShareApp: () -> Unit,
    onRateApp: () -> Unit,
    actions: ProfileActions
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isLoading && uiState.profile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Surface
        }

        uiState.error?.let { error ->
            ProfileErrorState(
                error = error.asString(context), 
                onRetry = { actions.onRefresh() }
            )
            return@Surface
        }

        val profile = uiState.profile
        val targetUid = profile?.id ?: ""
        val targetName = profile?.name ?: ""

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { actions.onRefresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp
                )
            ) {
                // 1. Tarjeta Principal de Identidad / Perfil (Header Hero Card)
                item {
                    ProfileHeaderCard(
                        uiState = uiState,
                        onEditProfile = actions.onEditProfile
                    )
                }

                if (uiState.isCurrentUser) {
                    // 2. Sección: Actividad y Comunidad
                    item {
                        ProfileSectionTitle(title = stringResource(R.string.settings_section_activity_community))
                    }
                    item {
                        ProfileGroupCard {
                            ProfileGroupItem(
                                icon = Icons.Default.LocalOffer,
                                title = stringResource(R.string.profile_tab_posts),
                                value = (profile?.postsCount ?: 0).toString(),
                                onClick = { actions.onNavigateToPosts(targetUid, targetName) }
                            )
                            ProfileGroupItem(
                                icon = Icons.Default.BookmarkBorder,
                                title = stringResource(R.string.profile_tab_saved),
                                showDivider = false,
                                onClick = actions.onNavigateToSaved
                            )
                        }
                    }

                    // 3. Sección: Cuenta y Seguridad
                    item {
                        ProfileSectionTitle(title = stringResource(R.string.settings_section_privacy))
                    }
                    item {
                        ProfileGroupCard {
                            ProfileGroupItem(
                                icon = if (isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                                title = stringResource(R.string.settings_item_privacy_label),
                                subtitle = if (isPrivate) {
                                    stringResource(R.string.settings_item_privacy_private_desc)
                                } else {
                                    stringResource(R.string.settings_item_privacy_public_desc)
                                },
                                showDivider = true,
                                trailingContent = {
                                    Switch(
                                        checked = isPrivate,
                                        onCheckedChange = actions.onTogglePrivacy
                                    )
                                }
                            )
                            ProfileGroupItem(
                                icon = Icons.Default.Block,
                                title = stringResource(R.string.profile_blocked_users),
                                subtitle = stringResource(R.string.profile_blocked_users_desc),
                                showDivider = false,
                                onClick = actions.onNavigateToBlocked
                            )
                        }
                    }

                    // 4. Sección: Aplicación y Medios
                    item {
                        ProfileSectionTitle(title = stringResource(R.string.settings_section_app))
                    }
                    item {
                        ProfileGroupCard {
                            val themeText = when (currentTheme) {
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            }
                            ProfileGroupItem(
                                icon = Icons.Default.Palette,
                                title = stringResource(R.string.settings_item_theme),
                                value = themeText,
                                showDivider = false,
                                onClick = onShowThemeSheet
                            )
                        }
                    }

                    // 5. Sección: Comunidad y Soporte
                    item {
                        ProfileSectionTitle(title = stringResource(R.string.settings_section_more))
                    }
                    item {
                        ProfileGroupCard {
                            ProfileGroupItem(
                                icon = Icons.Default.Share,
                                title = stringResource(R.string.settings_item_share),
                                onClick = onShareApp
                            )
                            ProfileGroupItem(
                                icon = Icons.Default.StarRate,
                                title = stringResource(R.string.settings_item_rate),
                                onClick = onRateApp
                            )
                            ProfileGroupItem(
                                icon = Icons.Default.Shield,
                                title = stringResource(R.string.settings_item_privacy),
                                onClick = { actions.onNavigateToLegal(0) }
                            )
                            ProfileGroupItem(
                                icon = Icons.Default.Gavel,
                                title = stringResource(R.string.settings_item_terms),
                                onClick = { actions.onNavigateToLegal(1) }
                            )
                            ProfileGroupItem(
                                icon = Icons.Default.Info,
                                title = stringResource(R.string.settings_item_about),
                                showDivider = false,
                                onClick = actions.onNavigateToAbout
                            )
                        }
                    }

                    // 6. Cerrar sesión
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        ProfileGroupCard {
                            ProfileGroupItem(
                                icon = Icons.AutoMirrored.Filled.Logout,
                                title = stringResource(R.string.settings_btn_logout),
                                titleColor = MaterialTheme.colorScheme.error,
                                showChevron = false,
                                showDivider = false,
                                onClick = actions.onLogout
                            )
                        }
                    }
                } else {
                    // Si es un perfil de otro usuario:
                    item {
                        ProfileSectionTitle(title = stringResource(R.string.profile_tab_posts))
                    }
                    if (uiState.isPrivateProfileRestricted) {
                        item {
                            ProfilePrivateNoticeCard()
                        }
                    } else {
                        item {
                            ProfileGroupCard {
                                ProfileGroupItem(
                                    icon = Icons.Default.LocalOffer,
                                    title = stringResource(R.string.profile_view_user_posts_format, targetName),
                                    value = (profile?.postsCount ?: 0).toString(),
                                    showDivider = false,
                                    onClick = { actions.onNavigateToPosts(targetUid, targetName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.profile_btn_retry))
        }
    }
}

@Composable
fun EmptyProfileState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Filled.Article
) {
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
        name = "Ronald Richards",
        email = "ronaldrichards@gmail.com",
        photoUrl = null,
        followersCount = 142,
        followingCount = 98,
        postsCount = 12,
        commentsCount = 34,
        rating = 4.7f,
        reviewsCount = 23,
        isVerified = true,
        isPrivate = false,
        isDummy = false
    )
    RindeTheme {
        Surface {
            ProfileContent(
                innerPadding = PaddingValues(0.dp),
                uiState = ProfileUiState(
                    profile = dummyProfile,
                    posts = emptyList(),
                    isCurrentUser = true,
                    computedRating = 4.8f,
                    ratedPostsCount = 12
                ),
                currentTheme = ThemeMode.SYSTEM,
                isBunkerMode = false,
                isPrivate = false,
                onShowThemeSheet = {},
                onToggleBunkerMode = {},
                onShareApp = {},
                onRateApp = {},
                actions = ProfileActions()
            )
        }
    }
}
