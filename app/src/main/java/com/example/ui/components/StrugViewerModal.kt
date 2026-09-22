package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.Coil
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.models.Strug
import com.example.data.music.GlobalAudioPlayer
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun StrugViewerModal(
  allUserGroups: List<List<Strug>>,
  initialUserIndex: Int = 0,
  initialStrugIndex: Int = 0,
  currentUserId: String,
  onDismiss: () -> Unit,
  onReply: (strug: Strug, text: String) -> Unit
) {
  if (allUserGroups.isEmpty()) {
    onDismiss()
    return
  }

  var currentUserIndex by remember {
    mutableIntStateOf(initialUserIndex.coerceIn(0, allUserGroups.size - 1))
  }
  val currentGroup = allUserGroups.getOrNull(currentUserIndex) ?: emptyList()
  if (currentGroup.isEmpty()) {
    onDismiss()
    return
  }

  var currentStrugIndex by remember(currentUserIndex) {
    mutableIntStateOf(if (currentUserIndex == initialUserIndex) initialStrugIndex.coerceIn(0, currentGroup.size - 1) else 0)
  }
  val currentStrug = currentGroup.getOrElse(currentStrugIndex) { currentGroup.first() }
  val (fontStyle, cleanCaption) = remember(currentStrug.caption) {
    parseStrugCaption(currentStrug.caption)
  }

  var isPaused by remember { mutableStateOf(false) }
  var replyText by remember { mutableStateOf("") }
  var isSending by remember { mutableStateOf(false) }
  val progress = remember { Animatable(0f) }
  var verticalDragOffset by remember { mutableFloatStateOf(0f) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val activeAudioUrl by GlobalAudioPlayer.activeTrackUrl.collectAsState()
  val isAudioPlaying by GlobalAudioPlayer.isPlaying.collectAsState()
  val isStrugAudioPlaying = currentStrug.musicAudioUrl.isNotBlank() && activeAudioUrl == currentStrug.musicAudioUrl && isAudioPlaying

  // Play audio when strug with music comes into view
  LaunchedEffect(currentStrug.strugId, currentStrug.musicAudioUrl) {
    if (currentStrug.musicAudioUrl.isNotBlank()) {
      GlobalAudioPlayer.play(currentStrug.musicAudioUrl)
    } else {
      GlobalAudioPlayer.stop()
    }
  }

  // Stop music when strug viewer is closed
  DisposableEffect(Unit) {
    onDispose {
      GlobalAudioPlayer.stop()
    }
  }

  // Pause music if user pauses story
  LaunchedEffect(isPaused) {
    if (currentStrug.musicAudioUrl.isNotBlank()) {
      if (isPaused) {
        GlobalAudioPlayer.pause()
      } else if (activeAudioUrl == currentStrug.musicAudioUrl) {
        GlobalAudioPlayer.resume()
      }
    }
  }

  // 1. Intelligent Next Strug Image Preloading
  LaunchedEffect(currentUserIndex, currentStrugIndex) {
    val nextUrl = if (currentStrugIndex < currentGroup.size - 1) {
      currentGroup[currentStrugIndex + 1].imageUrl
    } else if (currentUserIndex < allUserGroups.size - 1 && allUserGroups[currentUserIndex + 1].isNotEmpty()) {
      allUserGroups[currentUserIndex + 1].first().imageUrl
    } else null

    if (!nextUrl.isNullOrBlank()) {
      val request = ImageRequest.Builder(context)
        .data(nextUrl)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
      Coil.imageLoader(context).enqueue(request)
    }
  }

  // 2. Story Progress Timer: 6 seconds per Strug. Pauses if user taps image or types reply.
  LaunchedEffect(currentUserIndex, currentStrugIndex, currentStrug.strugId, isPaused, replyText.isNotEmpty()) {
    if (isPaused || replyText.isNotEmpty()) return@LaunchedEffect

    val remaining = 1f - progress.value
    val duration = (6000 * remaining).toInt().coerceAtLeast(100)

    progress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = duration, easing = LinearEasing)
    )

    delay(80)

    // Advance to next Strug of current user, or next user's Strugs
    if (currentStrugIndex < currentGroup.size - 1) {
      progress.snapTo(0f)
      currentStrugIndex++
    } else if (currentUserIndex < allUserGroups.size - 1) {
      progress.snapTo(0f)
      currentUserIndex++
      currentStrugIndex = 0
    } else {
      onDismiss()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = (1f - (verticalDragOffset / 800f)).coerceIn(0.2f, 1f)))
        .graphicsLayer {
          translationY = verticalDragOffset
        }
        // Downward vertical drag gesture to dismiss
        .pointerInput(Unit) {
          detectVerticalDragGestures(
            onDragEnd = {
              if (verticalDragOffset > 120f) {
                onDismiss()
              } else {
                verticalDragOffset = 0f
              }
            },
            onVerticalDrag = { _, dragAmount ->
              if (dragAmount > 0 || verticalDragOffset > 0) {
                verticalDragOffset = (verticalDragOffset + dragAmount).coerceAtLeast(0f)
              }
            }
          )
        }
        // Press and hold anywhere to pause story and music; release to resume!
        // Left/Right tap for navigation
        .pointerInput(currentUserIndex, currentStrugIndex) {
          detectTapGestures(
            onPress = {
              val wasPaused = isPaused
              isPaused = true
              try {
                tryAwaitRelease()
              } finally {
                isPaused = wasPaused
              }
            },
            onTap = { offset ->
              val screenWidth = size.width
              if (offset.x < screenWidth * 0.28f) {
                // Tap Left -> Previous Strug / Previous User
                if (currentStrugIndex > 0) {
                  scope.launch { progress.snapTo(0f) }
                  currentStrugIndex--
                } else if (currentUserIndex > 0) {
                  scope.launch { progress.snapTo(0f) }
                  currentUserIndex--
                  currentStrugIndex = allUserGroups[currentUserIndex].size - 1
                } else {
                  scope.launch { progress.snapTo(0f) }
                }
              } else if (offset.x > screenWidth * 0.72f) {
                // Tap Right -> Next Strug / Next User
                if (currentStrugIndex < currentGroup.size - 1) {
                  scope.launch { progress.snapTo(0f) }
                  currentStrugIndex++
                } else if (currentUserIndex < allUserGroups.size - 1) {
                  scope.launch { progress.snapTo(0f) }
                  currentUserIndex++
                  currentStrugIndex = 0
                } else {
                  onDismiss()
                }
              }
            }
          )
        }
    ) {
      // 1. Strug Media & Centered Text Container (positioned right at the bottom center of the image area)
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 85.dp
          ),
        contentAlignment = Alignment.Center
      ) {
        AsyncImage(
          model = currentStrug.imageUrl,
          contentDescription = "Strug by ${currentStrug.username}",
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize()
        )

        // Custom Font Caption placed right at the bottom center where image content is
        if (cleanCaption.isNotBlank()) {
          Surface(
            color = PureBlack.copy(alpha = 0.72f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(horizontal = 20.dp, vertical = 10.dp)
          ) {
            Text(
              text = cleanCaption,
              color = PureWhite,
              fontSize = 15.sp,
              fontFamily = fontStyle.fontFamily,
              fontWeight = fontStyle.fontWeight,
              fontStyle = fontStyle.fontStyle,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
          }
        }
      }

      // Top gradient overlay for header readability
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .align(Alignment.TopCenter)
          .background(
            Brush.verticalGradient(
              listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
            )
          )
      )

      // 2. Top Bar: Current User's Segments + Author Info + Pause Indicator + Close
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(horizontal = 14.dp, vertical = 8.dp)
          .align(Alignment.TopCenter)
      ) {
        // Multi-segment progress bar for the CURRENT user's Strugs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          currentGroup.forEachIndexed { index, _ ->
            val segProgress = when {
              index < currentStrugIndex -> 1f
              index == currentStrugIndex -> progress.value
              else -> 0f
            }
            LinearProgressIndicator(
              progress = { segProgress },
              modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
              color = PureWhite,
              trackColor = Color.White.copy(alpha = 0.25f)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .border(1.dp, PureWhite, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              if (currentStrug.userProfileImageUrl.isNotBlank()) {
                AsyncImage(
                  model = currentStrug.userProfileImageUrl,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = PureWhite,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
              Text(
                text = currentStrug.username,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = PureWhite
              )
              if (currentStrug.musicTitle.isNotBlank()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PureWhite.copy(alpha = 0.85f),
                    modifier = Modifier.size(11.dp)
                  )
                  Text(
                    text = if (currentStrug.musicArtist.isNotBlank()) "${currentStrug.musicTitle} • ${currentStrug.musicArtist}" else currentStrug.musicTitle,
                    fontSize = 11.sp,
                    color = PureWhite.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp)
                  )
                }
              } else if (currentGroup.size > 1) {
                Text(
                  text = "${currentStrugIndex + 1} of ${currentGroup.size}",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Visual Pause Indicator Badge (shown when user presses and holds)
            if (isPaused) {
              Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.padding(end = 8.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Paused",
                    tint = PureWhite,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Paused", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.SemiBold)
                }
              }
            }

            // Close Button
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = PureWhite,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }
      }

      // 3. Bottom Area: Direct Reply Bar
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
            )
          )
          .navigationBarsPadding()
          .imePadding()
          .padding(horizontal = 16.dp)
          .padding(bottom = 24.dp, top = 8.dp)
      ) {
        // Direct Reply Bar (only if viewing another user's Strug)
        if (currentStrug.userId != currentUserId) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = replyText,
              onValueChange = { replyText = it },
              placeholder = { Text("Reply to ${currentStrug.username}...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(24.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.16f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                focusedBorderColor = PureWhite,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
              ),
              singleLine = true
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
              onClick = {
                if (replyText.isNotBlank() && !isSending) {
                  isSending = true
                  val textToSend = replyText.trim()
                  replyText = ""
                  onReply(currentStrug, textToSend)
                  isSending = false
                }
              },
              enabled = replyText.isNotBlank() && !isSending,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (replyText.isNotBlank() && !isSending) PureWhite else Color.White.copy(alpha = 0.2f))
            ) {
              if (isSending) {
                CircularProgressIndicator(
                  color = PureBlack,
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Send,
                  contentDescription = "Send Reply",
                  tint = if (replyText.isNotBlank() && !isSending) PureBlack else Color.White.copy(alpha = 0.5f),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

// Backward-compatible overload for single list of Strugs
@Composable
fun StrugViewerModal(
  strugs: List<Strug>,
  initialIndex: Int = 0,
  currentUserId: String,
  onDismiss: () -> Unit,
  onReply: (strug: Strug, text: String) -> Unit
) {
  // Group by userId preserving order
  val grouped = remember(strugs) {
    val map = linkedMapOf<String, MutableList<Strug>>()
    for (s in strugs) {
      map.getOrPut(s.userId) { mutableListOf() }.add(s)
    }
    map.values.map { it.toList() }
  }

  // Find user group containing initialIndex Strug
  val targetStrug = strugs.getOrNull(initialIndex)
  val userIndex = if (targetStrug != null) {
    grouped.indexOfFirst { group -> group.any { it.strugId == targetStrug.strugId } }.coerceAtLeast(0)
  } else 0

  val strugIndex = if (targetStrug != null && userIndex in grouped.indices) {
    grouped[userIndex].indexOfFirst { it.strugId == targetStrug.strugId }.coerceAtLeast(0)
  } else 0

  StrugViewerModal(
    allUserGroups = grouped,
    initialUserIndex = userIndex,
    initialStrugIndex = strugIndex,
    currentUserId = currentUserId,
    onDismiss = onDismiss,
    onReply = onReply
  )
}
