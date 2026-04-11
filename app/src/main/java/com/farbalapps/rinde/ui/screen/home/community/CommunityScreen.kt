package com.farbalapps.rinde.ui.screen.home.community

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.ui.screen.home.community.components.FilterChipRow
import com.farbalapps.rinde.ui.screen.home.community.components.PostCard
import com.farbalapps.rinde.ui.screen.home.community.components.WishlistAddCard
import com.farbalapps.rinde.ui.theme.RindeTheme

@Composable
fun CommunityScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Mi Wishlist Section
            item {
                Column {
                    Text(
                        text = "Mi Wishlist",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        WishlistAddCard()
                        // Add some spacing to the right just in case
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }

            // Ofertas de hoy Section
            item {
                Column {
                    Text(
                        text = "Ofertas de hoy",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        FilterChipRow()
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }

            // Dummy Posts Feed
            item {
                PostCard(
                    username = "Carla Benavides",
                    timeLocation = "Hace 15 min • Jumbo Providencia",
                    imageUrl = null, // Placeholder will show red background as per original design color hint
                    title = "Frutillas 2 x 1 en Jumbo.\n¡Están muy frescas!",
                    description = "Solo hoy hasta agotar stock. Visto en el pasillo central, quedan unas 20 cajas aproximadamente.",
                    isRecommended = true,
                    votes = 42,
                    likes = 128,
                    commentsCount = 12
                )
            }
            
            item {
                PostCard(
                    username = "Pedro Soto",
                    timeLocation = "Hace 1 hora • Líder Express",
                    imageUrl = null, // Placeholder will show red background as per original design color hint
                    title = "Detergente Omo 3L a precio de 1L",
                    description = "Es un error de etiqueta pero está pasando por caja a $4.990. Aprovechen antes de que se den cuenta.",
                    isRecommended = false,
                    votes = 12,
                    likes = 89,
                    commentsCount = 4
                )
            }

            // Add padding at bottom for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@PreviewLightDark
@Composable
fun CommunityScreenPreview() {
    RindeTheme {
        CommunityScreen()
    }
}
