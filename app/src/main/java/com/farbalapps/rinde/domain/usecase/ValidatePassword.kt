package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.util.ValidationResult

import javax.inject.Inject

class ValidatePassword @Inject constructor() {
    fun execute(password: String): ValidationResult {
        if (password.length < 6) {
            return ValidationResult(
                successful = false,
                errorMessage = "Minimum 6 characters required"
            )
        }
        
        val containsNumber = password.any { it.isDigit() }
        if (!containsNumber) {
            return ValidationResult(
                successful = false,
                errorMessage = "Password must contain at least one number"
            )
        }
        
        return ValidationResult(successful = true)
    }
}
