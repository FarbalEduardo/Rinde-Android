package com.farbalapps.rinde.ui.screen.home.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import kotlinx.coroutines.delay

private data class SetupTip(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun SetupIncomeCard(
    onSetupIncomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tips = remember {
        listOf(
            SetupTip(
                titleRes = R.string.dashboard_empty_carousel_title_1,
                descRes = R.string.dashboard_empty_carousel_desc_1,
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = Color(0xFF66BB6A)
            ),
            SetupTip(
                titleRes = R.string.dashboard_empty_carousel_title_2,
                descRes = R.string.dashboard_empty_carousel_desc_2,
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                accentColor = Color(0xFFFFA726)
            ),
            SetupTip(
                titleRes = R.string.dashboard_empty_carousel_title_3,
                descRes = R.string.dashboard_empty_carousel_desc_3,
                icon = Icons.Default.Flag,
                accentColor = Color(0xFF26A69A)
            ),
            SetupTip(
                titleRes = R.string.dashboard_empty_carousel_title_4,
                descRes = R.string.dashboard_empty_carousel_desc_4,
                icon = Icons.AutoMirrored.Filled.EventNote,
                accentColor = Color(0xFF42A5F5)
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-carrusel de textos cada 3 segundos
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L)
            currentIndex = (currentIndex + 1) % tips.size
        }
    }

    // Micro-animaciones continuas
    val infiniteTransition = rememberInfiniteTransition(label = "setup_income_infinite")

    // Flotación y respiración suave del ícono
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_float_y"
    )

    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    // Borde con respiración ambiental sutil
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    // Micro-interacción de la flecha en el botón CTA
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Contenedor del ícono principal con animación de flotación y respiración
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer {
                        translationY = floatY
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Título principal
            Text(
                text = stringResource(id = R.string.dashboard_empty_income_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Carrusel dinámico de textos animados con altura mínima fija para evitar saltos en la UI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + slideInHorizontally(
                            animationSpec = tween(400),
                            initialOffsetX = { width -> width / 3 }
                        )).togetherWith(
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { width -> -width / 3 }
                            )
                        )
                    },
                    label = "carousel_content"
                ) { targetIdx ->
                    val tip = tips[targetIdx]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Badge con el pilar de valor
                        Surface(
                            shape = CircleShape,
                            color = tip.accentColor.copy(alpha = 0.14f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = tip.icon,
                                    contentDescription = null,
                                    tint = tip.accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = stringResource(id = tip.titleRes),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = tip.accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Descripción del beneficio
                        Text(
                            text = stringResource(id = tip.descRes),
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 17.sp,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de píldoras indicadoras interactivas
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tips.indices.forEach { index ->
                    val isSelected = index == currentIndex
                    val pillWidth by animateDpAsState(
                        targetValue = if (isSelected) 20.dp else 6.dp,
                        animationSpec = tween(300),
                        label = "pill_width_$index"
                    )
                    val pillColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
                        },
                        animationSpec = tween(300),
                        label = "pill_color_$index"
                    )

                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(pillWidth)
                            .clip(CircleShape)
                            .background(pillColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { currentIndex = index }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botón CTA con micro-interacción en la flecha
            Button(
                onClick = onSetupIncomeClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.dashboard_btn_setup_income),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            translationX = arrowOffset
                        }
                )
            }
        }
    }
}
