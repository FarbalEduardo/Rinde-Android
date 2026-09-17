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

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path

val RindeChartIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "RindeChart",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(androidx.compose.ui.graphics.Color.White)) {
            // Left bar
            moveTo(4f, 10f)
            lineTo(8f, 10f)
            lineTo(8f, 20f)
            lineTo(4f, 20f)
            close()
            // Center bar (tallest)
            moveTo(10f, 4f)
            lineTo(14f, 4f)
            lineTo(14f, 20f)
            lineTo(10f, 20f)
            close()
            // Right bar
            moveTo(16f, 13f)
            lineTo(20f, 13f)
            lineTo(20f, 20f)
            lineTo(16f, 20f)
            close()
        }
    }.build()
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    unreadNotificationsCount: Int = 0
) {
    val items = listOf(
        Pair(HomeRoute.Dashboard, Pair(stringResource(id = R.string.dashboard_title), RindeChartIcon)),
        Pair(HomeRoute.Community, Pair(stringResource(id = R.string.home_tab_community), Icons.Default.Public)),
        Pair(HomeRoute.List, Pair(stringResource(id = R.string.home_tab_home), Icons.Default.ShoppingCart)),
        Pair(HomeRoute.Goals, Pair(stringResource(id = R.string.home_tab_goals), Icons.Default.Flag)),
        Pair(HomeRoute.Profile, Pair(stringResource(id = R.string.home_tab_profile), Icons.Default.AccountCircle))
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
                                restoreState = (route != HomeRoute.Dashboard)
                            }
                        } else {
                            navController.popBackStack(route, inclusive = false)
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

