package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.home.community.CommunityTab

/**
 * Fila de pestañas primarias de la comunidad (Descubrir, Lo más Hot, Guardados).
 *
 * @param selectedTab Pestaña actualmente activa.
 * @param onTabSelected Callback invocado al seleccionar una pestaña.
 * @param modifier Modificador de diseño para el contenedor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityTabRow(
    selectedTab: CommunityTab,
    onTabSelected: (CommunityTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        CommunityTab.DISCOVER to stringResource(R.string.community_tab_discover),
        CommunityTab.HOT to stringResource(R.string.community_tab_hot),
        CommunityTab.SAVED to stringResource(R.string.community_tab_saved)
    )

    val selectedIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
    val indicatorWidth = dimensionResource(id = R.dimen.icon_size_large) // 32dp

    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex),
                width = indicatorWidth,
                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            )
        },
        divider = {}
    ) {
        tabs.forEach { (tab, title) ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }
    }
}
