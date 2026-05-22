package com.farbalapps.rinde.domain.usecase

import android.net.Uri
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.domain.model.VerificationStatus
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
        photoUris: List<Uri>,
        offerType: com.farbalapps.rinde.domain.model.OfferType = com.farbalapps.rinde.domain.model.OfferType.UNSPECIFIED,
        websiteName: String? = null,
        productLink: String? = null,
        storeName: String? = null
    ): Result<Unit> {
        // 1. Basic Validation
        if (title.isBlank()) return Result.failure(Exception("El título es obligatorio"))
        if (description.isBlank()) return Result.failure(Exception("La descripción es obligatoria"))

        // 2. Content Moderation
        val moderationResult = moderationExpert.analyzeText(title, description)
        if (moderationResult is ContentModerator.ModerationResult.Rejected) {
            return Result.failure(Exception(moderationResult.reason))
        }

        // 3. Orchestration
        val user = authRepository.getCurrentUser()
        val post = CommunityPost(
            id = "", // Generado por el repositorio o Firestore
            authorId = user?.id ?: "anonymous",
            authorName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Usuario",
            authorPhotoUrl = user?.photoUrl,
            timestamp = java.util.Date(),
            title = title,
            descriptionShort = if (description.length > 50) description.take(50) + "..." else description,
            descriptionLong = description,
            photos = emptyList(), // Se llenará en el repositorio
            category = category,
            location = PostLocation(name = locationName, latitude = null, longitude = null),
            isActive = true,
            likesCount = 0,
            commentsCount = 0,
            truthCount = 0,
            falseCount = 0,
            votesScore = 0,
            verificationStatus = VerificationStatus.PENDING,
            reportCount = 0,
            userReputationScore = user?.reputationScore ?: 0f,
            isAuthorVerified = false,
            offerType = offerType,
            websiteName = websiteName,
            productLink = productLink,
            storeName = storeName,
            isRecommended = false,
            expiresAt = null
        )

        return feedRepository.uploadPost(post, photoUris.map { it.toString() })
    }

}
