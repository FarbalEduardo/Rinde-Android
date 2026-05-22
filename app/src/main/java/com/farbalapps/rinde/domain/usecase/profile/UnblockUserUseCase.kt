package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.repository.ProfileRepository
import javax.inject.Inject

class UnblockUserUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(userId: String, targetUserId: String) = repository.unblockUser(userId, targetUserId)
}
