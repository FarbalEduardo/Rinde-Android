package com.farbalapps.rinde.ui.screen.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.usecase.SignUpUseCase
import com.farbalapps.rinde.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUIState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val googleSignInUseCase: com.farbalapps.rinde.domain.usecase.GoogleSignInUseCase,
    private val sessionManager: com.farbalapps.rinde.data.local.SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUIState())
    val state = _state.asStateFlow()

    fun onGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            googleSignInUseCase.execute(idToken).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val user = result.data
                        if (user != null) {
                            sessionManager.saveSession(user.id, user.email)
                            _state.update { it.copy(isLoading = false, isSuccess = true) }
                        }
                    }
                    is Resource.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                }
            }
        }
    }

    fun signUp(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            signUpUseCase.execute(fullName, email, password).collect { result ->
                when (result) {
                    is Resource.Success -> _state.update { it.copy(isLoading = false, isSuccess = true) }
                    is Resource.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
