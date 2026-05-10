package com.farbalapps.rinde.ui.screen.home.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.ui.screen.home.goals.components.ChefSuggestionCard
import com.farbalapps.rinde.ui.screen.home.goals.components.EmergencyFundCard
import com.farbalapps.rinde.ui.screen.home.goals.components.GoalCard
import com.farbalapps.rinde.ui.theme.Blue80
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.RindeTheme

@Composable
fun GoalsScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumen Total Section
            item {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "RESUMEN TOTAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$12,450.00",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = Blue80, // Using Blue80 from theme
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "+12%",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = RindePrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Has alcanzado el 45% de tus objetivos globales.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Grid of Top Goals
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GoalCard(
                        title = "Nueva Casa",
                        subtitle = "Ahorro para el enganche",
                        currentAmount = "$4,500",
                        totalAmount = "$10,000",
                        percentage = 45,
                        icon = Icons.Default.Home,
                        progressColor = RindePrimary,
                        iconContainerColor = Blue80,
                        iconColor = RindePrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GoalCard(
                        title = "Viaje Japón", // Need custom flight icon ideally
                        subtitle = "$2,300 / $5,000",
                        currentAmount = "",
                        totalAmount = "",
                        percentage = 46, // Approx based on image
                        icon = Icons.Default.FlightTakeoff,
                        progressColor = Color(0xFF8B5A62), // Brownish maroon
                        iconContainerColor = Color(0xFFEBE3E4),
                        iconColor = Color(0xFF8B5A62),
                        modifier = Modifier.weight(1f)
                    )

                    GoalCard(
                        title = "MacBook Pro", // Need custom laptop icon
                        subtitle = "$1,800 / $2,500",
                        currentAmount = "",
                        totalAmount = "",
                        percentage = 72, // Approx
                        icon = Icons.Default.LaptopMac,
                        progressColor = Color(0xFF635F70), // Grey/Purple
                        iconContainerColor = Color(0xFFE5E4E8),
                        iconColor = Color(0xFF635F70),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // AI Suggestion
            item {
                ChefSuggestionCard()
            }

            // Emergency Fund
            item {
                EmergencyFundCard()
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
fun GoalsScreenPreview() {
    RindeTheme {
        GoalsScreen()
    }
}
