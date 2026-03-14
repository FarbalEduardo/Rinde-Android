package com.farbalapps.rinde.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.data.local.SessionManager
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.usecase.LoginUseCase
import com.farbalapps.rinde.domain.usecase.ValidateEmail
import com.farbalapps.rinde.domain.usecase.ValidatePassword
import com.farbalapps.rinde.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUIState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val isSuccess: Boolean = false,
    val wasAttempted: Boolean = false
)


class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val sessionManager: SessionManager,
    private val validateEmail: ValidateEmail = ValidateEmail(),
    private val validatePassword: ValidatePassword = ValidatePassword()
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUIState())
    val state: StateFlow<LoginUIState> = _state.asStateFlow()

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
                            sessionManager.saveSession(user.id, user.email)
                            _state.update { it.copy(isLoading = false, isSuccess = true) }
                        }
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(isLoading = false, loginError = result.message) }
                    }
                }
            }
        }
    }
}
