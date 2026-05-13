package com.farbalapps.rinde.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToBlocked: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        SettingsContent(
            padding = padding,
            onNavigateToSaved = onNavigateToSaved,
            onNavigateToBlocked = onNavigateToBlocked,
            onShowLogout = { showLogoutDialog = true }
        )
    }
}

@Composable
fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_dialog_logout_title)) },
        text = { Text(stringResource(R.string.settings_dialog_logout_text)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.settings_dialog_logout_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

@Composable
fun SettingsContent(
    padding: PaddingValues,
    onNavigateToSaved: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onShowLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        item { SettingsSectionHeader(stringResource(R.string.settings_section_usage)) }
        item {
            SettingsItem(
                icon = Icons.Default.BookmarkBorder,
                label = stringResource(R.string.profile_tab_saved),
                onClick = onNavigateToSaved
            )
        }

        item { SettingsSectionHeader(stringResource(R.string.settings_section_privacy)) }
        item {
            SettingsItem(
                icon = Icons.Default.LockOpen,
                label = stringResource(R.string.settings_item_privacy_label),
                value = stringResource(R.string.settings_item_privacy_public),
                onClick = { /* TODO */ }
            )
        }
        item {
            SettingsItem(
                icon = Icons.Default.Block,
                label = stringResource(R.string.settings_item_blocked),
                onClick = onNavigateToBlocked
            )
        }
        item {
            SettingsItem(
                icon = Icons.Default.Verified,
                label = stringResource(R.string.settings_item_verify_account),
                onClick = { /* TODO: Verification Flow */ }
            )
        }


        item { SettingsSectionHeader(stringResource(R.string.settings_section_app)) }
        item {
            SettingsItem(
                icon = Icons.Default.Palette,
                label = stringResource(R.string.settings_item_theme),
                value = stringResource(R.string.settings_item_theme_dark),
                onClick = { /* TODO */ }
            )
        }
        item {
            SettingsItem(
                icon = Icons.Default.Language,
                label = stringResource(R.string.settings_item_language),
                value = stringResource(R.string.settings_item_language_es),
                onClick = { /* TODO */ }
            )
        }

        item { SettingsSectionHeader(stringResource(R.string.settings_section_payments)) }
        item {
            SettingsItem(
                icon = Icons.Default.CreditCard,
                label = stringResource(R.string.settings_item_payments),
                onClick = { /* TODO */ }
            )
        }

        item { SettingsSectionHeader(stringResource(R.string.settings_section_more)) }
        item {
            SettingsItem(
                icon = Icons.Default.Info,
                label = stringResource(R.string.settings_item_about),
                onClick = { /* TODO */ }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { SettingsSectionHeader(stringResource(R.string.settings_section_login)) }
        item {
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = stringResource(R.string.settings_btn_logout),
                labelColor = MaterialTheme.colorScheme.error,
                showChevron = false,
                onClick = onShowLogout
            )
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    label: String,
    value: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (labelColor == MaterialTheme.colorScheme.error) labelColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    RindeTheme {
        SettingsContent(
            padding = PaddingValues(0.dp),
            onNavigateToSaved = {},
            onNavigateToBlocked = {},
            onShowLogout = {}
        )
    }
}
