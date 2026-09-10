package se.kjellstrand.lsystemcamera

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun LSystemTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = dynamicColorSchemeOrNull(dark)
        ?: if (dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = scheme, content = content)
}
