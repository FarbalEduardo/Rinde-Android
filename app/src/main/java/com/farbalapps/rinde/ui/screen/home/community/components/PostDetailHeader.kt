package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.ui.theme.RindeTheme
import com.farbalapps.rinde.ui.theme.VoteTrueContainerDark

/**
 * Carrusel horizontal de imágenes para la publicación de oferta.
 */
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
                imageVector = Icons.Default.Image,
                contentDescription = null,
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

/**
 * Encabezado de la oferta con carrusel, badges de tipo y veredicto, título y precios.
 */
@Composable
fun PostDetailHeaderCard(
    post: CommunityPost,
    isPostExpired: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            PostImageCarousel(
                photos = post.photos,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isOnline = post.offerType == OfferType.ONLINE
                    val offerBadgeColor = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    val offerBadgeIcon = if (isOnline) Icons.Default.Language else Icons.Default.Store
                    val offerBadgeLabel = if (isOnline) "Online" else "Física"

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

                    if (!isPostExpired) {
                        VerdictBadge(
                            truthCount = post.truthCount,
                            falseCount = post.falseCount
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "-${post.discountPercentage}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onError,
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

fun LazyListScope.ImageAndHeaderSection(
    post: CommunityPost,
    isPostExpired: Boolean
) {
    item(key = "image_and_header") {
        PostDetailHeaderCard(post = post, isPostExpired = isPostExpired)
    }
}

@PreviewLightDark
@Composable
private fun PostDetailHeaderPreview() {
    RindeTheme {
        PostDetailHeaderCard(
            post = CommunityPost(
                id = "1",
                title = "Laptop Gamer ASUS TUF RTX 4060",
                normalPrice = 3500.0,
                discountPrice = 2800.0,
                discountPercentage = 20,
                currency = "S/",
                offerType = OfferType.ONLINE,
                truthCount = 12,
                falseCount = 1
            ),
            isPostExpired = false
        )
    }
}
