package com.farbalapps.rinde.ui.screen.profile.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindeTheme
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.legal_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.legal_tab_privacy), fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.legal_tab_terms), fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                PrivacyPolicyContent()
            } else {
                TermsOfUseContent()
            }
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    val lastUpdated = stringResource(R.string.legal_last_updated_date)
    val complianceTag = stringResource(R.string.legal_badge_compliance)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeaderBadge(
                date = lastUpdated,
                tag = complianceTag
            )
        }

        item {
            HighlightCard(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.legal_privacy_commitment_title),
                description = stringResource(R.string.legal_privacy_commitment_desc),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                iconColor = MaterialTheme.colorScheme.primary
            )
        }

        item {
            LegalSectionCard(
                number = "1",
                icon = Icons.Default.Badge,
                title = stringResource(R.string.legal_privacy_sec1_title),
                content = stringResource(R.string.legal_privacy_sec1_content)
            )
        }

        item {
            LegalSectionCard(
                number = "2",
                icon = Icons.Default.Handshake,
                title = stringResource(R.string.legal_privacy_sec2_title),
                content = stringResource(R.string.legal_privacy_sec2_content)
            )
        }

        item {
            LegalSectionCard(
                number = "3",
                icon = Icons.Default.PhoneAndroid,
                title = stringResource(R.string.legal_privacy_sec3_title),
                content = stringResource(R.string.legal_privacy_sec3_content)
            )
        }

        item {
            LegalSectionCard(
                number = "4",
                icon = Icons.Default.Shield,
                title = stringResource(R.string.legal_privacy_sec4_title),
                content = stringResource(R.string.legal_privacy_sec4_content)
            )
        }

        item {
            LegalSectionCard(
                number = "5",
                icon = Icons.Default.Storage,
                title = stringResource(R.string.legal_privacy_sec5_title),
                content = stringResource(R.string.legal_privacy_sec5_content)
            )
        }

        item {
            LegalSectionCard(
                number = "6",
                icon = Icons.Default.PersonRemove,
                title = stringResource(R.string.legal_privacy_sec6_title),
                content = stringResource(R.string.legal_privacy_sec6_content)
            )
        }

        item {
            LegalSectionCard(
                number = "7",
                icon = Icons.Default.Mail,
                title = stringResource(R.string.legal_privacy_sec7_title),
                content = stringResource(R.string.legal_privacy_sec7_content)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TermsOfUseContent() {
    val lastUpdated = stringResource(R.string.legal_last_updated_date)
    val termsTag = stringResource(R.string.legal_terms_badge_tag)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeaderBadge(
                date = lastUpdated,
                tag = termsTag
            )
        }

        item {
            HighlightCard(
                icon = Icons.Default.WarningAmber,
                title = stringResource(R.string.legal_terms_notice_title),
                description = stringResource(R.string.legal_terms_notice_desc),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                iconColor = MaterialTheme.colorScheme.tertiary
            )
        }

        item {
            LegalSectionCard(
                number = "1",
                icon = Icons.Default.Storefront,
                title = stringResource(R.string.legal_terms_sec1_title),
                content = stringResource(R.string.legal_terms_sec1_content)
            )
        }

        item {
            LegalSectionCard(
                number = "2",
                icon = Icons.Default.Groups,
                title = stringResource(R.string.legal_terms_sec2_title),
                content = stringResource(R.string.legal_terms_sec2_content)
            )
        }

        item {
            LegalSectionCard(
                number = "3",
                icon = Icons.Default.Block,
                title = stringResource(R.string.legal_terms_sec3_title),
                content = stringResource(R.string.legal_terms_sec3_content)
            )
        }

        item {
            LegalSectionCard(
                number = "4",
                icon = Icons.Default.Copyright,
                title = stringResource(R.string.legal_terms_sec4_title),
                content = stringResource(R.string.legal_terms_sec4_content)
            )
        }

        item {
            LegalSectionCard(
                number = "5",
                icon = Icons.Default.Shield,
                title = stringResource(R.string.legal_terms_sec5_title),
                content = stringResource(R.string.legal_terms_sec5_content)
            )
        }

        item {
            LegalSectionCard(
                number = "6",
                icon = Icons.Default.Update,
                title = stringResource(R.string.legal_terms_sec6_title),
                content = stringResource(R.string.legal_terms_sec6_content)
            )
        }

        item {
            LegalSectionCard(
                number = "7",
                icon = Icons.Default.Gavel,
                title = stringResource(R.string.legal_terms_sec7_title),
                content = stringResource(R.string.legal_terms_sec7_content)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderBadge(date: String, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = stringResource(R.string.legal_badge_updated, date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun HighlightCard(
    icon: ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun LegalSectionCard(
    number: String,
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "$number. $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LegalScreenPreview() {
    RindeTheme {
        LegalScreen(onBack = {})
    }
}
