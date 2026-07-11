package com.unmsm.habitflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private fun darkHabitFlowColorScheme(accentColor: String) = darkColorScheme(
    primary = habitFlowAccentColor(accentColor, dark = true),
    onPrimary = HabitFlowColors.DarkBackground,
    primaryContainer = Color(0xFF184E44),
    onPrimaryContainer = Color(0xFFDDF8F1),
    secondary = HabitFlowColors.Coral,
    onSecondary = HabitFlowColors.DarkBackground,
    secondaryContainer = Color(0xFF52301C),
    onSecondaryContainer = Color(0xFFFFE4D0),
    tertiary = Color(0xFF9FC8E5),
    onTertiary = HabitFlowColors.DarkBackground,
    tertiaryContainer = Color(0xFF203B52),
    onTertiaryContainer = Color(0xFFD9E6EF),
    background = HabitFlowColors.DarkBackground,
    onBackground = HabitFlowColors.DarkInk,
    surface = HabitFlowColors.DarkSurface,
    onSurface = HabitFlowColors.DarkInk,
    surfaceVariant = HabitFlowColors.DarkSurfaceVariant,
    onSurfaceVariant = HabitFlowColors.DarkMutedInk,
    surfaceContainer = HabitFlowColors.DarkSurfaceContainer,
    outline = HabitFlowColors.DarkOutline,
    outlineVariant = HabitFlowColors.DarkOutline.copy(alpha = 0.6f),
    error = HabitFlowColors.DarkError,
    onError = HabitFlowColors.DarkBackground
)

private fun lightHabitFlowColorScheme(accentColor: String) = lightColorScheme(
    primary = habitFlowAccentColor(accentColor, dark = false),
    onPrimary = Color.White,
    primaryContainer = HabitFlowColors.MintContainer,
    onPrimaryContainer = Color(0xFF153F36),
    secondary = HabitFlowColors.Coral,
    onSecondary = Color.White,
    secondaryContainer = HabitFlowColors.CoralContainer,
    onSecondaryContainer = Color(0xFF4B2510),
    tertiary = HabitFlowColors.Ocean,
    onTertiary = Color.White,
    tertiaryContainer = HabitFlowColors.OceanContainer,
    onTertiaryContainer = Color(0xFF183044),
    background = HabitFlowColors.WarmBackground,
    onBackground = HabitFlowColors.Ink,
    surface = HabitFlowColors.WarmSurface,
    onSurface = HabitFlowColors.Ink,
    surfaceVariant = HabitFlowColors.WarmSurfaceVariant,
    onSurfaceVariant = HabitFlowColors.MutedInk,
    surfaceContainer = HabitFlowColors.WarmSurfaceContainer,
    outline = HabitFlowColors.SoftOutline,
    outlineVariant = HabitFlowColors.SoftOutline.copy(alpha = 0.55f),
    error = HabitFlowColors.Error,
    onError = Color.White
)

private val HabitFlowShapesMaterial = Shapes(
    extraSmall = RoundedCornerShape(HabitFlowShapes.Small),
    small = RoundedCornerShape(HabitFlowShapes.Small),
    medium = RoundedCornerShape(HabitFlowShapes.Medium),
    large = RoundedCornerShape(HabitFlowShapes.Large),
    extraLarge = RoundedCornerShape(HabitFlowShapes.XLarge)
)

@Composable
fun HabitFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: String = HabitFlowAccent.Mint.key,
    dynamicColor: Boolean = false,
    textScale: String = HabitFlowTextScale.Standard.key,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkHabitFlowColorScheme(accentColor)
        else -> lightHabitFlowColorScheme(accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = HabitFlowShapesMaterial,
        typography = habitFlowTypography(textScale),
        content = content
    )
}
