package com.farbalapps.rinde.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.navigation.HomeNavHost
import com.farbalapps.rinde.ui.navigation.HomeRoute
import com.farbalapps.rinde.ui.components.BottomNavigationBar
import com.farbalapps.rinde.ui.screen.home.list.ListViewModel
import com.farbalapps.rinde.ui.screen.home.list.components.AddProductBottomSheet
import com.farbalapps.rinde.ui.theme.RindeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    var showAddProductSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    // ViewModels
    val listViewModel: ListViewModel = hiltViewModel()
    val uiState by listViewModel.uiState.collectAsStateWithLifecycle()

    // Trigger catalog load
    LaunchedEffect(Unit) {
        listViewModel.loadCatalog(context)
    }

    // Intercept back button to show confirmation dialog
    androidx.activity.compose.BackHandler(enabled = currentRoute == HomeRoute.List.route) {
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = { activity?.finish() },
            onDismiss = { showExitDialog = false }
        )
    }

    val appBarTitle = when (currentRoute) {
        HomeRoute.List.route -> stringResource(id = R.string.app_name)
        HomeRoute.Community.route -> stringResource(id = R.string.home_tab_community)
        HomeRoute.Goals.route -> stringResource(id = R.string.home_tab_goals)
        HomeRoute.Assistant.route -> stringResource(id = R.string.home_tab_chef_ai)
        HomeRoute.Profile.route -> stringResource(id = R.string.home_tab_profile)
        else -> stringResource(id = R.string.app_name)
    }

    var isFabVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y < -2f) isFabVisible = false
                if (consumed.y > 2f) isFabVisible = true
                return Offset.Zero
            }
        }
    }

    // Reset FAB visibility when switching tabs
    LaunchedEffect(currentRoute) {
        isFabVisible = true
    }

    val isTopLevelRoute = currentRoute in listOf(
        HomeRoute.List.route,
        HomeRoute.Community.route,
        HomeRoute.Goals.route,
        HomeRoute.Assistant.route,
        HomeRoute.Profile.route
    )

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = {
            if (currentRoute == HomeRoute.List.route || currentRoute == HomeRoute.Profile.route) {
                HomeScreenTopBar(
                    title = appBarTitle,
                    showSearch = false,
                    onSearchClick = {},
                    showSettings = currentRoute == HomeRoute.Profile.route,
                    onSettingsClick = { navController.navigate(HomeRoute.Settings.route) }
                )
            }
        },
        bottomBar = {
            if (isTopLevelRoute) {
                BottomNavigationBar(navController = navController)
            }
        },
        floatingActionButton = {
            HomeScreenFab(
                isVisible = isFabVisible,
                currentRoute = currentRoute,
                onAddProduct = { showAddProductSheet = true },
                onAddGoal = { /* TODO: Crear meta */ },
                onAddCommunityPost = { navController.navigate(HomeRoute.CreatePost.route) }
            )
        }
    ) { innerPadding ->
        HomeNavHost(
            navController = navController,
            innerPadding = innerPadding,
            listViewModel = listViewModel,
            onLogout = onLogout,
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showAddProductSheet || uiState.editingItem != null) {
        val editingItem = uiState.editingItem
        AddProductBottomSheet(
            onDismiss = { 
                showAddProductSheet = false
                listViewModel.stopEditing()
                listViewModel.triggerPendingHighlights() 
            },
            catalogItems = uiState.catalogItems,
            productCategories = uiState.catalogCategories,
            targetGroup = uiState.selectedFilterGroup,
            initialItem = editingItem,
            onShowMessage = { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() },
            onProductAdded = { n, c, g, q, u, e, ic -> listViewModel.addItem(n, c, g, q, u, e, ic) },
            onProductUpdated = { id, n, c, q, u, e -> listViewModel.updateItem(id, n, c, q, u, e); listViewModel.stopEditing() },
            onAddCategory = { listViewModel.addCategory(it) },
            customHistory = uiState.customProductsHistory
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenTopBar(
    title: String,
    showSearch: Boolean,
    onSearchClick: () -> Unit,
    showSettings: Boolean = false,
    onSettingsClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            if (showSearch) {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, stringResource(R.string.action_search))
                }
            }
            if (showSettings) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, stringResource(R.string.action_settings))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.exit_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.exit_dialog_text),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.exit_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun HomeScreenFab(
    isVisible: Boolean,
    currentRoute: String?,
    onAddProduct: () -> Unit,
    onAddGoal: () -> Unit,
    onAddCommunityPost: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        when (currentRoute) {
            HomeRoute.List.route -> {
                FloatingActionButton(
                    onClick = onAddProduct,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, stringResource(id = R.string.add_entry))
                }
            }
            HomeRoute.Community.route -> {
                FloatingActionButton(
                    onClick = onAddCommunityPost,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, stringResource(id = R.string.community_fab_desc))
                }
            }
            HomeRoute.Goals.route -> {
                FloatingActionButton(
                    onClick = onAddGoal,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.home_fab_add_goal))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenTopBarPreview() {
    RindeTheme {
        HomeScreenTopBar(title = "Rinde", showSearch = true, onSearchClick = {})
    }
}
