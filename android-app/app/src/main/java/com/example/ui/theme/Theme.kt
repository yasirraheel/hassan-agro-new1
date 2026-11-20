package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorSurfaceLow = androidx.compose.ui.graphics.Color(0xFFF9FCF9)

private val AgriLightColorScheme = lightColorScheme(
    primary = AgriLeafPrimary,
    onPrimary = AgriLeafOnPrimary,
    primaryContainer = AgriSproutContainer,
    onPrimaryContainer = AgriSproutOnContainer,
    secondary = HarvestAmberSecondary,
    onSecondary = HarvestAmberOnSecondary,
    secondaryContainer = PaleWheatContainer,
    onSecondaryContainer = PaleWheatOnContainer,
    tertiary = SlateTealTertiary,
    onTertiary = SlateTealOnTertiary,
    tertiaryContainer = SoftMistContainer,
    onTertiaryContainer = SoftMistOnContainer,
    error = CrimsonError,
    onError = CrimsonOnError,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = CrimsonOnErrorContainer,
    background = BotanicalIvoryBackground,
    onBackground = DeepForestInk,
    surface = PureCardSurface,
    onSurface = DeepForestInk,
    surfaceVariant = SoftLedgerSurfaceVariant,
    onSurfaceVariant = MutedFoliageInk,
    outline = SubtleOutline,
    outlineVariant = HairlineBorder,
    surfaceContainerLowest = PureCardSurface,
    surfaceContainerLow = ColorSurfaceLow,
    surfaceContainer = SoftLedgerSurfaceVariant
)

val HassanAgroShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun HassanAgroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AgriLightColorScheme,
        typography = Typography,
        shapes = HassanAgroShapes,
        content = content
    )
}
