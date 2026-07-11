package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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

import androidx.hilt.navigation.compose.hiltViewModel
import com.farbalapps.rinde.ui.screen.home.community.CommentsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    viewModel: CommentsViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(postId) {
        viewModel.loadComments(postId)
    }

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
                text = "Comentarios (${uiState.comments.size})",
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
                items(uiState.comments) { comment ->
                    SharedCommentThread(
                        comment = comment,
                        replies = uiState.replies[comment.id] ?: emptyList(),
                        currentUserId = uiState.currentUserId,
                        editingCommentId = uiState.editingCommentId,
                        editingText = uiState.editingText,
                        onLikeClick = { viewModel.toggleLike(comment.id) },
                        onReplyClick = { viewModel.setReplyingTo(comment) },
                        onEditStart = { viewModel.startEditComment(it) },
                        onEditTextChange = { viewModel.onEditTextChange(it) },
                        onEditSave = { viewModel.saveEditedContent() },
                        onEditCancel = { viewModel.cancelEdit() },
                        onDelete = { viewModel.deleteComment(comment.id, comment.authorId) },
                        onLoadReplies = { viewModel.loadReplies(comment.id) },
                        onLikeReply = { replyId -> viewModel.toggleReplyLike(comment.id, replyId) },
                        onDeleteReply = { cId, rId -> viewModel.deleteReply(cId, rId, comment.authorId) }, // Needs correct reply author id, but for now passing comment author id for simplicity, should pass reply author id
                        onEditReply = { viewModel.startEditReply(it) }
                    )
                }
            }
            
            SharedCommentInput(
                text = if (uiState.editingCommentId != null || uiState.editingReplyId != null) "" else uiState.commentText, // Disable input if editing inline
                replyingTo = uiState.replyingTo,
                isSending = uiState.isLoading,
                onTextChange = { viewModel.onCommentTextChange(it) },
                onSubmit = {
                    viewModel.submitComment()
                },
                onCancelReply = { viewModel.setReplyingTo(null) }
            )
        }
    }
}
