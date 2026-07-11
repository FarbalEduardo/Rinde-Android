package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.VerificationStatus
import java.util.Date
import androidx.room.Index

@Entity(
    tableName = "community_posts",
    indices = [
        Index(value = ["isActive", "timestamp"]),
        Index(value = ["isActive", "votesScore"])
    ]
)
data class CommunityPostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val timestamp: Long,
    val title: String,
    val descriptionShort: String,
    val descriptionLong: String,
    /** Photo URL list stored as a JSON array by [com.farbalapps.rinde.data.local.db.Converters]. */
    val photos: List<String>,
    val category: String,
    val locationName: String,
    val latitude: Double?,
    val longitude: Double?,
    val isActive: Boolean,
    val likesCount: Int,
    val commentsCount: Int,
    val truthCount: Int,
    val falseCount: Int,
    val votesScore: Int,
    val verificationStatus: String,
    val reportCount: Int,
    val userReputationScore: Float,
    val isAuthorVerified: Boolean,
    val offerType: String,
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
    val myVoteValue: Int,
    val isSavedByMe: Boolean,
    val authorTrustScore: Float,
    val authorTrustLevel: String
)

fun CommunityPostEntity.toDomainModel(): CommunityPost {
    return CommunityPost(
        id = id,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        timestamp = Date(timestamp),
        title = title,
        descriptionShort = descriptionShort,
        descriptionLong = descriptionLong,
        photos = photos,
        category = category,
        location = PostLocation(locationName, latitude, longitude),
        isActive = isActive,
        likesCount = likesCount,
        commentsCount = commentsCount,
        truthCount = truthCount,
        falseCount = falseCount,
        votesScore = votesScore,
        verificationStatus = VerificationStatus.valueOf(verificationStatus),
        reportCount = reportCount,
        userReputationScore = userReputationScore,
        isAuthorVerified = isAuthorVerified,
        offerType = OfferType.valueOf(offerType),
        websiteName = websiteName,
        productLink = productLink,
        storeName = storeName,
        isRecommended = isRecommended,
        expiresAt = expiresAt?.let { Date(it) },
        normalPrice = normalPrice,
        discountPrice = discountPrice,
        currency = currency,
        couponCode = couponCode,
        discountPercentage = discountPercentage,
        isAvailable = isAvailable,
        condition = condition,
        myVoteValue = myVoteValue,
        isSavedByMe = isSavedByMe,
        authorTrustScore = authorTrustScore,
        authorTrustLevel = authorTrustLevel
    )
}

fun CommunityPost.toEntity(): CommunityPostEntity {
    return CommunityPostEntity(
        id = id,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        timestamp = timestamp?.time ?: System.currentTimeMillis(),
        title = title,
        descriptionShort = descriptionShort,
        descriptionLong = descriptionLong,
        photos = photos,
        category = category,
        locationName = location.name,
        latitude = location.latitude,
        longitude = location.longitude,
        isActive = isActive,
        likesCount = likesCount,
        commentsCount = commentsCount,
        truthCount = truthCount,
        falseCount = falseCount,
        votesScore = votesScore,
        verificationStatus = verificationStatus.name,
        reportCount = reportCount,
        userReputationScore = userReputationScore,
        isAuthorVerified = isAuthorVerified,
        offerType = offerType.name,
        websiteName = websiteName,
        productLink = productLink,
        storeName = storeName,
        isRecommended = isRecommended,
        expiresAt = expiresAt?.time,
        normalPrice = normalPrice,
        discountPrice = discountPrice,
        currency = currency,
        couponCode = couponCode,
        discountPercentage = discountPercentage,
        isAvailable = isAvailable,
        condition = condition,
        myVoteValue = myVoteValue,
        isSavedByMe = isSavedByMe,
        authorTrustScore = authorTrustScore,
        authorTrustLevel = authorTrustLevel
    )
}

