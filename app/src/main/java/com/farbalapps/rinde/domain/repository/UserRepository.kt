package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun saveUserProfile(user: User): Flow<Resource<Boolean>>
    fun getUserProfile(userId: String): Flow<Resource<User>>
}
