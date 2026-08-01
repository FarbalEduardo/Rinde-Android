package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.farbalapps.rinde.domain.model.SavingsGoal
import kotlin.math.roundToInt

/**
 * Wraps a list of goals with native Compose Drag & Drop support.
 * When [isDragEnabled] is true, long-pressing a card activates drag mode.
 * On drop, [onOrderChange] is called with the new ordered list.
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
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var currentList by remember(goals) { mutableStateOf(goals) }
    val density = LocalDensity.current
    val spacingPx = with(density) { 16.dp.toPx() }

    Column {
        currentList.forEachIndexed { index, goal ->
            key(goal.id) {
                val isDragging = index == draggedIndex
                val animatedElevation by animateDpAsState(
                    targetValue = if (isDragging) 12.dp else 0.dp,
                    label = "elevation_$index"
                )
                val animatedAlpha by animateFloatAsState(
                    targetValue = if (isDragging) 0.92f else 1f,
                    label = "alpha_$index"
                )
                val animatedScale by animateFloatAsState(
                    targetValue = if (isDragging) 1.04f else 1f,
                    label = "scale_$index"
                )

                val dragModifier = if (isDragEnabled) {
                    Modifier
                        .zIndex(if (isDragging) 999f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else 0f
                            scaleX = animatedScale
                            scaleY = animatedScale
                            alpha = animatedAlpha
                        }
                        .shadow(
                            elevation = animatedElevation,
                            shape = RoundedCornerShape(24.dp),
                            clip = false
                        )
                        .pointerInput(index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y

                                    val cardHeightPx = size.height.toFloat()
                                    val stepPx = cardHeightPx + spacingPx

                                    if (stepPx > 0) {
                                        val deltaIndex = (dragOffsetY / stepPx).roundToInt()
                                        val targetIndex = (draggedIndex + deltaIndex).coerceIn(0, currentList.lastIndex)

                                        if (targetIndex != draggedIndex) {
                                            val shift = targetIndex - draggedIndex
                                            currentList = currentList.toMutableList().apply {
                                                add(targetIndex, removeAt(draggedIndex))
                                            }
                                            dragOffsetY -= shift * stepPx
                                            draggedIndex = targetIndex
                                            onOrderChange(currentList)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                    onOrderChange(currentList)
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                }
                            )
                        }
                } else {
                    Modifier
                }

                content(goal, isDragging, dragModifier)
            }
        }
    }
}
