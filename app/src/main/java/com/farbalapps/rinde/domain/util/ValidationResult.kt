package com.farbalapps.rinde.domain.util

data class ValidationResult(
    val successful: Boolean,
    val errorMessage: String? = null
)
