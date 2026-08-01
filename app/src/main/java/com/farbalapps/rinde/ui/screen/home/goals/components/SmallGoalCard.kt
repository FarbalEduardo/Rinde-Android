package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.domain.model.SavingsGoal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmallGoalCard(
    goal: SavingsGoal,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isPrivacyMode: Boolean = false,
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val progressPercent = (fraction * 100).toInt()

            SmallGoalHeader(
                icon = icon,
                progressColor = progressColor,
                progressPercent = progressPercent,
                title = goal.title
            )

            Spacer(modifier = Modifier.height(12.dp))

            SmallGoalInfo(
                title = goal.title,
                currentAmount = goal.currentAmount,
                targetAmount = goal.targetAmount,
                isPrivacyMode = isPrivacyMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            SmallGoalProgressBar(
                animatedProgress = animatedProgress,
                progressColor = progressColor
            )
        }
    }
}

@Composable
private fun SmallGoalHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    progressColor: androidx.compose.ui.graphics.Color,
    progressPercent: Int,
    title: String
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(progressColor.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = progressColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "$progressPercent%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = progressColor
        )
    }
}

@Composable
private fun SmallGoalInfo(
    title: String,
    currentAmount: Double,
    targetAmount: Double,
    isPrivacyMode: Boolean = false
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
        text = if (isPrivacyMode) "$ ***.** / $ ***.**" else String.format("$%,.0f / $%,.0f", currentAmount, targetAmount),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SmallGoalProgressBar(
    animatedProgress: Float,
    progressColor: androidx.compose.ui.graphics.Color
) {
    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50)),
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}
