package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

enum class AppThemeOption(
    val key: String,
    val label: String,
    val primaryColor: Color,
    val primaryContainerColor: Color,
    val onPrimaryContainerColor: Color,
    val secondaryColor: Color,
    val secondaryContainerColor: Color,
    val onSecondaryContainerColor: Color,
    val tertiaryColor: Color,
    val tertiaryContainerColor: Color,
    val onTertiaryContainerColor: Color,
    val backgroundColor: Color,
    val surfaceVariantColor: Color,
    val outlineVariantColor: Color,
    val heroStart: Color,
    val heroEnd: Color
) {
    OCEAN(
        key = "OCEAN",
        label = "Ocean Blue",
        primaryColor = Color(0xFF0D47A1),
        primaryContainerColor = Color(0xFFD6E4FF),
        onPrimaryContainerColor = Color(0xFF001B3E),
        secondaryColor = Color(0xFF0284C7),
        secondaryContainerColor = Color(0xFFE0F2FE),
        onSecondaryContainerColor = Color(0xFF07314A),
        tertiaryColor = Color(0xFF0F766E),
        tertiaryContainerColor = Color(0xFFCCFBF1),
        onTertiaryContainerColor = Color(0xFF00201D),
        backgroundColor = Color(0xFFF4F7FC),
        surfaceVariantColor = Color(0xFFE8EEF7),
        outlineVariantColor = Color(0xFFD0DCEB),
        heroStart = Color(0xFF0A367A),
        heroEnd = Color(0xFF1565C0)
    ),
    EMERALD(
        key = "EMERALD",
        label = "Emerald Green",
        primaryColor = Color(0xFF1B6B3A),
        primaryContainerColor = Color(0xFFD6F0DE),
        onPrimaryContainerColor = Color(0xFF072111),
        secondaryColor = Color(0xFFB86B14),
        secondaryContainerColor = Color(0xFFFDEBD2),
        onSecondaryContainerColor = Color(0xFF3A1F00),
        tertiaryColor = Color(0xFF1E6570),
        tertiaryContainerColor = Color(0xFFCDEEF4),
        onTertiaryContainerColor = Color(0xFF001F24),
        backgroundColor = Color(0xFFF5F9F6),
        surfaceVariantColor = Color(0xFFEAF2EC),
        outlineVariantColor = Color(0xFFD4E1D8),
        heroStart = Color(0xFF15572F),
        heroEnd = Color(0xFF237D47)
    ),
    INDIGO(
        key = "INDIGO",
        label = "Royal Indigo",
        primaryColor = Color(0xFF4338CA),
        primaryContainerColor = Color(0xFFE0E7FF),
        onPrimaryContainerColor = Color(0xFF1E1B4B),
        secondaryColor = Color(0xFF7C3AED),
        secondaryContainerColor = Color(0xFFEDE9FE),
        onSecondaryContainerColor = Color(0xFF2E1065),
        tertiaryColor = Color(0xFF0369A1),
        tertiaryContainerColor = Color(0xFFE0F2FE),
        onTertiaryContainerColor = Color(0xFF082F49),
        backgroundColor = Color(0xFFF6F6FC),
        surfaceVariantColor = Color(0xFFECECF8),
        outlineVariantColor = Color(0xFFD7D8EE),
        heroStart = Color(0xFF312E81),
        heroEnd = Color(0xFF5B4BDB)
    ),
    AMBER(
        key = "AMBER",
        label = "Warm Amber",
        primaryColor = Color(0xFFB45309),
        primaryContainerColor = Color(0xFFFEF3C7),
        onPrimaryContainerColor = Color(0xFF451A03),
        secondaryColor = Color(0xFFC2410C),
        secondaryContainerColor = Color(0xFFFFEDD5),
        onSecondaryContainerColor = Color(0xFF431407),
        tertiaryColor = Color(0xFF0F766E),
        tertiaryContainerColor = Color(0xFFCCFBF1),
        onTertiaryContainerColor = Color(0xFF042F2E),
        backgroundColor = Color(0xFFFCF9F4),
        surfaceVariantColor = Color(0xFFF4EDE1),
        outlineVariantColor = Color(0xFFE6D8C3),
        heroStart = Color(0xFF92400E),
        heroEnd = Color(0xFFD97706)
    ),
    CRIMSON(
        key = "CRIMSON",
        label = "Ruby Red",
        primaryColor = Color(0xFFB91C1C),
        primaryContainerColor = Color(0xFFFEE2E2),
        onPrimaryContainerColor = Color(0xFF450A0A),
        secondaryColor = Color(0xFFD97706),
        secondaryContainerColor = Color(0xFFFEF3C7),
        onSecondaryContainerColor = Color(0xFF451A03),
        tertiaryColor = Color(0xFF334155),
        tertiaryContainerColor = Color(0xFFE2E8F0),
        onTertiaryContainerColor = Color(0xFF0F172A),
        backgroundColor = Color(0xFFFCF6F6),
        surfaceVariantColor = Color(0xFFF5EAEA),
        outlineVariantColor = Color(0xFFE6D2D2),
        heroStart = Color(0xFF881337),
        heroEnd = Color(0xFFDC2626)
    ),
    TEAL(
        key = "TEAL",
        label = "Deep Teal",
        primaryColor = Color(0xFF0F766E),
        primaryContainerColor = Color(0xFFCCFBF1),
        onPrimaryContainerColor = Color(0xFF042F2E),
        secondaryColor = Color(0xFF0284C7),
        secondaryContainerColor = Color(0xFFE0F2FE),
        onSecondaryContainerColor = Color(0xFF0C4A6E),
        tertiaryColor = Color(0xFFB45309),
        tertiaryContainerColor = Color(0xFFFEF3C7),
        onTertiaryContainerColor = Color(0xFF451A03),
        backgroundColor = Color(0xFFF3FAF9),
        surfaceVariantColor = Color(0xFFE6F3F1),
        outlineVariantColor = Color(0xFFCDE2DF),
        heroStart = Color(0xFF115E59),
        heroEnd = Color(0xFF0D9488)
    );

    fun toColorScheme(): ColorScheme = lightColorScheme(
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

    fun primaryAndroidColorInt(): Int = primaryColor.toArgb()
    fun softAndroidColorInt(): Int = primaryContainerColor.toArgb()

    companion object {
        fun fromKey(key: String?): AppThemeOption =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: OCEAN
    }
}

val LocalAppThemeOption = staticCompositionLocalOf { AppThemeOption.OCEAN }

val HassanAgroShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun HassanAgroTheme(
    themeKey: String = AppThemeOption.OCEAN.key,
    content: @Composable () -> Unit
) {
    val option = AppThemeOption.fromKey(themeKey)
    CompositionLocalProvider(LocalAppThemeOption provides option) {
        MaterialTheme(
            colorScheme = option.toColorScheme(),
            typography = Typography,
            shapes = HassanAgroShapes,
            content = content
        )
    }
}
