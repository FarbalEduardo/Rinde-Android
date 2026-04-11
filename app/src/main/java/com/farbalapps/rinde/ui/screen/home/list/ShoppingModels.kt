package com.farbalapps.rinde.ui.screen.home.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.farbalapps.rinde.R

enum class ProductCategory(
    val displayNameRes: Int,
    val icon: ImageVector
) {
    GROCERIES(R.string.cat_groceries, Icons.Default.LocalGroceryStore),
    FRUITS_VEGETABLES(R.string.cat_fruits, Icons.Default.LocalFlorist),
    PROTEINS(R.string.cat_proteins, Icons.Default.SetMeal),
    ELECTRONICS(R.string.cat_electronics, Icons.Default.Devices),
    HOUSEHOLD(R.string.cat_household, Icons.Default.Kitchen),
    PHARMACY(R.string.cat_pharmacy, Icons.Default.MedicalServices),
    CLOTHING(R.string.cat_clothing, Icons.Default.Checkroom),
    HOME(R.string.cat_home, Icons.Default.Home),
    OTHERS(R.string.cat_others, Icons.Default.Category)
}

fun String.toProductCategory(): ProductCategory =
    when (this.lowercase().trim()) {
        "frutas", "verduras", "frutas y verduras" -> ProductCategory.FRUITS_VEGETABLES
        "carnes", "pescados", "proteínas" -> ProductCategory.PROTEINS
        "lácteos", "abarrotes", "despensa", "lácteos y huevo" -> ProductCategory.GROCERIES
        "hogar", "limpieza", "línea blanca" -> ProductCategory.HOUSEHOLD
        "salud", "farmacia", "higiene" -> ProductCategory.PHARMACY
        "tecnología", "electrónica" -> ProductCategory.ELECTRONICS
        "ropa" -> ProductCategory.CLOTHING
        else -> try { 
            ProductCategory.valueOf(this.uppercase().replace(" ", "_")) 
        } catch (e: Exception) { 
            ProductCategory.OTHERS 
        }
    }
