package com.farbalapps.rinde.ui.screen.profile.edit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun EditAvatarSection(
    photoUrl: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.BottomEnd
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.AccountCircle),
            placeholder = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.AccountCircle)
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(8.dp).fillMaxSize()
            )
        }
    }
}

@Composable
fun PrivacyToggleSection(
    isPrivate: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onToggle(!isPrivate) },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Perfil Privado", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isPrivate) "Solo tus seguidores ven tus posts" else "Cualquiera puede ver tus posts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = isPrivate, onCheckedChange = onToggle)
        }
    }
}
