package com.newdubai.shop.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Atelier collection: oxblood, warm ivory and brushed copper. */
// NEW DUBAI Premium palette: deep wine, brushed gold and warm porcelain.
val Lapis = Color(0xFF64243A)
val LapisDeep = Color(0xFF1D1118)
val Saffron = Color(0xFFD2A36F)
val Emerald = Color(0xFF397C68)
val Ruby = Color(0xFFB94B58)
val Champagne = Color(0xFFF4E9DC)

private val Dark = darkColorScheme(
    primary = Color(0xFFE0B27D),
    onPrimary = Color(0xFF29170F),
    primaryContainer = Color(0xFF573323),
    onPrimaryContainer = Color(0xFFFFE4CC),
    secondary = Color(0xFFCB9870),
    onSecondary = Color(0xFF351521),
    secondaryContainer = Color(0xFF552A3A),
    onSecondaryContainer = Color(0xFFFFD9E5),
    tertiary = Color(0xFF78B79F),
    onTertiary = Color(0xFF09291E),
    background = Color(0xFF171116),
    onBackground = Color(0xFFF5ECE5),
    surface = Color(0xFF261A21),
    onSurface = Color(0xFFF5ECE5),
    surfaceVariant = Color(0xFF382832),
    onSurfaceVariant = Color(0xFFD1BDC4),
    outline = Color(0xFF654B57),
    error = Ruby
)

private val Light = lightColorScheme(
    primary = Color(0xFF68283F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0DDE2),
    onPrimaryContainer = Color(0xFF45182A),
    secondary = Color(0xFFAD784E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E1D1),
    onSecondaryContainer = Color(0xFF4A2816),
    tertiary = Color(0xFF397C68),
    onTertiary = Color.White,
    background = Color(0xFFF5EFE8),
    onBackground = Color(0xFF2C2025),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF2C2025),
    surfaceVariant = Color(0xFFECE1D9),
    onSurfaceVariant = Color(0xFF6D5C62),
    outline = Color(0xFFD5C3B8),
    error = Color(0xFFB53F50)
)

@Composable
fun NewDubaiTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(9.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(27.dp),
            extraLarge = RoundedCornerShape(34.dp)
        ),
        content = content
    )
}
