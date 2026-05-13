package com.farbalapps.rinde.ui.screen.home.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                horizontal = dimensionResource(id = R.dimen.padding_medium), 
                vertical = dimensionResource(id = R.dimen.padding_medium)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
        ) {

            // Resumen Total Section
            item {
                Column(modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_medium))) {
                    Text(
                        text = stringResource(id = R.string.goals_total_summary),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_small)))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$12,450.00",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_medium)))
                        Surface(
                            color = Blue80,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xsmall))
                        ) {
                            Text(
                                text = "+12%",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = RindePrimary,
                                modifier = Modifier.padding(
                                    horizontal = dimensionResource(id = R.dimen.padding_small), 
                                    vertical = dimensionResource(id = R.dimen.padding_xsmall)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_small)))
                    Text(
                        text = stringResource(id = R.string.goals_progress_msg, 45),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Grid of Top Goals
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
                ) {
                    GoalCard(
                        title = stringResource(id = R.string.goal_house_title),
                        subtitle = stringResource(id = R.string.goal_house_subtitle),
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
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
                ) {
                    GoalCard(
                        title = stringResource(id = R.string.goal_japan_title),
                        subtitle = "$2,300 / $5,000",
                        currentAmount = "",
                        totalAmount = "",
                        percentage = 46,
                        icon = Icons.Default.FlightTakeoff,
                        progressColor = Color(0xFF8B5A62),
                        iconContainerColor = Color(0xFFEBE3E4),
                        iconColor = Color(0xFF8B5A62),
                        modifier = Modifier.weight(1f)
                    )

                    GoalCard(
                        title = stringResource(id = R.string.goal_macbook_title),
                        subtitle = "$1,800 / $2,500",
                        currentAmount = "",
                        totalAmount = "",
                        percentage = 72,
                        icon = Icons.Default.LaptopMac,
                        progressColor = Color(0xFF635F70),
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
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xxlarge)))
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
