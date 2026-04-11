package com.farbalapps.rinde.ui.screen.home.list.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.ShoppingItem as DomainShoppingItem
import com.farbalapps.rinde.ui.screen.home.list.toProductCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListItem(
    item: DomainShoppingItem,
    isHighlighted: Boolean = false,
    isSwiped: Boolean,
    onSwipeStateChange: (Boolean) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val uiCategory = item.category.toProductCategory()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { newValue ->
            when (newValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeStateChange(true)
                    true
                }
                SwipeToDismissBoxValue.Settled -> {
                    onSwipeStateChange(false)
                    true
                }
                else -> false
            }
        }
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSwiped) {
        if (!isSwiped && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    val elevation by animateDpAsState(
        targetValue = if (isHighlighted) 12.dp else 1.dp,
        animationSpec = if (isHighlighted) {
            infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(500)
        },
        label = "elevation"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        label = "color"
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            scope.launch { dismissState.reset() }
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.btn_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    IconButton(
                        onClick = { 
                            onDelete() 
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.action_delete),
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        content = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                shadowElevation = elevation,
                tonalElevation = elevation,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) 
                                    else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        val categoryText = if (uiCategory == com.farbalapps.rinde.ui.screen.home.list.ProductCategory.OTHERS) {
                            item.category
                        } else {
                            stringResource(id = uiCategory.displayNameRes)
                        }
                        
                        Text(
                            text = categoryText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
                                    else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingContent = {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (item.emoji.isNotEmpty()) {
                                    Text(item.emoji, fontSize = 32.sp)
                                } else {
                                    Icon(
                                        imageVector = uiCategory.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val quantityText = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                            Text(
                                text = "$quantityText ${item.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(onClick = onEdit) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(id = R.string.action_edit),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = onCheckedChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.scale(0.85f)
                            )
                        }
                    }
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ShoppingListItemPreview() {
    val sampleItem = DomainShoppingItem(
        id = "1",
        name = "Manzanas",
        category = "Frutas",
        quantity = 2.0,
        unit = "kg",
        isCompleted = false,
        emoji = "🍎"
    )
    MaterialTheme {
        ShoppingListItem(
            item = sampleItem,
            isSwiped = false,
            onSwipeStateChange = {},
            onCheckedChange = {},
            onEdit = {},
            onDelete = {}
        )
    }
}
