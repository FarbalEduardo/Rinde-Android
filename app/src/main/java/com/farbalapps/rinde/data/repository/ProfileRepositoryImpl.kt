package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.repository.delegate.ProfileCrudDelegate
import com.farbalapps.rinde.data.repository.delegate.ProfilePrivacyDelegate
import com.farbalapps.rinde.data.repository.delegate.ProfileSocialDelegate
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val crudDelegate: ProfileCrudDelegate,
    private val privacyDelegate: ProfilePrivacyDelegate,
    private val socialDelegate: ProfileSocialDelegate
) : ProfileRepository {

    override fun getProfile(userId: String): Flow<Profile> =
        crudDelegate.getProfile(userId)

    override suspend fun syncProfile(userId: String) =
        crudDelegate.syncProfile(userId)

    override suspend fun updateProfile(userId: String, name: String, photoUrl: String?): Result<Unit> =
        crudDelegate.updateProfile(userId, name, photoUrl)

    override suspend fun clearUploadStatus(userId: String): Result<Unit> =
        crudDelegate.clearUploadStatus(userId)

    override fun getProfilePosts(userId: String): Flow<List<CommunityPost>> =
        socialDelegate.getProfilePosts(userId)

    override suspend fun toggleSavePost(userId: String, postId: String, save: Boolean): Result<Unit> =
        socialDelegate.toggleSavePost(userId, postId, save)

    override suspend fun blockUser(userId: String, targetUserId: String): Result<Unit> =
        socialDelegate.blockUser(userId, targetUserId)

    override suspend fun unblockUser(userId: String, targetUserId: String): Result<Unit> =
        socialDelegate.unblockUser(userId, targetUserId)

    override fun getSavedProfilePosts(userId: String): Flow<List<CommunityPost>> =
        socialDelegate.getSavedProfilePosts(userId)

    override fun getBlockedUsers(userId: String): Flow<List<Profile>> =
        socialDelegate.getBlockedUsers(userId)

    override suspend fun updatePrivacy(userId: String, isPrivate: Boolean): Result<Unit> =
        privacyDelegate.updatePrivacy(userId, isPrivate)

    override suspend fun updateInterests(userId: String, interests: List<String>): Result<Unit> =
        privacyDelegate.updateInterests(userId, interests)

    override suspend fun updateZonasDeCaza(userId: String, zonas: List<String>): Result<Unit> =
        privacyDelegate.updateZonasDeCaza(userId, zonas)
}
