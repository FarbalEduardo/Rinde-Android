package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, password: String): Flow<Resource<User>>
    fun logout()
    fun getCurrentUser(): User?
    fun isUserLoggedIn(): Boolean
}
