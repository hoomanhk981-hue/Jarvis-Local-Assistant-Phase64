package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassCardElevatedGradient
import com.example.ui.theme.GlassCardGradient
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightBorderVibrant
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.NeonPurplePrimary

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = LightBorder,
    isElevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundBrush = if (isElevated) GlassCardElevatedGradient else GlassCardGradient

    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = backgroundBrush)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .then(clickableModifier),
        content = content
    )
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(50.dp)
    val background = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(NeonPurplePrimary, NeonPurpleLight)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFFF3F4F6), Color(0xFFE5E7EB))
        )
    }

    val border = if (isSelected) LightBorderVibrant else LightBorder
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = background)
            .border(1.dp, border, shape)
            .then(clickModifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        content = content
    )
}
