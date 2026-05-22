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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.farbalapps.rinde.R
import com.farbalapps.rinde.data.local.AppLanguage
import com.farbalapps.rinde.data.local.ThemeMode
import com.farbalapps.rinde.ui.theme.RindeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    val themeMode by viewModel.themeMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isPrivate by viewModel.isProfilePrivate.collectAsState()

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showThemeSheet) {
        SettingsSelectionSheet(
            title = stringResource(R.string.settings_item_theme),
            options = ThemeMode.entries.map { it.name },
            selectedOption = themeMode.name,
            onOptionSelected = { viewModel.setTheme(ThemeMode.valueOf(it)) },
            onDismiss = { showThemeSheet = false }
        )
    }

    if (showLanguageSheet) {
        SettingsSelectionSheet(
            title = stringResource(R.string.settings_item_language),
            options = AppLanguage.entries.map { it.name },
            selectedOption = appLanguage.name,
            onOptionSelected = { viewModel.setLanguage(AppLanguage.valueOf(it)) },
            onDismiss = { showLanguageSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
            isPrivate = isPrivate,
            currentTheme = themeMode,
            currentLanguage = appLanguage,
            onTogglePrivacy = { viewModel.togglePrivacy(it) },
            onNavigateToSaved = onNavigateToSaved,
            onNavigateToBlocked = onNavigateToBlocked,
            onShowTheme = { showThemeSheet = true },
            onShowLanguage = { showLanguageSheet = true },
            onShowLogout = { showLogoutDialog = true }
        )
    }
}

@Composable
fun SettingsContent(
    padding: PaddingValues,
    isPrivate: Boolean,
    currentTheme: ThemeMode,
    currentLanguage: AppLanguage,
    onTogglePrivacy: (Boolean) -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onShowTheme: () -> Unit,
    onShowLanguage: () -> Unit,
    onShowLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        item { SettingsSectionHeader(stringResource(R.string.settings_section_usage)) }
        item {
            SettingsListItem(
                icon = Icons.Default.BookmarkBorder,
                label = stringResource(R.string.profile_tab_saved),
                onClick = onNavigateToSaved
            )
        }

        item { SettingsSectionHeader(stringResource(R.string.settings_section_privacy)) }
        item {
            SettingsListItem(
                icon = if (isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                label = stringResource(R.string.settings_item_privacy_label),
                supportingText = if (isPrivate) "Perfil privado (solo seguidores)" else "Perfil público (visible para todos)",
                trailingContent = {
                    Switch(checked = isPrivate, onCheckedChange = onTogglePrivacy)
                }
            )
        }
        item {
            SettingsListItem(
                icon = Icons.Default.Block,
                label = stringResource(R.string.settings_item_blocked),
                onClick = onNavigateToBlocked
            )
        }
        item {
            SettingsListItem(
                icon = Icons.Default.VerifiedUser,
                label = stringResource(R.string.settings_item_verify_account),
                onClick = { /* TODO */ }
            )
        }

        item { SettingsSectionHeader(stringResource(R.string.settings_section_app)) }
        item {
            SettingsListItem(
                icon = Icons.Default.Palette,
                label = stringResource(R.string.settings_item_theme),
                value = currentTheme.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = onShowTheme
            )
        }
        item {
            SettingsListItem(
                icon = Icons.Default.Language,
                label = stringResource(R.string.settings_item_language),
                value = currentLanguage.name,
                onClick = onShowLanguage
            )
        }

        item { SettingsSectionHeader(stringResource(R.string.settings_section_more)) }
        item {
            SettingsListItem(
                icon = Icons.Default.Info,
                label = stringResource(R.string.settings_item_about),
                onClick = { /* TODO */ }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            SettingsListItem(
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
fun SettingsListItem(
    icon: ImageVector,
    label: String,
    supportingText: String? = null,
    value: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
        headlineContent = { Text(label, color = labelColor) },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = { Icon(icon, null, tint = if (labelColor == MaterialTheme.colorScheme.error) labelColor else MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = trailingContent ?: {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (showChevron) {
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSelectionSheet(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            options.forEach { option ->
                ListItem(
                    modifier = Modifier.clickable {
                        onOptionSelected(option)
                        onDismiss()
                    },
                    headlineContent = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) },
                    trailingContent = {
                        RadioButton(selected = option == selectedOption, onClick = null)
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
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
