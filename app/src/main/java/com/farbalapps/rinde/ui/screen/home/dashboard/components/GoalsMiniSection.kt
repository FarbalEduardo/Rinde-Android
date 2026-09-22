package com.farbalapps.rinde.ui.screen.home.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.ui.screen.home.goals.components.GoalThemeMapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoalsMiniSection(
    goals: List<SavingsGoal>,
    onGoalClick: (String) -> Unit,
    onNavigateToGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_goals_title),
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = onNavigateToGoals,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.dashboard_goals_view_all),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (goals.isEmpty()) {
            EmptyGoalsMini(onNavigateToGoals = onNavigateToGoals)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(goals, key = { it.id }) { goal ->
                    GoalDashboardCard(
                        goal = goal,
                        onClick = { onGoalClick(goal.id) },
                        modifier = Modifier.width(230.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalDashboardCard(
    goal: SavingsGoal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressColor = GoalThemeMapper.mapColor(goal.colorKey)
    val icon = GoalThemeMapper.mapIcon(goal.iconKey)

    val target = goal.targetAmount.coerceAtLeast(0.01)
    val fraction = (goal.currentAmount / target).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600),
        label = "goal_anim_progress"
    )
    val progressPercent = (fraction * 100).toInt()

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header con ícono, título y porcentaje
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(progressColor.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = progressColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de progreso
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Montos
            val currentFormatted = String.format(Locale.getDefault(), "$%,.0f", goal.currentAmount)
            val targetFormatted = String.format(Locale.getDefault(), "$%,.0f", goal.targetAmount)
            Text(
                text = "$currentFormatted / $targetFormatted",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Fecha límite si existe
            if (goal.targetDate != null && goal.targetDate > 0L) {
                val dateStr = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(goal.targetDate))
                Text(
                    text = stringResource(id = R.string.dashboard_goals_deadline_format, dateStr),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer con badge de ritmo y CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAhead = fraction >= 0.3f || goal.isCompleted
                val badgeColor = if (isAhead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                val badgeText = if (goal.isCompleted) {
                    stringResource(id = R.string.dashboard_goals_completed_badge)
                } else if (isAhead) {
                    stringResource(id = R.string.dashboard_goals_on_track)
                } else {
                    stringResource(id = R.string.dashboard_goals_needs_boost)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.12f), shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = badgeColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.dashboard_btn_add_savings),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
