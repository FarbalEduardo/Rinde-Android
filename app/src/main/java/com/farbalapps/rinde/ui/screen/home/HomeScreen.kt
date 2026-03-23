package com.farbalapps.rinde.ui.screen.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.navigation.HomeNavHost
import com.farbalapps.rinde.ui.navigation.HomeRoute
import com.farbalapps.rinde.ui.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val appBarTitle = when (currentRoute) {
        HomeRoute.List.route -> "Rinde"
        HomeRoute.Community.route -> "Comunidad"
        HomeRoute.Goals.route -> "Metas"
        HomeRoute.Assistant.route -> "Chef AI"
        else -> stringResource(id = R.string.app_name)
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
        }
    ) { innerPadding ->
        HomeNavHost(
            navController = navController,
            innerPadding = innerPadding
        )
    }
}
@Preview(showSystemUi = true, showBackground = true)
@Composable
fun HomeScreenPreview() {
    
    HomeScreen(onLogout = {})

}
