package com.farbalapps.rinde.ui.screen.profile.edit

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.profile.edit.components.ChangePasswordDialog
import com.farbalapps.rinde.ui.screen.profile.edit.components.EditAvatarSection
import com.farbalapps.rinde.ui.screen.profile.edit.components.PermanentDeleteConfirmationDialog
import com.farbalapps.rinde.ui.screen.profile.edit.components.PrivacyToggleSection
import com.farbalapps.rinde.ui.screen.profile.edit.components.SoftAccountActionDialog
import com.farbalapps.rinde.ui.theme.RindeTheme

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit = onBack,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBack()
        }
    }

    LaunchedEffect(uiState.isAccountDeleted) {
        if (uiState.isAccountDeleted) {
            Toast.makeText(
                context,
                context.getString(R.string.account_deleted_success),
                Toast.LENGTH_LONG
            ).show()
            onAccountDeleted()
        }
    }

    LaunchedEffect(uiState.isAccountSuspended) {
        if (uiState.isAccountSuspended) {
            Toast.makeText(
                context,
                context.getString(R.string.account_suspended_success),
                Toast.LENGTH_LONG
            ).show()
            onAccountDeleted()
        }
    }

    LaunchedEffect(uiState.changePasswordSuccess) {
        if (uiState.changePasswordSuccess) {
            Toast.makeText(
                context,
                context.getString(R.string.change_password_success),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    EditProfileContent(
        uiState = uiState,
        onBack = onBack,
        onNameChange = { viewModel.onNameChange(it) },
        onPhotoChange = { viewModel.onPhotoChange(it) },
        onPrivacyToggle = { viewModel.togglePrivacy(it) },
        onSave = { viewModel.saveProfile() },
        onOpenChangePassword = { viewModel.openChangePasswordDialog() },
        onChangePasswordConfirm = { current, new, confirm ->
            viewModel.changePassword(current, new, confirm)
        },
        onOpenAccountManagement = { viewModel.openAccountManagementDialog() },
        onSuspendAccount = { viewModel.suspendAccount() },
        onProceedToPermanentDelete = { viewModel.proceedToPermanentDeleteDialog() },
        onConfirmPermanentDelete = { viewModel.deleteAccountPermanently() },
        onDismissDialog = { viewModel.dismissDialog() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileContent(
    uiState: EditProfileUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onPhotoChange: (String?) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onSave: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onChangePasswordConfirm: (current: String, new: String, confirm: String) -> Unit,
    onOpenAccountManagement: () -> Unit,
    onSuspendAccount: () -> Unit,
    onProceedToPermanentDelete: () -> Unit,
    onConfirmPermanentDelete: () -> Unit,
    onDismissDialog: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.edit_profile_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            EditProfileForm(
                uiState = uiState,
                onNameChange = onNameChange,
                onPhotoChange = onPhotoChange,
                onPrivacyToggle = onPrivacyToggle,
                onSave = onSave,
                onOpenChangePassword = onOpenChangePassword,
                onOpenAccountManagement = onOpenAccountManagement
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // --- Diálogos Modal Conectados ---

    when (uiState.activeDialog) {
        EditProfileActiveDialog.CHANGE_PASSWORD -> {
            ChangePasswordDialog(
                onDismiss = onDismissDialog,
                onConfirm = onChangePasswordConfirm,
                isLoading = uiState.isChangingPassword,
                errorMessage = uiState.changePasswordError
            )
        }
        EditProfileActiveDialog.SOFT_OPTIONS -> {
            SoftAccountActionDialog(
                onDismiss = onDismissDialog,
                onSuspend = onSuspendAccount,
                onProceedToPermanentDelete = onProceedToPermanentDelete,
                isLoading = uiState.isLoading
            )
        }
        EditProfileActiveDialog.PERMANENT_DELETE -> {
            PermanentDeleteConfirmationDialog(
                onDismiss = onDismissDialog,
                onConfirmDelete = onConfirmPermanentDelete,
                isLoading = uiState.isLoading
            )
        }
        EditProfileActiveDialog.NONE -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileForm(
    uiState: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onPhotoChange: (String?) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onSave: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onOpenAccountManagement: () -> Unit
) {
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPhotoChange(it.toString()) }
    }

    var showPhotoOptions by remember { mutableStateOf(false) }

    if (showPhotoOptions) {
        PhotoOptionsBottomSheet(
            onDismiss = { showPhotoOptions = false },
            onChooseFromGallery = {
                showPhotoOptions = false
                photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDeletePhoto = {
                showPhotoOptions = false
                onPhotoChange(null)
            }
        )
    }

    // 1. Hero Section: Avatar compacto con edición
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        EditAvatarSection(
            photoUrl = uiState.photoUrl,
            size = 120.dp,
            onClick = {
                if (uiState.photoUrl.isNullOrBlank()) {
                    photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } else {
                    showPhotoOptions = true
                }
            }
        )
    }

    // 2. Tarjeta: Información Personal
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Campo Nombre
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.edit_profile_label_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = MaterialTheme.shapes.small
            )

            // Fila Correo (Informativo / no editable compacto)
            if (uiState.email.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    // 3. Tarjeta: Seguridad y Privacidad
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (uiState.isGoogleUser) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.change_password_google_user),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onOpenChangePassword() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.edit_profile_btn_change_password),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Toggle de Privacidad compacto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Perfil Privado",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (uiState.isPrivate) "Solo tus seguidores ven tus posts" else "Público para todos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.isPrivate,
                    onCheckedChange = onPrivacyToggle
                )
            }
        }
    }

    if (uiState.error != null) {
        Text(
            text = uiState.error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp)
        )
    }

    // 4. Botón Guardar Cambios (Compacto)
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
    } else {
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                stringResource(R.string.edit_profile_btn_save),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // 5. Zona de Peligro / Gestión de Cuenta (Compacto)
    TextButton(
        onClick = onOpenAccountManagement,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            imageVector = Icons.Default.DeleteForever,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.settings_btn_delete_account),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoOptionsBottomSheet(
    onDismiss: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onDeletePhoto: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Foto de perfil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onChooseFromGallery() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Elegir de la galería", style = MaterialTheme.typography.bodyLarge)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onDeletePhoto() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Eliminar foto actual",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    RindeTheme {
        EditProfileContent(
            uiState = EditProfileUiState(
                name = "Eduardo Farbal",
                email = "eduardo@example.com",
                isPrivate = true
            ),
            onBack = {},
            onNameChange = {},
            onPhotoChange = {},
            onPrivacyToggle = {},
            onSave = {},
            onOpenChangePassword = {},
            onChangePasswordConfirm = { _, _, _ -> },
            onOpenAccountManagement = {},
            onSuspendAccount = {},
            onProceedToPermanentDelete = {},
            onConfirmPermanentDelete = {},
            onDismissDialog = {}
        )
    }
}
