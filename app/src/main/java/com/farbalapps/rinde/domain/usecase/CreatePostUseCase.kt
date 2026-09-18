package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.moderation.ContentModerator
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * UseCase para crear y subir una nueva publicación de comunidad.
 * Orquesta la moderación de contenido y la construcción del modelo antes de delegar al repositorio.
 *
 * [HU-01] Permite a un usuario publicar una oferta con fotos, precio y ubicación.
 *
 * @property feedRepository Repositorio que gestiona la subida del post.
 * @property authRepository Repositorio para obtener el usuario actual.
 * @property moderationExpert Servicio de dominio que analiza el contenido.
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
        photos: List<String>,   // URIs convertidas a String en la capa UI antes de llamar al UseCase
        offerType: com.farbalapps.rinde.domain.model.OfferType = com.farbalapps.rinde.domain.model.OfferType.UNSPECIFIED,
        websiteName: String? = null,
        productLink: String? = null,
        storeName: String? = null,
        normalPrice: Double? = null,
        discountPrice: Double? = null,
        currency: String = "MXN",
        couponCode: String? = null,
        discountPercentage: Int? = null,
        isAvailable: Boolean = true,
        condition: String = "Nuevo",
        latitude: Double? = null,
        longitude: Double? = null
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
        val post = buildCommunityPost(
            user = user,
            title = title,
            description = description,
            category = category,
            locationName = locationName,
            offerType = offerType,
            websiteName = websiteName,
            productLink = productLink,
            storeName = storeName,
            normalPrice = normalPrice,
            discountPrice = discountPrice,
            currency = currency,
            couponCode = couponCode,
            discountPercentage = discountPercentage,
            isAvailable = isAvailable,
            condition = condition,
            latitude = latitude,
            longitude = longitude
        )

        return feedRepository.uploadPost(post, photos)
    }

    private fun buildCommunityPost(
        user: com.farbalapps.rinde.domain.model.User?,
        title: String,
        description: String,
        category: String,
        locationName: String,
        offerType: com.farbalapps.rinde.domain.model.OfferType,
        websiteName: String?,
        productLink: String?,
        storeName: String?,
        normalPrice: Double?,
        discountPrice: Double?,
        currency: String,
        couponCode: String?,
        discountPercentage: Int?,
        isAvailable: Boolean,
        condition: String,
        latitude: Double?,
        longitude: Double?
    ): CommunityPost {
        return CommunityPost(
            id = "", // Generado por el repositorio o Firestore
            authorId = user?.id ?: "anonymous",
            authorName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Usuario",
            authorPhotoUrl = user?.photoUrl,
            timestamp = System.currentTimeMillis(),
            title = title,
            descriptionShort = if (description.length > 50) description.take(50) + "..." else description,
            descriptionLong = description,
            photos = emptyList(), // Se llenará en el repositorio
            category = category,
            location = PostLocation(name = locationName, latitude = latitude, longitude = longitude),
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
            expiresAt = null,
            normalPrice = normalPrice,
            discountPrice = discountPrice,
            currency = currency,
            couponCode = couponCode,
            discountPercentage = discountPercentage,
            isAvailable = isAvailable,
            condition = condition
        )
    }

}
