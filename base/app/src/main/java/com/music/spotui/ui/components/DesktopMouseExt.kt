package com.music.spotui.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard mouse & touch interaction modifier.
 * - Provides desktop mouse pointer (hand pointer).
 * - Enforces minimum 40-48dp touch target without blowing up visible icon dimensions.
 * - Adds a subtle hover overlay highlight if desired.
 */
fun Modifier.desktopClickable(
    minTargetSize: Dp = 44.dp,
    hoverHighlight: Boolean = false,
    hoverColor: Color = Color.White.copy(alpha = 0.08f),
    focusBorderColor: Color = Color.White.copy(alpha = 0.40f),
    shape: Shape? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = com.music.spotui.ui.look.rememberReduceMotion()

    val scale by animateFloatAsState(
        targetValue = when {
            reduceMotion -> 1f
            isPressed -> 0.94f
            isHovered || isFocused -> 1.04f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "desktopClickScale",
    )

    this
        .defaultMinSize(minWidth = minTargetSize, minHeight = minTargetSize)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
        .hoverable(interactionSource = interactionSource, enabled = enabled)
        .focusable(interactionSource = interactionSource, enabled = enabled)
        .then(
            if (isFocused && enabled) {
                Modifier.drawBehind {
                    val stroke = 2.dp.toPx()
                    if (shape != null) {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        drawOutline(outline, color = focusBorderColor, style = Stroke(width = stroke))
                    } else {
                        drawRect(color = focusBorderColor, style = Stroke(width = stroke))
                    }
                }
            } else if (hoverHighlight && isHovered && enabled) {
                Modifier.drawBehind {
                    if (shape != null) {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        drawOutline(outline, color = hoverColor)
                    } else {
                        drawRect(color = hoverColor)
                    }
                }
            } else Modifier
        )
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
}

/**
 * Hand pointer for interactive elements where clickable is managed internally (e.g. Buttons, Tabs).
 */
fun Modifier.handPointer(enabled: Boolean = true): Modifier =
    if (enabled) this.pointerHoverIcon(PointerIcon.Hand) else this
