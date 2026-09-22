package com.farbalapps.rinde.domain.usecase.account

import com.farbalapps.rinde.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso responsable de reactivar una cuenta previamente suspendida,
 * restaurando el estado activo y la visibilidad de sus publicaciones.
 */
class ReactivateAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Reactiva la cuenta del usuario indicado.
     *
     * @param userId Identificador único del usuario.
     * @return [Result.success] si se reactivó con éxito, o [Result.failure] en caso de error.
     */
    suspend operator fun invoke(userId: String): Result<Unit> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("El ID del usuario no puede estar vacío"))
        }
        return authRepository.reactivateAccount(userId)
    }

    /**
     * Verifica si una cuenta de usuario se encuentra en estado suspendido.
     *
     * @param userId Identificador único del usuario.
     * @return [Result.success] conteniendo true si está suspendida, o [Result.failure].
     */
    suspend fun isSuspended(userId: String): Result<Boolean> {
        if (userId.isBlank()) {
            return Result.success(false)
        }
        return authRepository.isAccountSuspended(userId)
    }
}
