package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StrugxColorScheme = darkColorScheme(
  primary = PureWhite,
  onPrimary = PureBlack,
  primaryContainer = DarkSurfaceElevated,
  onPrimaryContainer = PureWhite,
  secondary = CyanAccent,
  onSecondary = PureBlack,
  tertiary = PurpleAccent,
  background = DarkBackground,
  onBackground = PureWhite,
  surface = DarkSurface,
  onSurface = PureWhite,
  surfaceVariant = CardBackground,
  onSurfaceVariant = TextSecondary,
  outline = BorderLight,
  error = DangerRed,
  onError = PureWhite
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = StrugxColorScheme,
    typography = Typography,
    content = content
  )
}

