package com.videodownloader.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videodownloader.app.ui.theme.GlassAcrylicBase
import com.videodownloader.app.ui.theme.GlassAcrylicElevated
import com.videodownloader.app.ui.theme.GlassBackgroundDark
import com.videodownloader.app.ui.theme.GlassBorderEnd
import com.videodownloader.app.ui.theme.GlassBorderStart
import com.videodownloader.app.ui.theme.NeonAccentBrush
import com.videodownloader.app.ui.theme.NeonCyan
import com.videodownloader.app.ui.theme.NeonPink
import com.videodownloader.app.ui.theme.NeonViolet
import com.videodownloader.app.ui.theme.TextPrimary
import com.videodownloader.app.ui.theme.TextSecondary

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF14151F), // Dark obsidian
                        Color(0xFF0C0D12), // Deep charcoal black
                        Color(0xFF07080B)  // Pitch dark
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderBrush: Brush = Brush.linearGradient(listOf(GlassBorderStart, GlassBorderEnd)),
    borderWidth: Dp = 1.dp,
    acrylicColor: Color = GlassAcrylicBase,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        Modifier.clip(shape)
    }

    Box(
        modifier = modifier
            .then(clickableModifier)
            .background(acrylicColor)
            .border(borderWidth, borderBrush, shape)
    ) {
        // Specular highlight gradient at the top edge of the card
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x35FFFFFF),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 40.dp.toPx()
                ),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
            )
        }
        content()
    }
}

// The signature NovaGet Frosted Glass styling (matches top header logo pill)
val NovaPillBackgroundBrush = Brush.horizontalGradient(
    listOf(
        Color(0x28FFFFFF),
        Color(0x12FFFFFF)
    )
)

val NovaPillBorderBrush = Brush.linearGradient(
    listOf(
        Color(0x60FFFFFF),
        Color(0x20FFFFFF)
    )
)

val NovaAccentPurple = Color(0xFFA855F7)
val NovaAccentPurpleSoft = Color(0xFFC084FC)

// Primary elevated action button brush (frosted glass with subtle NovaGet violet glow)
val NovaElevatedButtonBrush = Brush.horizontalGradient(
    listOf(
        Color(0x408B5CF6),
        Color(0x25A855F7),
        Color(0x18FFFFFF)
    )
)

val NovaElevatedButtonBorderBrush = Brush.linearGradient(
    listOf(
        Color(0x80FFFFFF),
        Color(0x40A855F7),
        Color(0x20FFFFFF)
    )
)

val FrostedGlassButtonBrush = NovaPillBackgroundBrush
val FrostedGlassBorderBrush = NovaPillBorderBrush

@Composable
fun NovaPill(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = NovaAccentPurple,
    textColor: Color = TextPrimary,
    backgroundBrush: Brush = NovaPillBackgroundBrush,
    borderBrush: Brush = NovaPillBorderBrush,
    shape: Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Modifier
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    } else {
        Modifier.clip(shape)
    }

    Box(
        modifier = modifier
            .then(clickModifier)
            .background(backgroundBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    brush: Brush = NovaPillBackgroundBrush,
    borderBrush: Brush = NovaPillBorderBrush,
    contentColor: Color = TextPrimary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .border(
                    width = 1.dp,
                    brush = borderBrush,
                    shape = shape
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor.copy(alpha = alpha)) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    leadingIcon: ImageVector? = null,
    iconTint: Color = NovaAccentPurple,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    brush: Brush = NovaPillBackgroundBrush,
    borderBrush: Brush = NovaPillBorderBrush,
    contentColor: Color = TextPrimary,
    testTag: String? = null
) {
    GlassButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        brush = brush,
        borderBrush = borderBrush,
        contentColor = contentColor
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = NovaAccentPurple,
    backgroundColor: Color = Color(0x20FFFFFF),
    borderBrush: Brush = NovaPillBorderBrush,
    size: Dp = 42.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderBrush, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(NovaAccentPurple),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF23242B),
                                Color(0xFF191A20)
                            )
                        )
                    )
                    .border(
                        width = 1.1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0x50FFFFFF),
                                Color(0x1AFFFFFF)
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = NovaAccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        trailingIcon()
                    }
                }
            }
        }
    )
}

/**
 * Stadium Capsule Search Bar matching the sleek ChatGPT text input capsule
 * from IMG_20260925_093355.jpg:
 * - Stadium Pill / Capsule shape (32dp corners)
 * - Matte dark charcoal glass finish
 * - Leading icon on the left
 * - Inner input field with clean typography
 * - Vibrant circular action button on the right (like the circular action button in ChatGPT)
 */
@Composable
fun StadiumCapsuleSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onAnalyzeClick: () -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Paste video URL (YouTube, Insta, X...)",
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    onClearClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF22232A),
                        Color(0xFF191A20)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0x55FFFFFF),
                        Color(0x1EFFFFFF)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon (Clean icon on the left)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { onPasteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon ?: NovaIcons.ContentPaste,
                    contentDescription = "Paste",
                    tint = Color(0xFFD1D5DB),
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Inner Input Text Field
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(NovaAccentPurple),
                    singleLine = true
                )
            }

            // Trailing Actions (Clear button or Paste Pill + Circular Action button)
            if (value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { onClearClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1EFFFFFF))
                        .clickable { onPasteClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Paste",
                        color = Color(0xFFE5E7EB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Circular Action Button (Vibrant purple circle - matches the circular action button in ChatGPT UI)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFA855F7), // Nova Accent Purple
                                Color(0xFF7C3AED)  // Deep Royal Violet
                            )
                        )
                    )
                    .border(1.dp, Color(0x60FFFFFF), CircleShape)
                    .clickable(enabled = !isLoading && value.isNotBlank()) { onAnalyzeClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = NovaIcons.Download,
                        contentDescription = "Analyze & Download",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = NovaAccentPurple,
    color: Color = NovaAccentPurple,
    textColor: Color = TextPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(NovaPillBackgroundBrush)
            .border(1.dp, NovaPillBorderBrush, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = Color(0x20FFFFFF),
    progressBrush: Brush = Brush.horizontalGradient(
        listOf(
            Color(0xFFA855F7),
            Color(0xFFC084FC),
            Color(0xFFE2E8F0)
        )
    )
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(progressBrush)
        )
    }
}

/**
 * Custom glass-morphism style progress indicator with an animated specular glass effect
 */
@Composable
fun GlassLinearProgressIndicator(
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = Color(0x1E0F172A),
    borderBrush: Brush = NovaPillBorderBrush,
    glowColors: List<Color> = listOf(
        Color(0x40FFFFFF),
        Color(0xFFA855F7),
        Color(0xFFC084FC),
        Color(0x70FFFFFF)
    )
) {
    val infiniteTransition = rememberInfiniteTransition()
    val sweepPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val shape = RoundedCornerShape(height / 2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
            .border(1.dp, borderBrush, shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val heightPx = size.height

            val beamLength = width * 0.45f
            val startX = (width * sweepPosition) - (beamLength / 2f)
            val endX = startX + beamLength

            val sweepBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    glowColors[0].copy(alpha = 0.35f * pulseGlow),
                    glowColors[0].copy(alpha = 0.9f * pulseGlow),
                    glowColors[1].copy(alpha = 0.95f),
                    glowColors[2].copy(alpha = 0.9f * pulseGlow),
                    glowColors[3].copy(alpha = 0.4f * pulseGlow),
                    Color.Transparent
                ),
                startX = startX,
                endX = endX
            )

            drawRoundRect(
                brush = sweepBrush,
                size = Size(width, heightPx),
                cornerRadius = CornerRadius(heightPx / 2f, heightPx / 2f)
            )

            // Specular glass highlight on top half of the capsule
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x70FFFFFF),
                        Color(0x05FFFFFF)
                    ),
                    startY = 0f,
                    endY = heightPx * 0.55f
                ),
                size = Size(width, heightPx * 0.55f),
                cornerRadius = CornerRadius(heightPx / 2f, heightPx / 2f)
            )
        }
    }
}
