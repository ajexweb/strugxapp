package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.music.GlobalAudioPlayer
import com.example.data.music.MusicService
import com.example.data.music.MusicTrack
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSearchSheet(
  onDismiss: () -> Unit,
  onTrackSelected: (MusicTrack) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var selectedGenre by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  val activeAudioUrl by GlobalAudioPlayer.activeTrackUrl.collectAsState()
  val isAudioPlaying by GlobalAudioPlayer.isPlaying.collectAsState()

  // Clean up playback when dismissing sheet
  DisposableEffect(Unit) {
    onDispose {
      GlobalAudioPlayer.stop()
    }
  }

  // Load initial trending tracks
  LaunchedEffect(Unit) {
    isLoading = true
    tracks = MusicService.getTrendingTracks()
    isLoading = false
  }

  // Debounced search
  LaunchedEffect(searchQuery, selectedGenre) {
    val queryToRun = if (searchQuery.isNotBlank()) searchQuery else selectedGenre ?: ""
    if (queryToRun.isBlank()) return@LaunchedEffect

    delay(300)
    isLoading = true
    tracks = MusicService.searchTracks(queryToRun)
    isLoading = false
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = DarkSurfaceElevated,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.85f)
        .padding(horizontal = 18.dp)
        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.MusicNote, contentDescription = null, tint = PureWhite, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Add Music",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = {
          searchQuery = it
          if (it.isNotBlank()) selectedGenre = null
        },
        placeholder = { Text("Search songs, artists, genres...", color = TextMuted, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Genre pills
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(MusicService.POPULAR_GENRES) { genre ->
          val isSelected = selectedGenre == genre
          Surface(
            color = if (isSelected) PureWhite else GlassSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, if (isSelected) PureWhite else GlassBorder, RoundedCornerShape(16.dp))
              .clickable {
                if (isSelected) {
                  selectedGenre = null
                  searchQuery = ""
                  scope.launch {
                    isLoading = true
                    tracks = MusicService.getTrendingTracks()
                    isLoading = false
                  }
                } else {
                  selectedGenre = genre
                  searchQuery = genre
                }
              }
          ) {
            Text(
              text = genre,
              color = if (isSelected) PureBlack else PureWhite,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Track List / Loading
      if (isLoading) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(36.dp))
        }
      } else if (tracks.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No tracks found", color = TextSecondary, fontSize = 14.sp)
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(tracks, key = { it.trackId }) { track ->
            val isCurrentTrackPlaying = activeAudioUrl == track.previewUrl && isAudioPlaying

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassSurface)
                .border(1.dp, if (isCurrentTrackPlaying) PureWhite.copy(alpha = 0.6f) else GlassBorder, RoundedCornerShape(14.dp))
                .clickable {
                  GlobalAudioPlayer.stop()
                  onTrackSelected(track)
                }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Artwork with Play/Pause overlay
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(DarkBackground)
                  .clickable {
                    GlobalAudioPlayer.togglePlayPause(track.previewUrl)
                  },
                contentAlignment = Alignment.Center
              ) {
                if (track.artworkUrl.isNotBlank()) {
                  AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                }
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = if (isCurrentTrackPlaying) 0.5f else 0.3f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (isCurrentTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentTrackPlaying) "Pause" else "Play preview",
                    tint = PureWhite,
                    modifier = Modifier.size(26.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              // Title & Artist
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = track.trackName,
                  color = if (isCurrentTrackPlaying) ElectricIndigo else PureWhite,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = track.artistName,
                  color = TextSecondary,
                  fontSize = 12.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              // Select button
              Button(
                onClick = {
                  GlobalAudioPlayer.stop()
                  onTrackSelected(track)
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = PureWhite,
                  contentColor = PureBlack
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
