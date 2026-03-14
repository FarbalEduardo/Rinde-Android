package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.util.ValidationResult

class ValidateEmail {
    fun execute(email: String): ValidationResult {
        if (!email.contains("@")) {
            return ValidationResult(
                successful = false,
                errorMessage = "Email must contain @"
            )
        }
        return ValidationResult(successful = true)
    }
}
