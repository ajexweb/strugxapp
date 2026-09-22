package com.example.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.FirebaseService
import com.example.data.models.User
import com.example.data.storage.ImageOptimizer
import com.example.data.storage.SupabaseStorageService
import com.example.ui.components.StrugxEmblem
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProfileSetupScreen(
  currentUid: String,
  email: String,
  initialUser: User?,
  onProfileCompleted: (User) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val suggestedBase = email.substringBefore("@").replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase(Locale.ROOT)
  var username by remember { mutableStateOf(initialUser?.username?.ifBlank { suggestedBase } ?: suggestedBase) }
  var displayName by remember { mutableStateOf(initialUser?.displayName?.ifBlank { suggestedBase } ?: suggestedBase) }
  var bio by remember { mutableStateOf(initialUser?.bio ?: "") }

  var avatarUri by remember { mutableStateOf<Uri?>(null) }
  var uploadedAvatarUrl by remember { mutableStateOf(initialUser?.profileImageUrl ?: "") }

  var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
  var isCheckingUsername by remember { mutableStateOf(false) }
  var checkJob by remember { mutableStateOf<Job?>(null) }

  var isSubmitting by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      avatarUri = uri
    }
  }

  // Live username uniqueness debounced check
  LaunchedEffect(username) {
    val clean = username.trim().lowercase(Locale.ROOT)
    if (clean.length < 3 || !clean.matches(Regex("^[a-z0-9._]{3,24}$"))) {
      isUsernameAvailable = null
      isCheckingUsername = false
      return@LaunchedEffect
    }
    isCheckingUsername = true
    checkJob?.cancel()
    checkJob = launch {
      delay(350)
      val available = FirebaseService.checkUsernameAvailable(clean, currentUid)
      isUsernameAvailable = available
      isCheckingUsername = false
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Black safe status bar
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(top = 40.dp, bottom = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      StrugxEmblem(size = 46.dp)

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "SET UP YOUR PROFILE",
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = PureWhite
      )

      Text(
        text = "Choose your username and appearance on Strugx",
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = Modifier.padding(top = 6.dp)
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Avatar selection container
      Box(
        modifier = Modifier
          .size(100.dp)
          .clip(CircleShape)
          .background(GlassSurface)
          .border(2.dp, GlassBorder, CircleShape)
          .clickable {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        contentAlignment = Alignment.Center
      ) {
        if (avatarUri != null) {
          AsyncImage(
            model = avatarUri,
            contentDescription = "Avatar Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else if (uploadedAvatarUrl.isNotBlank()) {
          AsyncImage(
            model = uploadedAvatarUrl,
            contentDescription = "Avatar Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = "Select Photo",
              tint = PureWhite.copy(alpha = 0.8f),
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Add Photo",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = TextSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Username Field
      OutlinedTextField(
        value = username,
        onValueChange = { input ->
          username = input.filter { !it.isWhitespace() }.lowercase(Locale.ROOT)
          errorMessage = null
        },
        label = { Text("Username", color = TextSecondary) },
        placeholder = { Text("e.g. ajay", color = TextMuted) },
        prefix = { Text("@", color = TextSecondary, fontWeight = FontWeight.Bold) },
        singleLine = true,
        isError = isUsernameAvailable == false,
        trailingIcon = {
          if (isCheckingUsername) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PureWhite)
          } else if (isUsernameAvailable == true) {
            Icon(Icons.Default.Check, contentDescription = "Available", tint = SuccessGreen)
          } else if (isUsernameAvailable == false) {
            Icon(Icons.Default.Close, contentDescription = "Taken", tint = DangerRed)
          }
        },
        supportingText = {
          when (isUsernameAvailable) {
            true -> Text("Username is available", color = SuccessGreen, fontSize = 11.sp)
            false -> Text("Username is already taken", color = DangerRed, fontSize = 11.sp)
            null -> Text("Letters, numbers, . and _ only (min 3 chars)", color = TextMuted, fontSize = 11.sp)
          }
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Profile Name Field
      OutlinedTextField(
        value = displayName,
        onValueChange = {
          displayName = it
          errorMessage = null
        },
        label = { Text("Profile Name", color = TextSecondary) },
        placeholder = { Text("e.g. Ajay Saini", color = TextMuted) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Bio Field
      OutlinedTextField(
        value = bio,
        onValueChange = { if (it.length <= 160) bio = it },
        label = { Text("Bio", color = TextSecondary) },
        placeholder = { Text("Write a short bio...", color = TextMuted) },
        maxLines = 3,
        supportingText = {
          Text("${bio.length}/160", color = TextMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          color = DangerRedLight,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage!!,
            color = DangerRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Complete Profile Action Button
      Button(
        onClick = {
          val cleanUser = username.trim().lowercase(Locale.ROOT)
          val cleanName = displayName.trim().ifBlank { cleanUser }

          if (cleanUser.length < 3) {
            errorMessage = "Username must be at least 3 characters."
            return@Button
          }
          if (!cleanUser.matches(Regex("^[a-z0-9._]{3,24}$"))) {
            errorMessage = "Username can only contain lowercase letters, numbers, '.', and '_'."
            return@Button
          }

          isSubmitting = true
          errorMessage = null

          scope.launch {
            try {
              // 1. Upload avatar if newly picked
              var finalAvatarUrl = uploadedAvatarUrl
              if (avatarUri != null) {
                val compressRes = ImageOptimizer.compressAndValidate(context, avatarUri!!, maxDimension = 512, quality = 85)
                if (compressRes.isSuccess) {
                  val uploadRes = SupabaseStorageService.uploadImage(
                    bytes = compressRes.getOrThrow(),
                    folder = "profiles",
                    userId = currentUid
                  )
                  if (uploadRes.isSuccess) {
                    finalAvatarUrl = uploadRes.getOrThrow()
                    uploadedAvatarUrl = finalAvatarUrl
                  }
                }
              }

              // 2. Save complete profile in Firestore
              val result = FirebaseService.completeProfile(
                uid = currentUid,
                username = cleanUser,
                displayName = cleanName,
                bio = bio.trim(),
                profileImageUrl = finalAvatarUrl
              )

              if (result.isSuccess) {
                onProfileCompleted(result.getOrThrow())
              } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to save profile."
              }
            } catch (e: Exception) {
              errorMessage = e.message ?: "An unexpected error occurred."
            } finally {
              isSubmitting = false
            }
          }
        },
        enabled = !isSubmitting && isUsernameAvailable != false,
        colors = ButtonDefaults.buttonColors(
          containerColor = PureWhite,
          contentColor = PureBlack,
          disabledContainerColor = PureWhite.copy(alpha = 0.35f),
          disabledContentColor = PureBlack.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PureBlack)
          Spacer(modifier = Modifier.width(10.dp))
          Text("Saving Profile...", fontWeight = FontWeight.Bold)
        } else {
          Text("Complete Profile & Enter", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }
    }
  }
}
