package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farbalapps.rinde.domain.model.Profile

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int,
    val rating: Float,
    val reviewsCount: Int,
    val isPrivate: Boolean,
    val isDummy: Boolean,
    val uploadStatus: String?
)

fun ProfileEntity.toDomainModel(): Profile {
    return Profile(
        id = id,
        name = name,
        email = email,
        photoUrl = photoUrl,
        followersCount = followersCount,
        followingCount = followingCount,
        postsCount = postsCount,
        rating = rating,
        reviewsCount = reviewsCount,
        isPrivate = isPrivate,
        isDummy = isDummy,
        uploadStatus = uploadStatus
    )
}

fun Profile.toEntity(): ProfileEntity {
    return ProfileEntity(
        id = id,
        name = name,
        email = email,
        photoUrl = photoUrl,
        followersCount = followersCount,
        followingCount = followingCount,
        postsCount = postsCount,
        rating = rating,
        reviewsCount = reviewsCount,
        isPrivate = isPrivate,
        isDummy = isDummy,
        uploadStatus = uploadStatus
    )
}
