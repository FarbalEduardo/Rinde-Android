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
import kotlinx.coroutines.tasks.await

import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.GoalsDao
import com.farbalapps.rinde.data.local.dao.FinancialDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.local.dao.UserVoteDao
import com.farbalapps.rinde.data.util.SavedPostsMemoryCache
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.database.FirebaseDatabase
import com.farbalapps.rinde.data.local.dao.ProfileDao
import javax.inject.Inject
import javax.inject.Provider

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val savedPostsMemoryCache: SavedPostsMemoryCache,
    private val userVoteDao: UserVoteDao,
    private val postDao: PostDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val goalsDao: GoalsDao,
    private val financialDao: FinancialDao,
    private val goalsRepositoryProvider: Provider<GoalsRepository>,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val rtdb: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val profileDao: ProfileDao
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
                        displayName = fbUser.displayName,
                        photoUrl = fbUser.photoUrl?.toString()
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
                        displayName = fbUser.displayName,
                        photoUrl = fbUser.photoUrl?.toString()
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
                    displayName = fbUser.displayName,
                    photoUrl = fbUser.photoUrl?.toString()
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
        savedPostsMemoryCache.clear()
    }

    override suspend fun clearUserLocalState() {
        val uid = getCurrentUser()?.id ?: ""
        if (uid.isNotEmpty()) {
            try {
                goalsRepositoryProvider.get().forceSyncBeforeLogout(uid)
            } catch (_: Exception) { }
            userVoteDao.clearUserVotes(uid)
            goalsDao.deleteGoalsByUserId(uid)
            goalsDao.deleteTransactionsByUserId(uid)
            financialDao.deleteProfileByUserId(uid)
            financialDao.deleteExpensesByUserId(uid)
        }
        goalsDao.clearAllGoals() // Limpieza total de metas en Room para evitar remanentes de sesión
        goalsDao.clearAllTransactions()
        financialDao.clearAllFinancialProfiles() // Limpieza de salud financiera para evitar fuga entre sesiones
        financialDao.clearAllExtraExpenses()
        postDao.clearAll() // Borra completamente el feed local de Room para evitar fugas y obligar re-sync
        syncMetadataDao.clearAll() // Borra metadatos para reiniciar sincronizaciones del nuevo usuario
    }
    
    override fun getCurrentUser(): User? {
        val fbUser = firebaseAuth.currentUser
        return fbUser?.let {
            User(
                id = it.uid,
                email = it.email ?: "",
                displayName = it.displayName,
                photoUrl = it.photoUrl?.toString()
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

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("No hay sesión activa de usuario")
        val email = user.email ?: throw IllegalStateException("El usuario no cuenta con un correo registrado")

        // Reautenticación con contraseña actual
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()

        // Actualización de contraseña en Firebase Auth
        user.updatePassword(newPassword).await()
    }

    override suspend fun suspendAccount(userId: String): Result<Unit> = runCatching {
        if (userId.isBlank()) throw IllegalArgumentException("User ID no puede estar vacío")

        // 1. Marcar usuario como suspendido en Firestore
        firestore.collection("users").document(userId).set(
            mapOf(
                "isSuspended" to true,
                "status" to "SUSPENDED",
                "suspendedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()

        // 2. Ocultar todas las publicaciones del usuario en Firestore (isActive = false)
        val postsSnapshot = firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .get().await()

        if (!postsSnapshot.isEmpty) {
            postsSnapshot.documents.chunked(450).forEach { chunk ->
                firestore.runBatch { batch ->
                    chunk.forEach { doc ->
                        batch.update(doc.reference, "isActive", false)
                    }
                }.await()
            }
        }

        // 3. Limpiar estado local y cerrar sesión
        clearUserLocalState()
        logout()
    }

    override suspend fun isAccountSuspended(userId: String): Result<Boolean> = runCatching {
        if (userId.isBlank()) return@runCatching false
        val doc = firestore.collection("users").document(userId).get().await()
        doc.getBoolean("isSuspended") == true || doc.getString("status") == "SUSPENDED"
    }

    override suspend fun reactivateAccount(userId: String): Result<Unit> = runCatching {
        if (userId.isBlank()) throw IllegalArgumentException("User ID no puede estar vacío")

        // 1. Restaurar estado activo en Firestore
        firestore.collection("users").document(userId).set(
            mapOf(
                "isSuspended" to false,
                "status" to "ACTIVE",
                "reactivatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()

        // 2. Restaurar visibilidad de las publicaciones del usuario (isActive = true)
        val postsSnapshot = firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .get().await()

        if (!postsSnapshot.isEmpty) {
            postsSnapshot.documents.chunked(450).forEach { chunk ->
                firestore.runBatch { batch ->
                    chunk.forEach { doc ->
                        batch.update(doc.reference, "isActive", true)
                    }
                }.await()
            }
        }
    }

    override suspend fun deleteAccountPermanently(userId: String): Result<Unit> = runCatching {
        val targetUid = userId.ifBlank { firebaseAuth.currentUser?.uid ?: throw IllegalStateException("No active user") }

        // 1. Obtener publicaciones del usuario para purgar comentarios en RTDB
        val postsSnapshot = firestore.collection("posts")
            .whereEqualTo("authorId", targetUid)
            .get().await()

        // 2. Eliminar comentarios de cada publicación en Realtime Database
        for (doc in postsSnapshot.documents) {
            try {
                rtdb.getReference("comments").child(doc.id).removeValue().await()
            } catch (e: Exception) {
                android.util.Log.e("FirebaseAuthRepository", "Error eliminando comentarios RTDB de post ${doc.id}: ${e.message}")
            }
        }

        // 3. Eliminar publicaciones de Firestore en lotes
        if (!postsSnapshot.isEmpty) {
            postsSnapshot.documents.chunked(450).forEach { chunk ->
                firestore.runBatch { batch ->
                    chunk.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                }.await()
            }
        }

        // 4. Eliminar documento del usuario en Firestore
        try {
            firestore.collection("users").document(targetUid).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthRepository", "Error eliminando /users/$targetUid: ${e.message}")
        }

        // 5. Limpieza de Room local
        try {
            profileDao.deleteProfile(targetUid)
        } catch (_: Exception) {}
        clearUserLocalState()

        // 6. Eliminar usuario de Firebase Auth y cerrar sesión
        val user = firebaseAuth.currentUser
        user?.delete()?.await()
        logout()
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val uid = firebaseAuth.currentUser?.uid ?: ""
        return deleteAccountPermanently(uid)
    }
}

