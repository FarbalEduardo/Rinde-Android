package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.PostVerdict
import com.farbalapps.rinde.domain.model.VerdictCalculator
import com.farbalapps.rinde.ui.theme.VoteFalseContainerDark
import com.farbalapps.rinde.ui.theme.VoteFalseContentDark
import com.farbalapps.rinde.ui.theme.VoteTrueContainerDark
import com.farbalapps.rinde.ui.theme.VoteTrueContentDark
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Barra de acciones de votación comunitaria (Real vs Falsa) con animación de rebote y visualizador de porcentaje.
 *
 * @param userVote Voto emitido por el usuario (-1 = Falsa, 0 = Sin voto, 1 = Real).
 * @param truthCount Conteo acumulado de votos verídicos.
 * @param falseCount Conteo acumulado de votos falsos.
 * @param onVoteTrue Callback al emitir voto positivo (Real).
 * @param onVoteFalse Callback al emitir voto negativo (Falsa).
 * @param modifier Modificador para el contenedor.
 */
@Composable
fun VotingActions(
    userVote: Int,
    truthCount: Int,
    falseCount: Int,
    onVoteTrue: () -> Unit,
    onVoteFalse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalVotes = truthCount + falseCount
    val verdict = VerdictCalculator.calculate(truthCount, falseCount)
    val showResults = verdict != PostVerdict.VALIDATING
    val truthRatio: Float? = if (showResults && totalVotes > 0) {
        truthCount.toFloat() / totalVotes
    } else null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VoteActionButton(
                label = if (showResults) "Falsa · $falseCount" else "Falsa",
                icon = Icons.Default.ThumbDown,
                isSelected = userVote == -1,
                selectedContainerColor = VoteFalseContainerDark,
                selectedContentColor = VoteFalseContentDark,
                modifier = Modifier.weight(1f),
                onClick = onVoteFalse
            )

            if (truthRatio != null) {
                VotePercentBadge(truthPercent = truthRatio)
            }

            VoteActionButton(
                label = if (showResults) "Real · $truthCount" else "Real",
                icon = Icons.Default.ThumbUp,
                isSelected = userVote == 1,
                selectedContainerColor = VoteTrueContainerDark,
                selectedContentColor = VoteTrueContentDark,
                modifier = Modifier.weight(1f),
                onClick = onVoteTrue
            )
        }

        if (truthRatio != null) {
            VoteProgressIndicator(truthRatio = truthRatio, userVote = userVote)
        } else {
            val needed = VerdictCalculator.MIN_VOTES_THRESHOLD - totalVotes
            Text(
                text = "Se necesitan $needed ${if (needed == 1) "voto más" else "votos más"} para ver resultados",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Botón interactivo de votación con retroalimentación física mediante rebote elástico.
 */
@Composable
fun VoteActionButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    selectedContainerColor: Color,
    selectedContentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bounceScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val containerColor = if (isSelected) selectedContainerColor
    else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) selectedContentColor
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = {
            scope.launch {
                bounceScale.animateTo(targetValue = 0.85f, animationSpec = tween(durationMillis = 80))
                bounceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            }
            onClick()
        },
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer { scaleX = bounceScale.value; scaleY = bounceScale.value },
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * Badge circular que muestra el porcentaje de votos verídicos.
 */
@Composable
fun VotePercentBadge(truthPercent: Float) {
    val displayPercent = (truthPercent * 100).roundToInt()
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$displayPercent%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Barra de progreso bicolor proporcional al ratio de votos verídicos vs falsos.
 */
@Composable
fun VoteProgressIndicator(
    truthRatio: Float,
    userVote: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50))
    ) {
        val falseRatio = (1f - truthRatio).coerceAtLeast(0f)
        if (falseRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(falseRatio)
                    .background(VoteFalseContainerDark.copy(alpha = if (userVote == -1) 1f else 0.4f))
            )
        }
        if (truthRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(truthRatio)
                    .background(VoteTrueContainerDark.copy(alpha = if (userVote == 1) 1f else 0.4f))
            )
        }
    }
}
