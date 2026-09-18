package com.farbalapps.rinde.data.remote.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data Transfer Object (DTO) para almacenar el snapshot desnormalizado
 * de una oferta en /users/{userId}/saved_posts/{postId}.
 *
 * Permite renderizar la lista de guardados en una sola lectura sin queries
 * secundarias ni límites de whereIn(30), cumpliendo con firestore-denormalization-skill.
 */
data class SavedPostSnapshotDto(
    val postId: String = "",
    val title: String = "",
    val descriptionShort: String = "",
    val descriptionLong: String = "",
    val normalPrice: Double? = null,
    val discountPrice: Double? = null,
    val currency: String = "MXN",
    val category: String = "Otros",
    val storeName: String? = null,
    val photos: List<String> = emptyList(),
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val verificationStatus: String = "PENDING",
    val offerType: String = "UNSPECIFIED",
    val isAvailable: Boolean = true,
    val condition: String = "Nuevo",
    val isExpired: Boolean = false,
    val isActive: Boolean = true,
    val websiteName: String? = null,
    val productLink: String? = null,
    val couponCode: String? = null,
    @ServerTimestamp
    val savedAt: Date? = null,
    val originalTimestamp: Long? = null
)
