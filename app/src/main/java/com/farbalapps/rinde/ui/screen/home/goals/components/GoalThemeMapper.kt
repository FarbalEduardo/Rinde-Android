package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Mapea claves del modelo a recursos de interfaz (Material Icons e Hilos de Color de marca)
 */
object GoalThemeMapper {

    /**
     * Mapea una clave de icono predefinida a un ImageVector de Material Design.
     * Soporta los casos de ahorro y ofrece un fallback genérico.
     */
    fun mapIcon(key: String): ImageVector {
        return when (key.lowercase()) {
            "flight" -> Icons.Default.FlightTakeoff
            "home" -> Icons.Default.Home
            "laptop" -> Icons.Default.LaptopMac
            "car" -> Icons.Default.DirectionsCar
            "shopping" -> Icons.Default.LocalMall
            "education" -> Icons.Default.School
            "savings" -> Icons.Default.Savings
            else -> Icons.Default.Savings // Fallback genérico E7.5
        }
    }

    /**
     * Mapea una clave de color predefinida a una tonalidad premium (Primary, Accent, etc.).
     */
    fun mapColor(key: String): Color {
        return when (key.lowercase()) {
            "blue" -> Color(0xFF1976D2)
            "rose" -> Color(0xFFC2185B)
            "purple" -> Color(0xFF7B1FA2)
            "orange" -> Color(0xFFF57C00)
            "green" -> Color(0xFF388E3C)
            else -> Color(0xFF6200EE) // Color primario por defecto
        }
    }

    /**
     * Retorna una lista con todos los iconos seleccionables disponibles.
     */
    fun getAvailableIcons(): List<Pair<String, ImageVector>> {
        return listOf(
            "flight" to Icons.Default.FlightTakeoff,
            "home" to Icons.Default.Home,
            "laptop" to Icons.Default.LaptopMac,
            "car" to Icons.Default.DirectionsCar,
            "shopping" to Icons.Default.LocalMall,
            "education" to Icons.Default.School,
            "savings" to Icons.Default.Savings
        )
    }

    /**
     * Retorna una lista con todos los colores seleccionables disponibles.
     */
    fun getAvailableColors(): List<Pair<String, Color>> {
        return listOf(
            "blue" to Color(0xFF1976D2),
            "rose" to Color(0xFFC2185B),
            "purple" to Color(0xFF7B1FA2),
            "orange" to Color(0xFFF57C00),
            "green" to Color(0xFF388E3C)
        )
    }
}
