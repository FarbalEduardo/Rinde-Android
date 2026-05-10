package com.farbalapps.rinde.domain.model

data class Category(
    val name: String,
    val userId: String = "",
    val isCustom: Boolean = true,
    val orderIndex: Int = 0
) {
    companion object {
        val FIXED_COMMUNITY_CATEGORIES = listOf(
            "Abarrotes",
            "Tecnología",
            "Hogar",
            "Moda",
            "Mascotas",
            "Salud y Belleza",
            "Otros"
        )
    }
}
