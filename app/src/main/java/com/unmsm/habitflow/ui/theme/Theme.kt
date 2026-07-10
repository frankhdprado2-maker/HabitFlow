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

private fun darkClayColorScheme(accentColor: String) = darkColorScheme(
    primary = accentFor(accentColor, dark = true),
    onPrimary = DarkBackground,
    primaryContainer = Color(0xFF3A2E76),
    secondary = ClayMint,
    tertiary = ClayCoral,
    background = Color(0xFF191629),
    surface = Color(0xFF26213A),
    surfaceVariant = Color(0xFF342E4C),
    onBackground = Color(0xFFF6F3FF),
    onSurface = Color(0xFFF6F3FF),
    onSurfaceVariant = Color(0xFFD8D0EA),
    outline = Color(0xFF6F668B)
)

private fun lightClayColorScheme(accentColor: String) = lightColorScheme(
    primary = accentFor(accentColor, dark = false),
    onPrimary = Color.White,
    primaryContainer = ClayLavender,
    onPrimaryContainer = ClayPurpleDeep,
    secondary = ClayMint,
    onSecondary = ClayInk,
    secondaryContainer = Color(0xFFDDF8F1),
    tertiary = ClayCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0E7),
    background = ClayBackground,
    surface = ClaySurface,
    surfaceVariant = Color(0xFFEDE7FF),
    onBackground = ClayInk,
    onSurface = ClayInk,
    onSurfaceVariant = Color(0xFF6F668B),
    outline = Color(0xFFCFC6E8),
    error = Color(0xFFD94F70)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val ClayShapes = Shapes(
    extraSmall = RoundedCornerShape(ClayTokens.Radius.Small),
    small = RoundedCornerShape(ClayTokens.Radius.Small),
    medium = RoundedCornerShape(ClayTokens.Radius.Medium),
    large = RoundedCornerShape(ClayTokens.Radius.Large),
    extraLarge = RoundedCornerShape(ClayTokens.Radius.XLarge)
)

@Composable
fun HabitFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: String = "violet",
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkClayColorScheme(accentColor)
        else -> lightClayColorScheme(accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ClayShapes,
        typography = Typography,
        content = content
    )
}

private fun accentFor(accentColor: String, dark: Boolean): Color =
    when (accentColor) {
        "mint" -> if (dark) Color(0xFF8CEAD9) else Color(0xFF208C7D)
        "coral" -> if (dark) Color(0xFFFFB0BE) else Color(0xFFD94F70)
        "amber" -> if (dark) Color(0xFFFFD97A) else Color(0xFFAA6B00)
        else -> if (dark) Purple80 else ClayPurple
    }
