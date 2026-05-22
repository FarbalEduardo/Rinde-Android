package com.farbalapps.rinde.data.mapper

import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.data.remote.model.PostLocationDto
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.domain.model.VerificationStatus

fun CommunityPostDto.toDomain(): CommunityPost {
    return CommunityPost(
        id = id,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        timestamp = timestamp,
        title = title,
        descriptionShort = descriptionShort,
        descriptionLong = descriptionLong,
        photos = photos,
        category = category,
        location = location.toDomain(),
        isActive = isActive,
        likesCount = likesCount,
        commentsCount = commentsCount,
        truthCount = truthCount,
        falseCount = falseCount,
        votesScore = votesScore,
        verificationStatus = try { VerificationStatus.valueOf(verificationStatus) } catch (e: Exception) { VerificationStatus.PENDING },
        reportCount = reportCount,
        userReputationScore = userReputationScore,
        isAuthorVerified = isAuthorVerified,
        offerType = try { OfferType.valueOf(offerType) } catch (e: Exception) { OfferType.UNSPECIFIED },
        websiteName = websiteName,
        productLink = productLink,
        storeName = storeName,
        isRecommended = isRecommended,
        expiresAt = expiresAt
    )
}

fun PostLocationDto.toDomain(): PostLocation {
    return PostLocation(
        name = name,
        latitude = latitude,
        longitude = longitude
    )
}

fun CommunityPost.toDto(): CommunityPostDto {
    return CommunityPostDto(
        id = id,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        timestamp = timestamp,
        title = title,
        descriptionShort = descriptionShort,
        descriptionLong = descriptionLong,
        photos = photos,
        category = category,
        location = location.toDto(),
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
        expiresAt = expiresAt
    )
}

fun PostLocation.toDto(): PostLocationDto {
    return PostLocationDto(
        name = name,
        latitude = latitude,
        longitude = longitude
    )
}
