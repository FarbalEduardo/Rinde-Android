package com.farbalapps.rinde.ui.screen.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.farbalapps.rinde.ui.screen.profile.ProfileUiState

@Composable
fun ProfileHeader(
    uiState: ProfileUiState,
    onEditProfile: () -> Unit,
    toggleFollow: () -> Unit
) {
    val profile = uiState.profile
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(modifier = Modifier.size(86.dp)) {
                AsyncImage(
                    model = profile?.photoUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.AccountCircle),
                    placeholder = rememberVectorPainter(Icons.Default.AccountCircle)
                )
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem(count = profile?.postsCount ?: 0, label = "Posts")
                ProfileStatItem(count = profile?.followersCount ?: 0, label = "Seguidores")
                ProfileStatItem(count = profile?.followingCount ?: 0, label = "Seguidos")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Name & Bio Area
        Text(
            text = profile?.name ?: "Usuario",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        
        if (profile?.rating ?: 0f > 0f) {
            Spacer(modifier = Modifier.height(4.dp))
            StarRating(rating = profile?.rating ?: 0f, reviewsCount = profile?.reviewsCount ?: 0)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.isCurrentUser) {
                Button(
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Editar Perfil", fontWeight = FontWeight.Bold)
                }
                
                IconButton(
                    onClick = { /* Share profile logic */ },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val isFollowing = uiState.isFollowing
                Button(
                    onClick = toggleFollow,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    colors = if (isFollowing) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (isFollowing) "Siguiendo" else "Seguir", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = { /* Message logic */ },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mensaje", fontWeight = FontWeight.Bold)
                }
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

@Composable
fun StarRating(rating: Float, reviewsCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            val color = if (index < rating.toInt()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
        if (reviewsCount > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($reviewsCount)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


