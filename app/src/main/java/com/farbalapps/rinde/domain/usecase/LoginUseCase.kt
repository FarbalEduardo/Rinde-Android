package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword
) {
    fun execute(email: String, password: String): Flow<Resource<User>> {
        val emailResult = validateEmail.execute(email)
        val passwordResult = validatePassword.execute(password)
        
        val hasError = listOf(emailResult, passwordResult).any { !it.successful }
        
        if (hasError) {
            val combinedError = emailResult.errorMessage ?: passwordResult.errorMessage ?: "Unknown error"
            return flow { emit(Resource.Error(combinedError)) }
        }
        
        return repository.login(email, password)
    }
}
