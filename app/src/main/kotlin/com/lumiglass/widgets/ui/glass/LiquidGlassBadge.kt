package com.lumiglass.widgets.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Optical Material Chip designed for telemetry pills, status tags, and inner items.
 * Strictly adheres to Iron Rule 3 (no glass-on-glass):
 * Uses directional top-lit ambient reflection + specular rim highlight.
 */
@Composable
fun LiquidMaterialChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    shape: Shape = CircleShape,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    horizontalPadding: Dp = 11.dp,
    verticalPadding: Dp = 5.dp
) {
    val dark = isSystemInDarkTheme()

    val bgBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.55f else 0.85f),
                MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.35f else 0.65f)
            )
        )
    } else {
        if (dark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.88f),
                    Color(0xFFE2E8F0).copy(alpha = 0.65f)
                )
            )
        }
    }

    val rimBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.80f),
                Color.White.copy(alpha = 0.20f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (dark) 0.45f else 0.90f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }

    val textColor = if (isSelected) {
        if (dark) Color.White else MaterialTheme.colorScheme.onPrimary
    } else {
        if (dark) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurface
    }

    val effectiveIconTint = iconTint ?: if (isSelected) {
        if (dark) Color.White else MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.primary
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .then(clickModifier)
            .background(brush = bgBrush, shape = shape)
            .border(width = 0.8.dp, brush = rimBrush, shape = shape)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveIconTint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

