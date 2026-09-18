package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.home.community.CommunityTab
import com.farbalapps.rinde.ui.theme.RindeTheme

/**
 * Estado vacío para las pestañas de la comunidad (Discover, Hot, Saved).
 * Diseñado con elevación tonal M3 y animaciones sutiles continuas.
 */
@Composable
fun EmptyFeedState(
    tab: CommunityTab,
    modifier: Modifier = Modifier
) {
    val (title, subtitle) = when (tab) {
        CommunityTab.DISCOVER -> Pair(
            stringResource(R.string.community_empty_discover_title),
            stringResource(R.string.community_empty_discover_desc)
        )
        CommunityTab.HOT -> Pair(
            stringResource(R.string.community_empty_hot_title),
            stringResource(R.string.community_empty_hot_desc)
        )
        CommunityTab.SAVED -> Pair(
            stringResource(R.string.community_empty_saved_title),
            stringResource(R.string.community_empty_saved_desc)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "communityEmptyTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingOffset"
    )

    val swingAngle by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swingAngle"
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Contenedor Ilustrativo con ícono central grande + 3 íconos flotantes
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(220.dp)
                        .padding(bottom = 8.dp)
                ) {
                    // Resplandor radial de fondo
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = glowAlpha),
                                        primaryColor.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // 3 Íconos flotantes decorativos contextuales
                    when (tab) {
                        CommunityTab.DISCOVER -> {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .size(46.dp)
                                    .align(Alignment.TopStart)
                                    .offset(x = 10.dp, y = (24 + floatingOffset).dp)
                                    .rotate(-18f)
                            )
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.32f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-14).dp, y = (18 + floatingOffset).dp)
                                    .rotate(14f)
                            )
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.24f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-20).dp, y = (-20 - floatingOffset).dp)
                                    .rotate(-10f)
                            )
                        }
                        CommunityTab.HOT -> {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.30f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.TopStart)
                                    .offset(x = 10.dp, y = (20 + floatingOffset).dp)
                                    .rotate(-15f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-16).dp, y = (22 + floatingOffset).dp)
                                    .rotate(15f)
                            )
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .align(Alignment.BottomStart)
                                    .offset(x = 16.dp, y = (-18 - floatingOffset).dp)
                                    .rotate(-12f)
                            )
                        }
                        CommunityTab.SAVED -> {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .size(46.dp)
                                    .align(Alignment.TopStart)
                                    .offset(x = 12.dp, y = (22 + floatingOffset).dp)
                                    .rotate(-14f)
                            )
                            Icon(
                                imageVector = Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.32f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-14).dp, y = (16 + floatingOffset).dp)
                                    .rotate(16f)
                            )
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .size(42.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-22).dp, y = (-18 - floatingOffset).dp)
                                    .rotate(-10f)
                            )
                        }
                    }

                    // Contenedor Principal con el Ícono Central Grande (130dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .graphicsLayer {
                                rotationZ = swingAngle
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .size(130.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = CircleShape,
                                spotColor = primaryColor.copy(alpha = 0.40f)
                            )
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 2.dp,
                                color = primaryColor.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                    ) {
                        val mainIcon = when (tab) {
                            CommunityTab.DISCOVER -> Icons.Default.Explore
                            CommunityTab.HOT -> Icons.Default.Whatshot
                            CommunityTab.SAVED -> Icons.Default.Bookmark
                        }
                        Icon(
                            imageVector = mainIcon,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Textos informativos
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun EmptyFeedStatePreview() {
    RindeTheme {
        EmptyFeedState(tab = CommunityTab.DISCOVER)
    }
}
