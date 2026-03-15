package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.UserRepository
import com.farbalapps.rinde.domain.util.Resource
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword
) {
    fun execute(fullName: String, email: String, password: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())

        val emailResult = validateEmail.execute(email)
        val passwordResult = validatePassword.execute(password)

        if (!emailResult.successful || !passwordResult.successful) {
            emit(Resource.Error(emailResult.errorMessage ?: passwordResult.errorMessage ?: "Validation failed"))
            return@flow
        }

        authRepository.signUp(email, password).collect { result ->
            when (result) {
                is Resource.Success -> {
                    val user = result.data?.copy(displayName = fullName)
                    if (user != null) {
                        userRepository.saveUserProfile(user).collect { profileResult ->
                            when (profileResult) {
                                is Resource.Success -> emit(Resource.Success(true))
                                is Resource.Error -> emit(Resource.Error(profileResult.message ?: "Failed to save profile"))
                                is Resource.Loading -> { /* Handled by initial emit */ }
                            }
                        }
                    } else {
                        emit(Resource.Error("User data lost during signup"))
                    }
                }
                is Resource.Error -> emit(Resource.Error(result.message ?: "Signup failed"))
                is Resource.Loading -> { /* Handled */ }
            }
        }
    }
}
