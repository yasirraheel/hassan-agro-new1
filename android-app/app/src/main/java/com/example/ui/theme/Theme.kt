package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.local.ThemeAppearanceMode

/**
 * Adaptive semantic colors that maintain crisp contrast in both Light and Dark modes
 * so status chips, P&L indicators, and warning banners never wash out or blend into surfaces.
 */
data class AppSemanticColors(
    val positiveFg: Color,
    val positiveBg: Color,
    val warningFg: Color,
    val warningBg: Color,
    val negativeFg: Color,
    val negativeBg: Color,
    val isDark: Boolean
)

private val LightSemanticColors = AppSemanticColors(
    positiveFg = Color(0xFF157A3E),
    positiveBg = Color(0xFFE3F5E9),
    warningFg = Color(0xFFB4630E),
    warningBg = Color(0xFFFFF3DF),
    negativeFg = Color(0xFFC62828),
    negativeBg = Color(0xFFFFE5E3),
    isDark = false
)

private val DarkSemanticColors = AppSemanticColors(
    positiveFg = Color(0xFF4ADE80),
    positiveBg = Color(0xFF123020),
    warningFg = Color(0xFFFBBF24),
    warningBg = Color(0xFF332610),
    negativeFg = Color(0xFFF87171),
    negativeBg = Color(0xFF3A1619),
    isDark = true
)

enum class AppThemeOption(
    val key: String,
    val label: String,
    val primaryColor: Color,
    val primaryContainerColor: Color,
    val onPrimaryContainerColor: Color,
    val darkPrimaryColor: Color,
    val darkPrimaryContainerColor: Color,
    val darkOnPrimaryContainerColor: Color,
    val secondaryColor: Color,
    val secondaryContainerColor: Color,
    val onSecondaryContainerColor: Color,
    val darkSecondaryColor: Color,
    val darkSecondaryContainerColor: Color,
    val tertiaryColor: Color,
    val tertiaryContainerColor: Color,
    val onTertiaryContainerColor: Color,
    val darkTertiaryColor: Color,
    val darkTertiaryContainerColor: Color,
    val backgroundColor: Color,
    val surfaceVariantColor: Color,
    val outlineVariantColor: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val darkHeroStart: Color,
    val darkHeroEnd: Color
) {
    OCEAN(
        key = "OCEAN",
        label = "Ocean Blue",
        primaryColor = Color(0xFF0D47A1),
        primaryContainerColor = Color(0xFFD6E4FF),
        onPrimaryContainerColor = Color(0xFF001B3E),
        darkPrimaryColor = Color(0xFF60A5FA),
        darkPrimaryContainerColor = Color(0xFF172C4D),
        darkOnPrimaryContainerColor = Color(0xFFDBEAFE),
        secondaryColor = Color(0xFF0284C7),
        secondaryContainerColor = Color(0xFFE0F2FE),
        onSecondaryContainerColor = Color(0xFF07314A),
        darkSecondaryColor = Color(0xFF38BDF8),
        darkSecondaryContainerColor = Color(0xFF122C3E),
        tertiaryColor = Color(0xFF0F766E),
        tertiaryContainerColor = Color(0xFFCCFBF1),
        onTertiaryContainerColor = Color(0xFF00201D),
        darkTertiaryColor = Color(0xFF2DD4BF),
        darkTertiaryContainerColor = Color(0xFF10302E),
        backgroundColor = Color(0xFFF4F7FC),
        surfaceVariantColor = Color(0xFFE8EEF7),
        outlineVariantColor = Color(0xFFD0DCEB),
        heroStart = Color(0xFF0A367A),
        heroEnd = Color(0xFF1565C0),
        darkHeroStart = Color(0xFF0F2952),
        darkHeroEnd = Color(0xFF1D4ED8)
    ),
    EMERALD(
        key = "EMERALD",
        label = "Emerald Green",
        primaryColor = Color(0xFF1B6B3A),
        primaryContainerColor = Color(0xFFD6F0DE),
        onPrimaryContainerColor = Color(0xFF072111),
        darkPrimaryColor = Color(0xFF34D399),
        darkPrimaryContainerColor = Color(0xFF123225),
        darkOnPrimaryContainerColor = Color(0xFFD1FAE5),
        secondaryColor = Color(0xFFB86B14),
        secondaryContainerColor = Color(0xFFFDEBD2),
        onSecondaryContainerColor = Color(0xFF3A1F00),
        darkSecondaryColor = Color(0xFFFBBF24),
        darkSecondaryContainerColor = Color(0xFF332612),
        tertiaryColor = Color(0xFF1E6570),
        tertiaryContainerColor = Color(0xFFCDEEF4),
        onTertiaryContainerColor = Color(0xFF001F24),
        darkTertiaryColor = Color(0xFF22D3EE),
        darkTertiaryContainerColor = Color(0xFF112E36),
        backgroundColor = Color(0xFFF5F9F6),
        surfaceVariantColor = Color(0xFFEAF2EC),
        outlineVariantColor = Color(0xFFD4E1D8),
        heroStart = Color(0xFF15572F),
        heroEnd = Color(0xFF237D47),
        darkHeroStart = Color(0xFF0F3D24),
        darkHeroEnd = Color(0xFF15803D)
    ),
    INDIGO(
        key = "INDIGO",
        label = "Royal Indigo",
        primaryColor = Color(0xFF4338CA),
        primaryContainerColor = Color(0xFFE0E7FF),
        onPrimaryContainerColor = Color(0xFF1E1B4B),
        darkPrimaryColor = Color(0xFF818CF8),
        darkPrimaryContainerColor = Color(0xFF21204C),
        darkOnPrimaryContainerColor = Color(0xFFE0E7FF),
        secondaryColor = Color(0xFF7C3AED),
        secondaryContainerColor = Color(0xFFEDE9FE),
        onSecondaryContainerColor = Color(0xFF2E1065),
        darkSecondaryColor = Color(0xFFA78BFA),
        darkSecondaryContainerColor = Color(0xFF281C47),
        tertiaryColor = Color(0xFF0369A1),
        tertiaryContainerColor = Color(0xFFE0F2FE),
        onTertiaryContainerColor = Color(0xFF082F49),
        darkTertiaryColor = Color(0xFF38BDF8),
        darkTertiaryContainerColor = Color(0xFF102A3E),
        backgroundColor = Color(0xFFF6F6FC),
        surfaceVariantColor = Color(0xFFECECF8),
        outlineVariantColor = Color(0xFFD7D8EE),
        heroStart = Color(0xFF312E81),
        heroEnd = Color(0xFF5B4BDB),
        darkHeroStart = Color(0xFF1E1B4B),
        darkHeroEnd = Color(0xFF4338CA)
    ),
    AMBER(
        key = "AMBER",
        label = "Warm Amber",
        primaryColor = Color(0xFFB45309),
        primaryContainerColor = Color(0xFFFEF3C7),
        onPrimaryContainerColor = Color(0xFF451A03),
        darkPrimaryColor = Color(0xFFF59E0B),
        darkPrimaryContainerColor = Color(0xFF38240E),
        darkOnPrimaryContainerColor = Color(0xFFFEF3C7),
        secondaryColor = Color(0xFFC2410C),
        secondaryContainerColor = Color(0xFFFFEDD5),
        onSecondaryContainerColor = Color(0xFF431407),
        darkSecondaryColor = Color(0xFFFB923C),
        darkSecondaryContainerColor = Color(0xFF3B1E11),
        tertiaryColor = Color(0xFF0F766E),
        tertiaryContainerColor = Color(0xFFCCFBF1),
        onTertiaryContainerColor = Color(0xFF042F2E),
        darkTertiaryColor = Color(0xFF2DD4BF),
        darkTertiaryContainerColor = Color(0xFF10302E),
        backgroundColor = Color(0xFFFCF9F4),
        surfaceVariantColor = Color(0xFFF4EDE1),
        outlineVariantColor = Color(0xFFE6D8C3),
        heroStart = Color(0xFF92400E),
        heroEnd = Color(0xFFD97706),
        darkHeroStart = Color(0xFF542508),
        darkHeroEnd = Color(0xFFB45309)
    ),
    CRIMSON(
        key = "CRIMSON",
        label = "Ruby Red",
        primaryColor = Color(0xFFB91C1C),
        primaryContainerColor = Color(0xFFFEE2E2),
        onPrimaryContainerColor = Color(0xFF450A0A),
        darkPrimaryColor = Color(0xFFF87171),
        darkPrimaryContainerColor = Color(0xFF3F161A),
        darkOnPrimaryContainerColor = Color(0xFFFEE2E2),
        secondaryColor = Color(0xFFD97706),
        secondaryContainerColor = Color(0xFFFEF3C7),
        onSecondaryContainerColor = Color(0xFF451A03),
        darkSecondaryColor = Color(0xFFFBBF24),
        darkSecondaryContainerColor = Color(0xFF332610),
        tertiaryColor = Color(0xFF334155),
        tertiaryContainerColor = Color(0xFFE2E8F0),
        onTertiaryContainerColor = Color(0xFF0F172A),
        darkTertiaryColor = Color(0xFF94A3B8),
        darkTertiaryContainerColor = Color(0xFF1E293B),
        backgroundColor = Color(0xFFFCF6F6),
        surfaceVariantColor = Color(0xFFF5EAEA),
        outlineVariantColor = Color(0xFFE6D2D2),
        heroStart = Color(0xFF881337),
        heroEnd = Color(0xFFDC2626),
        darkHeroStart = Color(0xFF5C1124),
        darkHeroEnd = Color(0xFFB91C1C)
    ),
    TEAL(
        key = "TEAL",
        label = "Deep Teal",
        primaryColor = Color(0xFF0F766E),
        primaryContainerColor = Color(0xFFCCFBF1),
        onPrimaryContainerColor = Color(0xFF042F2E),
        darkPrimaryColor = Color(0xFF2DD4BF),
        darkPrimaryContainerColor = Color(0xFF103230),
        darkOnPrimaryContainerColor = Color(0xFFCCFBF1),
        secondaryColor = Color(0xFF0284C7),
        secondaryContainerColor = Color(0xFFE0F2FE),
        onSecondaryContainerColor = Color(0xFF0C4A6E),
        darkSecondaryColor = Color(0xFF38BDF8),
        darkSecondaryContainerColor = Color(0xFF102A3E),
        tertiaryColor = Color(0xFFB45309),
        tertiaryContainerColor = Color(0xFFFEF3C7),
        onTertiaryContainerColor = Color(0xFF451A03),
        darkTertiaryColor = Color(0xFFFBBF24),
        darkTertiaryContainerColor = Color(0xFF332610),
        backgroundColor = Color(0xFFF3FAF9),
        surfaceVariantColor = Color(0xFFE6F3F1),
        outlineVariantColor = Color(0xFFCDE2DF),
        heroStart = Color(0xFF115E59),
        heroEnd = Color(0xFF0D9488),
        darkHeroStart = Color(0xFF0B3B38),
        darkHeroEnd = Color(0xFF0F766E)
    ),
    VIOLET(
        key = "VIOLET",
        label = "Electric Violet",
        primaryColor = Color(0xFF7C3AED),
        primaryContainerColor = Color(0xFFEDE9FE),
        onPrimaryContainerColor = Color(0xFF2E1065),
        darkPrimaryColor = Color(0xFFA78BFA),
        darkPrimaryContainerColor = Color(0xFF2B1D4E),
        darkOnPrimaryContainerColor = Color(0xFFEDE9FE),
        secondaryColor = Color(0xFFDB2777),
        secondaryContainerColor = Color(0xFFFCE7F3),
        onSecondaryContainerColor = Color(0xFF500724),
        darkSecondaryColor = Color(0xFFF472B6),
        darkSecondaryContainerColor = Color(0xFF3E182B),
        tertiaryColor = Color(0xFF0284C7),
        tertiaryContainerColor = Color(0xFFE0F2FE),
        onTertiaryContainerColor = Color(0xFF0C4A6E),
        darkTertiaryColor = Color(0xFF38BDF8),
        darkTertiaryContainerColor = Color(0xFF102A3E),
        backgroundColor = Color(0xFFF9F7FD),
        surfaceVariantColor = Color(0xFFF0EBFA),
        outlineVariantColor = Color(0xFFDDD4F0),
        heroStart = Color(0xFF5B21B6),
        heroEnd = Color(0xFF8B5CF6),
        darkHeroStart = Color(0xFF3B157A),
        darkHeroEnd = Color(0xFF6D28D9)
    ),
    ROSE(
        key = "ROSE",
        label = "Velvet Rose",
        primaryColor = Color(0xFFBE185D),
        primaryContainerColor = Color(0xFFFCE7F3),
        onPrimaryContainerColor = Color(0xFF500724),
        darkPrimaryColor = Color(0xFFF472B6),
        darkPrimaryContainerColor = Color(0xFF3E162A),
        darkOnPrimaryContainerColor = Color(0xFFFCE7F3),
        secondaryColor = Color(0xFF9333EA),
        secondaryContainerColor = Color(0xFFF3E8FF),
        onSecondaryContainerColor = Color(0xFF3B0764),
        darkSecondaryColor = Color(0xFFC084FC),
        darkSecondaryContainerColor = Color(0xFF2D1942),
        tertiaryColor = Color(0xFF0F766E),
        tertiaryContainerColor = Color(0xFFCCFBF1),
        onTertiaryContainerColor = Color(0xFF042F2E),
        darkTertiaryColor = Color(0xFF2DD4BF),
        darkTertiaryContainerColor = Color(0xFF103230),
        backgroundColor = Color(0xFFFDF6F9),
        surfaceVariantColor = Color(0xFFF7EBF1),
        outlineVariantColor = Color(0xFFE8D1DD),
        heroStart = Color(0xFF831843),
        heroEnd = Color(0xFFDB2777),
        darkHeroStart = Color(0xFF54102D),
        darkHeroEnd = Color(0xFFBE185D)
    ),
    SLATE(
        key = "SLATE",
        label = "Obsidian Slate",
        primaryColor = Color(0xFF1E293B),
        primaryContainerColor = Color(0xFFE2E8F0),
        onPrimaryContainerColor = Color(0xFF0F172A),
        darkPrimaryColor = Color(0xFF94A3B8),
        darkPrimaryContainerColor = Color(0xFF232E3F),
        darkOnPrimaryContainerColor = Color(0xFFF1F5F9),
        secondaryColor = Color(0xFF0284C7),
        secondaryContainerColor = Color(0xFFE0F2FE),
        onSecondaryContainerColor = Color(0xFF0C4A6E),
        darkSecondaryColor = Color(0xFF38BDF8),
        darkSecondaryContainerColor = Color(0xFF102A3E),
        tertiaryColor = Color(0xFF0D9488),
        tertiaryContainerColor = Color(0xFFCCFBF1),
        onTertiaryContainerColor = Color(0xFF042F2E),
        darkTertiaryColor = Color(0xFF2DD4BF),
        darkTertiaryContainerColor = Color(0xFF103230),
        backgroundColor = Color(0xFFF6F7F9),
        surfaceVariantColor = Color(0xFFEAEEF3),
        outlineVariantColor = Color(0xFFD3DCE6),
        heroStart = Color(0xFF0F172A),
        heroEnd = Color(0xFF334155),
        darkHeroStart = Color(0xFF111827),
        darkHeroEnd = Color(0xFF273549)
    ),
    GOLD(
        key = "GOLD",
        label = "Royal Gold",
        primaryColor = Color(0xFF9A6700),
        primaryContainerColor = Color(0xFFFEF3C7),
        onPrimaryContainerColor = Color(0xFF3B2400),
        darkPrimaryColor = Color(0xFFFACC15),
        darkPrimaryContainerColor = Color(0xFF362B0E),
        darkOnPrimaryContainerColor = Color(0xFFFEF9C3),
        secondaryColor = Color(0xFF1E3A8A),
        secondaryContainerColor = Color(0xFFDBEAFE),
        onSecondaryContainerColor = Color(0xFF172554),
        darkSecondaryColor = Color(0xFF60A5FA),
        darkSecondaryContainerColor = Color(0xFF172B4D),
        tertiaryColor = Color(0xFF15803D),
        tertiaryContainerColor = Color(0xFFDCFCE7),
        onTertiaryContainerColor = Color(0xFF052E16),
        darkTertiaryColor = Color(0xFF4ADE80),
        darkTertiaryContainerColor = Color(0xFF11311E),
        backgroundColor = Color(0xFFFCFAF5),
        surfaceVariantColor = Color(0xFFF4EFE4),
        outlineVariantColor = Color(0xFFE4DAC6),
        heroStart = Color(0xFF713F12),
        heroEnd = Color(0xFFCA8A04),
        darkHeroStart = Color(0xFF4A2B09),
        darkHeroEnd = Color(0xFFA16207)
    );

    fun toColorScheme(darkTheme: Boolean = false): ColorScheme =
        if (darkTheme) toDarkColorScheme() else toLightColorScheme()

    private fun toLightColorScheme(): ColorScheme = lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryContainerColor,
        onPrimaryContainer = onPrimaryContainerColor,
        secondary = secondaryColor,
        onSecondary = Color.White,
        secondaryContainer = secondaryContainerColor,
        onSecondaryContainer = onSecondaryContainerColor,
        tertiary = tertiaryColor,
        onTertiary = Color.White,
        tertiaryContainer = tertiaryContainerColor,
        onTertiaryContainer = onTertiaryContainerColor,
        error = CrimsonError,
        onError = CrimsonOnError,
        errorContainer = CrimsonErrorContainer,
        onErrorContainer = CrimsonOnErrorContainer,
        background = backgroundColor,
        onBackground = Color(0xFF111827),
        surface = PureCardSurface,
        onSurface = Color(0xFF111827),
        surfaceVariant = surfaceVariantColor,
        onSurfaceVariant = Color(0xFF475569),
        outline = Color(0xFF64748B),
        outlineVariant = outlineVariantColor,
        surfaceContainerLowest = PureCardSurface,
        surfaceContainerLow = backgroundColor,
        surfaceContainer = surfaceVariantColor
    )

    private fun toDarkColorScheme(): ColorScheme = darkColorScheme(
        primary = darkPrimaryColor,
        onPrimary = Color(0xFF07111E),
        primaryContainer = darkPrimaryContainerColor,
        onPrimaryContainer = darkOnPrimaryContainerColor,
        secondary = darkSecondaryColor,
        onSecondary = Color(0xFF07111E),
        secondaryContainer = darkSecondaryContainerColor,
        onSecondaryContainer = Color(0xFFE2E8F0),
        tertiary = darkTertiaryColor,
        onTertiary = Color(0xFF061614),
        tertiaryContainer = darkTertiaryContainerColor,
        onTertiaryContainer = Color(0xFFCCFBF1),
        error = Color(0xFFF87171),
        onError = Color(0xFF2D0A0A),
        errorContainer = Color(0xFF47191D),
        onErrorContainer = Color(0xFFFECACA),
        // Carefully tiered obsidian dark surfaces so cards, dialogs, and chips NEVER merge with background
        background = Color(0xFF0B1017),
        onBackground = Color(0xFFF1F5F9),
        surface = Color(0xFF141B25),
        onSurface = Color(0xFFF1F5F9),
        surfaceVariant = Color(0xFF1E2837),
        onSurfaceVariant = Color(0xFF9FB0C7),
        outline = Color(0xFF475569),
        outlineVariant = Color(0xFF2B3749),
        surfaceContainerLowest = Color(0xFF0E141D),
        surfaceContainerLow = Color(0xFF141B25),
        surfaceContainer = Color(0xFF1A2331),
        surfaceContainerHigh = Color(0xFF212C3D),
        surfaceContainerHighest = Color(0xFF29364A)
    )

    fun primaryAndroidColorInt(): Int = primaryColor.toArgb()
    fun softAndroidColorInt(): Int = primaryContainerColor.toArgb()

    companion object {
        fun fromKey(key: String?): AppThemeOption =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: OCEAN
    }
}

val LocalAppThemeOption = staticCompositionLocalOf { AppThemeOption.OCEAN }
val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

val HassanAgroShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun HassanAgroTheme(
    themeKey: String = AppThemeOption.OCEAN.key,
    appearanceMode: ThemeAppearanceMode = ThemeAppearanceMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (appearanceMode) {
        ThemeAppearanceMode.SYSTEM -> systemDark
        ThemeAppearanceMode.LIGHT -> false
        ThemeAppearanceMode.DARK -> true
    }
    val option = AppThemeOption.fromKey(themeKey)
    val semanticColors = if (useDarkTheme) DarkSemanticColors else LightSemanticColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAppThemeOption provides option,
        LocalSemanticColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = option.toColorScheme(darkTheme = useDarkTheme),
            typography = Typography,
            shapes = HassanAgroShapes,
            content = content
        )
    }
}
