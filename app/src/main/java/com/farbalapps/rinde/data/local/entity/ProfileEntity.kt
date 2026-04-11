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
    val isDummy: Boolean
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
        isDummy = isDummy
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
        isDummy = isDummy
    )
}
