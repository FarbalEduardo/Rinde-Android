package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.ProfilePost
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfile(userId: String): Flow<Profile>
    fun getProfilePosts(userId: String): Flow<List<ProfilePost>>
    suspend fun syncProfile(userId: String)
}
