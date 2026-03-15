package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.UserRepository
import com.farbalapps.rinde.domain.util.Resource
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class GoogleSignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    fun execute(idToken: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        
        authRepository.signInWithGoogle(idToken).collect { authResult ->
            when (authResult) {
                is Resource.Success -> {
                    val user = authResult.data
                    if (user != null) {
                        // Guardamos el perfil en Firestore tras el login exitoso
                        userRepository.saveUserProfile(user).collect { profileResult ->
                            when (profileResult) {
                                is Resource.Success -> emit(Resource.Success(user))
                                is Resource.Error -> emit(Resource.Error(profileResult.message ?: "Auth success but profile save failed"))
                                is Resource.Loading -> { /* Handled */ }
                            }
                        }
                    } else {
                        emit(Resource.Error("User data lost during Google Sign In"))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(authResult.message ?: "Google Sign In failed"))
                }
                is Resource.Loading -> { /* Handled by initial emit */ }
            }
        }
    }
}
