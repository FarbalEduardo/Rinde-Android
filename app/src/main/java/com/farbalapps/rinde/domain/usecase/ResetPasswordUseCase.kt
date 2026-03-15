package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val validateEmail: ValidateEmail
) {
    fun execute(email: String): Flow<Resource<Unit>> = flow {
        val emailResult = validateEmail.execute(email)
        if (!emailResult.successful) {
            emit(Resource.Error(emailResult.errorMessage ?: "Invalid email"))
            return@flow
        }
        
        repository.sendPasswordResetEmail(email).collect { result ->
            emit(result)
        }
    }
}
