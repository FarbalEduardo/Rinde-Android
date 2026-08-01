package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.style.TextOverflow

/**
 * Lista de 15 recomendaciones e inspiraciones de IA Chef sobre finanzas y ahorro.
 */
private val CHEF_RECOMMENDATIONS = listOf(
    "Planifica tus compras semanales con anticipación para evitar compras impulsivas y ahorrar hasta un 20%.",
    "Revisa tu despensa antes de ir al super. Comprar solo lo necesario acelera tus metas de ahorro.",
    "Ahorrar $20 diarios suma $600 al mes. Los pequeños hábitos generan grandes resultados.",
    "Compara precios por unidad o kilo. Los empaques familiares suelen rendir más por tu dinero.",
    "Reducir consumos hormiga semanales te acerca 1 mes antes a tu meta principal.",
    "Asigna un presupuesto fijo a tu lista antes de salir de casa y mantente dentro del límite.",
    "El mejor momento para guardar dinero en tu meta es el día que recibes tu ingreso.",
    "Aprovecha frutas y verduras de temporada; son más frescas, nutritivas y económicas.",
    "No vayas a comprar con hambre: gastas hasta 30% más en antojos de acuerdo a estudios.",
    "Cada peso que no gastas en productos innecesarios es un paso directo a tu libertad financiera.",
    "Mantén tu meta visible todos los días para recordar por qué estás construyendo este hábito.",
    "Revisa suscripciones que no utilices. Ese dinero redirigido impulsa tus metas de ahorro.",
    "Cocinar en casa y llevar tus propios alimentos incrementa tu capacidad de ahorro semanal.",
    "Celebrar los pequeños logros de tu avance mantiene alta tu motivación para ahorrar.",
    "El ahorro no es lo que te sobra al gastar, es lo primero que separas para tu futuro."
)

/**
 * Tarjeta de recomendación aleatoria de IA Chef (Banner In-Feed ultra compacto M3).
 */
@Composable
fun ChefRandomRecommendationCard(
    modifier: Modifier = Modifier
) {
    var isDismissed by remember { mutableStateOf(false) }
    if (isDismissed) return

    val randomTip = remember { CHEF_RECOMMENDATIONS.random() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CONSEJO RINDE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = randomTip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            IconButton(
                onClick = { isDismissed = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Descartar tip",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Tarjeta comodín punteada para agregar nueva meta si existen slots disponibles (menos de 2 metas).
 */
@Composable
fun DashedAddGoalCard(
    currentGoalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        DashedAddGoalCardContent(currentGoalCount = currentGoalCount)
    }
}

@Composable
private fun DashedAddGoalCardContent(currentGoalCount: Int) {
    val availableCount = (2 - currentGoalCount).coerceAtLeast(0)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Crea un nuevo objetivo",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Disponible $availableCount de 2 metas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
