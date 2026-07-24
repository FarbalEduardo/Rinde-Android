package com.farbalapps.rinde.domain.model

data class PostLocation(
    val name: String,
    val latitude: Double?,
    val longitude: Double?
)

enum class OfferType {
    ONLINE, PHYSICAL, UNSPECIFIED
}

enum class VerificationStatus {
    PENDING, VERIFIED, EXPIRED, DISPUTED
}

data class CommunityPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val timestamp: Long,
    val title: String,
    val descriptionShort: String,
    val descriptionLong: String,
    val photos: List<String>,
    val category: String,
    val location: PostLocation,
    val isActive: Boolean,
    val likesCount: Int,
    val commentsCount: Int,
    val truthCount: Int,
    val falseCount: Int,
    val votesScore: Int,
    val verificationStatus: VerificationStatus,
    val reportCount: Int,
    val userReputationScore: Float,
    val isAuthorVerified: Boolean,
    val offerType: OfferType,
    val websiteName: String?,
    val productLink: String?,
    val storeName: String?,
    val isRecommended: Boolean,
    val expiresAt: Long?,
    val normalPrice: Double?,
    val discountPrice: Double?,
    val currency: String,
    val couponCode: String?,
    val discountPercentage: Int?,
    val isAvailable: Boolean,
    val condition: String,
    // Estado de interacción del usuario actual (no se persiste en Firestore,
    // se enriquece en el repositorio al cargar el feed)
    val isLikedByMe: Boolean = false,
    val myVoteValue: Int = 0,       // -1 = falso, 0 = sin voto, 1 = verdadero
    val isSavedByMe: Boolean = false,
    val authorTrustScore: Float = 0f,
    val authorTrustLevel: String = "NEW"
)
