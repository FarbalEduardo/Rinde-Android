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
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("¿Estás seguro de que deseas salir de Rinde?") },
            confirmButton = {
                Button(
                    onClick = { 
                        showLogoutDialog = false
                        onLogout() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración y actividad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SettingsSectionHeader("Cómo usas Rinde") }
            item {
                SettingsItem(
                    icon = Icons.Default.BookmarkBorder,
                    label = "Guardados",
                    onClick = onNavigateToSaved
                )
            }

            item { SettingsSectionHeader("Quién puede ver tu contenido") }
            item {
                SettingsItem(
                    icon = Icons.Default.LockOpen,
                    label = "Privacidad de la cuenta",
                    value = "Pública",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Block,
                    label = "Usuarios bloqueados",
                    onClick = onNavigateToBlocked
                )
            }

            item { SettingsSectionHeader("Tu aplicación y medios") }
            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    label = "Tema",
                    value = "Oscuro",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    label = "Lenguaje",
                    value = "Español",
                    onClick = { /* TODO */ }
                )
            }

            item { SettingsSectionHeader("Tus pedidos y herramientas") }
            item {
                SettingsItem(
                    icon = Icons.Default.CreditCard,
                    label = "Pedidos y pagos",
                    onClick = { /* TODO */ }
                )
            }

            item { SettingsSectionHeader("Más información y soporte") }
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    label = "Acerca de Rinde",
                    onClick = { /* TODO */ }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { SettingsSectionHeader("Inicio de sesión") }
            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Cerrar sesión",
                    labelColor = MaterialTheme.colorScheme.error,
                    showChevron = false,
                    onClick = { showLogoutDialog = true }
                )
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
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
