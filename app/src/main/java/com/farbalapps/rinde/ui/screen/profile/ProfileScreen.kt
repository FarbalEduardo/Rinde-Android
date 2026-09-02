package com.farbalapps.rinde.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.farbalapps.rinde.R
import com.farbalapps.rinde.data.local.AppLanguage
import com.farbalapps.rinde.data.local.ThemeMode
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.ui.screen.profile.components.*
import com.farbalapps.rinde.ui.theme.RindeTheme

data class ProfileActions(
    val onBack: () -> Unit = {},
    val onEditProfile: () -> Unit = {},
    val onNavigateToPosts: (userId: String, userName: String) -> Unit = { _, _ -> },
    val onNavigateToSaved: () -> Unit = {},
    val onNavigateToBlocked: () -> Unit = {},
    val onNavigateToAbout: () -> Unit = {},
    val onLogout: () -> Unit = {},
    val onSetTheme: (ThemeMode) -> Unit = {},
    val onSetLanguage: (AppLanguage) -> Unit = {},
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
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appCurrency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val isBunkerMode by viewModel.isBunkerMode.collectAsStateWithLifecycle()
    val isProfilePrivate by viewModel.isProfilePrivate.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
            snackbarHostState.showSnackbar(msg)
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

    if (showLanguageSheet) {
        LanguageSelectorSheet(
            currentLanguage = appLanguage,
            onLanguageSelected = { viewModel.setLanguage(it) },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showCurrencySheet) {
        CurrencySelectorSheet(
            currentCurrency = appCurrency,
            onCurrencySelected = { viewModel.setCurrency(it) },
            onDismiss = { showCurrencySheet = false }
        )
    }

    val actions = remember(viewModel, onBack, onEditProfile, onNavigateToPosts, onNavigateToSaved, onNavigateToBlocked, onNavigateToAbout, onLogout) {
        ProfileActions(
            onBack = { onBack?.invoke() },
            onEditProfile = onEditProfile,
            onNavigateToPosts = onNavigateToPosts,
            onNavigateToSaved = onNavigateToSaved,
            onNavigateToBlocked = onNavigateToBlocked,
            onNavigateToAbout = onNavigateToAbout,
            onLogout = { showLogoutDialog = true },
            onSetTheme = { viewModel.setTheme(it) },
            onSetLanguage = { viewModel.setLanguage(it) },
            onTogglePrivacy = { viewModel.togglePrivacy(it) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isCurrentUser) {
                            stringResource(R.string.home_tab_profile)
                        } else {
                            uiState.profile?.name ?: stringResource(R.string.home_tab_profile)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!uiState.isCurrentUser) {
                        IconButton(onClick = actions.onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
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
        ProfileContent(
            innerPadding = PaddingValues (
                top = padding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() 
            ),
            uiState = uiState,
            currentTheme = themeMode,
            currentLanguage = appLanguage,
            currentCurrency = appCurrency,
            isBunkerMode = isBunkerMode,
            isPrivate = isProfilePrivate,
            onShowThemeSheet = { showThemeSheet = true },
            onShowLanguageSheet = { showLanguageSheet = true },
            onShowCurrencySheet = { showCurrencySheet = true },
            onToggleBunkerMode = { viewModel.toggleBunkerMode(it) },
            onClearCache = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { context.cacheDir.deleteRecursively() }
                    }
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_storage_cleared))
                }
            },
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

@Composable
fun ProfileContent(
    innerPadding: PaddingValues,
    uiState: ProfileUiState,
    currentTheme: ThemeMode,
    currentLanguage: AppLanguage,
    currentCurrency: com.farbalapps.rinde.data.local.AppCurrency,
    isBunkerMode: Boolean,
    isPrivate: Boolean,
    onShowThemeSheet: () -> Unit,
    onShowLanguageSheet: () -> Unit,
    onShowCurrencySheet: () -> Unit,
    onToggleBunkerMode: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onShareApp: () -> Unit,
    onRateApp: () -> Unit,
    actions: ProfileActions
) {
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
            ProfileErrorState(error = error, onRetry = actions.onBack)
            return@Surface
        }

        val profile = uiState.profile
        val targetUid = profile?.id ?: ""
        val targetName = profile?.name ?: ""

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 16.dp
            )
        ) {
            // 1. Tarjeta Principal de Identidad / Perfil (Header)
            item {
                ProfileHeaderCard(
                    uiState = uiState,
                    onEditProfile = actions.onEditProfile
                )
            }

            if (uiState.isCurrentUser) {
                // 2. Sección: Actividad
                item {
                    ProfileSectionTitle(title = stringResource(R.string.settings_section_usage))
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

                // 3. Sección: Preferencias de Compra
//                item {
//                    ProfileSectionTitle(title = stringResource(R.string.settings_section_preferences))
//                }
//                item {
//                    ProfileGroupCard {
//                        ProfileGroupItem(
//                            icon = Icons.Default.AttachMoney,
//                            title = stringResource(R.string.settings_item_currency),
//                            value = currentCurrency.code,
//                            onClick = onShowCurrencySheet
//                        )
//                        ProfileGroupItem(
//                            icon = Icons.Default.WifiOff,
//                            title = stringResource(R.string.settings_item_bunker),
//                            subtitle = stringResource(R.string.settings_item_bunker_desc),
//                            trailingContent = {
//                                Switch(
//                                    checked = isBunkerMode,
//                                    onCheckedChange = onToggleBunkerMode
//                                )
//                            },
//                            showDivider = false
//                        )
//                    }
//                }

                // 4. Sección: Cuenta y Seguridad
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
                            trailingContent = {
                                Switch(
                                    checked = isPrivate,
                                    onCheckedChange = actions.onTogglePrivacy
                                )
                            }
                        )
//                        ProfileGroupItem(
//                            icon = Icons.Default.Block,
//                            title = stringResource(R.string.settings_item_blocked),
//                            showDivider = false,
//                            onClick = actions.onNavigateToBlocked
//                        )
                    }
                }

                // 5. Sección: Aplicación y Medios
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
                        val languageText = when (currentLanguage) {
                            AppLanguage.ES -> stringResource(R.string.language_es)
                            AppLanguage.EN -> stringResource(R.string.language_en)
                        }
                        ProfileGroupItem(
                            icon = Icons.Default.Palette,
                            title = stringResource(R.string.settings_item_theme),
                            value = themeText,
                            onClick = onShowThemeSheet
                        )
                        ProfileGroupItem(
                            icon = Icons.Default.Language,
                            title = stringResource(R.string.settings_item_language),
                            value = languageText,
                            onClick = onShowLanguageSheet
                        )
//                        ProfileGroupItem(
//                            icon = Icons.Default.CleaningServices,
//                            title = stringResource(R.string.settings_item_storage),
//                            subtitle = stringResource(R.string.settings_item_storage_desc),
//                            showChevron = false,
//                            showDivider = false,
//                            onClick = onClearCache
//                        )
                    }
                }

                // 6. Sección: Comunidad y Soporte
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
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_item_about),
                            showDivider = false,
                            onClick = actions.onNavigateToAbout
                        )
                    }
                }

                // 7. Al final de la lista: Cerrar sesión
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
                item {
                    ProfileGroupCard {
                        ProfileGroupItem(
                            icon = Icons.Default.LocalOffer,
                            title = "Ver publicaciones de $targetName",
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

@Composable
fun ProfileErrorState(error: String, onRetry: () -> Unit) {
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
            Text(stringResource(R.string.profile_btn_retry))
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
                currentLanguage = AppLanguage.ES,
                currentCurrency = com.farbalapps.rinde.data.local.AppCurrency.CLP,
                isBunkerMode = false,
                isPrivate = false,
                onShowThemeSheet = {},
                onShowLanguageSheet = {},
                onShowCurrencySheet = {},
                onToggleBunkerMode = {},
                onClearCache = {},
                onShareApp = {},
                onRateApp = {},
                actions = ProfileActions()
            )
        }
    }
}
