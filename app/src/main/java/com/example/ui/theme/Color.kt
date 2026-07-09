package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// Obsidian Frosted Glass Backgrounds & Accent Colors
val DarkBg = Color(0xFF111318)
val DarkSurface = Color(0x0DFFFFFF) // Translates to white/5 (semi-translucent)
val DarkSurfaceVariant = Color(0x1AFFFFFF) // Translates to white/10 (semi-translucent active)
val GlassBorder = Color(0x1AFFFFFF) // Very thin white/10 border
val GlassBorderActive = Color(0x33FFFFFF) // Thicker white/20 border for highlights

// Accent Theme Colors from design spec (now dynamic!)
var LavenderAccent by mutableStateOf(Color(0xFFD0BCFF))
var DeepPurpleContainer by mutableStateOf(Color(0xFF381E72))

val LightBlueAccent = Color(0xFFA8C7FA)
val BrightAmber = Color(0xFFFFD740)

val DarkTextPrimary = Color(0xFFE2E2E6)
val DarkTextSecondary = Color(0xFFC4C6D0)

val HighPriorityRed = Color(0xFFF2B8B5)
val HighPriorityText = Color(0xFF601410)

// Presets for the accent colors
data class AccentPreset(
    val name: String,
    val accentHex: String,
    val containerHex: String
)

val AccentPresets = listOf(
    AccentPreset("Purple", "#D0BCFF", "#381E72"),
    AccentPreset("Blue", "#A8C7FA", "#0D47A1"),
    AccentPreset("Teal", "#4DD0E1", "#004D40"),
    AccentPreset("Green", "#81C784", "#1B5E20"),
    AccentPreset("Orange", "#FFFFB74D", "#E65100"),
    AccentPreset("Pink", "#FFFF80AB", "#880E4F"),
    AccentPreset("Red", "#E57373", "#B71C1C")
)

fun applyAccentPreset(presetName: String) {
    try {
        val preset = AccentPresets.find { it.name.equals(presetName, ignoreCase = true) } ?: AccentPresets[0]
        LavenderAccent = Color(android.graphics.Color.parseColor(preset.accentHex))
        DeepPurpleContainer = Color(android.graphics.Color.parseColor(preset.containerHex))
    } catch (e: Exception) {
        LavenderAccent = Color(0xFFD0BCFF)
        DeepPurpleContainer = Color(0xFF381E72)
    }
}

// Category Colors tuned for high contrast glass layers
val CategoryHealth = Color(0xFFA8C7FA)     // Light Blue
val CategoryWork = Color(0xFFD0BCFF)       // Lavender
val CategoryPersonal = Color(0xFFFFB74D)   // Soft Orange
val CategoryMind = Color(0xFFE040FB)       // Neon Purple
val CategoryCreative = Color(0xFFFF80AB)   // Soft Pink
val CategoryGeneral = Color(0xFFCFD8DC)    // Slate Grey

