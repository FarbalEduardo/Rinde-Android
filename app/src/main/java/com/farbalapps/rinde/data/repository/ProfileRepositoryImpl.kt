package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.local.dao.ProfileDao
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.ProfilePost
import com.farbalapps.rinde.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao
) : ProfileRepository {

    override fun getProfile(userId: String): Flow<Profile> {
        return profileDao.getProfile(userId).map { entity ->
            entity?.toDomainModel() ?: Profile(
                id = userId,
                name = "Cargando perfil...",
                isDummy = true
            )
        }
    }

    override fun getProfilePosts(userId: String): Flow<List<ProfilePost>> {
        // Dummy data equivalent to the one in CommunityScreen for start
        return flow {
            emit(listOf(
                ProfilePost(
                    id = "post_1",
                    title = "Frutillas 2 x 1 en Jumbo.\n¡Están muy frescas!",
                    description = "Solo hoy hasta agotar stock...",
                    timeLocation = "Hace 15 min • Jumbo Providencia",
                    isRecommended = true,
                    votes = 42,
                    likes = 128,
                    commentsCount = 12
                ),
                ProfilePost(
                    id = "post_2",
                    title = "Detergente Omo 3L a precio de 1L",
                    description = "Es un error de etiqueta pero está pasando por caja a $4.990.",
                    timeLocation = "Hace 1 hora • Líder Express",
                    isRecommended = false,
                    votes = 12,
                    likes = 89,
                    commentsCount = 4
                )
            ))
        }
    }

    override suspend fun syncProfile(userId: String) {
        // TODO: Sync with Firebase Firestore and insert into ProfileDao
        // Example: 
        // val snapshot = firestore.collection("users").document(userId).get().await()
        // profileDao.insertProfile(...)
    }
}
