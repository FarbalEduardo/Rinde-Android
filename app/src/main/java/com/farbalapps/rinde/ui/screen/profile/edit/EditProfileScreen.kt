package com.farbalapps.rinde.ui.screen.profile.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.profile.edit.components.EditAvatarSection
import com.farbalapps.rinde.ui.screen.profile.edit.components.PrivacyToggleSection
import com.farbalapps.rinde.ui.theme.RindeTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit = onBack,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBack()
        }
    }

    LaunchedEffect(uiState.isAccountDeleted) {
        if (uiState.isAccountDeleted) {
            onAccountDeleted()
        }
    }

    EditProfileContent(
        uiState = uiState,
        onBack = onBack,
        onNameChange = { viewModel.onNameChange(it) },
        onPhotoChange = { viewModel.onPhotoChange(it) },
        onPrivacyToggle = { viewModel.togglePrivacy(it) },
        onSave = { viewModel.saveProfile() },
        onDeleteAccount = { viewModel.deleteAccount() }
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
    onDeleteAccount: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(stringResource(R.string.edit_profile_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) 
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
                .padding(horizontal = 24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            EditProfileForm(
                uiState = uiState,
                onNameChange = onNameChange,
                onPhotoChange = onPhotoChange,
                onPrivacyToggle = onPrivacyToggle,
                onSave = onSave,
                onDeleteAccount = onDeleteAccount
            )
        }
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
    onDeleteAccount: () -> Unit
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

    Spacer(modifier = Modifier.height(16.dp))

    // Avatar Selection
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        EditAvatarSection(
            photoUrl = uiState.photoUrl,
            onClick = {
                if (uiState.photoUrl.isNullOrBlank()) {
                    photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } else {
                    showPhotoOptions = true
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(40.dp))

    Text(
        text = stringResource(R.string.edit_profile_label_name),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    
    OutlinedTextField(
        value = uiState.name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Privacy Option
    PrivacyToggleSection(
        isPrivate = uiState.isPrivate,
        onToggle = onPrivacyToggle
    )

    Spacer(modifier = Modifier.height(40.dp))

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(stringResource(R.string.edit_profile_btn_save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (uiState.error != null) {
        Text(
            text = uiState.error!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_delete_account_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_delete_account_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(R.string.settings_delete_account_confirm),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(36.dp))

    // Zona de eliminación de cuenta
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedButton(
        onClick = { showDeleteDialog = true },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        )
    ) {
        Icon(
            imageVector = Icons.Default.DeleteForever,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.settings_btn_delete_account),
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
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
                    .clickable { onChooseFromGallery() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Elegir de la galería", style = MaterialTheme.typography.bodyLarge)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeletePhoto() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Eliminar foto actual", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
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
                isPrivate = true
            ),
            onBack = {},
            onNameChange = {},
            onPhotoChange = {},
            onPrivacyToggle = {},
            onSave = {},
            onDeleteAccount = {}
        )
    }
}
