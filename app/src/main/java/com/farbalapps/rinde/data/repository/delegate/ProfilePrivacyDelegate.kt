package com.farbalapps.rinde.data.repository.delegate

import com.farbalapps.rinde.data.local.dao.ProfileDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProfilePrivacyDelegate @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileDao: ProfileDao,
    private val crudDelegate: ProfileCrudDelegate
) {
    suspend fun updatePrivacy(userId: String, isPrivate: Boolean): Result<Unit> = runCatching {
        val current = profileDao.getProfile(userId).firstOrNull()
        current?.let {
            profileDao.insertProfile(it.copy(isPrivate = isPrivate))
        }

        firestore.collection("users").document(userId)
            .set(mapOf("isPrivate" to isPrivate), SetOptions.merge())
            .await()
    }

    suspend fun updateInterests(userId: String, interests: List<String>): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .set(mapOf("interests" to interests), SetOptions.merge())
            .await()
        crudDelegate.syncProfile(userId)
    }

    suspend fun updateZonasDeCaza(userId: String, zonas: List<String>): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .set(mapOf("zonasDeCaza" to zonas), SetOptions.merge())
            .await()
        crudDelegate.syncProfile(userId)
    }
}
