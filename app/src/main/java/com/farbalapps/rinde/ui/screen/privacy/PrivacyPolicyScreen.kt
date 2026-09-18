package com.farbalapps.rinde.ui.screen.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindeTheme

@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit) {
    com.farbalapps.rinde.ui.screen.profile.legal.LegalScreen(
        initialTab = 0,
        onBack = onBackClick
    )
}

@PreviewLightDark
@Composable
fun PrivacyPolicyScreenPreview() {
    RindeTheme {
        PrivacyPolicyScreen(onBackClick = {})
    }
}
