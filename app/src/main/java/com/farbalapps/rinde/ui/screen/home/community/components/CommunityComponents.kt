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
                contentDescription = androidx.compose.ui.res.stringResource(com.farbalapps.rinde.R.string.wishlist_add_desc),
                tint = RindePrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(com.farbalapps.rinde.R.string.wishlist_add_title),
                style = MaterialTheme.typography.labelMedium,
                color = RindePrimary,
                fontWeight = FontWeight.Bold
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
            selected = selectedTab == CommunityTab.HOT,
            onClick = { onTabSelected(CommunityTab.HOT) },
            label = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Lo más Hot", fontWeight = if (selectedTab == CommunityTab.HOT) FontWeight.SemiBold else FontWeight.Normal)
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
// VERDICT BADGE — M3 Dynamic Voting System
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VerdictBadge(
    truthCount: Int,
    falseCount: Int,
    modifier: Modifier = Modifier
) {
    val verdict = com.farbalapps.rinde.domain.model.VerdictCalculator.calculate(truthCount, falseCount)
    val percentage = com.farbalapps.rinde.domain.model.VerdictCalculator.truthPercent(truthCount, falseCount)
    
    val (color, icon, text) = when (verdict) {
        com.farbalapps.rinde.domain.model.PostVerdict.MOSTLY_TRUE -> {
            Triple(
                com.farbalapps.rinde.ui.theme.VerdictTrueColor,
                Icons.Default.Check,
                "$percentage% Real"
            )
        }
        com.farbalapps.rinde.domain.model.PostVerdict.MOSTLY_FALSE -> {
            Triple(
                com.farbalapps.rinde.ui.theme.VerdictFalseColor,
                Icons.Default.Close,
                "${100 - percentage}% Falso"
            )
        }
        com.farbalapps.rinde.domain.model.PostVerdict.DISPUTED -> {
            Triple(
                com.farbalapps.rinde.ui.theme.VerdictDisputedColor,
                Icons.Default.Scale,
                "Opiniones divididas"
            )
        }
        com.farbalapps.rinde.domain.model.PostVerdict.VALIDATING -> {
            Triple(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                Icons.Default.HourglassEmpty,
                "En validación"
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        modifier = modifier.height(28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
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
