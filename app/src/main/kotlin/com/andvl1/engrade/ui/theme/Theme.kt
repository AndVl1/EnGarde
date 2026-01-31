package com.andvl1.engrade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = White,
    secondary = Secondary,
    onSecondary = Black,
    background = DarkGray,
    onBackground = White,
    surface = MediumGray,
    onSurface = White,
    error = Red,
    onError = White
)

private val AmoledColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = White,
    secondary = Secondary,
    onSecondary = White,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    error = Red,
    onError = White
)

@Composable
fun EnGardeTheme(
    useBlackBackground: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (useBlackBackground) AmoledColorScheme else DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
