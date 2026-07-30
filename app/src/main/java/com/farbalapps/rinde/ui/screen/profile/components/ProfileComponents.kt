package com.farbalapps.rinde.ui.screen.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.profile.ProfileUiState
import com.farbalapps.rinde.ui.theme.AmberStarColor

@Composable
fun ProfileHeader(
    uiState: ProfileUiState,
    onEditProfile: () -> Unit
) {
    val profile = uiState.profile
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.padding_small))
    ) {
        // ZONA 1: Header superior (Avatar + Estadísticas de publicaciones y comentarios)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con protección de URL nula/vacía
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.profile_avatar_size))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val photoUrl = profile?.photoUrl?.takeIf { it.isNotBlank() }
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = stringResource(id = R.string.profile_avatar_desc),
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_large)))

            // Estadísticas (Publicaciones & Comentarios)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem(
                    count = profile?.postsCount ?: 0,
                    label = stringResource(id = R.string.profile_stat_posts)
                )
                ProfileStatItem(
                    count = profile?.commentsCount ?: 0,
                    label = "Comentarios"
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))

        // ZONA 2: Nombre de Usuario + Badge de Verificación
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = profile?.name ?: stringResource(id = R.string.profile_default_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            if (profile?.isVerified == true) {
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_xsmall)))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = stringResource(id = R.string.badge_verified),
                    tint = com.farbalapps.rinde.ui.theme.VerifiedBadgeColor,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_small))
                )
            }
        }

        // ZONA 4: Calificación de la comunidad (Estrellas doradas promediadas de publicaciones validadas)
        val computedRating = uiState.computedRating
        val ratedPostsCount = uiState.ratedPostsCount
        if (computedRating != null && ratedPostsCount > 0) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_small)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        val isFull = index + 1 <= computedRating.toInt()
                        val isHalf = !isFull && (index < computedRating)
                        val starIcon = when {
                            isFull -> Icons.Default.Star
                            isHalf -> Icons.Default.StarHalf
                            else -> Icons.Default.StarOutline
                        }
                        val tint = if (isFull || isHalf) AmberStarColor else MaterialTheme.colorScheme.outlineVariant
                        Icon(
                            imageVector = starIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = tint
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${"%.1f".format(computedRating)} ($ratedPostsCount ofertas)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ZONA 5: Nivel de Confianza (Trust Level Badge)
        if ((profile?.trustScore ?: 0f) > 0f) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_small)))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nivel de confianza: ${profile?.trustLevel ?: "NUEVO"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // ZONA 6: Chips de Zonas de Caza e Intereses
        if (!profile?.zonasDeCaza.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_small)))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(profile?.zonasDeCaza ?: emptyList()) { zona ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(text = zona, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = null
                    )
                }
            }
        }

        if (!profile?.interests.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xsmall)))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(profile?.interests ?: emptyList()) { interest ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(text = interest, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        ),
                        border = null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))

        // ZONA 7: Acciones (Perfil propio)
        if (uiState.isCurrentUser) {
            Button(
                onClick = onEditProfile,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(stringResource(id = R.string.profile_btn_edit), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun ProfileStatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
