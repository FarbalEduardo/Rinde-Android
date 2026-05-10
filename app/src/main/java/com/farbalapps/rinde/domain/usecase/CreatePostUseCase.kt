package com.farbalapps.rinde.domain.usecase

import android.net.Uri
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.domain.moderation.ContentModerator
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * Use case to create and upload a new community post.
 * Handles content moderation and data orchestration between repositories.
 * 
 * @property feedRepository Repository to handle post uploads.
 * @property authRepository Repository to get current user information.
 * @property moderationExpert Domain service to analyze content for prohibited material.
 */
class CreatePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository,
    private val moderationExpert: ContentModerator
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        category: String,
        locationName: String,
        photoUris: List<Uri>
    ): Result<Unit> {
        // 1. Basic Validation (Safety/Quality)
        if (title.isBlank()) return Result.failure(Exception("El título es obligatorio"))
        if (description.isBlank()) return Result.failure(Exception("La descripción es obligatoria"))

        // 2. Content Moderation (Guardian Agent)
        val moderationResult = moderationExpert.analyzeText(title, description)
        if (moderationResult is ContentModerator.ModerationResult.Rejected) {
            return Result.failure(Exception(moderationResult.reason))
        }

        // 3. Orchestration
        val user = authRepository.getCurrentUser()
        val post = CommunityPost(
            authorName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Usuario",
            authorId = user?.id ?: "anonymous",
            title = title,
            descriptionShort = if (description.length > 50) description.take(50) + "..." else description,
            descriptionLong = description,
            category = category,
            location = PostLocation(name = locationName)
        )

        return feedRepository.uploadPost(post, photoUris.map { it.toString() })
    }
}
