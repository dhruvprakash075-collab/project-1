package com.openfiles.core.ui.theme

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
import com.openfiles.core.data.ThemePref

private val TrueBlack = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color(0xFF0A0A0A),
)

/** Material 3 theme wrapper implementing Light / Dark / True Black (AMOLED) / System. */
@Composable
fun OpenFilesTheme(
    theme: ThemePref = ThemePref.System,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        ThemePref.Light -> false
        ThemePref.Dark, ThemePref.TrueBlack -> true
        ThemePref.System -> isSystemInDarkTheme()
    }
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val ctx = LocalContext.current
    val colors = when {
        theme == ThemePref.TrueBlack -> TrueBlack
        dynamic && dark -> dynamicDarkColorScheme(ctx)
        dynamic && !dark -> dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
