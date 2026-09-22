package com.farbalapps.rinde.ui.screen.profile.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.usecase.profile.GetProfileUseCase
import com.farbalapps.rinde.domain.usecase.profile.UpdateProfileUseCase
import com.farbalapps.rinde.domain.usecase.profile.UpdatePrivacyUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import com.farbalapps.rinde.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.farbalapps.rinde.domain.usecase.account.ChangePasswordUseCase
import com.farbalapps.rinde.domain.usecase.account.DeleteAccountUseCase
import com.farbalapps.rinde.domain.usecase.account.SuspendAccountUseCase

enum class EditProfileActiveDialog {
    NONE,
    CHANGE_PASSWORD,
    SOFT_OPTIONS,
    PERMANENT_DELETE
}

data class EditProfileUiState(
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isPrivate: Boolean = false,
    val isGoogleUser: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isAccountDeleted: Boolean = false,
    val isAccountSuspended: Boolean = false,
    val activeDialog: EditProfileActiveDialog = EditProfileActiveDialog.NONE,
    val isChangingPassword: Boolean = false,
    val changePasswordSuccess: Boolean = false,
    val changePasswordError: String? = null,
    val error: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val updatePrivacyUseCase: UpdatePrivacyUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val suspendAccountUseCase: SuspendAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        val user = firebaseAuth.currentUser ?: return
        val userId = user.uid
        val email = user.email ?: ""
        val isGoogle = user.providerData.any { it.providerId == "google.com" } &&
                user.providerData.none { it.providerId == "password" }

        _uiState.update { it.copy(email = email, isGoogleUser = isGoogle) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val profile = getProfileUseCase(userId).first()
                _uiState.update { 
                    it.copy(
                        name = profile.name,
                        photoUrl = profile.photoUrl,
                        isPrivate = profile.isPrivate,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onPhotoChange(newPhotoUri: String?) {
        _uiState.update { it.copy(photoUrl = newPhotoUri) }
    }

    fun togglePrivacy(isPrivate: Boolean) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPrivate = isPrivate) }
            val result = updatePrivacyUseCase(userId, isPrivate)
            if (result.isFailure) {
                _uiState.update { it.copy(isPrivate = !isPrivate, error = "Error al actualizar privacidad") }
            }
        }
    }

    fun saveProfile() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = updateProfileUseCase(
                userId = userId,
                name = _uiState.value.name,
                photoUrl = _uiState.value.photoUrl
            )
            
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    // --- Gestión de Diálogos ---

    fun openChangePasswordDialog() {
        _uiState.update {
            it.copy(
                activeDialog = EditProfileActiveDialog.CHANGE_PASSWORD,
                changePasswordError = null,
                changePasswordSuccess = false
            )
        }
    }

    fun openAccountManagementDialog() {
        _uiState.update { it.copy(activeDialog = EditProfileActiveDialog.SOFT_OPTIONS) }
    }

    fun proceedToPermanentDeleteDialog() {
        _uiState.update { it.copy(activeDialog = EditProfileActiveDialog.PERMANENT_DELETE) }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                activeDialog = EditProfileActiveDialog.NONE,
                changePasswordError = null
            )
        }
    }

    // --- Operaciones de Cuenta y Seguridad ---

    fun changePassword(currentPass: String, newPass: String, confirmPass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, changePasswordError = null) }
            val result = changePasswordUseCase(currentPass, newPass, confirmPass)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        changePasswordSuccess = true,
                        activeDialog = EditProfileActiveDialog.NONE
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        changePasswordError = result.exceptionOrNull()?.localizedMessage ?: "Error al cambiar contraseña"
                    )
                }
            }
        }
    }

    fun suspendAccount() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, activeDialog = EditProfileActiveDialog.NONE) }
            val result = suspendAccountUseCase(userId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isAccountSuspended = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.localizedMessage ?: "Error al suspender cuenta"
                    )
                }
            }
        }
    }

    fun deleteAccountPermanently() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, activeDialog = EditProfileActiveDialog.NONE) }
            val result = deleteAccountUseCase(userId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isAccountDeleted = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.localizedMessage ?: "Error al eliminar cuenta"
                    )
                }
            }
        }
    }

    fun deleteAccount() {
        deleteAccountPermanently()
    }
}
