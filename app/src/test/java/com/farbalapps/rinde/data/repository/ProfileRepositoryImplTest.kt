package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.repository.delegate.ProfileCrudDelegate
import com.farbalapps.rinde.data.repository.delegate.ProfilePrivacyDelegate
import com.farbalapps.rinde.data.repository.delegate.ProfileSocialDelegate
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.Profile
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileRepositoryImplTest {

    private lateinit var crudDelegate: ProfileCrudDelegate
    private lateinit var privacyDelegate: ProfilePrivacyDelegate
    private lateinit var socialDelegate: ProfileSocialDelegate
    private lateinit var repository: ProfileRepositoryImpl

    private val userId = "user123"

    @Before
    fun setUp() {
        crudDelegate = mockk()
        privacyDelegate = mockk()
        socialDelegate = mockk()
        repository = ProfileRepositoryImpl(crudDelegate, privacyDelegate, socialDelegate)
    }

    @Test
    fun getProfile_delegatesToCrudDelegate() = runBlocking {
        val expectedProfile = Profile(id = userId, name = "Eduardo")
        every { crudDelegate.getProfile(userId) } returns flowOf(expectedProfile)

        val result = repository.getProfile(userId).toList()

        assertEquals(1, result.size)
        assertEquals(expectedProfile, result.first())
        verify(exactly = 1) { crudDelegate.getProfile(userId) }
    }

    @Test
    fun syncProfile_delegatesToCrudDelegate() = runBlocking {
        coEvery { crudDelegate.syncProfile(userId) } just Runs

        repository.syncProfile(userId)

        coVerify(exactly = 1) { crudDelegate.syncProfile(userId) }
    }

    @Test
    fun updateProfile_delegatesToCrudDelegate() = runBlocking {
        val name = "Nuevo Nombre"
        val photoUrl = "https://res.cloudinary.com/demo/image/upload/v1234/profile.jpg"
        coEvery { crudDelegate.updateProfile(userId, name, photoUrl) } returns Result.success(Unit)

        val result = repository.updateProfile(userId, name, photoUrl)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { crudDelegate.updateProfile(userId, name, photoUrl) }
    }

    @Test
    fun updateProfile_withNullPhotoUrl_removesPhoto() = runBlocking {
        val name = "Eduardo"
        val photoUrl = null
        coEvery { crudDelegate.updateProfile(userId, name, null) } returns Result.success(Unit)

        val result = repository.updateProfile(userId, name, null)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { crudDelegate.updateProfile(userId, name, null) }
    }

    @Test
    fun getProfilePosts_delegatesToSocialDelegate() = runBlocking {
        val dummyPost = mockk<CommunityPost>()
        every { socialDelegate.getProfilePosts(userId) } returns flowOf(listOf(dummyPost))

        val result = repository.getProfilePosts(userId).toList()

        assertEquals(1, result.size)
        assertEquals(dummyPost, result.first().first())
        verify(exactly = 1) { socialDelegate.getProfilePosts(userId) }
    }

    @Test
    fun updatePrivacy_delegatesToPrivacyDelegate() = runBlocking {
        coEvery { privacyDelegate.updatePrivacy(userId, true) } returns Result.success(Unit)

        val result = repository.updatePrivacy(userId, true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { privacyDelegate.updatePrivacy(userId, true) }
    }
}
