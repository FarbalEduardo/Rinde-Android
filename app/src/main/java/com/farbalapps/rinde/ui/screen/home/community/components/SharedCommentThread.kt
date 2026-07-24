package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
    onDeleteReply: (String, String, String) -> Unit,
    onEditReply: (Reply) -> Unit,
    onReportComment: () -> Unit,
    onReportReply: (Reply) -> Unit,
    showLikeOption: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showReplies by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReportConfirm by remember { mutableStateOf(false) }

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

    if (showReportConfirm) {
        AlertDialog(
            onDismissRequest = { showReportConfirm = false },
            title = { Text("Reportar comentario") },
            text = { Text("¿Deseas reportar este comentario por contenido inapropiado? El equipo de moderación lo revisará.") },
            confirmButton = {
                TextButton(onClick = { showReportConfirm = false; onReportComment() }) {
                    Text("Reportar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    val isEditing = editingCommentId == comment.id

    Surface(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp).clickable { showMenu = true }
                        )
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (comment.authorId == currentUserId) {
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
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Reportar") },
                                    leadingIcon = { Icon(Icons.Default.Report, null) },
                                    onClick = { showMenu = false; showReportConfirm = true }
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
                    if (showLikeOption) {
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
                    }
                    Text(
                        text = "Responder",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onReplyClick() }
                    )
                }

                // Previews de respuestas (hilo de respuestas con conector visual)
                if (comment.repliesCount > 0) {
                    Surface(
                        onClick = { 
                            if (!showReplies) {
                                showReplies = true
                                onLoadReplies()
                            } else {
                                showReplies = false
                            }
                        },
                        shape = CircleShape,
                        color = if (!showReplies) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(
                            text = if (!showReplies) (if (comment.repliesCount == 1) "Ver 1 respuesta" else "Ver ${comment.repliesCount} respuestas ▼") else "Ocultar respuestas ▲",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (!showReplies) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = showReplies,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        if (replies.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                // Contenedor de respuestas con línea conector vertical al lado izquierdo
                                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                    // Línea vertical conectora
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)
                                            .width(3.dp)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        replies.forEach { reply ->
                                            SharedReplyItem(
                                                reply = reply,
                                                currentUserId = currentUserId,
                                                showLikeOption = showLikeOption,
                                                onLikeClick = { onLikeReply(reply.id) },
                                                onEditStart = { onEditReply(reply) },
                                                onDelete = { onDeleteReply(comment.id, reply.id, reply.authorId) },
                                                onReport = { onReportReply(reply) }
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                    }
                                }
                            }
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
    showLikeOption: Boolean = true,
    onLikeClick: () -> Unit,
    onEditStart: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReportConfirm by remember { mutableStateOf(false) }

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

    if (showReportConfirm) {
        AlertDialog(
            onDismissRequest = { showReportConfirm = false },
            title = { Text("Reportar respuesta") },
            text = { Text("¿Deseas reportar esta respuesta por contenido inapropiado?") },
            confirmButton = {
                TextButton(onClick = { showReportConfirm = false; onReport() }) {
                    Text("Reportar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
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
                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(reply.authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Box {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp).clickable { showMenu = true }
                    )
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (reply.authorId == currentUserId) {
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
                        } else {
                            DropdownMenuItem(
                                text = { Text("Reportar") },
                                leadingIcon = { Icon(Icons.Default.Report, null) },
                                onClick = { showMenu = false; showReportConfirm = true }
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
                if (showLikeOption) {
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
