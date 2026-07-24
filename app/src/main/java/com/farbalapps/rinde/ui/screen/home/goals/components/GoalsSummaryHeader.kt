package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.usecase.goals.GoalsSummary

@Composable
fun GoalsSummaryHeader(
    summary: GoalsSummary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
                text = String.format("$%,.2f", summary.totalSaved),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Badge de crecimiento mensual (E4.3)
            if (summary.monthlyGrowthPercent != 0) {
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_medium)))
                val isPositive = summary.monthlyGrowthPercent > 0
                val badgeColor = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                val textColor = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                val sign = if (isPositive) "+" else ""

                Surface(
                    color = badgeColor,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_xsmall))
                ) {
                    Text(
                        text = "$sign${summary.monthlyGrowthPercent}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(id = R.dimen.padding_small),
                            vertical = dimensionResource(id = R.dimen.padding_xsmall)
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_small)))
        
        // Mensaje de porcentaje global (E4.2)
        Text(
            text = stringResource(id = R.string.goals_progress_msg, summary.progressPercent),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
