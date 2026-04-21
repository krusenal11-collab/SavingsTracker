package com.savings.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Primary        = Color(0xFF007AFF)
    val Success        = Color(0xFF34C759)
    val Warning        = Color(0xFFFF9500)
    val Purple         = Color(0xFFAF52DE)
    val TithePurple    = Color(0xFF5E35B1)   // Tithe screen accent
    val Pink           = Color(0xFFFF2D55)
    val LightBlue      = Color(0xFF5AC8FA)
    val Destructive    = Color(0xFFFF3B30)

    val HeaderStart    = Color(0xFF1B3A6B)
    val HeaderEnd      = Color(0xFF0D2A50)

    val SystemBg       = Color(0xFFF2F2F7)
    val CardBg         = Color(0xFFFFFFFF)
    val Separator      = Color(0xFFE5E5EA)
    val LabelPrimary   = Color(0xFF000000)
    val LabelSecondary = Color(0xFF8E8E93)

    private val accountPalette = listOf(Primary, Success, Warning, Purple, Pink, LightBlue)
    fun forAccount(id: Int): Color = accountPalette[id % accountPalette.size]
}

private val AppColorScheme = lightColorScheme(
    primary          = AppColors.Primary,
    onPrimary        = Color.White,
    primaryContainer = AppColors.Primary.copy(alpha = 0.12f),
    secondary        = AppColors.Success,
    background       = AppColors.SystemBg,
    surface          = AppColors.CardBg,
    onBackground     = AppColors.LabelPrimary,
    onSurface        = AppColors.LabelPrimary
)

@Composable
fun SavingsTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
