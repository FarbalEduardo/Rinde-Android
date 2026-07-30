package com.farbalapps.rinde.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.navigation.HomeRoute

import androidx.navigation.NavDestination.Companion.hasRoute

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox

@Composable
fun BottomNavigationBar(
    navController: NavController,
    unreadNotificationsCount: Int = 0
) {
    val items = listOf(
        Pair(HomeRoute.Community, Pair(stringResource(id = R.string.home_tab_community), Icons.Default.Public)),
        Pair(HomeRoute.List, Pair(stringResource(id = R.string.home_tab_home), Icons.Default.ShoppingCart)),
        Pair(HomeRoute.Goals, Pair(stringResource(id = R.string.home_tab_goals), Icons.Default.Flag)),
        Pair(HomeRoute.Assistant, Pair("Chef", Icons.Default.Restaurant)),
        Pair(HomeRoute.Profile, Pair("Perfil", Icons.Default.AccountCircle))
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            items.forEach { (route, data) ->
                val title = data.first
                val icon = data.second
                val selected = destination?.hasRoute(route::class) == true
                val badgeCount = if (route == HomeRoute.Community) unreadNotificationsCount else 0

                CustomNavigationBarItem(
                    title = title,
                    icon = icon,
                    selected = selected,
                    badgeCount = badgeCount,
                    onClick = {
                        if (!selected) {
                            navController.navigate(route) {
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
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    NavigationBarItem(
        icon = { 
            if (badgeCount > 0) {
                BadgedBox(
                    badge = {
                        Badge {
                            Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                        }
                    }
                ) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = title
                    )
                }
            } else {
                Icon(
                    imageVector = icon, 
                    contentDescription = title
                )
            }
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

