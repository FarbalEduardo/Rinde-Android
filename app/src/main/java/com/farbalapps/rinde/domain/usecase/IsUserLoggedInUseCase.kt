package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.AuthRepository

class IsUserLoggedInUseCase(
    private val repository: AuthRepository
) {
    fun execute(): Boolean {
        return repository.isUserLoggedIn()
    }
}
