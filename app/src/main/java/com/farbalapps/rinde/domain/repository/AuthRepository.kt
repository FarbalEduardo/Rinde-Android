package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, password: String): Flow<Resource<User>>
    fun signUp(email: String, password: String): Flow<Resource<User>>
    fun signInWithGoogle(idToken: String): Flow<Resource<User>>
    fun logout()
    suspend fun clearUserLocalState()
    fun getCurrentUser(): User?
    fun isUserLoggedIn(): Boolean
    fun sendPasswordResetEmail(email: String): Flow<Resource<Unit>>
    suspend fun deleteAccount(): Result<Unit>

    /** Reautentica al usuario actual y actualiza su contraseña */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    /** Suspende la cuenta del usuario, oculta sus publicaciones y cierra sesión */
    suspend fun suspendAccount(userId: String): Result<Unit>

    /** Consulta si la cuenta especificada se encuentra suspendida */
    suspend fun isAccountSuspended(userId: String): Result<Boolean>

    /** Reactiva una cuenta suspendida y restaura la visibilidad de sus publicaciones */
    suspend fun reactivateAccount(userId: String): Result<Unit>

    /** Elimina permanentemente la cuenta, publicaciones, comentarios y datos locales */
    suspend fun deleteAccountPermanently(userId: String): Result<Unit>
}

