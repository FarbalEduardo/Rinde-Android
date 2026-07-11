package com.farbalapps.rinde.ui.screen.home.community

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.usecase.CreatePostUseCase
import com.farbalapps.rinde.util.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for CreatePostScreen.
 * Represents all possible states of the screen according to Mobile Development standards.
 */
data class CreatePostUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val locationName: String = "",
    val photoUris: List<Uri> = emptyList(),
    // New v3 fields
    val offerType: com.farbalapps.rinde.domain.model.OfferType = com.farbalapps.rinde.domain.model.OfferType.ONLINE,
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
    val isPrivateProfile: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFinished: Boolean = false,
    val priceError: String? = null,
    val productLinkError: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createPostUseCase: CreatePostUseCase,
    private val getProfileUseCase: com.farbalapps.rinde.domain.usecase.profile.GetProfileUseCase,
    private val locationService: LocationService,
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    init {
        checkUserPrivacy()
    }

    private fun checkUserPrivacy() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            getProfileUseCase(uid).collect { profile ->
                _uiState.update { it.copy(isPrivateProfile = profile.isPrivate) }
            }
        }
    }


    fun onOfferTypeChange(newType: com.farbalapps.rinde.domain.model.OfferType) {
        _uiState.update { it.copy(offerType = newType) }
    }

    fun onWebsiteNameChange(newName: String) {
        _uiState.update { it.copy(websiteName = newName) }
    }

    fun onProductLinkChange(newLink: String) {
        val error = if (newLink.isNotBlank() && !isValidUrl(newLink)) {
            "Ingresa una URL válida (ej: https://...)"
        } else null
        _uiState.update { it.copy(productLink = newLink, productLinkError = error) }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.scheme in listOf("http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }

    fun onStoreNameChange(newName: String) {
        _uiState.update { it.copy(storeName = newName) }
    }

    fun onNormalPriceChange(price: String) {
        val normal = price.toDoubleOrNull()
        val discount = _uiState.value.discountPriceInput.toDoubleOrNull()
        val error = if (normal != null && discount != null && discount > normal) {
            "El precio con descuento no puede ser mayor al precio normal"
        } else null
        _uiState.update { it.copy(normalPriceInput = price, priceError = error) }
    }

    fun onDiscountPriceChange(price: String) {
        val discount = price.toDoubleOrNull()
        val normal = _uiState.value.normalPriceInput.toDoubleOrNull()
        val error = if (normal != null && discount != null && discount > normal) {
            "El precio con descuento no puede ser mayor al precio normal"
        } else null
        _uiState.update { it.copy(discountPriceInput = price, priceError = error) }
    }

    fun onCurrencyChange(newCurrency: String) {
        _uiState.update { it.copy(currency = newCurrency) }
    }

    fun onHasCouponChange(has: Boolean) {
        _uiState.update { it.copy(hasCoupon = has) }
    }

    fun onCouponCodeChange(code: String) {
        _uiState.update { it.copy(couponCode = code) }
    }

    fun onIsAvailableChange(isAvailable: Boolean) {
        _uiState.update { it.copy(isAvailable = isAvailable) }
    }

    fun onConditionChange(condition: String) {
        _uiState.update { it.copy(condition = condition) }
    }


    /**
     * Updates the title of the post.
     */
    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, error = null) }
    }

    /**
     * Updates the description of the post.
     */
    fun onDescriptionChange(newDesc: String) {
        _uiState.update { it.copy(description = newDesc, error = null) }
    }

    /**
     * Updates the category.
     */
    fun onCategoryChange(newCat: String) {
        _uiState.update { it.copy(category = newCat) }
    }

    /**
     * Updates the location name.
     */
    fun onLocationNameChange(newName: String) {
        _uiState.update { it.copy(locationName = newName) }
    }

    /**
     * Updates the photo list, limited to 4.
     */
    fun onPhotosSelected(uris: List<Uri>) {
        val current = _uiState.value.photoUris
        val remaining = 4 - current.size
        val newUris = (current + uris).distinctBy { it.toString() }.take(4)
        _uiState.update { it.copy(photoUris = newUris) }
    }

    /**
     * Removes a specific photo from the selection.
     */
    fun onPhotoRemoved(uri: Uri) {
        _uiState.update { it.copy(photoUris = it.photoUris.filter { u -> u != uri }) }
    }

    /**
     * Tries to fetch the current location name using GPS.
     */
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val location = locationService.getCurrentLocation()
            if (location != null) {
                val address = locationService.getAddressFromLocation(location.latitude, location.longitude)
                val finalName = address ?: "Ubicación detectada (${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})"
                _uiState.update { it.copy(
                    locationName = finalName,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    isLoading = false
                ) }
            } else {
                _uiState.update { it.copy(
                    error = "No se pudo obtener la ubicación GPS.",
                    isLoading = false
                )}
            }
        }
    }

    /**
     * Orchestrates the post creation flow using the CreatePostUseCase.
     */
    fun submitPost() {
        val state = _uiState.value
        
        // 1. Privacy Restriction
        if (state.isPrivateProfile) {
            _uiState.update { it.copy(error = "Tu perfil es privado. Cambia a público para publicar ofertas.") }
            return
        }

        // 2. Full UI-side validation
        if (state.photoUris.isEmpty()) {
            _uiState.update { it.copy(error = "Debes agregar al menos 1 imagen (máximo 4)") }
            return
        }
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "El título es obligatorio") }
            return
        }
        if (state.title.length < 10) {
            _uiState.update { it.copy(error = "El título debe tener al menos 10 caracteres") }
            return
        }
        if (state.description.isBlank()) {
            _uiState.update { it.copy(error = "La descripción es obligatoria") }
            return
        }
        if (state.category.isBlank()) {
            _uiState.update { it.copy(error = "Debes seleccionar una categoría") }
            return
        }
        if (state.offerType == com.farbalapps.rinde.domain.model.OfferType.UNSPECIFIED) {
            _uiState.update { it.copy(error = "Debes indicar si la oferta es online o física") }
            return
        }
        if (state.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE) {
            if (state.websiteName.isBlank()) {
                _uiState.update { it.copy(error = "La página web es obligatoria para ofertas online") }
                return
            }
            if (state.productLink.isBlank()) {
                _uiState.update { it.copy(error = "El link del producto es obligatorio") }
                return
            }
            if (state.productLinkError != null) {
                _uiState.update { it.copy(error = "Corrige los errores antes de publicar") }
                return
            }
        }
        if (state.offerType == com.farbalapps.rinde.domain.model.OfferType.PHYSICAL) {
            if (state.storeName.isBlank()) {
                _uiState.update { it.copy(error = "El nombre de la tienda es obligatorio") }
                return
            }
            if (state.locationName.isBlank()) {
                _uiState.update { it.copy(error = "La ubicación es obligatoria para ofertas físicas") }
                return
            }
        }
        if (state.priceError != null) {
            _uiState.update { it.copy(error = "El precio con descuento no puede ser mayor al precio normal") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val normalPriceDouble = state.normalPriceInput.toDoubleOrNull()
            val discountPriceDouble = state.discountPriceInput.toDoubleOrNull()

            if (state.normalPriceInput.isNotBlank() && normalPriceDouble == null) {
                _uiState.update { it.copy(error = "El precio normal no es válido", isLoading = false) }
                return@launch
            }
            if (state.discountPriceInput.isNotBlank() && discountPriceDouble == null) {
                _uiState.update { it.copy(error = "El precio con descuento no es válido", isLoading = false) }
                return@launch
            }
            if (normalPriceDouble != null && discountPriceDouble != null && discountPriceDouble > normalPriceDouble) {
                _uiState.update { it.copy(error = "El precio con descuento no puede superar el precio normal", isLoading = false) }
                return@launch
            }

            var discountPercentage: Int? = null
            if (normalPriceDouble != null && discountPriceDouble != null && normalPriceDouble > 0) {
                discountPercentage = (((normalPriceDouble - discountPriceDouble) / normalPriceDouble) * 100).toInt()
            }

            val finalCoupon = if (state.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE && state.hasCoupon) {
                state.couponCode.takeIf { it.isNotBlank() }
            } else {
                null
            }

            val result = createPostUseCase(
                title = state.title,
                description = state.description,
                category = state.category,
                locationName = state.locationName,
                photoUris = state.photoUris,
                offerType = state.offerType,
                websiteName = state.websiteName.takeIf { it.isNotBlank() },
                productLink = state.productLink.takeIf { it.isNotBlank() },
                storeName = state.storeName.takeIf { it.isNotBlank() },
                normalPrice = normalPriceDouble,
                discountPrice = discountPriceDouble,
                currency = state.currency,
                couponCode = finalCoupon,
                discountPercentage = discountPercentage,
                isAvailable = state.isAvailable,
                condition = state.condition,
                latitude = state.latitude,
                longitude = state.longitude
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
