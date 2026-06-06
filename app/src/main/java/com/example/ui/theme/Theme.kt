package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ClockDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = MidnightBg,
    secondary = NeonAmber,
    onSecondary = MidnightBg,
    tertiary = NeonCoral,
    onTertiary = MidnightBg,
    background = MidnightBg,
    surface = MidnightSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = GridLines
)

// Force beautiful dark cosmic theme by default for a clock application!
@Composable
fun ClockTheme(
    darkTheme: Boolean = true, // Force gorgeous dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = ClockDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
