package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R

@Composable
fun EmptyGoalsContent(
    onCreateFirstGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animación infinita de pulso de respiración viva (breathing pulse effect)
    val infiniteTransition = rememberInfiniteTransition(label = "goalsBreathingPulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1.00f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Iconos flotantes decorativos de fondo (monedas / metas)
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            tint = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.16f),
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopStart)
                .offset(x = 16.dp, y = 40.dp)
                .rotate(-15f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.16f),
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-20).dp, y = (-80).dp)
                .rotate(12f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Contenedor de ilustración con animación de pulso y resplandor radial expansivo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .padding(bottom = 8.dp)
            ) {
                // Anillo exterior de onda expansiva (ripple)
                Box(
                    modifier = Modifier
                        .size((140 * rippleScale).dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = com.farbalapps.rinde.ui.theme.RindePrimary.copy(
                                alpha = ((1.35f - rippleScale) * 0.6f).coerceIn(0f, 1f)
                            ),
                            shape = CircleShape
                        )
                )

                // Resplandor / Glow de fondo azul sutil (Brand Primary color) que respira
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = glowAlpha),
                                    com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Icono temático principal de Metas / Ahorro (Cochinito) en contenedor circular
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(125.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            spotColor = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.45f)
                        )
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 2.5.dp,
                            color = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Metas y Ahorros",
                        tint = com.farbalapps.rinde.ui.theme.RindePrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }

                // Insignia / Badge pequeño flotante en la esquina inferior derecha
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-24).dp, y = (-28).dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🎯",
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título
            Text(
                text = "Empieza a ahorrar para tus sueños",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo
            Text(
                text = "Crea apartados personalizados con metas de ahorro y monitorea tu progreso semanal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de Acción Principal con icono y bordes suavemente redondeados (Pill)
            Button(
                onClick = onCreateFirstGoalClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.farbalapps.rinde.ui.theme.RindePrimary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crea tu primera meta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
