package com.farbalapps.rinde.ui.screen.home.community

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
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

    val remainingSlots = 4 - uiState.photoUris.size
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        viewModel.onPhotosSelected(uris)
    }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_post_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = dimensionResource(id = R.dimen.elevation_medium)
            ) {
                Column {
                    val requiredFields = if (uiState.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE) 6 else 6
                    // Campos: Fotos, Tipo, Tienda/Web, Ubicacion/Link, Titulo, Desc, Cat
                    var completedCount = 0
                    if (uiState.photoUris.isNotEmpty()) completedCount++
                    if (uiState.offerType != com.farbalapps.rinde.domain.model.OfferType.UNSPECIFIED) completedCount++
                    if (uiState.title.isNotBlank()) completedCount++
                    if (uiState.description.isNotBlank()) completedCount++
                    if (uiState.category.isNotBlank()) completedCount++
                    if (uiState.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE && uiState.websiteName.isNotBlank() && uiState.productLink.isNotBlank()) completedCount++
                    if (uiState.offerType == com.farbalapps.rinde.domain.model.OfferType.PHYSICAL && uiState.storeName.isNotBlank() && uiState.locationName.isNotBlank()) completedCount++
                    
                    val progress = completedCount.toFloat() / 6f
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = RindePrimary,
                        trackColor = RindePrimary.copy(alpha = 0.1f)
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Button(
                        onClick = { viewModel.submitPost() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.padding_large), vertical = dimensionResource(id = R.dimen.padding_medium))
                            .height(dimensionResource(id = R.dimen.button_height_standard)),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RindePrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = dimensionResource(id = R.dimen.elevation_none),
                            pressedElevation = dimensionResource(id = R.dimen.elevation_small)
                        ),
                        enabled = !uiState.isLoading &&
                                  !uiState.isPrivateProfile &&
                                  uiState.photoUris.isNotEmpty() &&
                                  uiState.title.isNotBlank() &&
                                  uiState.description.isNotBlank() &&
                                  uiState.category != "" &&
                                  (uiState.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE ||
                                   uiState.offerType == com.farbalapps.rinde.domain.model.OfferType.PHYSICAL) &&
                                  (uiState.offerType != com.farbalapps.rinde.domain.model.OfferType.ONLINE ||
                                   (uiState.websiteName.isNotBlank() && uiState.productLink.isNotBlank())) &&
                                  (uiState.offerType != com.farbalapps.rinde.domain.model.OfferType.PHYSICAL ||
                                   (uiState.storeName.isNotBlank() && uiState.locationName.isNotBlank()))
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_medium)),
                                color = Color.White,
                                strokeWidth = dimensionResource(id = R.dimen.stroke_medium)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_small))
                            )
                            Spacer(Modifier.width(dimensionResource(id = R.dimen.padding_medium)))
                            Text(
                                text = if (completedCount < 6) "COMPLETA LOS DATOS" else stringResource(R.string.btn_publish).uppercase(),
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
                .padding(horizontal = dimensionResource(id = R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_large))
        ) {
            // Privacy Alert
            if (uiState.isPrivateProfile) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.privacy_restriction_msg),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Error Message
            if (uiState.error != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(Modifier.padding(dimensionResource(id = R.dimen.padding_medium)), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(dimensionResource(id = R.dimen.padding_medium)))
                            Text(
                                text = uiState.error!!, 
                                color = MaterialTheme.colorScheme.onErrorContainer, 
                                fontSize = dimensionResource(id = R.dimen.text_size_small).value.sp
                            )
                        }
                    }
                }
            }

            // ── 1. FOTOS ──────────────────────────────────────────────────────
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.create_post_label_photos),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${uiState.photoUris.size}/4",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uiState.photoUris.size == 4) RindePrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Mínimo 1, máximo 4 imágenes (obligatorio)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_medium)))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))) {
                        items(uiState.photoUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .border(dimensionResource(id = R.dimen.stroke_thin), MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Botón para remover foto
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                                        .clickable { viewModel.onPhotoRemoved(uri) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Quitar foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        if (uiState.photoUris.size < 4) {
                            item {
                                val canAdd = remainingSlots > 0
                                Surface(
                                    modifier = Modifier.size(110.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    onClick = {
                                        if (canAdd) {
                                            galleryLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = RindePrimary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Agregar",
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

            // ── 2. TIPO DE OFERTA ─────────────────────────────────────────────
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.offer_type_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isOnline = uiState.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE
                        val isPhysical = uiState.offerType == com.farbalapps.rinde.domain.model.OfferType.PHYSICAL
                        Surface(
                            onClick = { viewModel.onOfferTypeChange(com.farbalapps.rinde.domain.model.OfferType.ONLINE) },
                            shape = RoundedCornerShape(50),
                            color = if (isOnline) RindePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isOnline) RindePrimary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.offer_type_online),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isOnline) RindePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isOnline) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                        Surface(
                            onClick = { viewModel.onOfferTypeChange(com.farbalapps.rinde.domain.model.OfferType.PHYSICAL) },
                            shape = RoundedCornerShape(50),
                            color = if (isPhysical) RindePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPhysical) RindePrimary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.offer_type_physical),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPhysical) RindePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isPhysical) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // ── 3A. CAMPOS DE OFERTA ONLINE (Sitio web + Link) ───────────────
            if (uiState.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.websiteName,
                            onValueChange = viewModel::onWebsiteNameChange,
                            label = { Text("Página web") },
                            placeholder = { Text(stringResource(R.string.create_post_hint_website)) },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RindePrimary,
                                focusedLabelColor = RindePrimary
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.productLink,
                            onValueChange = viewModel::onProductLinkChange,
                            label = { Text("Link del producto") },
                            placeholder = { Text(stringResource(R.string.create_post_hint_link)) },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RindePrimary,
                                focusedLabelColor = RindePrimary
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // ── 3B. CAMPOS DE OFERTA FÍSICA (Tienda → Ubicación) ─────────────
            if (uiState.offerType == com.farbalapps.rinde.domain.model.OfferType.PHYSICAL) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.storeName,
                            onValueChange = viewModel::onStoreNameChange,
                            label = { Text("Nombre de la tienda") },
                            placeholder = { Text(stringResource(R.string.create_post_hint_store)) },
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RindePrimary,
                                focusedLabelColor = RindePrimary
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.locationName,
                            onValueChange = viewModel::onLocationNameChange,
                            label = { Text(stringResource(R.string.create_post_hint_location)) },
                            placeholder = { Text(stringResource(R.string.create_post_placeholder_location)) },
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RindePrimary,
                                focusedLabelColor = RindePrimary
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.fetchCurrentLocation() }) {
                                    Icon(Icons.Default.MyLocation, contentDescription = stringResource(id = R.string.action_gps), tint = RindePrimary)
                                }
                            }
                        )
                        Text(
                            stringResource(R.string.create_post_location_helper),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = dimensionResource(id = R.dimen.padding_xsmall))
                        )
                    }
                }
            }

            // ── 4. TÍTULO ─────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(stringResource(R.string.create_post_hint_title)) },
                    placeholder = { Text(stringResource(R.string.create_post_placeholder_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RindePrimary,
                        focusedLabelColor = RindePrimary
                    ),
                    singleLine = true
                )
            }

            // ── 5. DESCRIPCIÓN ────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text(stringResource(R.string.create_post_label_description)) },
                    placeholder = { Text(stringResource(R.string.create_post_placeholder_description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RindePrimary,
                        focusedLabelColor = RindePrimary
                    )
                )
            }

            // ── 6. CATEGORÍA (sin valor por defecto, el usuario debe elegir) ──
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.create_post_label_category),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_small)))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))) {
                        items(com.farbalapps.rinde.domain.model.Category.FIXED_COMMUNITY_CATEGORIES) { cat ->
                            FilterChip(
                                selected = uiState.category == cat,
                                onClick = { viewModel.onCategoryChange(cat) },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RindePrimary.copy(alpha = 0.1f),
                                    selectedLabelColor = RindePrimary
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(dimensionResource(id = R.dimen.padding_xlarge))) }

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