package com.farbalapps.rinde.domain.usecase

import javax.inject.Inject

/**
 * Use case to determine whether shopping list items are cookable/edible for Chef IA.
 *
 * Strategy (applied in order):
 * 1. Category-based: if the item's category is a known non-food category → not cookable.
 * 2. Name-based keyword fallback: catches user-typed products that don't come from the catalog.
 *
 * This approach is robust because catalog items always carry a category, while
 * manually typed items are covered by keyword matching as a safety net.
 */
class FilterCookableItemsUseCase @Inject constructor() {

    /** Categories that are definitively non-food (sourced from catalog.json categories) */
    private val nonCookableCategories = setOf(
        "Limpieza",
        "Higiene",
        "Mascotas",
        "Bebé",
        "Bebe",
        "Farmacia",
        "Electrónica",
        "Electronica",
        "Papelería",
        "Papeleria"
    )

    /**
     * Keyword fallback for items without a category or with unknown categories.
     * Covers common non-food product names a user might type manually.
     */
    private val nonCookableKeywords = setOf(
        // Limpieza
        "detergente", "suavizante", "cloro", "limpiador", "escoba", "trapeador",
        "esponja", "fibra", "desinfectante", "lavatrastes", "insecticida",
        // Higiene personal
        "pañal", "pañales", "jabón", "jabon", "shampoo", "champú", "champu",
        "acondicionador", "pasta de dientes", "cepillo dental", "rastrillo",
        "toalla sanitaria", "desodorante", "perfume", "colonia",
        // Mascotas
        "croquetas", "croqueta", "arena para gato", "arena gato", "alimento para perro",
        "alimento mascota", "hueso mascota",
        // Higiene del hogar / descartables
        "papel higiénico", "papel higienico", "servilleta", "servilletas",
        "bolsa de basura", "bolsas de basura", "aluminio",
        // Electrónica / hogar
        "pila", "pilas", "foco", "focos", "vela", "cerillo", "cerillos",
        "encendedor", "cargador", "audífonos", "audifonos",
        // Farmacia
        "pastilla", "analgesico", "analgésico", "vitaminas", "curita", "curitas",
        "alcohol isopropilico"
    )

    /**
     * Evaluates a single item by category first, then by keyword fallback.
     *
     * @param name The product name.
     * @param category The product category (empty string if unknown).
     * @return true if the item is considered edible/cookable.
     */
    fun isCookable(name: String, category: String = ""): Boolean {
        // 1. Category-based check (fast and reliable for catalog items)
        if (category.isNotBlank() && nonCookableCategories.any { it.equals(category.trim(), ignoreCase = true) }) {
            return false
        }
        // 2. Keyword fallback (for manually typed items)
        val lower = name.trim().lowercase()
        return nonCookableKeywords.none { keyword -> lower.contains(keyword) }
    }

    /**
     * Legacy overload: filters a list of names only (no category).
     * Used for backward compatibility and unit tests.
     */
    operator fun invoke(itemNames: List<String>): List<String> {
        return itemNames.filter { isCookable(it, category = "") }
    }
}
