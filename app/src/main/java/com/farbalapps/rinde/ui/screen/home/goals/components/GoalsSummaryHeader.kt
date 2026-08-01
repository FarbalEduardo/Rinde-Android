package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.tooling.preview.Preview
import com.farbalapps.rinde.domain.usecase.goals.GoalsSummary
import com.farbalapps.rinde.ui.theme.RindeTheme
import android.content.res.Configuration

@Composable
fun GoalsSummaryHeader(
    summary: GoalsSummary,
    showOptionsMenu: Boolean = false,
    onShowOptionsChange: (Boolean) -> Unit = {},
    onCreateGoalClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SummaryOptionsMenu(
                showOptionsMenu = showOptionsMenu,
                onShowOptionsChange = onShowOptionsChange,
                onCreateGoalClick = onCreateGoalClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RESUMEN TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = String.format("$%,.2f", summary.totalSaved),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Has alcanzado el ${summary.progressPercent}% de tus objetivos",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${summary.progressPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { summary.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format("$%,.2f ahorrado", summary.totalSaved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("Meta: $%,.2f", summary.totalTarget),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryOptionsMenu(
    showOptionsMenu: Boolean,
    onShowOptionsChange: (Boolean) -> Unit,
    onCreateGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(onClick = { onShowOptionsChange(true) }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opciones de Metas",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
            expanded = showOptionsMenu,
            onDismissRequest = { onShowOptionsChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Nueva Meta") },
                onClick = {
                    onShowOptionsChange(false)
                    onCreateGoalClick()
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GoalsSummaryHeaderPreview() {
    val sampleSummary = GoalsSummary(
        totalSaved = 4500.0,
        totalTarget = 10000.0,
        progressPercent = 45,
        monthlyGrowthPercent = 12
    )
    RindeTheme {
        GoalsSummaryHeader(
            summary = sampleSummary,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GoalsSummaryHeaderDarkPreview() {
    val sampleSummary = GoalsSummary(
        totalSaved = 4500.0,
        totalTarget = 10000.0,
        progressPercent = 45,
        monthlyGrowthPercent = 12
    )
    RindeTheme {
        GoalsSummaryHeader(
            summary = sampleSummary,
            modifier = Modifier.padding(16.dp)
        )
    }
}
