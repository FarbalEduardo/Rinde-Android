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
        HomeRoute.Profile.route -> "Perfil"
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
                if (consumed.y < -5) isFabVisible = false
                if (consumed.y > 5) isFabVisible = true
                return Offset.Zero
            }
        }
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
            if (isTopLevelRoute && currentRoute != HomeRoute.Community.route) {
                HomeScreenTopBar(
                    title = appBarTitle,
                    showSearch = currentRoute == HomeRoute.Community.route,
                    onSearchClick = { /* TODO */ },
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
            AnimatedVisibility(
                visible = isFabVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                when (currentRoute) {
                    HomeRoute.List.route -> {
                        FloatingActionButton(
                            onClick = { showAddProductSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, stringResource(id = R.string.add_entry))
                        }
                    }
                    HomeRoute.Goals.route -> {
                        FloatingActionButton(
                            onClick = { /* TODO: Crear meta */ },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, "Añadir meta")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            HomeNavHost(
                navController = navController,
                innerPadding = PaddingValues(0.dp),
                listViewModel = listViewModel,
                onLogout = onLogout
            )
        }
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
                    Icon(Icons.Default.Search, "Buscar")
                }
            }
            if (showSettings) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, "Configuración")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
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
                text = "¿Salir de la aplicación?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Al salir se perderán los cambios no guardados en la nube. ¿Estás seguro de que deseas cerrar Rinde?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cerrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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

@Preview(showBackground = true)
@Composable
fun HomeScreenTopBarPreview() {
    com.farbalapps.rinde.ui.theme.RindeTheme {
        HomeScreenTopBar(title = "Rinde", showSearch = true, onSearchClick = {})
    }
}
