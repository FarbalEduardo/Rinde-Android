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
        timestamp = timestamp?.time ?: System.currentTimeMillis(),
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
        expiresAt = expiresAt?.time,
        normalPrice = normalPrice,
        discountPrice = discountPrice,
        currency = currency,
        couponCode = couponCode,
        discountPercentage = discountPercentage,
        isAvailable = isAvailable,
        condition = condition,
        authorTrustScore = authorTrustScore,
        authorTrustLevel = authorTrustLevel
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
        timestamp = java.util.Date(timestamp),
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
        expiresAt = expiresAt?.let { java.util.Date(it) },
        normalPrice = normalPrice,
        discountPrice = discountPrice,
        currency = currency,
        couponCode = couponCode,
        discountPercentage = discountPercentage,
        isAvailable = isAvailable,
        condition = condition,
        authorTrustScore = authorTrustScore,
        authorTrustLevel = authorTrustLevel
    )
}

fun PostLocation.toDto(): PostLocationDto {
    return PostLocationDto(
        name = name,
        latitude = latitude,
        longitude = longitude
    )
}

fun com.farbalapps.rinde.data.remote.model.SavedPostSnapshotDto.toDomain(): CommunityPost {
    return CommunityPost(
        id = postId,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        timestamp = originalTimestamp ?: savedAt?.time ?: System.currentTimeMillis(),
        title = title,
        descriptionShort = descriptionShort,
        descriptionLong = descriptionLong,
        photos = photos,
        category = category,
        location = PostLocation(name = storeName ?: "", latitude = null, longitude = null),
        isActive = isActive,
        likesCount = 0,
        commentsCount = 0,
        truthCount = 0,
        falseCount = 0,
        votesScore = 0,
        verificationStatus = try { VerificationStatus.valueOf(verificationStatus) } catch (e: Exception) { VerificationStatus.PENDING },
        reportCount = 0,
        userReputationScore = 0f,
        isAuthorVerified = false,
        offerType = try { OfferType.valueOf(offerType) } catch (e: Exception) { OfferType.UNSPECIFIED },
        websiteName = websiteName,
        productLink = productLink,
        storeName = storeName,
        isRecommended = false,
        expiresAt = null,
        normalPrice = normalPrice,
        discountPrice = discountPrice,
        currency = currency,
        couponCode = couponCode,
        discountPercentage = if (normalPrice != null && discountPrice != null && normalPrice > 0 && normalPrice > discountPrice) {
            (((normalPrice - discountPrice) / normalPrice) * 100).toInt()
        } else null,
        isAvailable = isAvailable,
        condition = condition,
        authorTrustScore = 0f,
        authorTrustLevel = "NEW",
        isSavedByMe = true
    )
}

fun CommunityPost.toSavedSnapshotMap(): Map<String, Any?> {
    return mapOf(
        "postId" to id,
        "title" to title,
        "descriptionShort" to descriptionShort,
        "descriptionLong" to descriptionLong,
        "normalPrice" to normalPrice,
        "discountPrice" to discountPrice,
        "currency" to currency,
        "category" to category,
        "storeName" to storeName,
        "photos" to photos,
        "authorId" to authorId,
        "authorName" to authorName,
        "authorPhotoUrl" to authorPhotoUrl,
        "verificationStatus" to verificationStatus.name,
        "offerType" to offerType.name,
        "isAvailable" to isAvailable,
        "condition" to condition,
        "isExpired" to false,
        "isActive" to isActive,
        "websiteName" to websiteName,
        "productLink" to productLink,
        "couponCode" to couponCode,
        "originalTimestamp" to timestamp,
        "savedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}
