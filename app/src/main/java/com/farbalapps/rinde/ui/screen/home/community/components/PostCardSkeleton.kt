package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.ui.theme.RindeTheme

/**
 * Genera un [Brush] animado con efecto shimmer tintado con los colores de Rinde.
 * Utiliza un ángulo de 20 grados para un efecto más natural y dinámico.
 */
@Composable
fun shimmerBrush(
    durationMillis: Int = 1200
): Brush {
    // Detectamos si el tema ACTUAL de la app es oscuro, ignorando el del sistema
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // Colores grises genéricos que se adaptan al tema claro/oscuro
    val baseColor = if (isDark) {
        androidx.compose.ui.graphics.Color.DarkGray.copy(alpha = 0.6f)
    } else {
        androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.5f)
    }
    
    val highlightColor = if (isDark) {
        androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.8f)
    } else {
        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
    }

    val shimmerColors = listOf(
        baseColor,
        highlightColor,
        baseColor,
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    
    // Ancho dinámico basado en la pantalla
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val shimmerWidth = screenWidthPx * 1.5f

    val translateAnim by transition.animateFloat(
        initialValue = -shimmerWidth,
        targetValue = shimmerWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    // Gradiente con ángulo de ~20 grados (tangente de 20° ≈ 0.364)
    val tan20 = 0.364f
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim * tan20),
        end = Offset(translateAnim + shimmerWidth, (translateAnim + shimmerWidth) * tan20)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER — Bloque shimmer genérico reutilizable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Caja rectangular con el efecto shimmer aplicado.
 * Úsala como bloque base para replicar cualquier elemento de la UI.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// POST CARD SKELETON — Réplica visual fiel al PostCard real
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Skeleton animado que imita la estructura visual de [PostCard].
 * Layout idéntico al card real para evitar el "flash" de transición:
 *   - Header compacto: avatar 24dp + nombre + fecha
 *   - Cuerpo horizontal: imagen 110dp × 110dp a la izquierda + columna de texto a la derecha
 *   - Footer: píldora de votos + nombre de tienda
 */
@Composable
fun PostCardSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = shimmerBrush()
    // Respetar el tema forzado de la app
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBgColor = if (isDark) com.farbalapps.rinde.ui.theme.BackgroundDark
                      else com.farbalapps.rinde.ui.theme.BackgroundLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(color = cardBgColor, shape = RectangleShape)
    ) {
        Column {
            // ── 1. HEADER: Avatar 24dp + Nombre + Fecha ──────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 6.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Nombre autor
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(Modifier.width(6.dp))
                // Separador ·
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush)
                )
                Spacer(Modifier.width(6.dp))
                // Fecha
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(Modifier.weight(1f))
                // Bookmark icon placeholder
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(Modifier.width(8.dp))
            }

            // ── 2. CUERPO HORIZONTAL: Imagen + Columna de texto ─────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Imagen cuadrada 110dp
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush)
                )

                Spacer(Modifier.width(16.dp))

                // Columna de texto derecha
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Título línea 1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(17.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    // Título línea 2
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(17.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    // Precio tachado
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    // Precio principal
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }

            // ── 3. FOOTER: Votos + Tienda ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Píldora de votos
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(50))
                        .background(brush)
                )
                // Nombre tienda
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }

            // Divider inferior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        if (isDark) com.farbalapps.rinde.ui.theme.Blue20.copy(alpha = 0.2f)
                        else com.farbalapps.rinde.ui.theme.Blue90.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

/**
 * Skeleton para scroll infinito.
 */
@Composable
fun PaginationSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ShimmerBox(
            modifier = Modifier.fillMaxWidth(0.4f).height(32.dp),
            shape = RoundedCornerShape(50)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostCardSkeletonPreview() {
    RindeTheme {
        PostCardSkeleton()
    }
}
