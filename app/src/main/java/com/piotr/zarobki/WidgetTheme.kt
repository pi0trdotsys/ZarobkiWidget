package com.piotr.zarobki

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

enum class ThemeName(val displayName: String) {
    TECH_DARK("Tech Dark"),
    MATERIAL_YOU("Material You"),
    NEON_PULSE("Neon Pulse"),
    DEEP_OCEAN("Deep Ocean"),
    SUNSET_EMBER("Sunset Ember"),
    ARCTIC("Arctic"),
    VIOLET_AURA("Violet Aura"),
    EMERALD_MATRIX("Emerald Matrix");

    companion object {
        fun fromString(value: String): ThemeName =
            entries.find { it.name == value } ?: TECH_DARK
    }
}

data class WidgetColors(
    val background: ColorProvider,
    val textPrimary: ColorProvider,
    val textMuted: ColorProvider,
    val accentPrimary: ColorProvider,
    val accentWarn: ColorProvider,
)

data class AppThemeColors(
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val tertiary: Color,
)

object WidgetThemes {

    val TECH_DARK = WidgetColors(
        background = ColorProvider(Color(0xFF0B1220)),
        textPrimary = ColorProvider(Color(0xFFE5E7EB)),
        textMuted = ColorProvider(Color(0xFF94A3B8)),
        accentPrimary = ColorProvider(Color(0xFF22D3EE)),
        accentWarn = ColorProvider(Color(0xFFFBBF24)),
    )

    val NEON_PULSE = WidgetColors(
        background = ColorProvider(Color(0xFF05050A)),
        textPrimary = ColorProvider(Color(0xFFF0F0F0)),
        textMuted = ColorProvider(Color(0xFF555555)),
        accentPrimary = ColorProvider(Color(0xFF00FF87)),
        accentWarn = ColorProvider(Color(0xFFFF2D95)),
    )

    val DEEP_OCEAN = WidgetColors(
        background = ColorProvider(Color(0xFF0D1B2A)),
        textPrimary = ColorProvider(Color(0xFFE0E8F0)),
        textMuted = ColorProvider(Color(0xFF778DA9)),
        accentPrimary = ColorProvider(Color(0xFF00B4D8)),
        accentWarn = ColorProvider(Color(0xFFFCA311)),
    )

    val SUNSET_EMBER = WidgetColors(
        background = ColorProvider(Color(0xFF1C0F0A)),
        textPrimary = ColorProvider(Color(0xFFFDE8D0)),
        textMuted = ColorProvider(Color(0xFFA0765A)),
        accentPrimary = ColorProvider(Color(0xFFFF6B35)),
        accentWarn = ColorProvider(Color(0xFFFFD166)),
    )

    val ARCTIC = WidgetColors(
        background = ColorProvider(Color(0xFFF7F9FC)),
        textPrimary = ColorProvider(Color(0xFF1E293B)),
        textMuted = ColorProvider(Color(0xFF94A3B8)),
        accentPrimary = ColorProvider(Color(0xFF3B82F6)),
        accentWarn = ColorProvider(Color(0xFFF59E0B)),
    )

    val VIOLET_AURA = WidgetColors(
        background = ColorProvider(Color(0xFF120B1C)),
        textPrimary = ColorProvider(Color(0xFFE8DEF8)),
        textMuted = ColorProvider(Color(0xFF8B7DA8)),
        accentPrimary = ColorProvider(Color(0xFFBB86FC)),
        accentWarn = ColorProvider(Color(0xFFFF7597)),
    )

    val EMERALD_MATRIX = WidgetColors(
        background = ColorProvider(Color(0xFF021A0C)),
        textPrimary = ColorProvider(Color(0xFFA7F3D0)),
        textMuted = ColorProvider(Color(0xFF4A7C5E)),
        accentPrimary = ColorProvider(Color(0xFF34D399)),
        accentWarn = ColorProvider(Color(0xFFF59E0B)),
    )

    fun get(theme: ThemeName): WidgetColors = when (theme) {
        ThemeName.TECH_DARK -> TECH_DARK
        ThemeName.MATERIAL_YOU -> TECH_DARK
        ThemeName.NEON_PULSE -> NEON_PULSE
        ThemeName.DEEP_OCEAN -> DEEP_OCEAN
        ThemeName.SUNSET_EMBER -> SUNSET_EMBER
        ThemeName.ARCTIC -> ARCTIC
        ThemeName.VIOLET_AURA -> VIOLET_AURA
        ThemeName.EMERALD_MATRIX -> EMERALD_MATRIX
    }

    fun getAppColors(theme: ThemeName): AppThemeColors = when (theme) {
        ThemeName.TECH_DARK, ThemeName.MATERIAL_YOU -> AppThemeColors(
            background = Color(0xFF0B1220), surface = Color(0xFF111A2E),
            onSurface = Color(0xFFE5E7EB), onSurfaceVariant = Color(0xFF94A3B8),
            primary = Color(0xFF22D3EE), tertiary = Color(0xFFFBBF24),
        )
        ThemeName.NEON_PULSE -> AppThemeColors(
            background = Color(0xFF05050A), surface = Color(0xFF111111),
            onSurface = Color(0xFFF0F0F0), onSurfaceVariant = Color(0xFF555555),
            primary = Color(0xFF00FF87), tertiary = Color(0xFFFF2D95),
        )
        ThemeName.DEEP_OCEAN -> AppThemeColors(
            background = Color(0xFF0D1B2A), surface = Color(0xFF1B2838),
            onSurface = Color(0xFFE0E8F0), onSurfaceVariant = Color(0xFF778DA9),
            primary = Color(0xFF00B4D8), tertiary = Color(0xFFFCA311),
        )
        ThemeName.SUNSET_EMBER -> AppThemeColors(
            background = Color(0xFF1C0F0A), surface = Color(0xFF291810),
            onSurface = Color(0xFFFDE8D0), onSurfaceVariant = Color(0xFFA0765A),
            primary = Color(0xFFFF6B35), tertiary = Color(0xFFFFD166),
        )
        ThemeName.ARCTIC -> AppThemeColors(
            background = Color(0xFFF7F9FC), surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1E293B), onSurfaceVariant = Color(0xFF94A3B8),
            primary = Color(0xFF3B82F6), tertiary = Color(0xFFF59E0B),
        )
        ThemeName.VIOLET_AURA -> AppThemeColors(
            background = Color(0xFF120B1C), surface = Color(0xFF1D1429),
            onSurface = Color(0xFFE8DEF8), onSurfaceVariant = Color(0xFF8B7DA8),
            primary = Color(0xFFBB86FC), tertiary = Color(0xFFFF7597),
        )
        ThemeName.EMERALD_MATRIX -> AppThemeColors(
            background = Color(0xFF021A0C), surface = Color(0xFF0A2818),
            onSurface = Color(0xFFA7F3D0), onSurfaceVariant = Color(0xFF4A7C5E),
            primary = Color(0xFF34D399), tertiary = Color(0xFFF59E0B),
        )
    }

    fun previewColors(theme: ThemeName): Triple<Color, Color, Color> = when (theme) {
        ThemeName.TECH_DARK -> Triple(Color(0xFF0B1220), Color(0xFF22D3EE), Color(0xFFFBBF24))
        ThemeName.MATERIAL_YOU -> Triple(Color(0xFF1C1B1F), Color(0xFF6750A4), Color(0xFFB3261E))
        ThemeName.NEON_PULSE -> Triple(Color(0xFF05050A), Color(0xFF00FF87), Color(0xFFFF2D95))
        ThemeName.DEEP_OCEAN -> Triple(Color(0xFF0D1B2A), Color(0xFF00B4D8), Color(0xFFFCA311))
        ThemeName.SUNSET_EMBER -> Triple(Color(0xFF1C0F0A), Color(0xFFFF6B35), Color(0xFFFFD166))
        ThemeName.ARCTIC -> Triple(Color(0xFFF7F9FC), Color(0xFF3B82F6), Color(0xFFF59E0B))
        ThemeName.VIOLET_AURA -> Triple(Color(0xFF120B1C), Color(0xFFBB86FC), Color(0xFFFF7597))
        ThemeName.EMERALD_MATRIX -> Triple(Color(0xFF021A0C), Color(0xFF34D399), Color(0xFFF59E0B))
    }
}
