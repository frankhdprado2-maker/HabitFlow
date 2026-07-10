package com.unmsm.habitflow.ui.theme

import androidx.compose.ui.graphics.Color

object HabitFlowColors {
    val WarmBackground = Color(0xFFF7F3E8)
    val WarmSurface = Color(0xFFFFFFFF)
    val WarmSurfaceVariant = Color(0xFFF0EBDD)
    val WarmSurfaceContainer = Color(0xFFFAF7EF)
    val MintPrimary = Color(0xFF2F7D6D)
    val MintPrimaryDark = Color(0xFF7BD0B8)
    val MintContainer = Color(0xFFDDF3EA)
    val Coral = Color(0xFFF59E5C)
    val CoralContainer = Color(0xFFFFE4D0)
    val Amber = Color(0xFFE2A226)
    val AmberContainer = Color(0xFFFFECC1)
    val Ocean = Color(0xFF3E5C76)
    val OceanContainer = Color(0xFFD9E6EF)
    val Violet = Color(0xFF7562B8)
    val VioletContainer = Color(0xFFE8E1FF)
    val Ink = Color(0xFF1F2A2E)
    val MutedInk = Color(0xFF66706C)
    val SoftOutline = Color(0xFFD8D2C4)
    val Error = Color(0xFFB84E45)

    val DarkBackground = Color(0xFF101715)
    val DarkSurface = Color(0xFF18211E)
    val DarkSurfaceVariant = Color(0xFF22302B)
    val DarkSurfaceContainer = Color(0xFF1D2925)
    val DarkInk = Color(0xFFF4F7F5)
    val DarkMutedInk = Color(0xFFB8C4BF)
    val DarkOutline = Color(0xFF42524B)
    val DarkError = Color(0xFFFFB4AB)
}

enum class HabitFlowAccent(val key: String, val label: String) {
    Mint("mint", "Menta"),
    Coral("coral", "Coral"),
    Amber("amber", "Ámbar"),
    Ocean("ocean", "Océano"),
    Violet("violet", "Violeta")
}

fun habitFlowAccentColor(key: String, dark: Boolean): Color =
    when (key) {
        HabitFlowAccent.Coral.key -> if (dark) Color(0xFFFFB36B) else HabitFlowColors.Coral
        HabitFlowAccent.Amber.key -> if (dark) Color(0xFFFFD47A) else HabitFlowColors.Amber
        HabitFlowAccent.Ocean.key -> if (dark) Color(0xFF9FC8E5) else HabitFlowColors.Ocean
        HabitFlowAccent.Violet.key -> if (dark) Color(0xFFC9BCFF) else HabitFlowColors.Violet
        else -> if (dark) HabitFlowColors.MintPrimaryDark else HabitFlowColors.MintPrimary
    }

val Purple80 = HabitFlowColors.VioletContainer
val PurpleGrey80 = Color(0xFFD8D0EA)
val Pink80 = HabitFlowColors.CoralContainer
val Purple40 = HabitFlowColors.Violet
val PurpleGrey40 = HabitFlowColors.MutedInk
val Pink40 = HabitFlowColors.Error
val DarkBackground = HabitFlowColors.DarkBackground
val DarkSurface = HabitFlowColors.DarkSurface
val Mint = HabitFlowColors.MintPrimary
val Amber = HabitFlowColors.Amber

val ClayPurple = HabitFlowColors.Violet
val ClayPurpleDeep = Color(0xFF4F3B91)
val ClayLavender = HabitFlowColors.VioletContainer
val ClayBackground = HabitFlowColors.WarmBackground
val ClaySurface = HabitFlowColors.WarmSurface
val ClayMint = HabitFlowColors.MintPrimary
val ClayCoral = HabitFlowColors.Coral
val ClayAmber = HabitFlowColors.Amber
val ClayInk = HabitFlowColors.Ink
