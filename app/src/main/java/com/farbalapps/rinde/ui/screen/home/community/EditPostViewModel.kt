package com.farbalapps.rinde.ui.screen.home.community

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.util.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// UI State para edición de publicación
// ─────────────────────────────────────────────────────────────────────────────

data class EditPostUiState(
    // Datos del post original (para detectar cambios en fotos)
    val originalPost: CommunityPost? = null,
    val postId: String = "",

    // Campos editables (texto)
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val locationName: String = "",
    val offerType: OfferType = OfferType.ONLINE,
    val websiteName: String = "",
    val productLink: String = "",
    val storeName: String = "",
    val normalPriceInput: String = "",
    val discountPriceInput: String = "",
    val currency: String = "MXN",
    val hasCoupon: Boolean = false,
    val couponCode: String = "",
    val isAvailable: Boolean = true,
    val condition: String = "Nuevo",

    // Fotos — dos tipos:
    //  · remotePhotoUrls: las URLs ya subidas (las originales del post)
    //  · newPhotoUris: fotos nuevas que el usuario acaba de seleccionar en el picker
    val remotePhotoUrls: List<String> = emptyList(), // fotos actuales (Cloudinary)
    val newPhotoUris: List<Uri> = emptyList(),       // fotos nuevas (aún locales)

    // Control de UI
    val isLoading: Boolean = false,
    val isLoadingPost: Boolean = true,
    val error: String? = null,
    val isFinished: Boolean = false
)

@HiltViewModel
class EditPostViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val postDao: PostDao,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditPostUiState())
    val uiState: StateFlow<EditPostUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Carga del post existente (desde Room primero, sin esperar red)
    // ─────────────────────────────────────────────────────────────────────────

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPost = true, postId = postId) }
            val entity = postDao.getPostById(postId)
            if (entity != null) {
                val post = entity.toDomainModel()
                _uiState.update { state ->
                    state.copy(
                        originalPost = post,
                        title = post.title,
                        description = post.descriptionLong,
                        category = post.category,
                        locationName = post.location.name,
                        offerType = post.offerType,
                        websiteName = post.websiteName ?: "",
                        productLink = post.productLink ?: "",
                        storeName = post.storeName ?: "",
                        normalPriceInput = post.normalPrice?.toString() ?: "",
                        discountPriceInput = post.discountPrice?.toString() ?: "",
                        currency = post.currency,
                        hasCoupon = !post.couponCode.isNullOrBlank(),
                        couponCode = post.couponCode ?: "",
                        isAvailable = post.isAvailable,
                        condition = post.condition,
                        remotePhotoUrls = post.photos, // Las URLs remotas actuales
                        newPhotoUris = emptyList(),     // Ninguna foto nueva seleccionada aún
                        isLoadingPost = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingPost = false, error = "No se encontró la publicación") }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Manejo de fotos
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * El usuario seleccionó fotos nuevas en el picker.
     * Las nuevas se AGREGAN a las existentes (remotas ya guardadas).
     * El límite total es 4 fotos entre remotas + nuevas.
     */
    fun onPhotosSelected(uris: List<Uri>) {
        val state = _uiState.value
        val remoteCount = state.remotePhotoUrls.size
        val availableSlots = (4 - remoteCount).coerceAtLeast(0)
        // Combinar con las ya-nuevas elegidas y respetar el límite de slots disponibles
        val combined = (state.newPhotoUris + uris)
            .distinctBy { it.toString() }
            .take(availableSlots)
        _uiState.update { it.copy(newPhotoUris = combined) }
    }

    /**
     * El usuario quita una foto de la preview.
     * → Si es una foto nueva (URI local): se elimina de newPhotoUris.
     * → Si es una foto remota (URL): se elimina de remotePhotoUrls.
     *   Al guardar, ya no estará en oldPhotoUrls → el repositorio la borrará de Cloudinary si es necesario.
     */
    fun onPhotoRemoved(uri: Uri) {
        val uriStr = uri.toString()
        // ¿Es una URI local nueva?
        val removedFromNew = _uiState.value.newPhotoUris.filter { it.toString() != uriStr }
        if (removedFromNew.size < _uiState.value.newPhotoUris.size) {
            _uiState.update { it.copy(newPhotoUris = removedFromNew) }
            return
        }
        // ¿Es una URL remota existente?
        val removedFromRemote = _uiState.value.remotePhotoUrls.filter { it != uriStr }
        _uiState.update { it.copy(remotePhotoUrls = removedFromRemote) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cambios de campos de texto
    // ─────────────────────────────────────────────────────────────────────────

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v, error = null) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onCategoryChange(v: String) = _uiState.update { it.copy(category = v) }
    fun onLocationNameChange(v: String) = _uiState.update { it.copy(locationName = v) }
    fun onOfferTypeChange(v: OfferType) = _uiState.update { it.copy(offerType = v) }
    fun onWebsiteNameChange(v: String) = _uiState.update { it.copy(websiteName = v) }
    fun onProductLinkChange(v: String) = _uiState.update { it.copy(productLink = v) }
    fun onStoreNameChange(v: String) = _uiState.update { it.copy(storeName = v) }
    fun onNormalPriceChange(v: String) = _uiState.update { it.copy(normalPriceInput = v) }
    fun onDiscountPriceChange(v: String) = _uiState.update { it.copy(discountPriceInput = v) }
    fun onCurrencyChange(v: String) = _uiState.update { it.copy(currency = v) }
    fun onHasCouponChange(v: Boolean) = _uiState.update { it.copy(hasCoupon = v) }
    fun onCouponCodeChange(v: String) = _uiState.update { it.copy(couponCode = v) }
    fun onIsAvailableChange(v: Boolean) = _uiState.update { it.copy(isAvailable = v) }
    fun onConditionChange(v: String) = _uiState.update { it.copy(condition = v) }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val location = locationService.getCurrentLocation()
            if (location != null) {
                _uiState.update { it.copy(locationName = "Ubicación detectada", isLoading = false) }
            } else {
                _uiState.update { it.copy(error = "No se pudo obtener la ubicación GPS.", isLoading = false) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Guardar cambios
    //
    // Lógica de detección de fotos:
    //  · Si newPhotoUris NO está vacío → el usuario eligió fotos nuevas.
    //    Los paths se pasan al repositorio que borrará las anteriores y subirá las nuevas.
    //  · Si newPhotoUris está vacío → se conservan remotePhotoUrls (las existentes).
    // ─────────────────────────────────────────────────────────────────────────

    fun submitChanges() {
        val state = _uiState.value
        val postId = state.postId.ifBlank { return }

        // Validación básica
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "El título es obligatorio") }
            return
        }
        if (state.description.isBlank()) {
            _uiState.update { it.copy(error = "La descripción es obligatoria") }
            return
        }
        val hasPhotos = state.remotePhotoUrls.isNotEmpty() || state.newPhotoUris.isNotEmpty()
        if (!hasPhotos) {
            _uiState.update { it.copy(error = "Debes tener al menos 1 imagen") }
            return
        }

        val normalPrice = state.normalPriceInput.toDoubleOrNull()
        val discountPrice = state.discountPriceInput.toDoubleOrNull()
        var discountPercentage: Int? = null
        if (normalPrice != null && discountPrice != null && normalPrice > 0) {
            discountPercentage = (((normalPrice - discountPrice) / normalPrice) * 100).toInt()
        }
        val coupon = if (state.offerType == OfferType.ONLINE && state.hasCoupon) {
            state.couponCode.takeIf { it.isNotBlank() }
        } else null

        // Convertir las nuevas URIs a String (el repositorio se encarga de procesarlas)
        val newUris = state.newPhotoUris.map { it.toString() }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = feedRepository.updatePost(
                postId = postId,
                title = state.title,
                descriptionLong = state.description,
                category = state.category,
                normalPrice = normalPrice,
                discountPrice = discountPrice,
                currency = state.currency,
                couponCode = coupon,
                discountPercentage = discountPercentage,
                isAvailable = state.isAvailable,
                condition = state.condition,
                websiteName = state.websiteName.takeIf { it.isNotBlank() },
                productLink = state.productLink.takeIf { it.isNotBlank() },
                storeName = state.storeName.takeIf { it.isNotBlank() },
                locationName = state.locationName,
                oldPhotoUrls = state.remotePhotoUrls,
                newPhotoUris = newUris
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(isFinished = true, isLoading = false) }
            } else {
                _uiState.update { it.copy(
                    error = result.exceptionOrNull()?.message ?: "Error desconocido",
                    isLoading = false
                )}
            }
        }
    }
}
