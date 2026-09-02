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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isPrivate by viewModel.isProfilePrivate.collectAsStateWithLifecycle()

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
        val themeOptions = listOf(
            ThemeMode.SYSTEM.name to stringResource(R.string.theme_system),
            ThemeMode.LIGHT.name to stringResource(R.string.theme_light),
            ThemeMode.DARK.name to stringResource(R.string.theme_dark)
        )
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = stringResource(R.string.settings_item_theme),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                themeOptions.forEach { (themeKey, label) ->
                    ListItem(
                        modifier = Modifier.clickable {
                            viewModel.setTheme(ThemeMode.valueOf(themeKey))
                            showThemeSheet = false
                        },
                        headlineContent = { Text(label) },
                        trailingContent = {
                            RadioButton(selected = themeKey == themeMode.name, onClick = null)
                        }
                    )
                }
            }
        }
    }

    if (showLanguageSheet) {
        val options = listOf(
            AppLanguage.ES.name to stringResource(R.string.language_es),
            AppLanguage.EN.name to stringResource(R.string.language_en)
        )
        ModalBottomSheet(onDismissRequest = { showLanguageSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = stringResource(R.string.settings_item_language),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                options.forEach { (langKey, label) ->
                    ListItem(
                        modifier = Modifier.clickable {
                            viewModel.setLanguage(AppLanguage.valueOf(langKey))
                            showLanguageSheet = false
                        },
                        headlineContent = { Text(label) },
                        trailingContent = {
                            RadioButton(selected = langKey == appLanguage.name, onClick = null)
                        }
                    )
                }
            }
        }
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
    // Resolver strings en el contexto @Composable antes del LazyColumn
    val themeLabel = when (currentTheme) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }
    val langLabel = when (currentLanguage) {
        AppLanguage.ES -> stringResource(R.string.language_es)
        AppLanguage.EN -> stringResource(R.string.language_en)
    }
    val sectionAppHeader = stringResource(R.string.settings_section_app)
    val labelTheme = stringResource(R.string.settings_item_theme)
    val labelLang = stringResource(R.string.settings_item_language)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        usageSection(onNavigateToSaved)
        privacySection(isPrivate, onTogglePrivacy, onNavigateToBlocked)
        appSection(
            currentTheme = currentTheme,
            currentLanguage = currentLanguage,
            themeLabel = themeLabel,
            langLabel = langLabel,
            sectionHeader = sectionAppHeader,
            labelTheme = labelTheme,
            labelLang = labelLang,
            onShowTheme = onShowTheme,
            onShowLanguage = onShowLanguage
        )
        moreSection(onShowLogout)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.usageSection(
    onNavigateToSaved: () -> Unit
) {
    item { SettingsSectionHeader(stringResource(R.string.settings_section_usage)) }
    item {
        SettingsListItem(
            icon = Icons.Default.BookmarkBorder,
            label = stringResource(R.string.profile_tab_saved),
            onClick = onNavigateToSaved
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.privacySection(
    isPrivate: Boolean,
    onTogglePrivacy: (Boolean) -> Unit,
    onNavigateToBlocked: () -> Unit
) {
    item { SettingsSectionHeader(stringResource(R.string.settings_section_privacy)) }
    item {
        SettingsListItem(
            icon = if (isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
            label = stringResource(R.string.settings_item_privacy_label),
            supportingText = if (isPrivate) {
                stringResource(R.string.settings_item_privacy_private_desc)
            } else {
                stringResource(R.string.settings_item_privacy_public_desc)
            },
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
}

private fun androidx.compose.foundation.lazy.LazyListScope.appSection(
    currentTheme: ThemeMode,
    currentLanguage: AppLanguage,
    themeLabel: String,
    langLabel: String,
    sectionHeader: String,
    labelTheme: String,
    labelLang: String,
    onShowTheme: () -> Unit,
    onShowLanguage: () -> Unit
) {
    item { SettingsSectionHeader(sectionHeader) }
    item {
        SettingsListItem(
            icon = Icons.Default.Palette,
            label = labelTheme,
            value = themeLabel,
            onClick = onShowTheme
        )
    }
    item {
        SettingsListItem(
            icon = Icons.Default.Language,
            label = labelLang,
            value = langLabel,
            onClick = onShowLanguage
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.moreSection(
    onShowLogout: () -> Unit
) {
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

@Preview(showBackground = true)
@Composable
fun SettingsContentPreview() {
    RindeTheme {
        SettingsContent(
            padding = PaddingValues(0.dp),
            isPrivate = false,
            currentTheme = ThemeMode.SYSTEM,
            currentLanguage = AppLanguage.ES,
            onTogglePrivacy = {},
            onNavigateToSaved = {},
            onNavigateToBlocked = {},
            onShowTheme = {},
            onShowLanguage = {},
            onShowLogout = {}
        )
    }
}

