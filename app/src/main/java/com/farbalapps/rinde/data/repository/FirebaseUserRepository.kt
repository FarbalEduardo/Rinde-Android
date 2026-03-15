package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.UserRepository
import com.farbalapps.rinde.domain.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun saveUserProfile(user: User): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading())

        firestore.collection("users").document(user.id).set(user)
            .addOnSuccessListener {
                trySend(Resource.Success(true))
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.localizedMessage ?: "Failed to save profile"))
            }

        awaitClose { }
    }

    override fun getUserProfile(userId: String): Flow<Resource<User>> = callbackFlow {
        trySend(Resource.Loading())

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if (user != null) {
                    trySend(Resource.Success(user))
                } else {
                    trySend(Resource.Error("User profile not found"))
                }
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.localizedMessage ?: "Failed to fetch profile"))
            }

        awaitClose { }
    }
}
