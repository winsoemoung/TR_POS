package com.example.offlinepos.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CompositionLocal providing whether Apple iOS Cupertino theme is currently active.
 */
val LocalIsCupertinoTheme = compositionLocalOf { false }

/**
 * Official Apple iOS / Cupertino Palette
 */
object CupertinoColors {
    // System Accents
    val SystemBlue = Color(0xFF007AFF)
    val SystemGreen = Color(0xFF34C759)
    val SystemIndigo = Color(0xFF5856D6)
    val SystemOrange = Color(0xFFFF9500)
    val SystemPink = Color(0xFFFF2D55)
    val SystemPurple = Color(0xFFAF52DE)
    val SystemRed = Color(0xFFFF3B30)
    val SystemTeal = Color(0xFF5AC8FA)
    val SystemYellow = Color(0xFFFFCC00)
    val SystemGray = Color(0xFF8E8E93)

    // Light Mode Backgrounds & Surfaces
    val LightScreenBg = Color(0xFFF2F2F7) // Grouped background
    val LightCardBg = Color(0xFFFFFFFF)
    val LightSecondaryBg = Color(0xFFE5E5EA)
    val LightSeparator = Color(0xFFC6C6C8)
    val LightLabel = Color(0xFF000000)
    val LightSecondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.6f)
    val LightTertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.3f)

    // Dark Mode Backgrounds & Surfaces
    val DarkScreenBg = Color(0xFF000000)
    val DarkCardBg = Color(0xFF1C1C1E)
    val DarkSecondaryBg = Color(0xFF2C2C2E)
    val DarkSeparator = Color(0xFF38383A)
    val DarkLabel = Color(0xFFFFFFFF)
    val DarkSecondaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.6f)
    val DarkTertiaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.3f)
}

/**
 * Cupertino-styled Rounded Shapes (Apple continuous corner style)
 */
object CupertinoShapes {
    val Small = RoundedCornerShape(10.dp)
    val Medium = RoundedCornerShape(14.dp)
    val Large = RoundedCornerShape(18.dp)
    val GroupedCard = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(50)
}

/**
 * iOS Cupertino Grouped Section / Inset Card
 */
@Composable
fun CupertinoGroupedCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.red < 0.2f,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardBg = if (isDark) CupertinoColors.DarkCardBg else CupertinoColors.LightCardBg
    val borderColor = if (isDark) CupertinoColors.DarkSeparator else Color(0xFFE5E5EA)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(CupertinoShapes.GroupedCard)
            .border(0.75.dp, borderColor, CupertinoShapes.GroupedCard),
        color = cardBg,
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

/**
 * iOS-styled List Item with optional Leading Icon and Trailing Chevron
 */
@Composable
fun CupertinoListItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = CupertinoColors.SystemBlue,
    iconBgColor: Color = iconTint.copy(alpha = 0.15f),
    trailingContent: @Composable (() -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val titleColor = if (isDark) CupertinoColors.DarkLabel else CupertinoColors.LightLabel
    val subColor = if (isDark) CupertinoColors.DarkSecondaryLabel else CupertinoColors.LightSecondaryLabel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = subColor
                    )
                }
            }
        }

        if (trailingContent != null) {
            trailingContent()
        } else if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CupertinoColors.SystemGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * iOS-styled Action Button with smooth tactile bounce
 */
@Composable
fun CupertinoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = CupertinoColors.SystemBlue,
    contentColor: Color = Color.White,
    isDestructive: Boolean = false,
    shape: Shape = CupertinoShapes.Button
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1.0f, label = "button_scale")

    val finalBg = when {
        !enabled -> Color(0xFFD1D1D6)
        isDestructive -> CupertinoColors.SystemRed
        else -> backgroundColor
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(finalBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) contentColor else Color.DarkGray
        )
    }
}
