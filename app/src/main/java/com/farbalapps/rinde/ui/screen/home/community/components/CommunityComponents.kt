package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.farbalapps.rinde.ui.theme.Blue80
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.VoteFalseContainerDark
import com.farbalapps.rinde.ui.theme.VoteFalseContentDark
import com.farbalapps.rinde.ui.theme.VoteTrueContainerDark
import com.farbalapps.rinde.ui.theme.VoteTrueContentDark
import com.farbalapps.rinde.ui.theme.RindeTheme
import com.farbalapps.rinde.ui.screen.home.community.CommunityTab
import com.farbalapps.rinde.ui.screen.home.community.PostImageCarousel
import com.farbalapps.rinde.util.DateUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WishlistAddCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .size(96.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Blue80.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir a Wishlist",
                tint = RindePrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Añadir",
                style = MaterialTheme.typography.labelMedium,
                color = RindePrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityTabRow(
    selectedTab: CommunityTab,
    onTabSelected: (CommunityTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        CommunityTab.DISCOVER to "Descubrir",
        CommunityTab.FOLLOWING to "Siguiendo",
        CommunityTab.SAVED to "Guardados"
    )

    PrimaryTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab },
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabs.indexOfFirst { it.first == selectedTab }),
                width = 32.dp,
                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            )
        },
        divider = {}
    ) {
        tabs.forEach { (tab, title) ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipRow(
    selectedTab: CommunityTab,
    onTabSelected: (CommunityTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedTab == CommunityTab.DISCOVER,
            onClick = { onTabSelected(CommunityTab.DISCOVER) },
            label = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Descubrir", fontWeight = if (selectedTab == CommunityTab.DISCOVER) FontWeight.SemiBold else FontWeight.Normal)
                }
            },
            leadingIcon = {},
            shape = RoundedCornerShape(50)
        )
        FilterChip(
            selected = selectedTab == CommunityTab.NEARBY,
            onClick = { onTabSelected(CommunityTab.NEARBY) },
            label = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cerca de ti", fontWeight = if (selectedTab == CommunityTab.NEARBY) FontWeight.SemiBold else FontWeight.Normal)
                }
            },
            leadingIcon = {},
            shape = RoundedCornerShape(50)
        )
        FilterChip(
            selected = selectedTab == CommunityTab.FOLLOWING,
            onClick = { onTabSelected(CommunityTab.FOLLOWING) },
            label = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Siguiendo", fontWeight = if (selectedTab == CommunityTab.FOLLOWING) FontWeight.SemiBold else FontWeight.Normal)
                }
            },
            leadingIcon = {},
            shape = RoundedCornerShape(50)
        )
        FilterChip(
            selected = selectedTab == CommunityTab.SAVED,
            onClick = { onTabSelected(CommunityTab.SAVED) },
            label = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardados", fontWeight = if (selectedTab == CommunityTab.SAVED) FontWeight.SemiBold else FontWeight.Normal)
                }
            },
            leadingIcon = {},
            shape = RoundedCornerShape(50)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POST CARD — Diseño compacto tipo Promodescuentos/Hotukdeals
// Layout horizontal: imagen cuadrada izquierda + info a la derecha
// Muestra: título, precios, % descuento, tipo oferta, votos, comentarios
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PostCard(
    post: com.farbalapps.rinde.domain.model.CommunityPost,
    isAuthorVerified: Boolean = false,
    currentUserId: String = "",
    onAuthorClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onPostClick: () -> Unit = {},
    onDeletePost: () -> Unit = {},
    onEditPost: () -> Unit = {},
    onSharePost: () -> Unit = {},
    onReportExpired: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ── Derivados memoizados: se calculan UNA VEZ por post, no en cada frame ──
    var showMenu by remember { mutableStateOf(false) }
    val isOnline = remember(post.id) {
        post.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE
    }
    val isUnreliable = remember(post.id) {
        post.verificationStatus == com.farbalapps.rinde.domain.model.VerificationStatus.EXPIRED ||
        post.verificationStatus == com.farbalapps.rinde.domain.model.VerificationStatus.DISPUTED
    }
    val hasDiscount = remember(post.discountPrice, post.normalPrice) {
        post.discountPrice != null && post.normalPrice != null && post.discountPrice < post.normalPrice
    }
    val hasAnyPrice = remember(post.discountPrice, post.normalPrice) {
        post.discountPrice != null || post.normalPrice != null
    }
    val formattedDate = remember(post.timestamp) {
        DateUtils.formatTimeAgo(post.timestamp?.time ?: 0L)
    }
    val formattedNormalPrice = remember(post.normalPrice, post.currency) {
        post.normalPrice?.let { "${post.currency} ${"%.2f".format(it)}" }
    }
    val formattedDiscountPrice = remember(post.discountPrice, post.currency) {
        post.discountPrice?.let { "${post.currency} ${"%.2f".format(it)}" }
    }
    val displayPrice = remember(post.discountPrice, post.normalPrice, post.currency) {
        (post.discountPrice ?: post.normalPrice)?.let { "${post.currency} ${"%.2f".format(it)}" } ?: ""
    }
    val storeName = remember(post.id) {
        if (post.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE) post.websiteName else post.storeName
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAuthorClick() }
                        .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (post.authorPhotoUrl != null) {
                            AsyncImage(
                                model = post.authorPhotoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))

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
                            Icons.Default.Verified, null,
                            tint = com.farbalapps.rinde.ui.theme.VerifiedBadgeColor,
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

                    Spacer(Modifier.weight(1f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSaveClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (post.isSavedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Guardar",
                                tint = if (post.isSavedByMe) RindePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

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

                        if (showReportConfirm) {
                            val isAuthor = post.authorId == currentUserId
                            AlertDialog(
                                onDismissRequest = { showReportConfirm = false },
                                title = { Text(if (isAuthor) "Marcar como expirada" else "Reportar expirada") },
                                text = { Text(if (isAuthor) "¿Estás seguro que deseas marcar esta oferta como expirada?" else "¿Estás seguro que deseas reportar esta oferta como expirada?") },
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
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (post.authorId == currentUserId && currentUserId.isNotEmpty()) {
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
                                        text = { Text("Marcar expirada") },
                                        leadingIcon = { Icon(Icons.Default.Warning, null) },
                                        onClick = { showMenu = false; showReportConfirm = true }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Compartir") },
                                        leadingIcon = { Icon(Icons.Default.Share, null) },
                                        onClick = { showMenu = false; onSharePost() }
                                    )
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
                        // En el feed mostramos solo la primera imagen (AsyncImage simple)
                        // para evitar lag en el scroll. El carousel completo está en PostDetailScreen.
                        if (post.photos.isNotEmpty()) {
                            AsyncImage(
                                model = com.farbalapps.rinde.util.CloudinaryUrlBuilder.feedThumbnail(post.photos.first()),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Image, null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        val offerBadgeColor = if (isOnline) Color(0xFF1565C0) else Color(0xFF2E7D32)
                        val offerBadgeIcon = if (isOnline) Icons.Default.Language else Icons.Default.Store
                        val offerBadgeLabel = if (isOnline) "Online" else "Física"

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
                                    offerBadgeIcon, null,
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

                        // Descripción corta — máximo 2 líneas
                        if (post.descriptionShort.isNotBlank()) {
                            Text(
                                text = post.descriptionShort,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                        }

                        if (hasAnyPrice) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (hasDiscount && formattedNormalPrice != null) {
                                    Text(
                                        text = formattedNormalPrice,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val priceColor = if (hasDiscount) VoteTrueContainerDark else MaterialTheme.colorScheme.onSurface

                                    Text(
                                        text = displayPrice,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = priceColor
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
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Votos reales ("Píldora")
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = VoteTrueContainerDark.copy(alpha = 0.1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Whatshot, null,
                                    tint = VoteTrueContainerDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${post.truthCount}°",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VoteTrueContainerDark,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Comentarios
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${post.commentsCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tienda a la derecha (memoizada arriba)
                    if (!storeName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isOnline) Icons.Default.Language else Icons.Default.Store,
                                null, tint = RindePrimary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp)
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

            // Overlay de oferta no confiable
            if (isUnreliable) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(com.farbalapps.rinde.ui.theme.OverlayWarningColor.copy(alpha = 0.45f))
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
                            Text(
                                if (post.verificationStatus == com.farbalapps.rinde.domain.model.VerificationStatus.EXPIRED)
                                    "OFERTA EXPIRADA" else "BAJA VERACIDAD",
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


// ─────────────────────────────────────────────────────────────────────────────
// FEED VERACITY BADGE — Versión estática para el card del feed
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FeedVeracityBadge(
    truthCount: Int,
    falseCount: Int
) {
    val totalVotes = truthCount + falseCount
    val showResults = totalVotes >= 30
    
    if (showResults && totalVotes > 0) {
        val truthRatio = truthCount.toFloat() / totalVotes
        val isMostlyTrue = truthRatio >= 0.5f
        
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isMostlyTrue) VoteTrueContainerDark.copy(alpha = 0.2f) else VoteFalseContainerDark.copy(alpha = 0.2f),
            modifier = Modifier.height(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = if (isMostlyTrue) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isMostlyTrue) VoteTrueContentDark else VoteFalseContentDark
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${(truthRatio * 100).roundToInt()}% de usuarios dicen que es ${if (isMostlyTrue) "Real" else "Falsa"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMostlyTrue) VoteTrueContentDark else VoteFalseContentDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Text(
            text = "En validación comunitaria...",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VOTING SYSTEM — Material Design 3
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VotingActions(
    userVote: Int,
    truthCount: Int,
    falseCount: Int,
    onVoteTrue: () -> Unit,
    onVoteFalse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalVotes = truthCount + falseCount
    val showResults = totalVotes >= 30
    val truthRatio: Float? = if (showResults && totalVotes > 0) {
        truthCount.toFloat() / totalVotes
    } else null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VoteActionButton(
                label = if (showResults) "Falsa · $falseCount" else "Falsa",
                icon = Icons.Default.ThumbDown,
                isSelected = userVote == -1,
                selectedContainerColor = VoteFalseContainerDark,
                selectedContentColor = VoteFalseContentDark,
                modifier = Modifier.weight(1f),
                onClick = onVoteFalse
            )

            if (truthRatio != null) {
                VotePercentBadge(truthPercent = truthRatio)
            }

            VoteActionButton(
                label = if (showResults) "Real · $truthCount" else "Real",
                icon = Icons.Default.ThumbUp,
                isSelected = userVote == 1,
                selectedContainerColor = VoteTrueContainerDark,
                selectedContentColor = VoteTrueContentDark,
                modifier = Modifier.weight(1f),
                onClick = onVoteTrue
            )
        }

        if (truthRatio != null) {
            VoteProgressIndicator(truthRatio = truthRatio, userVote = userVote)
        } else {
            Text(
                text = "Se necesitan ${30 - totalVotes} votos más para ver resultados",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun VoteActionButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    selectedContainerColor: Color,
    selectedContentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bounceScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val containerColor = if (isSelected) selectedContainerColor
                         else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) selectedContentColor
                       else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = {
            scope.launch {
                bounceScale.animateTo(targetValue = 0.85f, animationSpec = tween(durationMillis = 80))
                bounceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            }
            onClick()
        },
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer { scaleX = bounceScale.value; scaleY = bounceScale.value },
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun VotePercentBadge(truthPercent: Float) {
    val displayPercent = (truthPercent * 100).roundToInt()
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$displayPercent%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VoteProgressIndicator(
    truthRatio: Float,
    userVote: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50))
    ) {
        val falseRatio = (1f - truthRatio).coerceAtLeast(0f)
        if (falseRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(falseRatio)
                    .background(VoteFalseContainerDark.copy(alpha = if (userVote == -1) 1f else 0.4f))
            )
        }
        if (truthRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(truthRatio)
                    .background(VoteTrueContainerDark.copy(alpha = if (userVote == 1) 1f else 0.4f))
            )
        }
    }
}

@Composable
fun TrustStarRating(score: Float, level: String) {
    Row {
        repeat(5) { index ->
            val filled = index < score.roundToInt()
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = when {
                    !filled -> MaterialTheme.colorScheme.outlineVariant
                    level == "GOLD" || level == "PLATINUM" -> Color(0xFFFFD700)
                    level == "SILVER" -> Color(0xFFC0C0C0)
                    else -> Color(0xFFCD7F32)
                },
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
