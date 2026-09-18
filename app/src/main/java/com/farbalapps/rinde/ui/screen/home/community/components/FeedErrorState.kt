package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindeTheme

/**
 * Estado de error amigable para el usuario cuando falla la carga del feed en Community.
 * Oculta detalles técnicos innecesarios de Firestore/Firebase y provee acción de reintento.
 */
@Composable
fun FeedErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanErrorMessage = remember(errorMessage) {
        val errorLower = errorMessage.lowercase()
        if (errorLower.contains("failed_precondition") ||
            errorLower.contains("index") ||
            errorLower.contains("firestore") ||
            errorLower.contains("firebase") ||
            errorLower.contains("query")
        ) {
            "Ocurrió un problema temporal con el servidor de base de datos. Por favor, vuelve a intentarlo más tarde."
        } else {
            errorMessage
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(500)) +
                slideInVertically(initialOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp, bottom = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "error_pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "error_scale"
                )

                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.community_error_load_failed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = cleanErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
                TextButton(
                    onClick = onRetry
                ) {
                    Text(
                        text = stringResource(R.string.community_btn_retry),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FeedErrorStatePreview() {
    RindeTheme {
        FeedErrorState(
            errorMessage = "No se pudo conectar con el servidor",
            onRetry = {}
        )
    }
}
