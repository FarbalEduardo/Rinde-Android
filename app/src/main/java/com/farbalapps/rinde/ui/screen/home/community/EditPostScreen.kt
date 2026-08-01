package com.farbalapps.rinde.ui.screen.home.community

import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.farbalapps.rinde.domain.model.Category
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.ui.theme.RindePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    postId: String,
    onBack: () -> Unit,
    viewModel: EditPostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) { viewModel.loadPost(postId) }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onBack()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris -> viewModel.onPhotosSelected(uris) }

    val displayPhotos: List<Uri> = remember(uiState.remotePhotoUrls, uiState.newPhotoUris) {
        val remote = uiState.remotePhotoUrls.map { Uri.parse(it) }
        val newOnes = uiState.newPhotoUris
        (remote + newOnes).distinctBy { it.toString() }.take(4)
    }

    if (uiState.isLoadingPost) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RindePrimary)
        }
        return
    }

    EditPostScreenContent(
        uiState = uiState,
        displayPhotos = displayPhotos,
        onBack = onBack,
        onSubmit = { viewModel.submitChanges() },
        onPhotoRemoved = { viewModel.onPhotoRemoved(it) },
        onAddPhotoClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onOfferTypeChange = { viewModel.onOfferTypeChange(it) },
        onWebsiteNameChange = { viewModel.onWebsiteNameChange(it) },
        onProductLinkChange = { viewModel.onProductLinkChange(it) },
        onStoreNameChange = { viewModel.onStoreNameChange(it) },
        onLocationNameChange = { viewModel.onLocationNameChange(it) },
        onFetchCurrentLocation = { viewModel.fetchCurrentLocation() },
        onTitleChange = { viewModel.onTitleChange(it) },
        onDescriptionChange = { viewModel.onDescriptionChange(it) },
        onNormalPriceChange = { viewModel.onNormalPriceChange(it) },
        onDiscountPriceChange = { viewModel.onDiscountPriceChange(it) },
        onCurrencyChange = { viewModel.onCurrencyChange(it) },
        onIsAvailableChange = { viewModel.onIsAvailableChange(it) },
        onConditionChange = { viewModel.onConditionChange(it) },
        onHasCouponChange = { viewModel.onHasCouponChange(it) },
        onCouponCodeChange = { viewModel.onCouponCodeChange(it) },
        onCategoryChange = { viewModel.onCategoryChange(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreenContent(
    uiState: EditPostUiState,
    displayPhotos: List<Uri>,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onPhotoRemoved: (Uri) -> Unit,
    onAddPhotoClick: () -> Unit,
    onOfferTypeChange: (OfferType) -> Unit,
    onWebsiteNameChange: (String) -> Unit,
    onProductLinkChange: (String) -> Unit,
    onStoreNameChange: (String) -> Unit,
    onLocationNameChange: (String) -> Unit,
    onFetchCurrentLocation: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNormalPriceChange: (String) -> Unit,
    onDiscountPriceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onIsAvailableChange: (Boolean) -> Unit,
    onConditionChange: (String) -> Unit,
    onHasCouponChange: (Boolean) -> Unit,
    onCouponCodeChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit
) {
    val totalPhotos = displayPhotos.size

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Editar publicación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
                shadowElevation = 8.dp
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(containerColor = RindePrimary, contentColor = Color.White),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ErrorItem(uiState.error)
            PhotosSectionItem(
                displayPhotos = displayPhotos,
                totalPhotos = totalPhotos,
                newPhotoUrisCount = uiState.newPhotoUris.size,
                onPhotoRemoved = onPhotoRemoved,
                onAddPhotoClick = onAddPhotoClick
            )
            OfferTypeSectionItem(
                offerType = uiState.offerType,
                onOfferTypeChange = onOfferTypeChange
            )
            OnlineFieldsSectionItem(
                offerType = uiState.offerType,
                websiteName = uiState.websiteName,
                productLink = uiState.productLink,
                productLinkError = uiState.productLinkError,
                onWebsiteNameChange = onWebsiteNameChange,
                onProductLinkChange = onProductLinkChange
            )
            PhysicalFieldsSectionItem(
                offerType = uiState.offerType,
                storeName = uiState.storeName,
                locationName = uiState.locationName,
                onStoreNameChange = onStoreNameChange,
                onLocationNameChange = onLocationNameChange,
                onFetchCurrentLocation = onFetchCurrentLocation
            )
            TitleSectionItem(
                title = uiState.title,
                onTitleChange = onTitleChange
            )
            DescriptionSectionItem(
                description = uiState.description,
                onDescriptionChange = onDescriptionChange
            )
            PriceAndDetailsSectionItem(
                offerType = uiState.offerType,
                normalPriceInput = uiState.normalPriceInput,
                discountPriceInput = uiState.discountPriceInput,
                priceError = uiState.priceError,
                currency = uiState.currency,
                isAvailable = uiState.isAvailable,
                condition = uiState.condition,
                hasCoupon = uiState.hasCoupon,
                couponCode = uiState.couponCode,
                onNormalPriceChange = onNormalPriceChange,
                onDiscountPriceChange = onDiscountPriceChange,
                onCurrencyChange = onCurrencyChange,
                onIsAvailableChange = onIsAvailableChange,
                onConditionChange = onConditionChange,
                onHasCouponChange = onHasCouponChange,
                onCouponCodeChange = onCouponCodeChange
            )
            CategorySectionItem(
                category = uiState.category,
                onCategoryChange = onCategoryChange
            )
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun EditPostScreenPreview() {
    com.farbalapps.rinde.ui.theme.RindeTheme {
        EditPostScreenContent(
            uiState = EditPostUiState(),
            displayPhotos = emptyList(),
            onBack = {}, onSubmit = {}, onPhotoRemoved = {}, onAddPhotoClick = {},
            onOfferTypeChange = {}, onWebsiteNameChange = {}, onProductLinkChange = {},
            onStoreNameChange = {}, onLocationNameChange = {}, onFetchCurrentLocation = {},
            onTitleChange = {}, onDescriptionChange = {}, onNormalPriceChange = {},
            onDiscountPriceChange = {}, onCurrencyChange = {}, onIsAvailableChange = {},
            onConditionChange = {}, onHasCouponChange = {}, onCouponCodeChange = {},
            onCategoryChange = {}
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ErrorItem(error: String?) {
    if (error != null) {
        item {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.PhotosSectionItem(
    displayPhotos: List<Uri>,
    totalPhotos: Int,
    newPhotoUrisCount: Int,
    onPhotoRemoved: (Uri) -> Unit,
    onAddPhotoClick: () -> Unit
) {
    item {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fotos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(text = "$totalPhotos/4", style = MaterialTheme.typography.labelMedium, color = if (totalPhotos == 4) RindePrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (newPhotoUrisCount > 0) {
                Text("✅ Fotos existentes + $newPhotoUrisCount nueva(s) agregada(s). Toca X para quitar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Toca '+' para agregar fotos o X para eliminar una.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(displayPhotos) { uri ->
                    Box(modifier = Modifier.size(110.dp).clip(MaterialTheme.shapes.medium).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)) {
                        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape).clickable { onPhotoRemoved(uri) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (totalPhotos < 4) {
                    item {
                        Surface(modifier = Modifier.size(110.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), onClick = onAddPhotoClick) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = RindePrimary)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Cambiar", style = MaterialTheme.typography.labelSmall, color = RindePrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.OfferTypeSectionItem(
    offerType: OfferType,
    onOfferTypeChange: (OfferType) -> Unit
) {
    item {
        Column {
            Text("Tipo de Oferta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(OfferType.ONLINE to "Online", OfferType.PHYSICAL to "Física").forEach { (type, label) ->
                    val selected = offerType == type
                    Surface(onClick = { onOfferTypeChange(type) }, shape = RoundedCornerShape(50), color = if (selected) RindePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant, border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) RindePrimary else MaterialTheme.colorScheme.outlineVariant)) {
                        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = if (selected) RindePrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.OnlineFieldsSectionItem(
    offerType: OfferType,
    websiteName: String,
    productLink: String,
    productLinkError: String?,
    onWebsiteNameChange: (String) -> Unit,
    onProductLinkChange: (String) -> Unit
) {
    if (offerType == OfferType.ONLINE) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = websiteName, onValueChange = onWebsiteNameChange, label = { Text("Página web") }, leadingIcon = { Icon(Icons.Default.Language, null) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences))
                OutlinedTextField(value = productLink, onValueChange = onProductLinkChange, label = { Text("Link del producto") }, leadingIcon = { Icon(Icons.Default.Link, null) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary), singleLine = true, isError = productLinkError != null, supportingText = productLinkError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } })
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.PhysicalFieldsSectionItem(
    offerType: OfferType,
    storeName: String,
    locationName: String,
    onStoreNameChange: (String) -> Unit,
    onLocationNameChange: (String) -> Unit,
    onFetchCurrentLocation: () -> Unit
) {
    if (offerType == OfferType.PHYSICAL) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = storeName, onValueChange = onStoreNameChange, label = { Text("Nombre de la tienda") }, leadingIcon = { Icon(Icons.Default.Store, null) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences))
                OutlinedTextField(value = locationName, onValueChange = onLocationNameChange, label = { Text("Ubicación") }, leadingIcon = { Icon(Icons.Default.Place, null) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences), trailingIcon = { IconButton(onClick = onFetchCurrentLocation) { Icon(Icons.Default.MyLocation, null, tint = RindePrimary) } })
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.TitleSectionItem(
    title: String,
    onTitleChange: (String) -> Unit
) {
    item {
        OutlinedTextField(value = title, onValueChange = onTitleChange, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.DescriptionSectionItem(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    item {
        OutlinedTextField(value = description, onValueChange = onDescriptionChange, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth().height(150.dp), shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary), keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.PriceAndDetailsSectionItem(
    offerType: OfferType,
    normalPriceInput: String,
    discountPriceInput: String,
    priceError: String?,
    currency: String,
    isAvailable: Boolean,
    condition: String,
    hasCoupon: Boolean,
    couponCode: String,
    onNormalPriceChange: (String) -> Unit,
    onDiscountPriceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onIsAvailableChange: (Boolean) -> Unit,
    onConditionChange: (String) -> Unit,
    onHasCouponChange: (Boolean) -> Unit,
    onCouponCodeChange: (String) -> Unit
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Precio y Detalles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = normalPriceInput, onValueChange = onNormalPriceChange, label = { Text("Precio Normal") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary), isError = priceError != null)
                OutlinedTextField(value = discountPriceInput, onValueChange = onDiscountPriceChange, label = { Text("Precio c/ Desc.") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary), isError = priceError != null)
            }
            if (priceError != null) { Text(text = priceError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
            val nPrice = normalPriceInput.toDoubleOrNull()
            val dPrice = discountPriceInput.toDoubleOrNull()
            if (nPrice != null && dPrice != null && nPrice > 0 && nPrice > dPrice && priceError == null) {
                Text("Descuento: ${(((nPrice - dPrice) / nPrice) * 100).toInt()}%", color = RindePrimary, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedTextField(value = currency, onValueChange = onCurrencyChange, label = { Text("Moneda") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Producto Disponible")
                Switch(checked = isAvailable, onCheckedChange = onIsAvailableChange, colors = SwitchDefaults.colors(checkedThumbColor = RindePrimary, checkedTrackColor = RindePrimary.copy(alpha = 0.5f)))
            }
            Text("Condición", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Nuevo", "Usado", "Reacondicionado").forEach { cond ->
                    FilterChip(selected = condition == cond, onClick = { onConditionChange(cond) }, label = { Text(cond) }, shape = RoundedCornerShape(50), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RindePrimary.copy(alpha = 0.1f), selectedLabelColor = RindePrimary))
                }
            }
            if (offerType == OfferType.ONLINE) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("¿Tiene Código de Cupón?")
                    Switch(checked = hasCoupon, onCheckedChange = onHasCouponChange, colors = SwitchDefaults.colors(checkedThumbColor = RindePrimary, checkedTrackColor = RindePrimary.copy(alpha = 0.5f)))
                }
                if (hasCoupon) {
                    OutlinedTextField(value = couponCode, onValueChange = onCouponCodeChange, label = { Text("Código de Cupón") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RindePrimary, focusedLabelColor = RindePrimary))
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.CategorySectionItem(
    category: String,
    onCategoryChange: (String) -> Unit
) {
    item {
        Column {
            Text("Categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Category.FIXED_COMMUNITY_CATEGORIES) { cat ->
                    FilterChip(selected = category == cat, onClick = { onCategoryChange(cat) }, label = { Text(cat) }, shape = RoundedCornerShape(50), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RindePrimary.copy(alpha = 0.1f), selectedLabelColor = RindePrimary))
                }
            }
        }
    }
}
