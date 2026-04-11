package com.farbalapps.rinde.domain.model

/**
 * Domain model for suggested products from the catalog.
 */
data class CatalogItem(
    val id: Int,
    val nombre: String,
    val categoria: String,
    val emoji: String
)
