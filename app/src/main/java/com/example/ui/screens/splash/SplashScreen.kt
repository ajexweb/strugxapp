package com.example.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StrugxEmblem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onFinishSplash: () -> Unit
) {
  val animatableScale = remember { Animatable(0.92f) }
  val animatableAlpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    // Fast, crisp entrance
    animatableAlpha.animateTo(
      targetValue = 1f,
      animationSpec = tween(400, easing = FastOutSlowInEasing)
    )
    animatableScale.animateTo(
      targetValue = 1f,
      animationSpec = tween(500, easing = FastOutSlowInEasing)
    )
    delay(450) // Fast transition, no unnecessary delay
    onFinishSplash()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .scale(animatableScale.value)
        .alpha(animatableAlpha.value)
    ) {
      StrugxEmblem(size = 72.dp, animateGlow = true)

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "STRUGX",
        fontSize = 32.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 6.sp,
        color = PureWhite
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "FOLLOWERS • VIEWS • STRUGS",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.5.sp,
        color = TextMuted
      )
    }

    // Bottom subtle developer credit
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 36.dp)
        .alpha(animatableAlpha.value)
    ) {
      Text(
        text = "DEVELOPED BY AJAY SAINI",
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        color = TextMuted.copy(alpha = 0.6f)
      )
    }
  }
}
