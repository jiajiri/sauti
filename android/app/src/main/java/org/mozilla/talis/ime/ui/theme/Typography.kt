package com.sauti.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Roboto Typography Hierarchy for High-Precision Clinical Readability
val RobotoFont = FontFamily.SansSerif

val SautiTypographyLegacy = Typography(
    displayLarge = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
        color = TextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
        color = TextSecondary
    ),
    bodyLarge = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.25.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.25.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = TextMuted
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.0.sp,
        color = TextMuted
    )
)

