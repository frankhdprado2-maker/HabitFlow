package com.unmsm.habitflow.ui.theme

import androidx.compose.ui.unit.dp

object HabitFlowSpacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
}

object HabitFlowShapes {
    val Small = 12.dp
    val Medium = 18.dp
    val Large = 24.dp
    val XLarge = 28.dp
    val Pill = 50.dp
}

object HabitFlowElevation {
    val None = 0.dp
    val Soft = 2.dp
    val Floating = 8.dp
}

object HabitFlowIconSizes {
    val Small = 18.dp
    val Medium = 24.dp
    val Large = 32.dp
    val Orb = 72.dp
}

object HabitFlowMotion {
    const val Micro = 160
    const val Content = 260
    const val Celebration = 560
}

object HabitFlowDimensions {
    val ButtonHeight = 54.dp
    val FieldHeight = 56.dp
    val ProgressHeight = 10.dp
    val MinTouchTarget = 48.dp
    val NavigationHeight = 72.dp
}

object ClayTokens {
    object Radius {
        val Small = HabitFlowShapes.Small
        val Medium = HabitFlowShapes.Medium
        val Large = HabitFlowShapes.Large
        val XLarge = HabitFlowShapes.XLarge
    }

    object Elevation {
        val Resting = HabitFlowElevation.Soft
        val Pressed = HabitFlowElevation.None
        val Floating = HabitFlowElevation.Floating
    }

    object Spacing {
        val XSmall = HabitFlowSpacing.XSmall
        val Small = HabitFlowSpacing.Small
        val Medium = HabitFlowSpacing.Medium
        val Large = HabitFlowSpacing.Large
        val XLarge = HabitFlowSpacing.XLarge
    }

    object Size {
        val ButtonHeight = HabitFlowDimensions.ButtonHeight
        val IconTile = 46.dp
        val ProgressHeight = HabitFlowDimensions.ProgressHeight
    }
}
