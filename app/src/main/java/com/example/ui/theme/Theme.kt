package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Forced dark theme
    dynamicColor: Boolean = false, // Disabled to maintain our highly-polished custom look
    content: @Composable () -> Unit,
) {
    val dynamicColorScheme = darkColorScheme(
        primary = LavenderAccent,
        onPrimary = Color.Black,
        primaryContainer = DeepPurpleContainer,
        onPrimaryContainer = LavenderAccent,
        secondary = LightBlueAccent,
        tertiary = BrightAmber,
        background = DarkBg,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkTextSecondary
    )

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}
