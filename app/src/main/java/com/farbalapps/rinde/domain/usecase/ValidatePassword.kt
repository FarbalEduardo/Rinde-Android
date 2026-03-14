package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.util.ValidationResult

class ValidatePassword {
    fun execute(password: String): ValidationResult {
        if (password.length < 8) {
            return ValidationResult(
                successful = false,
                errorMessage = "Minimum 8 characters required"
            )
        }
        
        val containsNumber = password.any { it.isDigit() }
        if (!containsNumber) {
            return ValidationResult(
                successful = false,
                errorMessage = "Password must contain at least one number"
            )
        }
        
        val containsSpecialChar = password.any { !it.isLetterOrDigit() }
        if (!containsSpecialChar) {
            return ValidationResult(
                successful = false,
                errorMessage = "Password must contain at least one special character"
            )
        }
        
        return ValidationResult(successful = true)
    }
}
