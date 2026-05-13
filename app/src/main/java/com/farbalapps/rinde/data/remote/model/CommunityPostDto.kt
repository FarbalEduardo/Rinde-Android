package com.farbalapps.rinde.data.remote.model

import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data Transfer Object (DTO) para representar un post en Firestore.
 * Contiene anotaciones específicas de Firebase y valores por defecto para serialización.
 */
data class CommunityPostDto(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    @ServerTimestamp
    val timestamp: Date? = null,
    val title: String = "",
    val descriptionShort: String = "",
    val descriptionLong: String = "",
    val photos: List<String> = emptyList(),
    val category: String = "Otros",
    val location: PostLocationDto = PostLocationDto(),
    val isActive: Boolean = true,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val truthCount: Int = 0,
    val falseCount: Int = 0,
    val votesScore: Int = 0,
    val verificationStatus: String = VerificationStatus.PENDING.name,
    val reportCount: Int = 0,
    val userReputationScore: Float = 0f,
    val isAuthorVerified: Boolean = false,
    val offerType: String = OfferType.UNSPECIFIED.name,
    val websiteName: String? = null,
    val productLink: String? = null,
    val storeName: String? = null,
    val isRecommended: Boolean = false,
    val expiresAt: Date? = null
)

data class PostLocationDto(
    val name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)
