package com.farbalapps.rinde.domain.usecase.account

import com.farbalapps.rinde.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso responsable de la eliminación permanente de la cuenta del usuario,
 * purgando todas sus publicaciones, comentarios en RTDB, datos locales y credenciales de autenticación.
 */
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Ejecuta el borrado permanente e irreversible de la cuenta.
     *
     * @param userId Identificador único del usuario.
     * @return [Result.success] si se eliminó completamente, o [Result.failure] en caso de error.
     */
    suspend operator fun invoke(userId: String): Result<Unit> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("El ID del usuario no puede estar vacío"))
        }
        return authRepository.deleteAccountPermanently(userId)
    }
}
