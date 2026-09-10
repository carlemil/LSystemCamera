package se.kjellstrand.lsystemcamera

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** The platform's wallpaper-derived scheme, or null when there is none. */
@Composable
expect fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme?
