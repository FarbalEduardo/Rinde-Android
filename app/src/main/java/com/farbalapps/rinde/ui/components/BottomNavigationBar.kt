package com.farbalapps.rinde.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.farbalapps.rinde.ui.navigation.HomeRoute

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Pair(HomeRoute.List, Pair(stringResource(id = R.string.home_tab_home), Icons.Default.ShoppingCart)),
        Pair(HomeRoute.Community, Pair(stringResource(id = R.string.welcome_title), Icons.Default.Public)), // TODO: Add specific strings
        Pair(HomeRoute.Goals, Pair(stringResource(id = R.string.home_tab_history), Icons.Default.Flag)), // TODO: Add specific strings
        Pair(HomeRoute.Assistant, Pair(stringResource(id = R.string.social_google), Icons.Default.AutoAwesome)) // TODO: Add specific strings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            items.forEach { (route, data) ->
                val title = data.first
                val icon = data.second
                val selected = currentRoute == route.route

                CustomNavigationBarItem(
                    title = title,
                    icon = icon,
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.CustomNavigationBarItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        icon = { 
            Icon(
                imageVector = icon, 
                contentDescription = title
            ) 
        },
        label = { Text(title) },
        selected = selected,
        onClick = onClick,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
