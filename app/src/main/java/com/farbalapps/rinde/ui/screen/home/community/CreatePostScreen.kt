package com.farbalapps.rinde.ui.screen.home.community

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.Category
import com.farbalapps.rinde.ui.theme.RindePrimary
import com.farbalapps.rinde.ui.theme.RindeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onBack: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.onPhotosSelected(uris)
    }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onBack()
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.create_post_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // Un área de botón limpia y prominente
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp // Sombra sutil sin tinte
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Button(
                        onClick = { viewModel.submitPost() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RindePrimary, // Uso directo del color de marca para énfasis
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 4.dp
                        ),
                        enabled = !uiState.isLoading && uiState.title.isNotBlank() && uiState.description.isNotBlank()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle, // Icono de éxito/completado
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.btn_publish).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Error Message
            if (uiState.error != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Photos Section
            item {
                Column {
                    Text(
                        stringResource(R.string.create_post_label_photos),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.photoUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        if (uiState.photoUris.size < 4) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .size(110.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    onClick = { galleryLauncher.launch("image/*") }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.AddPhotoAlternate, 
                                                contentDescription = null,
                                                tint = RindePrimary
                                            )
                                            Text(
                                                "Añadir",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RindePrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Title Field
            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(stringResource(R.string.create_post_hint_title)) },
                    placeholder = { Text(stringResource(R.string.create_post_placeholder_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RindePrimary,
                        focusedLabelColor = RindePrimary
                    ),
                    singleLine = true
                )
            }

            // Description Field
            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text(stringResource(R.string.create_post_label_description)) },
                    placeholder = { Text(stringResource(R.string.create_post_placeholder_description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RindePrimary,
                        focusedLabelColor = RindePrimary
                    )
                )
            }

            // Category Picker
            item {
                Column {
                    Text(
                        stringResource(R.string.create_post_label_category),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Category.FIXED_COMMUNITY_CATEGORIES) { cat ->
                            FilterChip(
                                selected = uiState.category == cat,
                                onClick = { viewModel.onCategoryChange(cat) },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RindePrimary.copy(alpha = 0.1f),
                                    selectedLabelColor = RindePrimary,
                                    selectedLeadingIconColor = RindePrimary
                                )
                            )
                        }
                    }
                }
            }

            // Location
            item {
                Column {
                    Text(
                        stringResource(R.string.create_post_label_location),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.locationName,
                        onValueChange = viewModel::onLocationNameChange,
                        label = { Text(stringResource(R.string.create_post_hint_location)) },
                        placeholder = { Text(stringResource(R.string.create_post_placeholder_location)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.fetchCurrentLocation() }) {
                                Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = RindePrimary)
                            }
                        }
                    )
                    Text(
                        stringResource(R.string.create_post_location_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreatePostScreenPreview() {
    RindeTheme {
        CreatePostScreen(onBack = {})
    }
}