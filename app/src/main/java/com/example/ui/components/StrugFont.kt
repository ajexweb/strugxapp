package com.example.ui.components

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

enum class StrugFontStyle(
  val id: String,
  val displayName: String,
  val fontFamily: FontFamily,
  val fontWeight: FontWeight,
  val fontStyle: FontStyle = FontStyle.Normal
) {
  MODERN("modern", "Modern", FontFamily.SansSerif, FontWeight.Bold),
  CLASSIC("classic", "Classic", FontFamily.Serif, FontWeight.Bold),
  MONO("mono", "Typewriter", FontFamily.Monospace, FontWeight.SemiBold),
  CURSIVE("cursive", "Italic", FontFamily.Serif, FontWeight.Medium, FontStyle.Italic),
  BOLD("bold", "Impact", FontFamily.SansSerif, FontWeight.Black)
}

fun formatStrugCaption(font: StrugFontStyle, text: String): String {
  val clean = text.trim()
  if (clean.isBlank()) return ""
  return "[font:${font.id}]$clean"
}

fun parseStrugCaption(raw: String): Pair<StrugFontStyle, String> {
  if (raw.startsWith("[font:") && raw.contains("]")) {
    val endIdx = raw.indexOf("]")
    val fontId = raw.substring(6, endIdx)
    val text = raw.substring(endIdx + 1)
    val font = StrugFontStyle.values().find { it.id == fontId } ?: StrugFontStyle.MODERN
    return Pair(font, text)
  }
  return Pair(StrugFontStyle.MODERN, raw)
}
