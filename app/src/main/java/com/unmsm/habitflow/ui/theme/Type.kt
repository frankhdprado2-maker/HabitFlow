package com.unmsm.habitflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val HabitFlowFontFamily = FontFamily.Default

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = HabitFlowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = HabitFlowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HabitFlowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HabitFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HabitFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HabitFlowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HabitFlowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HabitFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HabitFlowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

enum class HabitFlowTextScale(val key: String, val label: String, val factor: Float) {
    Compact("compact", "Compacto", 0.92f),
    Standard("standard", "Estándar", 1f),
    Large("large", "Grande", 1.12f)
}

fun habitFlowTypography(scaleKey: String): Typography {
    val factor = HabitFlowTextScale.entries
        .firstOrNull { it.key == scaleKey }
        ?.factor
        ?: HabitFlowTextScale.Standard.factor
    if (factor == 1f) return Typography
    return Typography.copy(
        displaySmall = Typography.displaySmall.scaled(factor),
        headlineLarge = Typography.headlineLarge.scaled(factor),
        headlineMedium = Typography.headlineMedium.scaled(factor),
        titleLarge = Typography.titleLarge.scaled(factor),
        titleMedium = Typography.titleMedium.scaled(factor),
        bodyLarge = Typography.bodyLarge.scaled(factor),
        bodyMedium = Typography.bodyMedium.scaled(factor),
        labelLarge = Typography.labelLarge.scaled(factor),
        labelMedium = Typography.labelMedium.scaled(factor)
    )
}

private fun TextStyle.scaled(factor: Float): TextStyle = copy(
    fontSize = (fontSize.value * factor).sp,
    lineHeight = (lineHeight.value * factor).sp
)
