package com.farbalapps.rinde.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R

sealed class HomeTab(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Home : HomeTab("home_tab", R.string.home_tab_home, Icons.Default.Home)
    object History : HomeTab("history_tab", R.string.home_tab_history, Icons.Default.History)
    object Profile : HomeTab("profile_tab", R.string.home_tab_profile, Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf<HomeTab>(HomeTab.Home) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = stringResource(id = R.string.btn_logout))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val tabs = listOf(HomeTab.Home, HomeTab.History, HomeTab.Profile)
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(stringResource(id = tab.titleRes)) },
                        icon = { Icon(tab.icon, contentDescription = null) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: New Entry */ }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_entry))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                HomeTab.Home -> {
                    Text(text = "Bienvenido a Rinde")
                }
                HomeTab.History -> {
                    Text(text = "Tu historial aparecerá aquí")
                }
                HomeTab.Profile -> {
                    Text(text = "Configuración de perfil")
                }
            }
        }
    }
}
