package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.util.ValidationResult

import javax.inject.Inject

class ValidateEmail @Inject constructor() {
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun execute(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(
                successful = false,
                errorMessage = "Email cannot be empty"
            )
        }
        if (!email.matches(emailRegex)) {
            return ValidationResult(
                successful = false,
                errorMessage = "Invalid email format (must contain @ and a valid domain)"
            )
        }
        return ValidationResult(successful = true)
    }
}
