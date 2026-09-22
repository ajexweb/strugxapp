package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted

/**
 * Modern, futuristic, minimalist monochrome STRUGX emblem & wordmark.
 * Replaces the old generic colorful dot logo with a bold, razor-sharp geometric identity.
 */
@Composable
fun StrugxEmblem(
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  color: Color = PureWhite,
  animateGlow: Boolean = false
) {
  val infiniteTransition = rememberInfiniteTransition(label = "strugx_pulse")
  val alphaPulse by if (animateGlow) {
    infiniteTransition.animateFloat(
      initialValue = 0.75f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(1400, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "glow_alpha"
    )
  } else {
    rememberInfiniteTransition(label = "").animateFloat(
      initialValue = 1f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(1000)),
      label = ""
    )
  }

  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val strokeWidth = (w * 0.09f).coerceAtLeast(2f)

    // Futuristic angular upper blade / ribbon
    val upperBlade = Path().apply {
      moveTo(w * 0.18f, h * 0.22f)
      lineTo(w * 0.72f, h * 0.22f)
      lineTo(w * 0.84f, h * 0.36f)
      lineTo(w * 0.46f, h * 0.50f)
      lineTo(w * 0.32f, h * 0.40f)
    }

    // Futuristic angular lower blade / ribbon (interlocking geometric flow)
    val lowerBlade = Path().apply {
      moveTo(w * 0.82f, h * 0.78f)
      lineTo(w * 0.28f, h * 0.78f)
      lineTo(w * 0.16f, h * 0.64f)
      lineTo(w * 0.54f, h * 0.50f)
      lineTo(w * 0.68f, h * 0.60f)
    }

    // Central crossing cyber slash forming the distinctive 'X'
    val crossBar = Path().apply {
      moveTo(w * 0.25f, h * 0.72f)
      lineTo(w * 0.75f, h * 0.28f)
    }

    // Draw paths with high-contrast precision
    drawPath(
      path = upperBlade,
      color = color.copy(alpha = alphaPulse),
      style = Stroke(width = strokeWidth, cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )

    drawPath(
      path = lowerBlade,
      color = color.copy(alpha = alphaPulse),
      style = Stroke(width = strokeWidth, cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )

    drawPath(
      path = crossBar,
      color = color.copy(alpha = alphaPulse * 0.9f),
      style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
    )

    // Subtle micro-nodes at opposing vertices
    drawCircle(
      color = color,
      radius = strokeWidth * 0.6f,
      center = Offset(w * 0.18f, h * 0.22f)
    )
    drawCircle(
      color = color,
      radius = strokeWidth * 0.6f,
      center = Offset(w * 0.82f, h * 0.78f)
    )
  }
}

@Composable
fun StrugxWordmark(
  modifier: Modifier = Modifier,
  fontSize: Int = 26,
  showTagline: Boolean = false
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "STRUGX",
      fontSize = fontSize.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.SansSerif,
      letterSpacing = 4.sp,
      color = PureWhite
    )
    if (showTagline) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "FOLLOWERS • VIEWS • STRUGS",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = TextMuted
      )
    }
  }
}

@Composable
fun StrugxBrandHeader(
  modifier: Modifier = Modifier,
  emblemSize: Dp = 32.dp,
  fontSize: Int = 22
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    StrugxEmblem(size = emblemSize)
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = "STRUGX",
      fontSize = fontSize.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.SansSerif,
      letterSpacing = 3.sp,
      color = PureWhite
    )
  }
}
