package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task

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
    
    override fun signUp(email: String, password: String): Flow<Resource<User>> = callbackFlow {
        trySend(Resource.Loading())
        
        firebaseAuth.createUserWithEmailAndPassword(email, password)
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
                    trySend(Resource.Error("User not found after sign up"))
                }
            }
            .addOnFailureListener { exception ->
                trySend(Resource.Error(exception.localizedMessage ?: "Sign up failed"))
            }
            
        awaitClose { }
    }

    override fun signInWithGoogle(idToken: String): Flow<Resource<User>> = callbackFlow {
        trySend(Resource.Loading())
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                handleAuthResult(task, this)
            }
        awaitClose { }
    }

    private fun handleAuthResult(task: Task<AuthResult>, producerScope: kotlinx.coroutines.channels.ProducerScope<Resource<User>>) {
        if (task.isSuccessful) {
            val fbUser = task.result?.user
            if (fbUser != null) {
                val user = User(
                    id = fbUser.uid,
                    email = fbUser.email ?: "",
                    displayName = fbUser.displayName
                )
                producerScope.trySend(Resource.Success(user))
            } else {
                producerScope.trySend(Resource.Error("User not found after social login"))
            }
        } else {
            producerScope.trySend(Resource.Error(task.exception?.localizedMessage ?: "Social login failed"))
        }
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

    override fun sendPasswordResetEmail(email: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
            }
            .addOnFailureListener { exception ->
                trySend(Resource.Error(exception.localizedMessage ?: "Failed to send reset email"))
            }
        awaitClose { }
    }
}
