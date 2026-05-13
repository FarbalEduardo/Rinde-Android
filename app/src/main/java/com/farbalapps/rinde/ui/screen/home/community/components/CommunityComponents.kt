package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.farbalapps.rinde.ui.theme.Blue80
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.RindeSecondary
import androidx.compose.ui.tooling.preview.Preview
import com.farbalapps.rinde.ui.theme.RindeTheme
import com.farbalapps.rinde.ui.screen.home.community.CommunityTab
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import com.farbalapps.rinde.util.DateUtils

@Composable
fun WishlistAddCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .size(96.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Blue80.copy(alpha = 0.5f) // Light blueish/purple
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
        containerColor = MaterialTheme.colorScheme.surface,
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

@Composable
fun PostCard(
    post: com.farbalapps.rinde.domain.model.CommunityPost,
    isAuthorVerified: Boolean = false,
    userVote: Int = 0, // 1 for hot, -1 for cold, 0 for none
    onVoteHot: () -> Unit = {},
    onVoteCold: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isUnreliable = post.verificationStatus == com.farbalapps.rinde.domain.model.VerificationStatus.EXPIRED || 
                       post.verificationStatus == com.farbalapps.rinde.domain.model.VerificationStatus.DISPUTED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isUnreliable) 0.6f else 1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFCCAA), CircleShape),
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
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Avatar", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isAuthorVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verificado",
                                    tint = com.farbalapps.rinde.ui.theme.VerifiedBadgeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Reputación: ${"%.1f".format(post.userReputationScore)} • ${DateUtils.formatTimeAgo(post.timestamp?.time ?: 0L)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = { }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Image & Type Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (post.photos.isNotEmpty()) {
                        AsyncImage(
                            model = post.photos.first(),
                            contentDescription = post.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE53935)))
                    }
                    
                    // Offer Type Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(12.dp).align(Alignment.BottomStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (post.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE) Icons.Default.Language else Icons.Default.Store,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = (if (post.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE) post.websiteName else post.storeName) ?: "Oferta",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Save Button
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                            .align(Alignment.TopEnd)
                            .clickable { onSaveClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = "Guardar", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title & Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (post.verificationStatus == com.farbalapps.rinde.domain.model.VerificationStatus.VERIFIED) {
                        Icon(
                            imageVector = Icons.Default.GppGood,
                            contentDescription = "Verificado por comunidad",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
                    Text(
                        text = if (isExpanded) post.descriptionLong else post.descriptionShort,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (post.descriptionLong.length > post.descriptionShort.length) {
                        Text(
                            text = if (isExpanded) "Ver menos" else "Ver más...",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.farbalapps.rinde.ui.theme.RindePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Interaction Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Thermal Voting System
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                            IconToggleButton(
                                checked = userVote == 1,
                                onCheckedChange = { onVoteHot() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Caliente",
                                    tint = if (userVote == 1) com.farbalapps.rinde.ui.theme.VoteHotColor else com.farbalapps.rinde.ui.theme.VoteNeutralColor
                                )
                            }
                            Text(
                                text = "${post.votesScore}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    post.votesScore > 0 -> com.farbalapps.rinde.ui.theme.VoteHotColor
                                    post.votesScore < 0 -> com.farbalapps.rinde.ui.theme.VoteColdColor
                                    else -> com.farbalapps.rinde.ui.theme.VoteNeutralColor
                                }
                            )
                            IconToggleButton(
                                checked = userVote == -1,
                                onCheckedChange = { onVoteCold() }
                            ) {

                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = "Frío",
                                    tint = if (userVote == -1) com.farbalapps.rinde.ui.theme.VoteColdColor else com.farbalapps.rinde.ui.theme.VoteNeutralColor
                                )
                            }
                        }
                    }

                    // Comments and Likes
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onCommentClick) {
                            Icon(imageVector = Icons.Default.ModeComment, contentDescription = "Comentarios", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        Text("${post.commentsCount}", style = MaterialTheme.typography.labelLarge)
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(onClick = onLikeClick) {
                            Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = "Like", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        Text("${post.likesCount}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Trust Overlay
            if (isUnreliable) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(com.farbalapps.rinde.ui.theme.OverlayWarningColor.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (post.verificationStatus == com.farbalapps.rinde.domain.model.VerificationStatus.EXPIRED) "OFERTA EXPIRADA" else "BAJA VERACIDAD",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun WishlistAddCardPreview() {
    RindeTheme {
        WishlistAddCard(modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun FilterChipRowPreview() {
    RindeTheme {
        FilterChipRow(
            selectedTab = CommunityTab.DISCOVER,
            onTabSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PostCardPreview() {
    RindeTheme {
        PostCard(
            post = com.farbalapps.rinde.domain.model.CommunityPost(
                id = "1",
                authorId = "user1",
                authorName = "Eduardo Farbal",
                authorPhotoUrl = null,
                timestamp = java.util.Date(),
                title = "Frutillas 2 x 1 en Jumbo",
                descriptionShort = "Solo hoy hasta agotar stock...",
                descriptionLong = "Descripción larga aquí...",
                photos = listOf(""),
                category = "Frutas",
                location = com.farbalapps.rinde.domain.model.PostLocation("Jumbo Providencia", null, null),
                isActive = true,
                likesCount = 120,
                commentsCount = 15,
                truthCount = 10,
                falseCount = 2,
                votesScore = 42,
                verificationStatus = com.farbalapps.rinde.domain.model.VerificationStatus.VERIFIED,
                reportCount = 0,
                userReputationScore = 4.8f,
                isAuthorVerified = true,
                offerType = com.farbalapps.rinde.domain.model.OfferType.PHYSICAL,
                websiteName = null,
                productLink = null,
                storeName = "Jumbo Providencia",
                isRecommended = false,
                expiresAt = null
            ),
            isAuthorVerified = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

