package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.CommunityPost
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfile(userId: String): Flow<Profile>
    fun getProfilePosts(userId: String): Flow<List<CommunityPost>>
    suspend fun syncProfile(userId: String)
    suspend fun followUser(myUserId: String, targetUserId: String): Result<Unit>
    suspend fun unfollowUser(myUserId: String, targetUserId: String): Result<Unit>
    fun isFollowing(myUserId: String, targetUserId: String): Flow<Boolean>
    suspend fun updateProfile(userId: String, name: String, photoUrl: String?): Result<Unit>
    
    // New Feature Methods
    suspend fun updatePrivacy(userId: String, isPrivate: Boolean): Result<Unit>
    suspend fun toggleSavePost(userId: String, postId: String, save: Boolean): Result<Unit>
    suspend fun blockUser(userId: String, targetUserId: String): Result<Unit>
    suspend fun unblockUser(userId: String, targetUserId: String): Result<Unit>
    suspend fun clearUploadStatus(userId: String): Result<Unit>
    fun getSavedProfilePosts(userId: String): Flow<List<CommunityPost>>
    fun getBlockedUsers(userId: String): Flow<List<Profile>>
}
