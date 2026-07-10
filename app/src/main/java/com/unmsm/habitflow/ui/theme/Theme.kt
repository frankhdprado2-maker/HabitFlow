package com.unmsm.habitflow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
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

private val LightColorScheme = lightColorScheme(
    primary = ClayPurple,
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
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun HabitFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ClayShapes,
        typography = Typography,
        content = content
    )
}
