package com.music.spotui.ui.components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
    shape: Shape? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    this
        .defaultMinSize(minWidth = minTargetSize, minHeight = minTargetSize)
        .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
        .hoverable(interactionSource = interactionSource, enabled = enabled)
        .then(
            if (hoverHighlight && isHovered && enabled) {
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
