package com.farbalapps.rinde.ui.screen.home.community

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.ui.screen.home.community.components.SharedCommentInput
import com.farbalapps.rinde.ui.screen.home.community.components.SharedCommentThread
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.VoteFalseContainerDark
import com.farbalapps.rinde.ui.theme.VoteTrueContainerDark
import com.farbalapps.rinde.ui.screen.home.community.components.VotingActions
import com.farbalapps.rinde.ui.screen.home.community.components.VerdictBadge
import com.farbalapps.rinde.ui.screen.home.community.components.ShimmerBox
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
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

    val postStatusOverlay by viewModel.postStatusOverlay.collectAsStateWithLifecycle()
    val savedStatusOverlay by viewModel.savedStatusOverlay.collectAsStateWithLifecycle()
    val voteStatusOverlay by viewModel.voteStatusOverlay.collectAsStateWithLifecycle()

    // Fondo grisáceo claro / oscuro neutro para separar las tarjetas
    val scaffoldBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = scaffoldBgColor
    ) { paddingValues ->
        if (uiState.isLoadingPost && uiState.post == null) {
            // Skeleton en lugar de CircularProgressIndicator infinito
            PostDetailSkeleton(paddingValues = paddingValues)
        } else {
            uiState.post?.let { post ->
                val overriddenStatus = postStatusOverlay[post.id] ?: post.verificationStatus
                val overriddenSaved = savedStatusOverlay[post.id] ?: post.isSavedByMe
                val voteOver = voteStatusOverlay[post.id]
                val finalTruth = voteOver?.truthCount ?: post.truthCount
                val finalFalse = voteOver?.falseCount ?: post.falseCount
                val finalMyVote = voteOver?.myVote ?: post.myVoteValue
                val finalScore = if (voteOver != null) (finalTruth - finalFalse) else post.votesScore
                val finalPost = post.copy(
                    verificationStatus = overriddenStatus,
                    isSavedByMe = overriddenSaved,
                    truthCount = finalTruth,
                    falseCount = finalFalse,
                    myVoteValue = finalMyVote,
                    votesScore = finalScore
                )
                PostDetailContent(
                    post = finalPost,
                    uiState = uiState,
                    onBack = onBack,
                    onAuthorClick = { onAuthorClick(finalPost.authorId) },
                    onSaveClick = { viewModel.toggleSave() },
                    onDeletePost = { viewModel.deletePost() },
                    onEditPost = { onEditPost(finalPost.id) },
                    onSharePost = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "¡Mira esta oferta en Rinde!\n${finalPost.title}\nhttps://rinde.app/post/${finalPost.id}")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir publicación"))
                    },
                    onReportExpired = { 
                        if (finalPost.authorId == uiState.currentUserId) {
                            if (finalPost.verificationStatus == VerificationStatus.EXPIRED) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailContent(
    post: CommunityPost,
    uiState: PostDetailUiState,
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
    paddingValues: PaddingValues
) {
    val lazyListState = rememberLazyListState()

    val isScrolled by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            // ── SECCIÓN 1: IMAGEN Y ENCABEZADO ──────────────────────────────────
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        PostImageCarousel(
                            photos = post.photos,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f) 
                        )

                        Column(modifier = Modifier.padding(16.dp)) {
                            // Status / Tipo de oferta (Badges estilo feed)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isOnline = post.offerType == OfferType.ONLINE
                                val offerBadgeColor = if (isOnline) Color(0xFF1565C0) else Color(0xFF2E7D32)
                                val offerBadgeIcon = if (isOnline) Icons.Default.Language else Icons.Default.Store
                                val offerBadgeLabel = if (isOnline) "Online" else "Física"

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = offerBadgeColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(offerBadgeIcon, null, tint = offerBadgeColor, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = offerBadgeLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = offerBadgeColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // VerdictBadge — mismo componente que el feed, mismos datos (truthCount/falseCount del post ya enriquecido)
                                VerdictBadge(
                                    truthCount = post.truthCount,
                                    falseCount = post.falseCount
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            // Título (ahora semi-bold, más profesional)
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 28.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bloque de precios (estilo promodescuentos, headlineMedium)
                            if (post.discountPrice != null || post.normalPrice != null) {
                                val displayPrice = post.discountPrice ?: post.normalPrice
                                val hasDiscount = post.discountPrice != null && post.normalPrice != null && post.discountPrice < post.normalPrice

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (hasDiscount) {
                                        Text(
                                            text = "${post.currency} ${"%.2f".format(post.normalPrice)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textDecoration = TextDecoration.LineThrough
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${post.currency} ${"%.2f".format(displayPrice)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasDiscount) VoteTrueContainerDark else MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        if (hasDiscount && post.discountPercentage != null && post.discountPercentage > 0) {
                                            Surface(
                                                color = Color(0xFFE53935),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "-${post.discountPercentage}%",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ── SECCIÓN 2: TIENDA, CUPÓN, EXPIRACIÓN ───────────────────────────
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        
                        // Tienda
                        val storeName = if (post.offerType == OfferType.ONLINE) post.websiteName else post.storeName
                        if (!storeName.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (post.offerType == OfferType.ONLINE) Icons.Default.Language else Icons.Default.Store,
                                            null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Disponible en", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = RindePrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Cupón
                        if (!post.couponCode.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RindePrimary.copy(alpha = 0.3f)),
                                color = RindePrimary.copy(alpha = 0.05f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Código de cupón", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            post.couponCode,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RindePrimary
                                        )
                                    }
                                    Button(
                                        onClick = { /* Copiar portapapeles */ },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copiar", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Disponibilidad y expiración
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (post.isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    null, 
                                    tint = if (post.isAvailable) VoteTrueContainerDark else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (post.isAvailable) "Disponible" else "Agotado", 
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (post.isAvailable) VoteTrueContainerDark else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (post.expiresAt != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Expira el ${SimpleDateFormat("d MMM yyyy", Locale("es")).format(post.expiresAt)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ── SECCIÓN 3: AUTOR Y DESCRIPCIÓN ──────────────────────────────
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        
                        // Perfil de Autor tipo Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAuthorClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (post.authorPhotoUrl != null) {
                                    AsyncImage(
                                        model = post.authorPhotoUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(post.authorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (post.isAuthorVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, null, modifier = Modifier.size(14.dp), tint = com.farbalapps.rinde.ui.theme.VerifiedBadgeColor)
                                    }
                                }
                                Text(
                                    text = "Publicado hace ${com.farbalapps.rinde.util.DateUtils.formatTimeAgo(post.timestamp?.time ?: 0L)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Descripción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = post.descriptionLong.ifBlank { post.descriptionShort },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ── SECCIÓN 3.5: VOTACIÓN DE VERACIDAD ─────────────────────────
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "¿Qué te parece esta oferta?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vota para ayudar a otros usuarios a saber si esta oferta es real o falsa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        VotingActions(
                            userVote = post.myVoteValue,
                            truthCount = post.truthCount,
                            falseCount = post.falseCount,
                            onVoteTrue = onVoteTrue,
                            onVoteFalse = onVoteFalse,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ── SECCIÓN 4: COMENTARIOS ─────────────────────────────────────────
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Comentarios (${post.commentsCount})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SharedCommentInput(
                            text = if (uiState.replyingToComment != null) uiState.replyText else uiState.commentText,
                            replyingTo = uiState.replyingToComment,
                            isSending = uiState.isSendingComment,
                            onTextChange = { if (uiState.replyingToComment != null) onReplyTextChange(it) else onCommentTextChange(it) },
                            onSubmit = { if (uiState.replyingToComment != null) onReplySubmit() else onCommentSubmit() },
                            onCancelReply = onCancelReply
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        if (uiState.isLoadingComments && uiState.comments.isEmpty()) {
                            // Skeleton en lugar de CircularProgressIndicator
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                repeat(3) { CommentSkeleton() }
                            }
                        } else if (uiState.comments.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Aún no hay comentarios.\n¡Sé el primero en preguntar o dar tu opinión!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            uiState.comments.forEach { comment ->
                                SharedCommentThread(
                                    comment = comment,
                                    replies = uiState.replies[comment.id] ?: emptyList(),
                                    currentUserId = uiState.currentUserId,
                                    editingCommentId = uiState.editingCommentId,
                                    editingText = uiState.editingText,
                                    onLikeClick = { onLikeComment(comment.id) },
                                    onReplyClick = { onReplyClick(comment) },
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
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }

        // ── FLOATING TOPBAR ─────────────────────────────────────────────────
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
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(4.dp)
                        .background(if (!isScrolled) Color.Black.copy(alpha = 0.3f) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = topBarContentColor
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier
                            .padding(4.dp)
                            .background(if (!isScrolled) Color.Black.copy(alpha = 0.3f) else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            if (post.isSavedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Guardar",
                            tint = if (post.isSavedByMe) RindePrimary else topBarContentColor
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
                        val isAuthor = post.authorId == uiState.currentUserId
                        val titleText = if (isAuthor) {
                            if (isExpired) "Marcar como disponible" else "Marcar como expirada"
                        } else {
                            "Reportar expirada"
                        }
                        val bodyText = if (isAuthor) {
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
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .padding(4.dp)
                                .background(if (!isScrolled) Color.Black.copy(alpha = 0.3f) else Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = topBarContentColor
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
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IMAGE CAROUSEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PostImageCarousel(
    photos: List<String>,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Image, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { photos.size })

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = photos[page],
                contentDescription = "Imagen ${page + 1} de ${photos.size}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Gradiente superior muy sutil para protección de la topbar (back button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )

        // Indicadores de páginas (dots)
        if (photos.size > 1) {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${photos.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Removed duplicated DetailCommentItem and DetailCommentInputArea composables.

// ─────────────────────────────────────────────────────────────────────────────
// SKELETON LOADERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PostDetailSkeleton(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Imagen placeholder
        ShimmerBox(modifier = Modifier.fillMaxWidth().aspectRatio(1f), shape = androidx.compose.ui.graphics.RectangleShape)
        
        Column(modifier = Modifier.padding(16.dp)) {
            // Badges
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                ShimmerBox(modifier = Modifier.width(60.dp).height(20.dp), shape = RoundedCornerShape(8.dp))
                ShimmerBox(modifier = Modifier.width(80.dp).height(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Titulo
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(24.dp))
            Spacer(modifier = Modifier.height(16.dp))
            // Precio
            ShimmerBox(modifier = Modifier.width(80.dp).height(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerBox(modifier = Modifier.width(140.dp).height(32.dp))
        }
    }
}

@Composable
fun CommentSkeleton() {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        ShimmerBox(modifier = Modifier.size(36.dp), shape = CircleShape)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.width(100.dp).height(14.dp))
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp))
        }
    }
}

// TODO: Replace with the actual time formatting logic from DateUtils if preferred.
fun formatCommentTime(timestamp: Long): String {
    return com.farbalapps.rinde.util.DateUtils.formatTimeAgo(timestamp)
}
