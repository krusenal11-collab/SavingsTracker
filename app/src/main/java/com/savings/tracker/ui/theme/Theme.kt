package com.savings.tracker.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Fallback palette (used on Android < 12)
private val FallbackColors = lightColorScheme(
    primary          = Color(0xFF1B6CA8),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFD0E4FF),
    secondary        = Color(0xFF2D9B5A),
    background       = Color(0xFFF8F9FA),
    surface          = Color.White
)

@Composable
fun SavingsTrackerTheme(content: @Composable () -> Unit) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Material You — pulls wallpaper colors automatically on Android 12+
        dynamicLightColorScheme(LocalContext.current)
    } else {
        FallbackColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content     = content
    )
}
