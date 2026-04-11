package com.farbalapps.rinde.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.navigation.HomeNavHost
import com.farbalapps.rinde.ui.navigation.HomeRoute
import com.farbalapps.rinde.ui.components.BottomNavigationBar
import androidx.compose.ui.platform.LocalContext
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
    
    // Intercept back button to show confirmation dialog
    androidx.activity.compose.BackHandler(enabled = currentRoute == HomeRoute.List.route) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
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
                    onClick = { activity?.finish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cerrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
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

    // Shared ViewModel scoped to the HomeScreen NavHost so FAB and ListScreen share state
    val listViewModel: ListViewModel = hiltViewModel()
    val uiState by listViewModel.uiState.collectAsStateWithLifecycle()

    // Trigger catalog load
    LaunchedEffect(Unit) {
        listViewModel.loadCatalog(context)
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
                // Only hide if the list actually consumed some scroll (i.e., it moved)
                if (consumed.y < -5) {
                    isFabVisible = false
                }
                // Show if scrolling up and it moved
                if (consumed.y > 5) {
                    isFabVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appBarTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (currentRoute == HomeRoute.Community.route) {
                        IconButton(onClick = { /* TODO: Implement Search */ }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(id = R.string.btn_logout),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(
                visible = isFabVisible,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                when (currentRoute) {
                    HomeRoute.List.route -> {
                        FloatingActionButton(
                            onClick = { showAddProductSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.add_entry)
                            )
                        }
                    }
                    HomeRoute.Goals.route, HomeRoute.Community.route -> {
                        FloatingActionButton(
                            onClick = { /* TODO */ },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Añadir"
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            HomeNavHost(
                navController = navController,
                innerPadding = PaddingValues(0.dp),
                listViewModel = listViewModel
            )
        }
    }

    if (showAddProductSheet || uiState.editingItem != null) {
        val editingItem = uiState.editingItem
        AddProductBottomSheet(
            onDismiss = { 
                showAddProductSheet = false
                listViewModel.stopEditing()
                listViewModel.triggerPendingHighlights() // Trigger highlight animation for all items added while sheet was open
            },
            catalogItems = uiState.catalogItems,
            productCategories = uiState.catalogCategories,
            targetGroup = uiState.selectedFilterGroup,
            initialItem = editingItem,
            onShowMessage = { message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            },
            onProductAdded = { name, category, group, qty, unit, emoji, isCustom ->
                listViewModel.addItem(name, category, group, qty, unit, emoji, isCustom)
                // Do not close sheet here to allow multiple additions
            },
            onProductUpdated = { id, name, category, qty, unit, emoji ->
                listViewModel.updateItem(id, name, category, qty, unit, emoji)
                listViewModel.stopEditing()
            },
            onAddCategory = { listViewModel.addCategory(it) },
            customHistory = uiState.customProductsHistory
        )
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(onLogout = {})
}
