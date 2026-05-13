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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.profile.ProfileUiState

@Composable
fun ProfileHeader(
    uiState: ProfileUiState,
    onEditProfile: () -> Unit,
    toggleFollow: () -> Unit
) {
    val profile = uiState.profile
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = dimensionResource(id = R.dimen.padding_small))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(modifier = Modifier.size(dimensionResource(id = R.dimen.profile_avatar_size))) {
                AsyncImage(
                    model = profile?.photoUrl,
                    contentDescription = stringResource(id = R.string.profile_avatar_desc),
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.AccountCircle),
                    placeholder = rememberVectorPainter(Icons.Default.AccountCircle)
                )
            }
            
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_large)))
            
            // Stats Row
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem(count = profile?.postsCount ?: 0, label = stringResource(id = R.string.profile_stat_posts))
                ProfileStatItem(count = profile?.followersCount ?: 0, label = stringResource(id = R.string.profile_stat_followers))
                ProfileStatItem(count = profile?.followingCount ?: 0, label = stringResource(id = R.string.profile_stat_following))
            }
        }
        
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = profile?.name ?: stringResource(id = R.string.profile_default_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            if (profile?.isVerified == true) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = stringResource(id = R.string.badge_verified),
                    tint = com.farbalapps.rinde.ui.theme.VerifiedBadgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        
        if (profile?.rating ?: 0f > 0f) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xsmall)))
            StarRating(rating = profile?.rating ?: 0f, reviewsCount = profile?.reviewsCount ?: 0)
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))
        ) {
            if (uiState.isCurrentUser) {
                Button(
                    onClick = onEditProfile,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(stringResource(id = R.string.profile_btn_edit), fontWeight = FontWeight.ExtraBold)
                }
                
                IconButton(
                    onClick = { /* Share profile logic */ },
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                }
            } else {
                val isFollowing = uiState.isFollowing
                Button(
                    onClick = toggleFollow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f),
                    colors = if (isFollowing) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(
                        if (isFollowing) stringResource(id = R.string.profile_btn_following) else stringResource(id = R.string.profile_btn_follow), 
                        fontWeight = FontWeight.Bold
                    )
                }
                
                OutlinedButton(
                    onClick = { /* Message logic */ },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.profile_btn_message), fontWeight = FontWeight.Bold)
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
                modifier = Modifier.size(dimensionResource(id = R.dimen.padding_medium)),
                tint = color
            )
        }
        if (reviewsCount > 0) {
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_xsmall)))
            Text(
                text = "($reviewsCount)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
