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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.ui.theme.OverlayWarningColor
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.VerifiedBadgeColor
import com.farbalapps.rinde.ui.theme.VoteTrueContainerDark
import com.farbalapps.rinde.util.CloudinaryUrlBuilder
import com.farbalapps.rinde.util.DateUtils

/**
 * Tarjeta de presentación individual para una oferta de la comunidad.
 *
 * Muestra información del autor, imagen miniatura optimizada para feed, precios con descuento,
 * tipo de oferta (Online / Física), estado de verificación, tienda y menú de acciones contextuadas.
 *
 * @param post Publicación comunitaria con sus datos de oferta.
 * @param isAuthorVerified Indica si el autor cuenta con insignia de verificación oficial.
 * @param currentUserId Identificador del usuario autenticado actual.
 * @param onAuthorClick Callback al presionar el nombre o avatar del autor.
 * @param onSaveClick Callback al alternar el guardado/favorito de la publicación.
 * @param onPostClick Callback al presionar la tarjeta para ver su detalle.
 * @param onCommentClick Callback al presionar la sección de comentarios.
 * @param onDeletePost Callback para eliminar la publicación (disponible solo para el autor).
 * @param onEditPost Callback para editar la publicación.
 * @param onSharePost Callback para compartir la oferta externamente.
 * @param onMarkExpired Callback para marcar la oferta como expirada.
 * @param onReportExpired Callback para emitir reporte de expiración.
 * @param onMarkAvailable Callback para restablecer la oferta como disponible.
 * @param modifier Modificador de Compose para el contenedor.
 */
@Composable
fun PostCard(
    post: CommunityPost,
    isAuthorVerified: Boolean = false,
    currentUserId: String = "",
    onAuthorClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onPostClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onDeletePost: () -> Unit = {},
    onEditPost: () -> Unit = {},
    onSharePost: () -> Unit = {},
    onMarkExpired: () -> Unit = {},
    onReportExpired: () -> Unit = {},
    onMarkAvailable: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isOnline = remember(post.id, post.offerType) {
        post.offerType == OfferType.ONLINE
    }
    val isUnreliable = remember(post.id, post.verificationStatus) {
        post.verificationStatus == VerificationStatus.EXPIRED ||
                post.verificationStatus == VerificationStatus.DISPUTED
    }
    val isExpiredPost = remember(post.id, post.verificationStatus) {
        post.verificationStatus == VerificationStatus.EXPIRED
    }
    val hasDiscount = remember(post.id, post.discountPrice, post.normalPrice) {
        post.discountPrice != null && post.normalPrice != null && post.discountPrice < post.normalPrice
    }
    val hasAnyPrice = remember(post.id, post.discountPrice, post.normalPrice) {
        post.discountPrice != null || post.normalPrice != null
    }
    val formattedDate = remember(post.id, post.timestamp) {
        DateUtils.formatTimeAgo(post.timestamp)
    }
    val formattedNormalPrice = remember(post.id, post.normalPrice, post.currency) {
        post.normalPrice?.let { "${post.currency} ${"%.2f".format(it)}" }
    }
    val displayPrice = remember(post.id, post.discountPrice, post.normalPrice, post.currency) {
        (post.discountPrice ?: post.normalPrice)?.let { "${post.currency} ${"%.2f".format(it)}" } ?: ""
    }
    val storeName = remember(post.id, post.offerType, post.websiteName, post.storeName) {
        if (post.offerType == OfferType.ONLINE) post.websiteName else post.storeName
    }

    val paddingSmall = dimensionResource(id = R.dimen.padding_small)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isUnreliable) 0.55f else 1f)
            .clickable { onPostClick() },
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box {
            Column {
                // ── HEADER COMPACTO ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Izquierda (clickeable, ocupa el espacio sobrante)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAuthorClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val authorAvatarUrl = post.authorPhotoUrl?.takeIf { it.isNotBlank() }
                            if (authorAvatarUrl != null) {
                                AsyncImage(
                                    model = authorAvatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(paddingSmall))

                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (isAuthorVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = VerifiedBadgeColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }

                    // Derecha (Acciones de Guardado y Menú)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSaveClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (post.isSavedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = stringResource(if (post.isSavedByMe) R.string.community_tab_saved else R.string.wishlist_add_desc),
                                tint = if (post.isSavedByMe) RindePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        var showReportConfirm by remember { mutableStateOf(false) }
                        var showAvailableConfirm by remember { mutableStateOf(false) }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text(stringResource(R.string.community_dialog_delete_post_title)) },
                                text = { Text(stringResource(R.string.community_dialog_delete_post_desc)) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showDeleteConfirm = false
                                        onDeletePost()
                                    }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.btn_cancel)) }
                                }
                            )
                        }

                        if (showReportConfirm) {
                            val dialogTitle = if (post.authorId == currentUserId) {
                                stringResource(R.string.community_dialog_mark_expired_title)
                            } else {
                                stringResource(R.string.community_dialog_report_expired_title)
                            }
                            val dialogDesc = if (post.authorId == currentUserId) {
                                stringResource(R.string.community_dialog_mark_expired_desc)
                            } else {
                                stringResource(R.string.community_dialog_report_expired_desc)
                            }

                            AlertDialog(
                                onDismissRequest = { showReportConfirm = false },
                                title = { Text(dialogTitle) },
                                text = { Text(dialogDesc) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showReportConfirm = false
                                        if (post.authorId == currentUserId) {
                                            onMarkExpired()
                                        } else {
                                            onReportExpired()
                                        }
                                    }) { Text(stringResource(R.string.community_action_confirm)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showReportConfirm = false }) { Text(stringResource(R.string.btn_cancel)) }
                                }
                            )
                        }

                        if (showAvailableConfirm) {
                            AlertDialog(
                                onDismissRequest = { showAvailableConfirm = false },
                                title = { Text(stringResource(R.string.community_dialog_mark_available_title)) },
                                text = { Text(stringResource(R.string.community_dialog_mark_available_desc)) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showAvailableConfirm = false
                                        onMarkAvailable()
                                    }) { Text(stringResource(R.string.community_action_confirm)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAvailableConfirm = false }) { Text(stringResource(R.string.btn_cancel)) }
                                }
                            )
                        }

                        val isAuthor = post.authorId == currentUserId && currentUserId.isNotEmpty()

                        // Si está expirada y NO es el autor: no se muestra el botón de menú
                        if (!isExpiredPost || isAuthor) {
                            Box {
                                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    when {
                                        isExpiredPost && isAuthor -> {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.community_menu_mark_available)) },
                                                leadingIcon = { Icon(Icons.Default.Check, null) },
                                                onClick = { showMenu = false; showAvailableConfirm = true }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.community_menu_delete)) },
                                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                onClick = { showMenu = false; showDeleteConfirm = true }
                                            )
                                        }
                                        !isExpiredPost && isAuthor -> {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.community_menu_edit)) },
                                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                                onClick = { showMenu = false; onEditPost() }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.community_menu_delete)) },
                                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                onClick = { showMenu = false; showDeleteConfirm = true }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.community_menu_share)) },
                                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                                onClick = { showMenu = false; onSharePost() }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.community_menu_mark_expired)) },
                                                leadingIcon = { Icon(Icons.Default.Warning, null) },
                                                onClick = { showMenu = false; showReportConfirm = true }
                                            )
                                        }
                                        else -> {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.community_menu_share)) },
                                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                                onClick = { showMenu = false; onSharePost() }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.community_menu_report_expired)) },
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

                // ── CUERPO CENTRAL (Imagen + Info) ─────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (post.photos.isNotEmpty()) {
                            AsyncImage(
                                model = CloudinaryUrlBuilder.feedThumbnail(post.photos.first()),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        val offerBadgeColor = if (isOnline) Color(0xFF1565C0) else Color(0xFF2E7D32)
                        val offerBadgeIcon = if (isOnline) Icons.Default.Language else Icons.Default.Store
                        val offerBadgeLabel = stringResource(if (isOnline) R.string.community_badge_online else R.string.community_badge_physical)
                        Surface(
                            color = offerBadgeColor,
                            shape = RoundedCornerShape(bottomEnd = 8.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    offerBadgeIcon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = offerBadgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Badge Top (esquina superior derecha) — solo si no expirada
                        if (!isExpiredPost && post.votesScore >= 50) {
                            Surface(
                                color = Color(0xFFFF6D00),
                                shape = RoundedCornerShape(bottomStart = 8.dp),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = stringResource(R.string.community_badge_top),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )

                        if (hasAnyPrice) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (hasDiscount && formattedNormalPrice != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formattedNormalPrice,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textDecoration = TextDecoration.LineThrough
                                        )
                                        if (post.discountPercentage != null && post.discountPercentage > 0) {
                                            Surface(
                                                color = Color(0xFFE53935),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "-${post.discountPercentage}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                val priceColor = if (hasDiscount) VoteTrueContainerDark else MaterialTheme.colorScheme.onSurface
                                Text(
                                    text = displayPrice,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = priceColor
                                )
                            }
                        }
                    }
                }

                // ── NUEVO FOOTER (Votos, Comentarios, Tienda) ───────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Controles de Votos y Comentarios
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (post.verificationStatus != VerificationStatus.EXPIRED) {
                            VerdictBadge(
                                truthCount = post.truthCount,
                                falseCount = post.falseCount
                            )
                        }

                        // Comentarios — clickeable para abrir PostDetail
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onPostClick() }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${post.commentsCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tienda a la derecha
                    if (!storeName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isOnline) Icons.Default.Language else Icons.Default.Store,
                                contentDescription = null,
                                tint = RindePrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = storeName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = RindePrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 140.dp)
                            )
                        }
                    }
                }
            }

            // Overlay de oferta no confiable o expirada
            if (isUnreliable) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(OverlayWarningColor.copy(alpha = 0.45f))
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(color = Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(50)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, Modifier.size(12.dp), Color.Yellow)
                            Spacer(Modifier.width(5.dp))
                            val bannerText = if (post.verificationStatus == VerificationStatus.EXPIRED) {
                                stringResource(R.string.community_badge_expired)
                            } else {
                                stringResource(R.string.community_badge_unreliable)
                            }
                            Text(
                                text = bannerText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}
