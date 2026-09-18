package com.farbalapps.rinde.ui.screen.home.community

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.ui.screen.home.community.components.AuthorAndDescriptionSection
import com.farbalapps.rinde.ui.screen.home.community.components.CommentsHeaderItem
import com.farbalapps.rinde.ui.screen.home.community.components.CommentsListItems
import com.farbalapps.rinde.ui.screen.home.community.components.ImageAndHeaderSection
import com.farbalapps.rinde.ui.screen.home.community.components.PostDetailSkeleton
import com.farbalapps.rinde.ui.screen.home.community.components.PostImageCarousel
import com.farbalapps.rinde.ui.screen.home.community.components.SharedCommentInput
import com.farbalapps.rinde.ui.screen.home.community.components.StoreAndExpirationSection
import com.farbalapps.rinde.ui.screen.home.community.components.VoteStateBannerItem
import com.farbalapps.rinde.ui.screen.home.community.components.VotingSectionItem
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.RindeTheme
import kotlinx.coroutines.launch

/** Re-export para retrocompatibilidad con componentes existentes */
@Composable
fun PostImageCarousel(photos: List<String>, modifier: Modifier = Modifier) {
    com.farbalapps.rinde.ui.screen.home.community.components.PostImageCarousel(photos, modifier)
}

/**
 * Pantalla de Detalle de Oferta (Stateful).
 * Observa estado del ViewModel, Snackbar y gestiona el flujo de comentarios.
 *
 * [HU-03] Detalle de oferta, comentarios, votos y reporte de expiración.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    scrollToComments: Boolean = false,
    isExpiredNotice: Boolean = false,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onEditPost: (String) -> Unit = {},
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        bottomBar = {
            uiState.post?.let {
                SharedCommentInput(
                    text = if (uiState.replyingToComment != null) uiState.replyText else uiState.commentText,
                    replyingTo = uiState.replyingToComment,
                    isSending = uiState.isSendingComment,
                    onTextChange = { if (uiState.replyingToComment != null) viewModel.onReplyTextChange(it) else viewModel.onCommentTextChange(it) },
                    onSubmit = { if (uiState.replyingToComment != null) viewModel.submitReply() else viewModel.submitComment() },
                    onCancelReply = { viewModel.setReplyingTo(null) },
                    onFocus = {
                        if (uiState.replyingToComment == null) {
                            viewModel.onNewCommentInputFocused()
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoadingPost && uiState.post == null) {
            PostDetailSkeleton(paddingValues = paddingValues)
        } else {
            uiState.post?.let { post ->
                PostDetailContent(
                    post = post,
                    uiState = uiState,
                    scrollToComments = scrollToComments,
                    isExpiredNotice = isExpiredNotice,
                    onBack = onBack,
                    onAuthorClick = { onAuthorClick(post.authorId) },
                    onSaveClick = { viewModel.toggleSave() },
                    onDeletePost = { viewModel.deletePost() },
                    onEditPost = { onEditPost(post.id) },
                    onSharePost = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "¡Mira esta oferta en Rinde!\n${post.title}\nhttps://rinde.app/post/${post.id}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir publicación"))
                    },
                    onReportExpired = {
                        if (post.authorId == uiState.currentUserId) {
                            if (post.verificationStatus == VerificationStatus.EXPIRED) {
                                viewModel.markAsAvailable()
                            } else {
                                viewModel.markAsExpired()
                            }
                        } else {
                            viewModel.reportAsExpired()
                        }
                    },
                    onCommentTextChange = { viewModel.onCommentTextChange(it) },
                    onCommentSubmit = { viewModel.submitComment() },
                    onLikeComment = { viewModel.toggleCommentLike(it) },
                    onLoadReplies = { viewModel.loadReplies(it) },
                    onLikeReply = { commentId, replyId -> viewModel.toggleReplyLike(commentId, replyId) },
                    onDeleteComment = { commentId, authorId -> viewModel.deleteComment(commentId, authorId) },
                    onEditCommentStart = { viewModel.startEditComment(it) },
                    onEditTextChange = { viewModel.onEditTextChange(it) },
                    onEditCommentSave = { viewModel.saveEditedContent() },
                    onEditCommentCancel = { viewModel.cancelEdit() },
                    onDeleteReply = { commentId, replyId, authorId -> viewModel.deleteReply(commentId, replyId, authorId) },
                    onEditReplyStart = { viewModel.startEditReply(it) },
                    onReportComment = { viewModel.reportComment(it) },
                    onReportReply = { viewModel.reportReply(it) },
                    onReplyTextChange = { viewModel.onReplyTextChange(it) },
                    onReplySubmit = { viewModel.submitReply() },
                    onCancelReply = { viewModel.setReplyingTo(null) },
                    onReplyClick = { viewModel.setReplyingTo(it) },
                    onVoteTrue = { viewModel.toggleVote(1) },
                    onVoteFalse = { viewModel.toggleVote(-1) },
                    paddingValues = paddingValues
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se pudo cargar la publicación",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Contenido puro de Detalle de Oferta (Stateless).
 * Orquesta las secciones modulares (Header, Meta, Voting, Comments).
 */
@Composable
fun PostDetailContent(
    post: CommunityPost,
    uiState: PostDetailUiState,
    scrollToComments: Boolean = false,
    isExpiredNotice: Boolean = false,
    onBack: () -> Unit,
    onAuthorClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeletePost: () -> Unit = {},
    onEditPost: () -> Unit = {},
    onSharePost: () -> Unit = {},
    onReportExpired: () -> Unit = {},
    onCommentTextChange: (String) -> Unit,
    onCommentSubmit: () -> Unit,
    onLikeComment: (String) -> Unit,
    onLoadReplies: (String) -> Unit,
    onLikeReply: (String, String) -> Unit,
    onDeleteComment: (String, String) -> Unit,
    onEditCommentStart: (Comment) -> Unit,
    onEditTextChange: (String) -> Unit,
    onEditCommentSave: () -> Unit,
    onEditCommentCancel: () -> Unit,
    onDeleteReply: (String, String, String) -> Unit,
    onEditReplyStart: (Reply) -> Unit,
    onReportComment: (Comment) -> Unit,
    onReportReply: (Reply) -> Unit,
    onReplyTextChange: (String) -> Unit,
    onReplySubmit: () -> Unit,
    onCancelReply: () -> Unit,
    onReplyClick: (Comment) -> Unit,
    onVoteTrue: () -> Unit,
    onVoteFalse: () -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isAuthor = post.authorId == uiState.currentUserId
    val isPostExpired = post.verificationStatus == VerificationStatus.EXPIRED
    val hasExpiredBanner = if (isAuthor) (isExpiredNotice || isPostExpired) else isPostExpired
    val hasOfflineBanner = uiState.voteState == VoteUiState.OFFLINE || uiState.voteState == VoteUiState.ERROR
    var bannersCount = 0
    if (hasExpiredBanner) bannersCount++
    if (hasOfflineBanner) bannersCount++

    val commentListOffset = 9 + bannersCount
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(scrollToComments, uiState.comments) {
        if (scrollToComments && uiState.comments.isNotEmpty()) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(commentListOffset)
            }
        }
    }

    LaunchedEffect(uiState.replyingToComment, imeVisible) {
        val replyingTo = uiState.replyingToComment
        if (replyingTo != null && imeVisible) {
            val idx = uiState.comments.indexOfFirst { it.id == replyingTo.id }
            if (idx >= 0) {
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(commentListOffset + idx)
                }
            }
        }
    }

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }
    val topBarColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(300),
        label = "topBarColor"
    )
    val topBarContentColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
        animationSpec = tween(300),
        label = "topBarContentColor"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Floating TopBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarColor)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (!isScrolled) Color.Black.copy(alpha = 0.35f) else Color.Transparent)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = topBarContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (!isScrolled) Color.Black.copy(alpha = 0.35f) else Color.Transparent)
                            .clickable(onClick = onSaveClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (post.isSavedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Guardar",
                            tint = if (post.isSavedByMe) RindePrimary else topBarContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    var showReportConfirm by remember { mutableStateOf(false) }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Eliminar publicación") },
                            text = { Text("¿Estás seguro que deseas eliminar esta publicación? Esta acción no se puede deshacer.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                    onDeletePost()
                                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
                            }
                        )
                    }

                    val isExpired = post.verificationStatus == VerificationStatus.EXPIRED

                    if (showReportConfirm) {
                        val isAuthorLocal = post.authorId == uiState.currentUserId
                        val titleText = if (isAuthorLocal) {
                            if (isExpired) "Marcar como disponible" else "Marcar como expirada"
                        } else {
                            "Reportar expirada"
                        }
                        val bodyText = if (isAuthorLocal) {
                            if (isExpired) "¿Estás seguro que deseas marcar esta oferta como disponible nuevamente?" else "¿Estás seguro que deseas marcar esta oferta como expirada?"
                        } else {
                            "¿Estás seguro que deseas reportar esta oferta como expirada?"
                        }

                        AlertDialog(
                            onDismissRequest = { showReportConfirm = false },
                            title = { Text(titleText) },
                            text = { Text(bodyText) },
                            confirmButton = {
                                TextButton(onClick = {
                                    showReportConfirm = false
                                    onReportExpired()
                                }) { Text("Confirmar") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showReportConfirm = false }) { Text("Cancelar") }
                            }
                        )
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (!isScrolled) Color.Black.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable(onClick = { showMenu = true }),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = topBarContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (post.authorId == uiState.currentUserId && uiState.currentUserId.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { showMenu = false; onEditPost() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar") },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = { showMenu = false; showDeleteConfirm = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Compartir") },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick = { showMenu = false; onSharePost() }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isExpired) "Marcar disponible" else "Marcar expirada") },
                                    leadingIcon = { Icon(if (isExpired) Icons.Default.CheckCircle else Icons.Default.Warning, null) },
                                    onClick = { showMenu = false; showReportConfirm = true }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Compartir") },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick = { showMenu = false; onSharePost() }
                                )
                                if (!isExpired) {
                                    DropdownMenuItem(
                                        text = { Text("Reportar expirada") },
                                        leadingIcon = { Icon(Icons.Default.Warning, null) },
                                        onClick = { showMenu = false; showReportConfirm = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Banner Expiración
        AnimatedVisibility(
            visible = hasExpiredBanner,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPostExpired) "Publicación Expirada" else "Aviso de Expiración",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAuthor) {
                                if (isPostExpired)
                                    "Has marcado esta oferta como expirada."
                                else
                                    "Usuarios reportaron que esta oferta finalizó. ¿Deseas marcarla como expirada?"
                            } else {
                                "El creador marcó esta oferta como expirada o agotada."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isAuthor) {
                        TextButton(onClick = onReportExpired) {
                            Text(
                                text = if (isPostExpired) "Marcar Activa" else "Marcar Expirada",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // LazyColumn modular con las 4 secciones
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            VoteStateBannerItem(uiState.voteState, uiState.voteErrorMessage)

            ImageAndHeaderSection(post, isPostExpired)

            item { Spacer(modifier = Modifier.height(8.dp)) }

            StoreAndExpirationSection(post)

            item { Spacer(modifier = Modifier.height(8.dp)) }

            AuthorAndDescriptionSection(post, onAuthorClick)

            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (!isPostExpired) {
                VotingSectionItem(
                    post = post,
                    voteState = uiState.voteState,
                    onVoteTrue = onVoteTrue,
                    onVoteFalse = onVoteFalse
                )
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            CommentsHeaderItem(
                commentsCount = post.commentsCount,
                isLoadingComments = uiState.isLoadingComments,
                commentsEmpty = uiState.comments.isEmpty()
            )

            CommentsListItems(
                uiState = uiState,
                onLikeComment = onLikeComment,
                onReplyClick = onReplyClick,
                onEditCommentStart = onEditCommentStart,
                onEditTextChange = onEditTextChange,
                onEditCommentSave = onEditCommentSave,
                onEditCommentCancel = onEditCommentCancel,
                onDeleteComment = onDeleteComment,
                onLoadReplies = onLoadReplies,
                onLikeReply = onLikeReply,
                onDeleteReply = onDeleteReply,
                onEditReplyStart = onEditReplyStart,
                onReportComment = onReportComment,
                onReportReply = onReportReply
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PostDetailContentPreview() {
    RindeTheme {
        PostDetailContent(
            post = CommunityPost(
                id = "post_preview",
                title = "Oferta de prueba",
                authorName = "Carlos",
                truthCount = 10,
                falseCount = 2,
                commentsCount = 3
            ),
            uiState = PostDetailUiState(),
            onBack = {},
            onAuthorClick = {},
            onSaveClick = {},
            onCommentTextChange = {},
            onCommentSubmit = {},
            onLikeComment = {},
            onLoadReplies = {},
            onLikeReply = { _, _ -> },
            onDeleteComment = { _, _ -> },
            onEditCommentStart = {},
            onEditTextChange = {},
            onEditCommentSave = {},
            onEditCommentCancel = {},
            onDeleteReply = { _, _, _ -> },
            onEditReplyStart = {},
            onReportComment = {},
            onReportReply = {},
            onReplyTextChange = {},
            onReplySubmit = {},
            onCancelReply = {},
            onReplyClick = {},
            onVoteTrue = {},
            onVoteFalse = {},
            paddingValues = PaddingValues(0.dp)
        )
    }
}
