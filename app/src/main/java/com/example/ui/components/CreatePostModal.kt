package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.music.MusicTrack
import com.example.data.storage.ImageOptimizer
import com.example.data.storage.SupabaseStorageService
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

enum class CreateMode {
  SELECTION,
  IMAGE_POST,
  TEXT_POST,
  STRUG
}

enum class PhotoFilter(val displayName: String) {
  NORMAL("Normal"),
  WARM("Warm"),
  COOL("Cool"),
  VINTAGE("Vintage"),
  MONO("Mono"),
  DRAMATIC("Dramatic")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostModal(
  currentUserId: String,
  currentUsername: String,
  currentUserAvatar: String,
  onDismiss: () -> Unit,
  onCreateImagePost: (imageUrl: String, caption: String, musicTrack: MusicTrack?) -> Unit,
  onCreateTextPost: (text: String, musicTrack: MusicTrack?) -> Unit,
  onCreateStrug: (imageUrl: String, caption: String, musicTrack: MusicTrack?) -> Unit
) {
  var mode by remember { mutableStateOf(CreateMode.SELECTION) }
  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var captionText by remember { mutableStateOf("") }
  var selectedMusicTrack by remember { mutableStateOf<MusicTrack?>(null) }
  var selectedFilter by remember { mutableStateOf(PhotoFilter.NORMAL) }
  var selectedStrugFont by remember { mutableStateOf(StrugFontStyle.MODERN) }
  var showMusicPicker by remember { mutableStateOf(false) }
  var isUploading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedImageUri = uri
      errorMessage = null
    } else if (selectedImageUri == null && mode != CreateMode.SELECTION && mode != CreateMode.TEXT_POST) {
      mode = CreateMode.SELECTION
    }
  }

  // Helper color filter for compose AsyncImage
  val composeColorFilter = remember(selectedFilter) {
    getColorFilterForFilter(selectedFilter)
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
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
      when (mode) {
        CreateMode.SELECTION -> {
          Text(
            text = "Create on Strugx",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
          Spacer(modifier = Modifier.height(18.dp))

          // Option 1: Create Post (Image + Text + Filters + Music)
          CreateChoiceCard(
            icon = Icons.Default.Image,
            title = "Create Post",
            subtitle = "Share photo with filters, text, and music",
            onClick = {
              mode = CreateMode.IMAGE_POST
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Option 2: Create Strug (Story with Filters, Music & Text)
          CreateChoiceCard(
            icon = Icons.Default.AutoAwesome,
            title = "Create Strug (Story)",
            subtitle = "24-hour visual story with music and filters",
            onClick = {
              mode = CreateMode.STRUG
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Option 3: Create Text (Text-only Post with optional music)
          CreateChoiceCard(
            icon = Icons.Default.TextFields,
            title = "Create Text",
            subtitle = "Share your thoughts with optional background music",
            onClick = { mode = CreateMode.TEXT_POST }
          )

          Spacer(modifier = Modifier.height(16.dp))
        }

        CreateMode.IMAGE_POST, CreateMode.STRUG -> {
          val isStrug = mode == CreateMode.STRUG

          // Top Header Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  mode = CreateMode.SELECTION
                  selectedImageUri = null
                  selectedFilter = PhotoFilter.NORMAL
                  selectedMusicTrack = null
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isStrug) "New Strug Story" else "New Post",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
              )
            }

            // Quick Music Button on top right
            Surface(
              onClick = { showMusicPicker = true },
              shape = RoundedCornerShape(16.dp),
              color = if (selectedMusicTrack != null) PureWhite.copy(alpha = 0.15f) else GlassSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedMusicTrack != null) PureWhite.copy(alpha = 0.6f) else GlassBorder)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.MusicNote,
                  contentDescription = null,
                  tint = PureWhite,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (selectedMusicTrack != null) selectedMusicTrack!!.trackName else "Music",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = PureWhite,
                  maxLines = 1,
                  modifier = Modifier.widthIn(max = 110.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Image Preview with Live Filter and Live Overlay Caption
          if (selectedImageUri != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            ) {
              AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected image preview",
                contentScale = ContentScale.Crop,
                colorFilter = composeColorFilter,
                modifier = Modifier.fillMaxSize()
              )

              // Subtle gradient overlay for readability
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                      colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.35f)
                      )
                    )
                  )
              )

              // Change photo button (top-right)
              Surface(
                color = PureBlack.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(8.dp)
                  .clickable {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Change", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
              }

              // Selected Music Chip on the image preview
              if (selectedMusicTrack != null) {
                Surface(
                  color = PureBlack.copy(alpha = 0.75f),
                  shape = RoundedCornerShape(16.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                  modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "${selectedMusicTrack!!.trackName} • ${selectedMusicTrack!!.artistName}",
                      color = PureWhite,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      modifier = Modifier.widthIn(max = 160.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Remove",
                      tint = TextMuted,
                      modifier = Modifier
                        .size(14.dp)
                        .clickable { selectedMusicTrack = null }
                    )
                  }
                }
              }

              // Live text preview over the image (Centered at the bottom of the image for Strug)
              if (captionText.isNotBlank()) {
                Box(
                  modifier = Modifier
                    .align(if (isStrug) Alignment.BottomCenter else Alignment.BottomStart)
                    .padding(if (isStrug) PaddingValues(bottom = 14.dp, start = 16.dp, end = 16.dp) else PaddingValues(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                  Text(
                    text = captionText,
                    color = PureWhite,
                    fontSize = if (isStrug) 14.sp else 13.sp,
                    fontFamily = if (isStrug) selectedStrugFont.fontFamily else androidx.compose.ui.text.font.FontFamily.Default,
                    fontWeight = if (isStrug) selectedStrugFont.fontWeight else FontWeight.Medium,
                    fontStyle = if (isStrug) selectedStrugFont.fontStyle else androidx.compose.ui.text.font.FontStyle.Normal,
                    textAlign = if (isStrug) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
                    maxLines = 3
                  )
                }
              }
            }
          } else {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassSurface)
                .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                .clickable {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PureWhite, modifier = Modifier.size(38.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select an Image", fontWeight = FontWeight.SemiBold, color = PureWhite)
                Text("Supports standard image formats", fontSize = 12.sp, color = TextSecondary)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Instagram-style Filter Selector Carousel
          if (selectedImageUri != null) {
            Column {
              Text(
                text = "Filters",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                PhotoFilter.values().forEach { filter ->
                  val isSelected = selectedFilter == filter
                  Surface(
                    onClick = { selectedFilter = filter },
                    color = if (isSelected) PureWhite else GlassSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (isSelected) PureWhite else GlassBorder
                    )
                  ) {
                    Text(
                      text = filter.displayName,
                      color = if (isSelected) PureBlack else PureWhite,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          // Instagram-style Font Selector Carousel for Strug
          if (isStrug) {
            Column {
              Text(
                text = "Font Style",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                StrugFontStyle.values().forEach { font ->
                  val isSelected = selectedStrugFont == font
                  Surface(
                    onClick = { selectedStrugFont = font },
                    color = if (isSelected) PureWhite else GlassSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (isSelected) PureWhite else GlassBorder
                    )
                  ) {
                    Text(
                      text = font.displayName,
                      fontFamily = font.fontFamily,
                      fontWeight = font.fontWeight,
                      fontStyle = font.fontStyle,
                      color = if (isSelected) PureBlack else PureWhite,
                      fontSize = 12.sp,
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          // Caption / Text writing field
          OutlinedTextField(
            value = captionText,
            onValueChange = { captionText = it },
            placeholder = { Text(if (isStrug) "Add text to your story..." else "Write caption or text...", color = TextMuted) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder,
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface
            ),
            maxLines = 3
          )

          // Music selection row (Available for both Post and Strug)
          Spacer(modifier = Modifier.height(10.dp))
          if (selectedMusicTrack != null) {
            Surface(
              color = GlassSurface,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (selectedMusicTrack!!.artworkUrl.isNotBlank()) {
                  AsyncImage(
                    model = selectedMusicTrack!!.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .size(38.dp)
                      .clip(RoundedCornerShape(8.dp))
                  )
                } else {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(DarkBackground),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = PureWhite)
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = selectedMusicTrack!!.trackName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1
                  )
                  Text(
                    text = selectedMusicTrack!!.artistName,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                  )
                }

                IconButton(
                  onClick = { selectedMusicTrack = null },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Remove music", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
              }
            }
          } else {
            Surface(
              color = GlassSurface,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .clickable { showMusicPicker = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.MusicNote, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(10.dp))
                  Text("Add Music", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                }
                Text("Select track →", fontSize = 12.sp, color = TextMuted)
              }
            }
          }

          if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage ?: "", color = DangerRed, fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Submit button
          Button(
            onClick = {
              if (selectedImageUri == null) {
                errorMessage = "Please select an image first."
                return@Button
              }

              isUploading = true
              errorMessage = null

              scope.launch {
                try {
                  val compressResult = ImageOptimizer.compressAndValidate(
                    context = context,
                    uri = selectedImageUri!!,
                    maxDimension = 1280,
                    quality = 85
                  )

                  if (compressResult.isFailure) {
                    errorMessage = compressResult.exceptionOrNull()?.message ?: "Invalid image"
                    isUploading = false
                    return@launch
                  }

                  val rawBytes = compressResult.getOrThrow()

                  // Apply selected filter to bitmap if not NORMAL
                  val finalBytes = if (selectedFilter != PhotoFilter.NORMAL) {
                    applyFilterToImageBytes(rawBytes, selectedFilter)
                  } else {
                    rawBytes
                  }

                  val folder = if (isStrug) "strugs" else "posts"
                  val uploadResult = SupabaseStorageService.uploadImage(
                    bytes = finalBytes,
                    folder = folder,
                    userId = currentUserId
                  )

                  if (uploadResult.isFailure) {
                    errorMessage = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                    isUploading = false
                    return@launch
                  }

                  val uploadedUrl = uploadResult.getOrThrow()

                  if (isStrug) {
                    val finalStrugCaption = formatStrugCaption(selectedStrugFont, captionText)
                    onCreateStrug(uploadedUrl, finalStrugCaption, selectedMusicTrack)
                  } else {
                    onCreateImagePost(uploadedUrl, captionText.trim(), selectedMusicTrack)
                  }

                  isUploading = false
                  onDismiss()
                } catch (e: Exception) {
                  errorMessage = e.message ?: "Upload encountered an error"
                  isUploading = false
                }
              }
            },
            enabled = !isUploading && selectedImageUri != null,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = PureWhite,
              contentColor = PureBlack,
              disabledContainerColor = PureWhite.copy(alpha = 0.35f),
              disabledContentColor = PureBlack.copy(alpha = 0.5f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
          ) {
            if (isUploading) {
              CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Text("Uploading...", fontWeight = FontWeight.Bold)
            } else {
              Text(if (isStrug) "Share Strug" else "Publish Post", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }
        }

        CreateMode.TEXT_POST -> {
          // Top Header Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                mode = CreateMode.SELECTION
                selectedMusicTrack = null
              },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "New Text Post",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = PureWhite
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          OutlinedTextField(
            value = captionText,
            onValueChange = { captionText = it },
            placeholder = { Text("What's on your mind?", color = TextMuted) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder,
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface
            ),
            maxLines = 8
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Music selection row for Text post
          if (selectedMusicTrack != null) {
            Surface(
              color = GlassSurface,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (selectedMusicTrack!!.artworkUrl.isNotBlank()) {
                  AsyncImage(
                    model = selectedMusicTrack!!.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .size(38.dp)
                      .clip(RoundedCornerShape(8.dp))
                  )
                } else {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(DarkBackground),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = PureWhite)
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = selectedMusicTrack!!.trackName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1
                  )
                  Text(
                    text = selectedMusicTrack!!.artistName,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                  )
                }

                IconButton(
                  onClick = { selectedMusicTrack = null },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Remove music", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
              }
            }
          } else {
            Surface(
              color = GlassSurface,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .clickable { showMusicPicker = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.MusicNote, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(10.dp))
                  Text("Add Music", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                }
                Text("Select track →", fontSize = 12.sp, color = TextMuted)
              }
            }
          }

          if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage ?: "", color = DangerRed, fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.height(18.dp))

          Button(
            onClick = {
              if (captionText.isBlank()) {
                errorMessage = "Text post cannot be empty."
                return@Button
              }
              onCreateTextPost(captionText.trim(), selectedMusicTrack)
              onDismiss()
            },
            enabled = captionText.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
          ) {
            Text("Publish Text Post", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }
    }
  }

  // Music Picker Bottom Sheet
  if (showMusicPicker) {
    MusicSearchSheet(
      onDismiss = { showMusicPicker = false },
      onTrackSelected = { track ->
        selectedMusicTrack = track
        showMusicPicker = false
      }
    )
  }
}

@Composable
private fun CreateChoiceCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    color = GlassSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(GlassSurface)
          .border(1.dp, GlassBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = PureWhite, modifier = Modifier.size(24.dp))
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PureWhite)
        Text(subtitle, fontSize = 12.sp, color = TextSecondary)
      }
    }
  }
}

// Helper function to provide Compose ColorFilter for preview
private fun getColorFilterForFilter(filter: PhotoFilter): androidx.compose.ui.graphics.ColorFilter? {
  val matrix = getFilterColorMatrix(filter) ?: return null
  return androidx.compose.ui.graphics.ColorFilter.colorMatrix(
    androidx.compose.ui.graphics.ColorMatrix(matrix.array)
  )
}

// Android Graphics ColorMatrix for filters
private fun getFilterColorMatrix(filter: PhotoFilter): ColorMatrix? {
  return when (filter) {
    PhotoFilter.NORMAL -> null
    PhotoFilter.WARM -> ColorMatrix(
      floatArrayOf(
        1.1f, 0f, 0f, 0f, 20f,
        0f, 1.05f, 0f, 0f, 10f,
        0f, 0f, 0.9f, 0f, -10f,
        0f, 0f, 0f, 1f, 0f
      )
    )
    PhotoFilter.COOL -> ColorMatrix(
      floatArrayOf(
        0.9f, 0f, 0f, 0f, -10f,
        0f, 1.0f, 0f, 0f, 5f,
        0f, 0f, 1.15f, 0f, 25f,
        0f, 0f, 0f, 1f, 0f
      )
    )
    PhotoFilter.VINTAGE -> ColorMatrix(
      floatArrayOf(
        0.9f, 0f, 0f, 0f, 30f,
        0f, 0.8f, 0f, 0f, 20f,
        0f, 0f, 0.6f, 0f, 10f,
        0f, 0f, 0f, 1f, 0f
      )
    )
    PhotoFilter.MONO -> ColorMatrix().apply { setSaturation(0f) }
    PhotoFilter.DRAMATIC -> ColorMatrix(
      floatArrayOf(
        1.3f, 0f, 0f, 0f, -25f,
        0f, 1.3f, 0f, 0f, -25f,
        0f, 0f, 1.3f, 0f, -25f,
        0f, 0f, 0f, 1f, 0f
      )
    )
  }
}

// Apply filter directly to bitmap bytes before uploading
private suspend fun applyFilterToImageBytes(bytes: ByteArray, filter: PhotoFilter): ByteArray = withContext(Dispatchers.IO) {
  try {
    val srcBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext bytes
    val cm = getFilterColorMatrix(filter) ?: return@withContext bytes
    val filteredBitmap = Bitmap.createBitmap(srcBitmap.width, srcBitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(filteredBitmap)
    val paint = Paint().apply {
      colorFilter = ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(srcBitmap, 0f, 0f, paint)

    val outputStream = ByteArrayOutputStream()
    filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 88, outputStream)
    val result = outputStream.toByteArray()

    srcBitmap.recycle()
    filteredBitmap.recycle()
    result
  } catch (e: Exception) {
    bytes
  }
}
