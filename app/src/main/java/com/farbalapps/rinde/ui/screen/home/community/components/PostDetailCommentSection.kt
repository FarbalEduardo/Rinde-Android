package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.ui.screen.home.community.PostDetailUiState

/**
 * Encabezado de la lista de comentarios, incluyendo contador y estado de carga/vacío.
 */
fun LazyListScope.CommentsHeaderItem(
    commentsCount: Int,
    isLoadingComments: Boolean,
    commentsEmpty: Boolean
) {
    item(key = "comments_header") {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp
                )
            ) {
                Text(
                    text = "Comentarios ($commentsCount)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingComments && commentsEmpty) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(3) { CommentSkeleton() }
                    }
                }
            }
        }
    }
}

/**
 * Renderizado de cada hilo de comentario con sus respuestas anidadas y opciones de edición/eliminación/reporte.
 */
fun LazyListScope.CommentsListItems(
    uiState: PostDetailUiState,
    onLikeComment: (String) -> Unit,
    onReplyClick: (Comment) -> Unit,
    onEditCommentStart: (Comment) -> Unit,
    onEditTextChange: (String) -> Unit,
    onEditCommentSave: () -> Unit,
    onEditCommentCancel: () -> Unit,
    onDeleteComment: (String, String) -> Unit,
    onLoadReplies: (String) -> Unit,
    onLikeReply: (String, String) -> Unit,
    onDeleteReply: (String, String, String) -> Unit,
    onEditReplyStart: (Reply) -> Unit,
    onReportComment: (Comment) -> Unit,
    onReportReply: (Reply) -> Unit
) {
    items(
        items = uiState.comments,
        key = { comment -> comment.id }
    ) { comment ->
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SharedCommentThread(
                    comment = comment,
                    replies = uiState.replies[comment.id] ?: emptyList(),
                    currentUserId = uiState.currentUserId,
                    editingCommentId = uiState.editingCommentId,
                    editingText = uiState.editingText,
                    onLikeClick = { onLikeComment(comment.id) },
                    onReplyClick = { onReplyClick(comment) },
                    showLikeOption = false,
                    onEditStart = onEditCommentStart,
                    onEditTextChange = onEditTextChange,
                    onEditSave = onEditCommentSave,
                    onEditCancel = onEditCommentCancel,
                    onDelete = { onDeleteComment(comment.id, comment.authorId) },
                    onLoadReplies = { onLoadReplies(comment.id) },
                    onLikeReply = { replyId -> onLikeReply(comment.id, replyId) },
                    onDeleteReply = onDeleteReply,
                    onEditReply = onEditReplyStart,
                    onReportComment = { onReportComment(comment) },
                    onReportReply = onReportReply,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
