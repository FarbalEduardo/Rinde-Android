package com.farbalapps.rinde.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.data.local.SessionManager
import com.farbalapps.rinde.domain.usecase.LoginUseCase
import com.farbalapps.rinde.domain.usecase.ValidateEmail
import com.farbalapps.rinde.domain.usecase.ValidatePassword
import com.farbalapps.rinde.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.farbalapps.rinde.domain.usecase.account.ReactivateAccountUseCase

data class LoginUIState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val isSuccess: Boolean = false,
    val wasAttempted: Boolean = false,
    val showReactivateDialog: Boolean = false,
    val pendingUserId: String = "",
    val pendingUserEmail: String = "",
    val reactivatedSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: com.farbalapps.rinde.domain.usecase.SignUpUseCase,
    private val googleSignInUseCase: com.farbalapps.rinde.domain.usecase.GoogleSignInUseCase,
    private val resetPasswordUseCase: com.farbalapps.rinde.domain.usecase.ResetPasswordUseCase,
    private val reactivateAccountUseCase: ReactivateAccountUseCase,
    private val authRepository: com.farbalapps.rinde.domain.repository.AuthRepository,
    private val sessionManager: SessionManager,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUIState())
    val state: StateFlow<LoginUIState> = _state.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<Resource<Unit>?>(null)
    val resetPasswordState = _resetPasswordState.asStateFlow()

    fun resetPassword(email: String) {
        viewModelScope.launch {
            resetPasswordUseCase.execute(email).collect { result ->
                _resetPasswordState.value = result
            }
        }
    }

    fun clearResetPasswordState() {
        _resetPasswordState.value = null
    }

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, passwordError = null) }
    }

    fun onLoginClick() {
        _state.update { it.copy(wasAttempted = true) }
        val emailResult = validateEmail.execute(_state.value.email)
        val passwordResult = validatePassword.execute(_state.value.password)

        if (!emailResult.successful || !passwordResult.successful) {
            _state.update { 
                it.copy(
                    emailError = emailResult.errorMessage,
                    passwordError = passwordResult.errorMessage
                )
            }
            return
        }

        viewModelScope.launch {
            loginUseCase.execute(_state.value.email, _state.value.password).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true, loginError = null) }
                    }
                    is Resource.Success -> {
                        val user = result.data
                        if (user != null) {
                            handlePostLoginCheck(user)
                        }
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(isLoading = false, loginError = result.message) }
                    }
                }
            }
        }
    }

    fun onGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            googleSignInUseCase.execute(idToken).collect { result ->
                handleSocialResult(result)
            }
        }
    }

    private fun handleSocialResult(result: Resource<com.farbalapps.rinde.domain.model.User>) {
        when (result) {
            is Resource.Loading -> {
                _state.update { it.copy(isLoading = true, loginError = null) }
            }
            is Resource.Success -> {
                val user = result.data
                if (user != null) {
                    viewModelScope.launch {
                        handlePostLoginCheck(user)
                    }
                }
            }
            is Resource.Error -> {
                _state.update { it.copy(isLoading = false, loginError = result.message) }
            }
        }
    }

    private suspend fun handlePostLoginCheck(user: com.farbalapps.rinde.domain.model.User) {
        val isSuspended = reactivateAccountUseCase.isSuspended(user.id).getOrDefault(false)
        if (isSuspended) {
            _state.update {
                it.copy(
                    isLoading = false,
                    showReactivateDialog = true,
                    pendingUserId = user.id,
                    pendingUserEmail = user.email
                )
            }
        } else {
            sessionManager.saveSession(user.id, user.email)
            _state.update { it.copy(isLoading = false, isSuccess = true) }
        }
    }

    fun confirmReactivation() {
        val uid = _state.value.pendingUserId
        val email = _state.value.pendingUserEmail
        if (uid.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = reactivateAccountUseCase(uid)
            if (result.isSuccess) {
                sessionManager.saveSession(uid, email)
                _state.update {
                    it.copy(
                        isLoading = false,
                        showReactivateDialog = false,
                        isSuccess = true,
                        reactivatedSuccess = true
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        loginError = result.exceptionOrNull()?.localizedMessage ?: "Error al reactivar cuenta"
                    )
                }
            }
        }
    }

    fun dismissReactivation() {
        authRepository.logout()
        _state.update {
            it.copy(
                showReactivateDialog = false,
                pendingUserId = "",
                pendingUserEmail = "",
                isLoading = false
            )
        }
    }
}
