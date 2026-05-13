package com.farbalapps.rinde.ui.screen.profile.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBack()
        }
    }

    EditProfileContent(
        uiState = uiState,
        onBack = onBack,
        onNameChange = { viewModel.onNameChange(it) },
        onPhotoChange = { viewModel.onPhotoChange(it) },
        onPrivacyToggle = { viewModel.togglePrivacy(it) },
        onSave = { viewModel.saveProfile() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileContent(
    uiState: EditProfileUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onPhotoChange: (String) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPhotoChange(it.toString()) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(stringResource(R.string.edit_profile_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
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
        ) {
            EditProfileForm(
                uiState = uiState,
                onNameChange = onNameChange,
                onPhotoChange = onPhotoChange,
                onPrivacyToggle = onPrivacyToggle,
                onSave = onSave
            )
        }
    }
}

@Composable
fun EditProfileForm(
    uiState: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onPhotoChange: (String) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPhotoChange(it.toString()) }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Avatar Selection
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        EditAvatarSection(
            photoUrl = uiState.photoUrl,
            onClick = {
                photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
            onSave = {}
        )
    }
}
