package net.toload.main.hd.ui.compose.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import net.toload.main.hd.R

/**
 * LIME Theme
 *
 * Bridges the XML-based Material 3 theme to Jetpack Compose.
 * It reads the resolved colors from the Context resources, ensuring that
 * the Compose UI matches the Activity's theme (including Day/Night modes).
 */
@Composable
fun LimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // We primarily use the Context's resolved colors to ensure consistency with XML.
    // However, if dynamic color is requested and available, we can use the system's dynamic scheme.
    // For this bridge, we prioritize the XML resources which are already set up to map to 
    // either fixed M3 colors or dynamic colors (if configured in themes.xml).
    
    // Read colors from Context (which handles Day/Night automatically)
    // Note: This relies on the Activity/Context having the correct Theme applied.
    
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Fallback or explicit XML bridge if dynamic not enabled/supported
        // We construct the scheme using the resources defined in colors_material3.xml
        // ContextCompat.getColor will return the correct variant (values vs values-night)
        
        if (darkTheme) {
             darkColorScheme(
                primary = Color(ContextCompat.getColor(context, R.color.md_theme_primary)),
                onPrimary = Color(ContextCompat.getColor(context, R.color.md_theme_onPrimary)),
                primaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_primaryContainer)),
                onPrimaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer)),
                secondary = Color(ContextCompat.getColor(context, R.color.md_theme_secondary)),
                onSecondary = Color(ContextCompat.getColor(context, R.color.md_theme_onSecondary)),
                secondaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_secondaryContainer)),
                onSecondaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_onSecondaryContainer)),
                tertiary = Color(ContextCompat.getColor(context, R.color.md_theme_tertiary)),
                onTertiary = Color(ContextCompat.getColor(context, R.color.md_theme_onTertiary)),
                tertiaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_tertiaryContainer)),
                onTertiaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_onTertiaryContainer)),
                error = Color(ContextCompat.getColor(context, R.color.md_theme_error)),
                onError = Color(ContextCompat.getColor(context, R.color.md_theme_onError)),
                errorContainer = Color(ContextCompat.getColor(context, R.color.md_theme_errorContainer)),
                onErrorContainer = Color(ContextCompat.getColor(context, R.color.md_theme_onErrorContainer)),
                background = Color(ContextCompat.getColor(context, R.color.md_theme_background)),
                onBackground = Color(ContextCompat.getColor(context, R.color.md_theme_onBackground)),
                surface = Color(ContextCompat.getColor(context, R.color.md_theme_surface)),
                onSurface = Color(ContextCompat.getColor(context, R.color.md_theme_onSurface)),
                surfaceVariant = Color(ContextCompat.getColor(context, R.color.md_theme_surfaceVariant)),
                onSurfaceVariant = Color(ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant)),
                outline = Color(ContextCompat.getColor(context, R.color.md_theme_outline)),
                inverseSurface = Color(ContextCompat.getColor(context, R.color.md_theme_inverseSurface)),
                inverseOnSurface = Color(ContextCompat.getColor(context, R.color.md_theme_inverseOnSurface)),
                inversePrimary = Color(ContextCompat.getColor(context, R.color.md_theme_inversePrimary)),
                scrim = Color(ContextCompat.getColor(context, R.color.md_theme_scrim)),
            )
        } else {
            lightColorScheme(
                primary = Color(ContextCompat.getColor(context, R.color.md_theme_primary)),
                onPrimary = Color(ContextCompat.getColor(context, R.color.md_theme_onPrimary)),
                primaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_primaryContainer)),
                onPrimaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer)),
                secondary = Color(ContextCompat.getColor(context, R.color.md_theme_secondary)),
                onSecondary = Color(ContextCompat.getColor(context, R.color.md_theme_onSecondary)),
                secondaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_secondaryContainer)),
                onSecondaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_onSecondaryContainer)),
                tertiary = Color(ContextCompat.getColor(context, R.color.md_theme_tertiary)),
                onTertiary = Color(ContextCompat.getColor(context, R.color.md_theme_onTertiary)),
                tertiaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_tertiaryContainer)),
                onTertiaryContainer = Color(ContextCompat.getColor(context, R.color.md_theme_onTertiaryContainer)),
                error = Color(ContextCompat.getColor(context, R.color.md_theme_error)),
                onError = Color(ContextCompat.getColor(context, R.color.md_theme_onError)),
                errorContainer = Color(ContextCompat.getColor(context, R.color.md_theme_errorContainer)),
                onErrorContainer = Color(ContextCompat.getColor(context, R.color.md_theme_onErrorContainer)),
                background = Color(ContextCompat.getColor(context, R.color.md_theme_background)),
                onBackground = Color(ContextCompat.getColor(context, R.color.md_theme_onBackground)),
                surface = Color(ContextCompat.getColor(context, R.color.md_theme_surface)),
                onSurface = Color(ContextCompat.getColor(context, R.color.md_theme_onSurface)),
                surfaceVariant = Color(ContextCompat.getColor(context, R.color.md_theme_surfaceVariant)),
                onSurfaceVariant = Color(ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant)),
                outline = Color(ContextCompat.getColor(context, R.color.md_theme_outline)),
                inverseSurface = Color(ContextCompat.getColor(context, R.color.md_theme_inverseSurface)),
                inverseOnSurface = Color(ContextCompat.getColor(context, R.color.md_theme_inverseOnSurface)),
                inversePrimary = Color(ContextCompat.getColor(context, R.color.md_theme_inversePrimary)),
                scrim = Color(ContextCompat.getColor(context, R.color.md_theme_scrim)),
            )
        }
    }

    /*
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    */

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = Typography, // Use default M3 typography for now
        content = content
    )
}
