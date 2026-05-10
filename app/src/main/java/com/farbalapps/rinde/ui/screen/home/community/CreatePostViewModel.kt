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
    val category: String = "Otros",
    val locationName: String = "",
    val photoUris: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFinished: Boolean = false
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createPostUseCase: CreatePostUseCase,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

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
        _uiState.update { it.copy(photoUris = uris.take(4)) }
    }

    /**
     * Tries to fetch the current location name using GPS.
     */
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val location = locationService.getCurrentLocation()
            if (location != null) {
                // In a real app we would use Geocoder here
                _uiState.update { it.copy(locationName = "Ubicación detectada", isLoading = false) }
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
        
        // Basic UI-side validation
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "El título es obligatorio") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = createPostUseCase(
                title = state.title,
                description = state.description,
                category = state.category,
                locationName = state.locationName,
                photoUris = state.photoUris
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
