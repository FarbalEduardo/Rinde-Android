package com.farbalapps.rinde.domain.model

data class PostLocation(
    val name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class OfferType {
    ONLINE, PHYSICAL, UNSPECIFIED
}

enum class VerificationStatus {
    PENDING, VERIFIED, EXPIRED, DISPUTED
}

data class CommunityPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val timestamp: Long = 0L,
    val title: String = "",
    val descriptionShort: String = "",
    val descriptionLong: String = "",
    val photos: List<String> = emptyList(),
    val category: String = "",
    val location: PostLocation = PostLocation(),
    val isActive: Boolean = true,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val truthCount: Int = 0,
    val falseCount: Int = 0,
    val votesScore: Int = 0,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val reportCount: Int = 0,
    val userReputationScore: Float = 0f,
    val isAuthorVerified: Boolean = false,
    val offerType: OfferType = OfferType.UNSPECIFIED,
    val websiteName: String? = null,
    val productLink: String? = null,
    val storeName: String? = null,
    val isRecommended: Boolean = false,
    val expiresAt: Long? = null,
    val normalPrice: Double? = null,
    val discountPrice: Double? = null,
    val currency: String = "MXN",
    val couponCode: String? = null,
    val discountPercentage: Int? = null,
    val isAvailable: Boolean = true,
    val condition: String = "Nuevo",
    // Estado de interacción del usuario actual (no se persiste en Firestore,
    // se enriquece en el repositorio al cargar el feed)
    val isLikedByMe: Boolean = false,
    val myVoteValue: Int = 0,       // -1 = falso, 0 = sin voto, 1 = verdadero
    val isSavedByMe: Boolean = false,
    val authorTrustScore: Float = 0f,
    val authorTrustLevel: String = "NEW"
)
