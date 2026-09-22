package com.farbalapps.rinde.domain.usecase.account

import com.farbalapps.rinde.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso responsable de suspender temporalmente la cuenta del usuario,
 * ocultando sus publicaciones y cerrando la sesión de forma limpia.
 */
class SuspendAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Ejecuta la suspensión de la cuenta para el usuario indicado.
     *
     * @param userId Identificador único del usuario.
     * @return [Result.success] si se suspendió con éxito, o [Result.failure] en caso de error.
     */
    suspend operator fun invoke(userId: String): Result<Unit> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("El ID del usuario no puede estar vacío"))
        }
        return authRepository.suspendAccount(userId)
    }
}
