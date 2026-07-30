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
 * Tarjeta de recomendación aleatoria de IA Chef (compacta e inspiradora).
 */
@Composable
fun ChefRandomRecommendationCard(
    modifier: Modifier = Modifier
) {
    val randomTip = remember { CHEF_RECOMMENDATIONS.random() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "IA Chef",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "IA CHEF SUGERENCIA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = randomTip,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Tarjeta comodín punteada para agregar nueva meta si existen slots disponibles (menos de 3 metas).
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
            .height(100.dp)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Crea un nuevo objetivo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Disponible $currentGoalCount de 3 metas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
