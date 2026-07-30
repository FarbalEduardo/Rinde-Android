package com.farbalapps.rinde.ui.screen.welcome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.components.AuthBackground
import com.farbalapps.rinde.ui.theme.RindeTheme

@Composable
fun WelcomeScreen(
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    AuthBackground {
        // Fondo de partículas flotantes animadas representando los módulos clave de Rinde (lalo 3)
        WelcomeBackgroundParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = dimensionResource(id = R.dimen.max_width_phone))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.4f))

            // Sección Central: Logo 'R' Animado por Pulso + Títulos
            WelcomeHeaderSection()

            Spacer(modifier = Modifier.weight(0.4f))

            // Sección Inferior: Botones de Acción Estilo lalo 3 (Ubicados más arriba)
            WelcomeActionButtonsSection(
                onSignUpClick = onSignUpClick,
                onSignInClick = onSignInClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Fondo con iconos flotantes translúcidos representando los 4 pilares de la app Rinde.
 */
@Composable
private fun WelcomeBackgroundParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particleDrift")

    val drift1 by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift1"
    )

    val drift2 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift2"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Icono Lista de compras
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.14f),
            modifier = Modifier
                .size(68.dp)
                .align(Alignment.TopStart)
                .offset(x = 24.dp, y = (60 + drift1).dp)
                .rotate(-12f)
        )

        // Icono Comunidad
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            tint = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.12f),
            modifier = Modifier
                .size(76.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-28).dp, y = (90 + drift2).dp)
                .rotate(15f)
        )

        // Icono Metas / Ahorros
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            tint = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.14f),
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterStart)
                .offset(x = 16.dp, y = (140 + drift2).dp)
                .rotate(8f)
        )

        // Icono Chef AI
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            tint = com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.12f),
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-20).dp, y = (160 + drift1).dp)
                .rotate(-18f)
        )
    }
}

/**
 * Logo 'R' Animado mediante Pulso / Respiración Viva (distinto a Metas) y Títulos de Bienvenida.
 */
@Composable
private fun WelcomeHeaderSection() {
    // Animación de flotación vertical 3D dinámica para el logo oficial 'R'
    val infiniteTransition = rememberInfiniteTransition(label = "welcomeVerticalFloat")

    val translateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translateY"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotateAngle"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Contenedor del Logo 'R' con animación flotante elevada en 3D
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(190.dp)
                .padding(bottom = 12.dp)
        ) {
            // Halo de brillo radial de fondo
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.25f),
                                com.farbalapps.rinde.ui.theme.RindePrimary.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Círculo contenedor principal flotante 3D
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(y = translateY.dp)
                    .rotate(rotateAngle)
                    .size(130.dp)
                    .shadow(
                        elevation = 20.dp,
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
                    .padding(18.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = com.farbalapps.rinde.ui.theme.RindePrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

/**
 * Secciones de Botones de Acción apilados estilo "lalo 3".
 */
@Composable
private fun WelcomeActionButtonsSection(
    onSignUpClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Botón Primario: Registrarse (Conserva color azul RindePrimary en Light y Dark themes)
        Button(
            onClick = onSignUpClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = com.farbalapps.rinde.ui.theme.RindePrimary,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            )
        ) {
            Text(
                text = stringResource(id = R.string.btn_sign_up),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Botón Secundario: Iniciar sesión (Borde y texto azul RindePrimary en Light y Dark themes)
        OutlinedButton(
            onClick = onSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = com.farbalapps.rinde.ui.theme.RindePrimary
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = com.farbalapps.rinde.ui.theme.RindePrimary
            )
        ) {
            Text(
                text = stringResource(id = R.string.btn_sign_in),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = com.farbalapps.rinde.ui.theme.RindePrimary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    RindeTheme {
        WelcomeScreen(onSignInClick = {}, onSignUpClick = {})
    }
}
