package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.UserRepository
import com.farbalapps.rinde.domain.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun saveUserProfile(user: User): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            // Escribe todos los campos compatibles con Profile para que syncProfile pueda leerlos.
            // Usa SetOptions.merge() para no sobreescribir datos existentes (ej: followersCount).
            val profileData = mapOf(
                "id" to user.id,
                "email" to user.email,
                "name" to (user.displayName ?: "Usuario"), // Default name fallback
                "displayName" to (user.displayName ?: "Usuario"),
                "photoUrl" to user.photoUrl,
                "followersCount" to 0,
                "followingCount" to 0,
                "postsCount" to 0,
                "rating" to 0.0,
                "reviewsCount" to 0,
                "isPrivate" to false,
                "isDummy" to false
            ).filterValues { it != null } // no escribir nulls
            firestore.collection("users").document(user.id).set(profileData, SetOptions.merge()).await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to save profile"))
        }
    }

    override fun getUserProfile(userId: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val document = firestore.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java)
            if (user != null) {
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("User profile not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to fetch profile"))
        }
    }
}
