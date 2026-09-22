package com.farbalapps.rinde.domain.usecase.account

import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.usecase.ValidatePassword
import javax.inject.Inject

/**
 * Caso de uso responsable de validar y coordinar el cambio seguro de contraseña in-app.
 */
class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validatePassword: ValidatePassword
) {
    /**
     * Valida los campos y actualiza la contraseña mediante reautenticación segura.
     *
     * @param currentPass Contraseña actual del usuario.
     * @param newPass Nueva contraseña deseada.
     * @param confirmPass Confirmación de la nueva contraseña.
     * @return [Result.success] si se actualizó correctamente, o [Result.failure] con la causa del error.
     */
    suspend operator fun invoke(
        currentPass: String,
        newPass: String,
        confirmPass: String
    ): Result<Unit> {
        if (currentPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            return Result.failure(IllegalArgumentException("Todos los campos son obligatorios"))
        }

        if (newPass != confirmPass) {
            return Result.failure(IllegalArgumentException("Las nuevas contraseñas no coinciden"))
        }

        if (newPass == currentPass) {
            return Result.failure(IllegalArgumentException("La nueva contraseña debe ser diferente a la actual"))
        }

        val passwordValidation = validatePassword.execute(newPass)
        if (!passwordValidation.successful) {
            return Result.failure(IllegalArgumentException(passwordValidation.errorMessage ?: "Contraseña no válida"))
        }

        return authRepository.changePassword(currentPass, newPass)
    }
}
