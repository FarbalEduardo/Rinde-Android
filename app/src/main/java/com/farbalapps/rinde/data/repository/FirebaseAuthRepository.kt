package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.util.Resource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {
    
    override fun login(email: String, password: String): Flow<Resource<User>> = callbackFlow {
        trySend(Resource.Loading())
        
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val fbUser = result.user
                if (fbUser != null) {
                    val user = User(
                        id = fbUser.uid,
                        email = fbUser.email ?: "",
                        displayName = fbUser.displayName
                    )
                    trySend(Resource.Success(user))
                } else {
                    trySend(Resource.Error("User not found after sign in"))
                }
            }
            .addOnFailureListener { exception ->
                trySend(Resource.Error(exception.localizedMessage ?: "Login failed"))
            }
            
        awaitClose { /* Cleanup if needed */ }
    }
    
    override fun logout() {
        firebaseAuth.signOut()
    }
    
    override fun getCurrentUser(): User? {
        val fbUser = firebaseAuth.currentUser
        return fbUser?.let {
            User(
                id = it.uid,
                email = it.email ?: "",
                displayName = it.displayName
            )
        }
    }
    
    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
