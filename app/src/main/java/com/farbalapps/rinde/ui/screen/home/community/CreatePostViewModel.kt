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
        
        val validationError = validateInput(state)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val normalPriceDouble = state.normalPriceInput.toDoubleOrNull()
            val discountPriceDouble = state.discountPriceInput.toDoubleOrNull()
            val priceError = validatePrices(state.normalPriceInput, normalPriceDouble, state.discountPriceInput, discountPriceDouble)

            if (priceError != null) {
                _uiState.update { it.copy(error = priceError, isLoading = false) }
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

    private fun validateInput(state: CreatePostUiState): String? {
        if (state.isPrivateProfile) return "Tu perfil es privado. Cambia a público para publicar ofertas."
        if (state.photoUris.isEmpty()) return "Debes agregar al menos 1 imagen (máximo 4)"
        if (state.title.isBlank()) return "El título es obligatorio"
        if (state.title.length < 10) return "El título debe tener al menos 10 caracteres"
        if (state.description.isBlank()) return "La descripción es obligatoria"
        if (state.category.isBlank()) return "Debes seleccionar una categoría"
        if (state.offerType == com.farbalapps.rinde.domain.model.OfferType.UNSPECIFIED) return "Debes indicar si la oferta es online o física"
        
        if (state.offerType == com.farbalapps.rinde.domain.model.OfferType.ONLINE) {
            if (state.websiteName.isBlank()) return "La página web es obligatoria para ofertas online"
            if (state.productLink.isBlank()) return "El link del producto es obligatorio"
            if (state.productLinkError != null) return "Corrige los errores antes de publicar"
        }
        if (state.offerType == com.farbalapps.rinde.domain.model.OfferType.PHYSICAL) {
            if (state.storeName.isBlank()) return "El nombre de la tienda es obligatorio"
            if (state.locationName.isBlank()) return "La ubicación es obligatoria para ofertas físicas"
        }
        if (state.priceError != null) return "El precio con descuento no puede ser mayor al precio normal"
        
        return null
    }

    private fun validatePrices(normalInput: String, normalDouble: Double?, discountInput: String, discountDouble: Double?): String? {
        if (normalInput.isNotBlank() && normalDouble == null) return "El precio normal no es válido"
        if (discountInput.isNotBlank() && discountDouble == null) return "El precio con descuento no es válido"
        if (normalDouble != null && discountDouble != null && discountDouble > normalDouble) {
            return "El precio con descuento no puede superar el precio normal"
        }
        return null
    }

}
