package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

fun getLaboDarkColorScheme(accent: Color = LaboPrimarySlate) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    secondary = LaboPrimarySlate,
    onSecondary = Color.White,
    secondaryContainer = LaboDarkSurfaceElevated,
    onSecondaryContainer = LaboTextPrimaryDark,
    background = LaboDarkBackground,
    onBackground = LaboTextPrimaryDark,
    surface = LaboDarkSurface,
    onSurface = LaboTextPrimaryDark,
    surfaceVariant = LaboDarkSurfaceCard,
    onSurfaceVariant = LaboTextSecondaryDark,
    outline = LaboDarkBorder
)

fun getLaboLightColorScheme(accent: Color = LaboPrimarySlate) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.15f),
    onPrimaryContainer = LaboTextPrimaryLight,
    secondary = LaboPrimarySlate,
    onSecondary = Color.White,
    secondaryContainer = LaboLightSurfaceElevated,
    onSecondaryContainer = LaboTextPrimaryLight,
    background = LaboLightBackground,
    onBackground = LaboTextPrimaryLight,
    surface = LaboLightSurface,
    onSurface = LaboTextPrimaryLight,
    surfaceVariant = LaboLightSurfaceCard,
    onSurfaceVariant = LaboTextSecondaryLight,
    outline = LaboLightBorder
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: Color = LaboPrimarySlate,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> getLaboDarkColorScheme(accentColor)
        else -> getLaboLightColorScheme(accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
