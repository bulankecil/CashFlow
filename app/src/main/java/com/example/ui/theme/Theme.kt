package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.local.ThemeMode

@Immutable
data class FintekExtendedColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroAccent: Color,
    val cardBorder: Color,
    val subtleSurface: Color
)

val LocalFintekColors = staticCompositionLocalOf {
    FintekExtendedColors(
        income = Emerald600,
        incomeContainer = Emerald100,
        expense = CoralRedLight,
        expenseContainer = CoralContainerLight,
        transfer = SkyTransferLight,
        transferContainer = SkyContainerLight,
        heroStart = Navy900,
        heroEnd = Navy800,
        heroAccent = Emerald500,
        cardBorder = Slate200,
        subtleSurface = Slate100
    )
}

private val LightExtendedColors = FintekExtendedColors(
    income = Emerald600,
    incomeContainer = Emerald100,
    expense = CoralRedLight,
    expenseContainer = CoralContainerLight,
    transfer = SkyTransferLight,
    transferContainer = SkyContainerLight,
    heroStart = Navy900,
    heroEnd = Color(0xFF162440),
    heroAccent = Emerald400,
    cardBorder = Slate200,
    subtleSurface = Slate100
)

private val DarkExtendedColors = FintekExtendedColors(
    income = Emerald400,
    incomeContainer = Color(0xFF0A3329),
    expense = CoralRedDark,
    expenseContainer = CoralContainerDark,
    transfer = SkyTransferDark,
    transferContainer = SkyContainerDark,
    heroStart = Color(0xFF0D182D),
    heroEnd = Color(0xFF132544),
    heroAccent = Emerald400,
    cardBorder = Color(0xFF22314F),
    subtleSurface = Color(0xFF17223B)
)

private val FintekLightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald950,
    secondary = Navy900,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Navy900,
    tertiary = SkyTransferLight,
    onTertiary = Color.White,
    tertiaryContainer = SkyContainerLight,
    onTertiaryContainer = Navy900,
    error = CoralRedLight,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = Color(0xFF7F1D1D),
    background = Slate50,
    onBackground = Navy900,
    surface = Color.White,
    onSurface = Navy900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Navy600,
    outline = Slate300,
    outlineVariant = Slate200
)

private val FintekDarkColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Emerald950,
    primaryContainer = Color(0xFF093B2E),
    onPrimaryContainer = Emerald100,
    secondary = Slate200,
    onSecondary = Navy950,
    secondaryContainer = Navy800,
    onSecondaryContainer = Slate100,
    tertiary = SkyTransferDark,
    onTertiary = Navy950,
    tertiaryContainer = SkyContainerDark,
    onTertiaryContainer = Slate100,
    error = CoralRedDark,
    onError = Color(0xFF450A0A),
    errorContainer = CoralContainerDark,
    onErrorContainer = Color(0xFFFECACA),
    background = Navy950,
    onBackground = Slate100,
    surface = Navy850,
    onSurface = Slate100,
    surfaceVariant = Navy800,
    onSurfaceVariant = Slate400,
    outline = Navy700,
    outlineVariant = Color(0xFF1F2D49)
)

val FintekShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FintekTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    forceDarkSystemBars: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) FintekDarkColorScheme else FintekLightColorScheme
    val extendedColors = if (useDarkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        val lightSystemBars = if (forceDarkSystemBars) false else !useDarkTheme
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightSystemBars
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = lightSystemBars
            }
        }
    }

    CompositionLocalProvider(LocalFintekColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FintekTypography,
            shapes = FintekShapes,
            content = content
        )
    }
}
