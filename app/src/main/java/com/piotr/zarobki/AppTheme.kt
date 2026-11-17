package com.piotr.zarobki

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ZarobkiTheme(theme: ThemeName, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val ac = WidgetThemes.getAppColors(theme)
    val scheme = when {
        theme == ThemeName.MATERIAL_YOU && Build.VERSION.SDK_INT >= 31 -> dynamicDarkColorScheme(context)
        theme == ThemeName.ARCTIC -> lightColorScheme(
            background = ac.background, surface = ac.surface,
            onBackground = ac.onSurface, onSurface = ac.onSurface,
            onSurfaceVariant = ac.onSurfaceVariant,
            primary = ac.primary, tertiary = ac.tertiary,
        )
        else -> darkColorScheme(
            background = ac.background, surface = ac.surface,
            onBackground = ac.onSurface, onSurface = ac.onSurface,
            onSurfaceVariant = ac.onSurfaceVariant,
            primary = ac.primary, tertiary = ac.tertiary,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
