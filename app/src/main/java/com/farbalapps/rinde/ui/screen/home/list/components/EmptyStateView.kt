package com.farbalapps.rinde.ui.screen.home.list.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.RindeTheme

@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier
) {
    // Transición infinita para el balanceo pendular orbital suave (diferente a metas y bienvenida)
    val infiniteTransition = rememberInfiniteTransition(label = "orbitalSwing")

    val swingAngle by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swingAngle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val floatBadgeOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatBadgeOffset"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Contenedor Ilustrativo Animado de Lista Vacía
        EmptyStateIllustration(
            pulseScale = pulseScale,
            swingAngle = swingAngle,
            floatBadgeOffset = floatBadgeOffset
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.empty_list_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.empty_list_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStateViewPreview() {
    RindeTheme {
        EmptyStateView()
    }
}

@Composable
private fun EmptyStateIllustration(
    pulseScale: Float,
    swingAngle: Float,
    floatBadgeOffset: Float
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .padding(bottom = 16.dp)
    ) {
        // Halo de fondo con resplandor azul RindePrimary
        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RindePrimary.copy(alpha = 0.22f),
                            RindePrimary.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Contenedor Principal con animación de balanceo pendular
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .rotate(swingAngle)
                .scale(pulseScale)
                .size(130.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = RindePrimary.copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 2.dp,
                    color = RindePrimary.copy(alpha = 0.35f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.ChecklistRtl,
                contentDescription = null,
                tint = RindePrimary,
                modifier = Modifier.size(64.dp)
            )
        }

        // Ícono Flotante Decorativo 1 (Top Start)
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = RindePrimary.copy(alpha = 0.28f),
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = (24 + floatBadgeOffset).dp)
                .rotate(-16f)
        )

        // Ícono Flotante Decorativo 2 (Top End)
        Icon(
            imageVector = Icons.Default.ShoppingBag,
            contentDescription = null,
            tint = RindePrimary.copy(alpha = 0.32f),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = (18 + floatBadgeOffset).dp)
                .rotate(14f)
        )

        // Ícono Flotante Decorativo 3 (Bottom End)
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            tint = RindePrimary.copy(alpha = 0.25f),
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-15).dp, y = (-20 - floatBadgeOffset).dp)
                .rotate(-10f)
        )
    }
}

