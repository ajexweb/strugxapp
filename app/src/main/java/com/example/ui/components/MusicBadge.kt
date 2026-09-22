package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.music.GlobalAudioPlayer
import com.example.ui.theme.*

@Composable
fun MusicBadge(
  title: String,
  artist: String,
  audioUrl: String,
  coverUrl: String = "",
  modifier: Modifier = Modifier
) {
  if (title.isBlank()) return

  val activeAudioUrl by GlobalAudioPlayer.activeTrackUrl.collectAsState()
  val isAudioPlaying by GlobalAudioPlayer.isPlaying.collectAsState()
  val isThisTrackPlaying = activeAudioUrl == audioUrl && isAudioPlaying

  // Spin rotation for disc artwork when playing
  val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "disc_rotation"
  )

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(PureBlack.copy(alpha = 0.65f))
      .border(1.dp, if (isThisTrackPlaying) PureWhite.copy(alpha = 0.8f) else GlassBorder, RoundedCornerShape(20.dp))
      .clickable {
        if (audioUrl.isNotBlank()) {
          GlobalAudioPlayer.togglePlayPause(audioUrl)
        }
      }
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Spinning disc or Play/Pause Icon
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(DarkSurfaceElevated),
        contentAlignment = Alignment.Center
      ) {
        if (coverUrl.isNotBlank()) {
          AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .rotate(if (isThisTrackPlaying) rotation else 0f)
          )
          // Center hole like vinyl disc
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(PureWhite)
          )
        } else {
          Icon(
            imageVector = if (isThisTrackPlaying) Icons.Default.Pause else Icons.Default.MusicNote,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      // Track info + Equalizer icon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        if (isThisTrackPlaying) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Playing",
            tint = PureWhite,
            modifier = Modifier.size(14.dp)
          )
        }
        Text(
          text = if (artist.isNotBlank()) "$title • $artist" else title,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = PureWhite,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Small Play/Pause indicator
      if (audioUrl.isNotBlank()) {
        Icon(
          imageVector = if (isThisTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (isThisTrackPlaying) "Pause" else "Play",
          tint = PureWhite,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}
