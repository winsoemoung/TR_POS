package com.example.offlinepos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PosPrimaryDark,
    onPrimary = PosOnPrimaryDark,
    primaryContainer = PosPrimaryContainerDark,
    onPrimaryContainer = PosOnPrimaryContainerDark,
    secondary = PosSecondary,
    onSecondary = PosOnSecondary,
    secondaryContainer = PosSecondaryContainer,
    onSecondaryContainer = PosOnSecondaryContainer,
    tertiary = PosTertiary,
    background = PosBackgroundDark,
    surface = PosSurfaceDark,
    surfaceVariant = PosSurfaceVariantDark,
    onBackground = PosOnSurfaceDark,
    onSurface = PosOnSurfaceDark,
    onSurfaceVariant = PosOnSurfaceVariantDark,
    outline = PosOutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PosPrimary,
    onPrimary = PosOnPrimary,
    primaryContainer = PosPrimaryContainer,
    onPrimaryContainer = PosOnPrimaryContainer,
    secondary = PosSecondary,
    onSecondary = PosOnSecondary,
    secondaryContainer = PosSecondaryContainer,
    onSecondaryContainer = PosOnSecondaryContainer,
    tertiary = PosTertiary,
    background = PosBackground,
    surface = PosSurface,
    surfaceVariant = PosSurfaceVariant,
    onBackground = PosOnSurface,
    onSurface = PosOnSurface,
    onSurfaceVariant = PosOnSurfaceVariant,
    outline = PosOutline,
)

// Apple iOS Cupertino Light Theme
private val IosLightColorScheme = lightColorScheme(
    primary = CupertinoColors.SystemBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF0040DD),
    secondary = CupertinoColors.SystemIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAE9FC),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = CupertinoColors.SystemTeal,
    background = CupertinoColors.LightScreenBg,
    surface = CupertinoColors.LightCardBg,
    surfaceVariant = CupertinoColors.LightSecondaryBg,
    onBackground = CupertinoColors.LightLabel,
    onSurface = CupertinoColors.LightLabel,
    onSurfaceVariant = Color(0xFF636366),
    outline = CupertinoColors.LightSeparator,
)

// Apple iOS Cupertino Dark Theme
private val IosDarkColorScheme = darkColorScheme(
    primary = CupertinoColors.SystemBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = CupertinoColors.SystemIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3730A3),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = CupertinoColors.SystemTeal,
    background = CupertinoColors.DarkScreenBg,
    surface = CupertinoColors.DarkCardBg,
    surfaceVariant = CupertinoColors.DarkSecondaryBg,
    onBackground = CupertinoColors.DarkLabel,
    onSurface = CupertinoColors.DarkLabel,
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = CupertinoColors.DarkSeparator,
)

@Composable
fun OfflinePOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isCupertino: Boolean = false,
    dynamicColor: Boolean = false, // Keep branded consistent colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isCupertino && darkTheme -> IosDarkColorScheme
        isCupertino && !darkTheme -> IosLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalIsCupertinoTheme provides isCupertino
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}