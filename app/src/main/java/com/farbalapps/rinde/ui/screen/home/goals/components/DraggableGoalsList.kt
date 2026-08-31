package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.domain.model.SavingsGoal

/**
 * Manages ordered display of goals with intuitive Up/Down controls when reorder mode is active.
 */
@Composable
fun DraggableGoalsList(
    goals: List<SavingsGoal>,
    isDragEnabled: Boolean,
    onOrderChange: (List<SavingsGoal>) -> Unit,
    content: @Composable (
        goal: SavingsGoal,
        isDragging: Boolean,
        modifier: Modifier
    ) -> Unit
) {
    var currentList by remember(goals) { mutableStateOf(goals) }

    Column(modifier = Modifier.animateContentSize()) {
        currentList.forEachIndexed { index, goal ->
            key(goal.id) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        content(goal, false, Modifier)
                    }

                    AnimatedVisibility(
                        visible = isDragEnabled,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val mutable = currentList.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index - 1, item)
                                        currentList = mutable
                                        onOrderChange(mutable)
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Mover arriba",
                                    tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (index < currentList.lastIndex) {
                                        val mutable = currentList.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index + 1, item)
                                        currentList = mutable
                                        onOrderChange(mutable)
                                    }
                                },
                                enabled = index < currentList.lastIndex,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Mover abajo",
                                    tint = if (index < currentList.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
