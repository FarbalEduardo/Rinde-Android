package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.ui.theme.RindePrimary
import java.util.*

@Composable
fun SharedCommentThread(
    comment: Comment,
    replies: List<Reply>,
    currentUserId: String,
    editingCommentId: String?,
    editingText: String,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onEditStart: (Comment) -> Unit,
    onEditTextChange: (String) -> Unit,
    onEditSave: () -> Unit,
    onEditCancel: () -> Unit,
    onDelete: () -> Unit,
    onLoadReplies: () -> Unit,
    onLikeReply: (String) -> Unit,
    onDeleteReply: (String, String) -> Unit,
    onEditReply: (Reply) -> Unit,
    modifier: Modifier = Modifier
) {
    var showReplies by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar comentario") },
            text = { Text("¿Estás seguro de que deseas eliminar este comentario?") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    val isEditing = editingCommentId == comment.id

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (comment.authorPhotoUrl != null) {
                    AsyncImage(
                        model = comment.authorPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Cabecera con menú
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (comment.authorId == currentUserId) {
                        Box {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp).clickable { showMenu = true }
                            )
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { showMenu = false; onEditStart(comment) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; showDeleteConfirm = true }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))

                if (isEditing) {
                    OutlinedTextField(
                        value = editingText,
                        onValueChange = onEditTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        TextButton(onClick = onEditCancel) { Text("Cancelar") }
                        Button(onClick = onEditSave) { Text("Guardar") }
                    }
                } else {
                    Text(
                        text = comment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                    if (comment.imageUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = comment.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = formatSharedTime(comment.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (comment.isEdited) {
                        Text(
                            text = "· Editado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Row(
                        modifier = Modifier.clickable { onLikeClick() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (comment.likesCount > 0) {
                            Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(12.dp), tint = RindePrimary)
                            Text("${comment.likesCount}", style = MaterialTheme.typography.labelSmall, color = RindePrimary, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Me gusta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        text = "Responder",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onReplyClick() }
                    )
                }

                // Previews de respuestas
                if (comment.repliesCount > 0) {
                    if (!showReplies) {
                        // Si hay 1 respuesta o más, al estar colapsado mostramos "Ver X respuestas" si >1
                        if (comment.repliesCount == 1) {
                            // Cargar la respuesta automáticamente es complejo si no la tenemos. 
                            // Simplificamos: Mostramos el botón "Ver 1 respuesta" igual.
                            Text(
                                text = "Ver 1 respuesta",
                                style = MaterialTheme.typography.labelMedium,
                                color = RindePrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp).clickable { 
                                    showReplies = true
                                    onLoadReplies() 
                                }
                            )
                        } else {
                            Text(
                                text = "Ver ${comment.repliesCount} respuestas ▼",
                                style = MaterialTheme.typography.labelMedium,
                                color = RindePrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp).clickable {
                                    showReplies = true
                                    onLoadReplies()
                                }
                            )
                        }
                    } else {
                        // Ocultar respuestas
                        Text(
                            text = "Ocultar respuestas ▲",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp).clickable { showReplies = false }
                        )
                        
                        if (replies.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            replies.forEach { reply ->
                                SharedReplyItem(
                                    reply = reply,
                                    currentUserId = currentUserId,
                                    isEditing = editingCommentId == null && editingText.isNotBlank() && reply.id == null, // Simplified for now, passing editing state better would need more params
                                    onLikeClick = { onLikeReply(reply.id) },
                                    onEditStart = { onEditReply(reply) },
                                    onDelete = { onDeleteReply(comment.id, reply.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SharedReplyItem(
    reply: Reply,
    currentUserId: String,
    isEditing: Boolean,
    onLikeClick: () -> Unit,
    onEditStart: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar respuesta") },
            text = { Text("¿Estás seguro de que deseas eliminar esta respuesta?") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (reply.authorPhotoUrl != null) {
                AsyncImage(
                    model = reply.authorPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(reply.authorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (reply.authorId == currentUserId) {
                    Box {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp).clickable { showMenu = true }
                        )
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = { showMenu = false; onEditStart() }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; showDeleteConfirm = true }
                            )
                        }
                    }
                }
            }
            Text(reply.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatSharedTime(reply.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                if (reply.isEdited) {
                    Text(
                        text = "· Editado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                Row(
                    modifier = Modifier.clickable { onLikeClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (reply.likesCount > 0) {
                        Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(10.dp), tint = RindePrimary)
                        Text("${reply.likesCount}", style = MaterialTheme.typography.labelSmall, color = RindePrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    } else {
                        Text("Me gusta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

fun formatSharedTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${days}d"
    }
}
