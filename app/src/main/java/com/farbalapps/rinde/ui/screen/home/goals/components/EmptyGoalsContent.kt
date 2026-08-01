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
import androidx.compose.material.icons.filled.MonetizationOn
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

    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Contenedor de ilustración con animación de pulso y resplandor radial expansivo
            AnimatedGoalGraphic(
                rippleScale = rippleScale,
                pulseScale = pulseScale,
                glowAlpha = glowAlpha,
                floatingOffset = floatingOffset
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Textos
            EmptyGoalsTextContent()

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de Acción Principal
            CreateFirstGoalButton(onClick = onCreateFirstGoalClick)
        }
    }
}

@Composable
private fun AnimatedGoalGraphic(
    rippleScale: Float,
    pulseScale: Float,
    glowAlpha: Float,
    floatingOffset: Float
) {
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

        // 3 Íconos flotantes decorativos que rodean el ícono central
        // Flotante 1 (Top Start): Cochinito de Ahorro
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            tint = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.28f),
            modifier = Modifier
                .size(46.dp)
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = (24 + floatingOffset).dp)
                .rotate(-15f)
        )

        // Flotante 2 (Top End): Gráfica de Crecimiento
        Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.32f),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-14).dp, y = (18 + floatingOffset).dp)
                .rotate(12f)
        )

        // Flotante 3 (Bottom End): Moneda de Ahorro
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = null,
            tint = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.25f),
            modifier = Modifier
                .size(42.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-20).dp, y = (-20 - floatingOffset).dp)
                .rotate(-10f)
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


    }
}

@Composable
private fun EmptyGoalsTextContent() {
    Text(
        text = "Empieza a ahorrar para tus sueños",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Crea apartados personalizados con metas de ahorro y monitorea tu progreso semanal.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun CreateFirstGoalButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
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

@androidx.compose.ui.tooling.preview.Preview(name = "Empty Goals Light", showBackground = true)
@Composable
fun EmptyGoalsContentLightPreview() {
    com.farbalapps.rinde.ui.theme.RindeTheme {
        Surface {
            EmptyGoalsContent(onCreateFirstGoalClick = {})
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Empty Goals Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun EmptyGoalsContentDarkPreview() {
    com.farbalapps.rinde.ui.theme.RindeTheme {
        Surface {
            EmptyGoalsContent(onCreateFirstGoalClick = {})
        }
    }
}
