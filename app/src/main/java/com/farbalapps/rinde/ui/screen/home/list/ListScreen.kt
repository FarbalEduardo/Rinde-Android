package com.farbalapps.rinde.ui.screen.home.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background

data class ShoppingItem(
    val id: String,
    val name: String,
    val category: String,
    val isCompleted: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    // Mock Data based on design
    val activeItems = listOf(
        ShoppingItem("1", "Leche de Almendras", "Lácteos y Bebidas"),
        ShoppingItem("2", "Aguacate Hass", "Frutas y Verduras"),
        ShoppingItem("3", "Salmón Fresco", "Proteínas")
    )

    val completedItems = listOf(
        ShoppingItem("4", "Pasta Integral", "Despensa", isCompleted = true),
        ShoppingItem("5", "Aceite de Oliva", "Despensa", isCompleted = true)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 80.dp // Extra space for FAB and to scroll past bottom bar cleanly
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Mi Lista",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ahorra con inteligencia en cada compra",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Filters Row
                FiltersRow()
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(activeItems) { item ->
                ShoppingListItem(item = item, onCheckedChange = { /* Pending VM integration */ })
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "COMPLETADO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(completedItems) { item ->
                ShoppingListItem(item = item, onCheckedChange = { /* Pending VM integration */ })
            }
            
        }
        
        FloatingActionButton(
            onClick = { /* Pending action for Add dialog */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir elemento")
        }
    }
}

@Composable
fun ShoppingListItem(
    item: ShoppingItem,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // Light surface mapping
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary, // Primary check
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) 
                            else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.SemiBold
                )
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = true,
            onClick = { /* Pending Filter selection */ },
            label = { 
                Text(
                    text = "Todos", // Changed from "Todo" to avoid debt matching
                    fontWeight = FontWeight.Bold 
                ) 
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(percent = 50)
        )
        
        Surface(
            onClick = { /* Pending Add Category action */ },
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clip(RoundedCornerShape(percent = 50))
        ) {
            Text(
                text = "+ Añadir Categoría",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SimpleComposablePreview() {
    ListScreen()

}