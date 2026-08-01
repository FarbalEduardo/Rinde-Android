package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.domain.model.SavingsGoal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeaturedGoalCard(
    goal: SavingsGoal,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progressColor = GoalThemeMapper.mapColor(goal.colorKey)
    val icon = GoalThemeMapper.mapIcon(goal.iconKey)
    
    val target = goal.targetAmount.coerceAtLeast(0.01)
    val fraction = (goal.currentAmount / target).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(targetValue = fraction, label = "ProgressAnim")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            FeaturedGoalHeader(
                goal = goal,
                progressColor = progressColor,
                icon = icon,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick
            )

            Spacer(modifier = Modifier.height(14.dp))

            val progressPercent = (fraction * 100).toInt()

            FeaturedGoalProgressInfoTop(
                progressPercent = progressPercent,
                progressColor = progressColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            FeaturedGoalProgressBar(
                animatedProgress = animatedProgress,
                progressColor = progressColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            FeaturedGoalProgressInfoBottom(
                goal = goal
            )
        }
    }
}

@Composable
private fun FeaturedGoalHeader(
    goal: SavingsGoal,
    progressColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(progressColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = goal.title,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "PROGRESO DE AHORRO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        if (goal.isCompleted) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = "¡Cumplida!",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeaturedGoalProgressInfoTop(
    progressPercent: Int,
    progressColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$progressPercent% COMPLETADO",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = progressColor
        )
        Text(
            text = "$progressPercent%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = progressColor
        )
    }
}

@Composable
private fun FeaturedGoalProgressInfoBottom(
    goal: SavingsGoal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Actual: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format("$%,.2f", goal.currentAmount),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Meta: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format("$%,.2f", goal.targetAmount),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FeaturedGoalProgressBar(
    animatedProgress: Float,
    progressColor: Color
) {
    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50)),
        color = progressColor,
 trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}

@androidx.compose.ui.tooling.preview.Preview(name = "Featured Goal Card", showBackground = true)
@Composable
fun FeaturedGoalCardPreview() {
    val mockGoal = SavingsGoal(
        id = "1",
        userId = "user1",
        title = "Fondo de Emergencia",
        targetAmount = 5000.0,
        currentAmount = 2500.0,
        targetDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
        iconKey = "savings",
        colorKey = "blue",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        monthlySnapshotAmount = 1000.0
    )
    com.farbalapps.rinde.ui.theme.RindeTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FeaturedGoalCard(
                goal = mockGoal,
                onClick = {},
                onLongClick = {}
            )
        }
    }
}
