package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeajaoDarkColorScheme = darkColorScheme(
    primary = BrassGold,
    onPrimary = CadetNavyDeep,
    primaryContainer = BrassGoldContainer,
    onPrimaryContainer = BrassGoldBright,
    secondary = CyanTactical,
    onSecondary = CadetNavyDeep,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanTactical,
    tertiary = EmeraldVerified,
    onTertiary = CadetNavyDeep,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldVerified,
    background = CadetNavyDeep,
    onBackground = AlabasterText,
    surface = CadetNavySurface,
    onSurface = AlabasterText,
    surfaceVariant = CadetNavyCard,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder,
    error = CrimsonHonor,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonHonor
)

private val GeajaoLightColorScheme = lightColorScheme(
    primary = LightNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4F5),
    onPrimaryContainer = LightNavyPrimary,
    secondary = LightBrassSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDF3C7),
    onSecondaryContainer = Color(0xFF5C4300),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF0F3D39),
    background = LightParchmentBg,
    onBackground = LightNavyPrimary,
    surface = LightSurface,
    onSurface = LightNavyPrimary,
    surfaceVariant = Color(0xFFE7ECF5),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

object ThemePreferences {
    private const val PREFS_NAME = "geajao_theme_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode_enabled"

    private val _isDarkModeFlow = kotlinx.coroutines.flow.MutableStateFlow(true)
    val isDarkModeFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isDarkModeFlow

    fun init(context: android.content.Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            _isDarkModeFlow.value = prefs.getBoolean(KEY_DARK_MODE, true)
        } catch (_: Exception) {
            // Default to dark mode in headless test environments
        }
    }

    fun setDarkMode(context: android.content.Context, isDark: Boolean) {
        _isDarkModeFlow.value = isDark
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
        } catch (_: Exception) {
        }
    }

    fun toggleDarkMode(context: android.content.Context): Boolean {
        val next = !_isDarkModeFlow.value
        setDarkMode(context, next)
        return next
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GeajaoDarkColorScheme else GeajaoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

