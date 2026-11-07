package in.ledgerx.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF55E6B5),
    onPrimary = Color(0xFF003829),
    primaryContainer = Color(0xFF0B4F3D),
    secondary = Color(0xFF8ED8C1),
    background = Color(0xFF07110F),
    surface = Color(0xFF0D1B18),
    surfaceVariant = Color(0xFF172824),
    onBackground = Color(0xFFE4F1EC),
    onSurface = Color(0xFFE4F1EC),
    error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF80F8CD),
    secondary = Color(0xFF41665A),
    background = Color(0xFFF5FBF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDCEAE4),
    onBackground = Color(0xFF12201C),
    onSurface = Color(0xFF12201C),
)

@Composable
fun LedgerXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}

