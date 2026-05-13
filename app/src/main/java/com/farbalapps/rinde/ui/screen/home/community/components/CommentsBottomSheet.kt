package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    comments: List<Comment>,
    replies: Map<String, List<Reply>>,
    onCommentSubmit: (String) -> Unit,
    onReplySubmit: (String, String) -> Unit,
    onLikeComment: (String) -> Unit,
    onLikeReply: (String, String) -> Unit,
    onLoadReplies: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth()
        ) {
            Text(
                text = "Comentarios (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(comments) { comment ->
                    CommentItem(
                        comment = comment,
                        replies = replies[comment.id] ?: emptyList(),
                        onLikeClick = { onLikeComment(comment.id) },
                        onReplyClick = { /* Handle replying to logic */ },
                        onLoadReplies = { onLoadReplies(comment.id) },
                        onLikeReply = { replyId -> onLikeReply(comment.id, replyId) }
                    )
                }
            }
            
            // Input Area
            CommentInputArea(onSubmit = onCommentSubmit)
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    replies: List<Reply>,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onLoadReplies: () -> Unit,
    onLikeReply: (String) -> Unit
) {
    var showReplies by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = comment.authorPhotoUrl,
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = comment.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (comment.imageUrl != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = comment.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Hace ${formatTime(comment.timestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Me gusta",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (comment.likesCount > 0) RindePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onLikeClick() }
                    )
                    Text(
                        text = "Responder",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onReplyClick() }
                    )
                    if (comment.likesCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(10.dp), tint = RindePrimary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "${comment.likesCount}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (comment.repliesCount > 0 && !showReplies) {
                    Text(
                        text = "— Ver ${comment.repliesCount} réplicas...",
                        style = MaterialTheme.typography.labelSmall,
                        color = RindePrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp).clickable { 
                            showReplies = true
                            onLoadReplies()
                        }
                    )
                }

                if (showReplies) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        replies.forEach { reply ->
                            ReplyItem(reply = reply, onLikeClick = { onLikeReply(reply.id) })
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReplyItem(reply: Reply, onLikeClick: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        AsyncImage(
            model = reply.authorPhotoUrl,
            contentDescription = null,
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = reply.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reply.text,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (reply.imageUrl != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AsyncImage(
                            model = reply.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hace ${formatTime(reply.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = "Me gusta",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (reply.likesCount > 0) RindePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onLikeClick() },
                    fontSize = 10.sp
                )
                if (reply.likesCount > 0) {
                    Text(text = "❤️ ${reply.likesCount}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun CommentInputArea(onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp).navigationBarsPadding().imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Image picker logic */ }) {
                Icon(Icons.Default.PhotoCamera, null, tint = RindePrimary)
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Escribe un comentario...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = { 
                    if (text.isNotBlank()) {
                        onSubmit(text)
                        text = ""
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Icon(Icons.Default.Send, null, tint = if (text.isNotBlank()) RindePrimary else Color.Gray)
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
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
