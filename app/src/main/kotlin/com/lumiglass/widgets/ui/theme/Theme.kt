package com.lumiglass.widgets.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lumiglass.widgets.model.GlassThemeMode

private val LumiDarkColorScheme = darkColorScheme(
    primary = LumiElectricCyan,
    onPrimary = Color(0xFF001E2F),
    primaryContainer = Color(0xFF0A2B47),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = LumiPrismaticAzure,
    onSecondary = Color(0xFF0A1E3C),
    secondaryContainer = Color(0xFF132B4F),
    onSecondaryContainer = Color(0xFFDCE8FD),
    tertiary = LumiAuroraViolet,
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = DarkGlassBorder
)

private val LumiLightColorScheme = lightColorScheme(
    primary = LumiDeepCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF082F49),
    secondary = LumiPrismaticAzure,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF7C3AED),
    background = LightBackground,
    onBackground = Color(0xFF0A1424),
    surface = LightSurface,
    onSurface = Color(0xFF0A1424),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF334155),
    outline = LightGlassBorder
)

@Composable
fun LumiGlassTheme(
    themeMode: GlassThemeMode = GlassThemeMode.AUTO,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        GlassThemeMode.DARK -> true
        GlassThemeMode.LIGHT -> false
        GlassThemeMode.AUTO -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> LumiDarkColorScheme
        else -> LumiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
