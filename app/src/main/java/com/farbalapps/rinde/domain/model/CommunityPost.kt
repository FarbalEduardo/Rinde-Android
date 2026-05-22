package com.farbalapps.rinde.domain.model

import java.util.Date

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
    val timestamp: Date?,
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
    val expiresAt: Date?
)

