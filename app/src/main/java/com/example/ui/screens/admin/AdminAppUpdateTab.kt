package com.example.ui.screens.admin

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
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
import com.example.BuildConfig
import com.example.data.firebase.FirebaseService
import com.example.data.models.AppUpdateConfig
import com.example.data.storage.SupabaseStorageService
import com.example.ui.components.AppUpdateDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminAppUpdateTab(
  adminUid: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val currentVersionCode = BuildConfig.VERSION_CODE
  val currentVersionName = BuildConfig.VERSION_NAME

  var isEnabled by remember { mutableStateOf(false) }
  var isForceUpdate by remember { mutableStateOf(false) }
  var latestVersionCodeText by remember { mutableStateOf("${currentVersionCode + 1}") }
  var latestVersionNameText by remember { mutableStateOf("1.1") }
  var minVersionCodeText by remember { mutableStateOf("1") }
  var titleText by remember { mutableStateOf("New Update Available") }
  var descriptionText by remember { mutableStateOf("Performance improvements, new UI updates, and bug fixes.") }
  var imageUrlText by remember { mutableStateOf("") }
  var updateUrlText by remember { mutableStateOf("") }

  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var isUploadingImage by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var showPreviewModal by remember { mutableStateOf(false) }

  // Load existing configuration from Firestore
  LaunchedEffect(Unit) {
    val existing = FirebaseService.getAppUpdateConfig()
    isEnabled = existing.enabled
    isForceUpdate = existing.isForceUpdate
    latestVersionCodeText = existing.latestVersionCode.toString()
    latestVersionNameText = existing.latestVersionName
    minVersionCodeText = existing.minVersionCode.toString()
    if (existing.title.isNotBlank()) titleText = existing.title
    if (existing.description.isNotBlank()) descriptionText = existing.description
    imageUrlText = existing.imageUrl
    updateUrlText = existing.updateUrl
  }

  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedImageUri = uri
      scope.launch {
        isUploadingImage = true
        try {
          val inputStream = context.contentResolver.openInputStream(uri)
          val bytes = inputStream?.use { it.readBytes() } ?: ByteArray(0)
          val upRes = SupabaseStorageService.uploadImage(bytes, "updates", adminUid)
          if (upRes.isSuccess) {
            imageUrlText = upRes.getOrThrow()
            Toast.makeText(context, "Banner image uploaded!", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "Upload failed: ${upRes.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
          }
        } catch (e: Exception) {
          Toast.makeText(context, "Error uploading: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
          isUploadingImage = false
        }
      }
    }
  }

  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp)
  ) {
    // Header
    Text(
      text = "SYSTEM CONFIGURATION",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
    Text(
      text = "App Update Manager",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = PureWhite
    )
    Text(
      text = "Control in-app update popups, target version codes, and download URLs.",
      fontSize = 12.sp,
      color = TextSecondary,
      modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
    )

    // Current App Baseline Card
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = GlassSurfaceElevated,
      border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(PureWhite.copy(alpha = 0.1f))
            .border(1.dp, GlassBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = PureWhite, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Current Installed Build",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
          Text(
            text = "Version $currentVersionName • Code: $currentVersionCode",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = PureWhite.copy(alpha = 0.1f),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, PureWhite.copy(alpha = 0.3f))
        ) {
          Text(
            text = "Active APK",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Switch Card 1: Enable / Disable Update System
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = GlassSurface,
      border = androidx.compose.foundation.BorderStroke(1.dp, if (isEnabled) PureWhite.copy(alpha = 0.4f) else GlassBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Update Announcement Popup",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
          Text(
            text = if (isEnabled) "Active — Users with older versions will see the update dialog." else "Disabled — No update popups will be presented to users.",
            fontSize = 12.sp,
            color = if (isEnabled) PureWhite.copy(alpha = 0.8f) else TextSecondary
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
          checked = isEnabled,
          onCheckedChange = { isEnabled = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = PureBlack,
            checkedTrackColor = PureWhite,
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = GlassSurface
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Switch Card 2: Force Update
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = GlassSurface,
      border = androidx.compose.foundation.BorderStroke(1.dp, if (isForceUpdate) DangerRed.copy(alpha = 0.4f) else GlassBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Mandatory / Force Update",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isForceUpdate) DangerRed else PureWhite
          )
          Text(
            text = if (isForceUpdate) "Users cannot dismiss the dialog until they update the app." else "Optional — Users can tap 'Remind Me Later' and continue using the app.",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
          checked = isForceUpdate,
          onCheckedChange = { isForceUpdate = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = PureBlack,
            checkedTrackColor = DangerRed,
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = GlassSurface
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Section 2: Version Configuration
    Text(
      text = "TARGET VERSION DETAILS",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedTextField(
        value = latestVersionNameText,
        onValueChange = { latestVersionNameText = it },
        label = { Text("Version Name (e.g. 1.1)") },
        modifier = Modifier.weight(1f),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        shape = RoundedCornerShape(12.dp)
      )

      OutlinedTextField(
        value = latestVersionCodeText,
        onValueChange = { latestVersionCodeText = it.filter { ch -> ch.isDigit() } },
        label = { Text("Version Code") },
        modifier = Modifier.weight(1f),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        shape = RoundedCornerShape(12.dp)
      )
    }

    // Quick Increment Button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      horizontalArrangement = Arrangement.End
    ) {
      TextButton(
        onClick = {
          val nextCode = (latestVersionCodeText.toIntOrNull() ?: currentVersionCode) + 1
          latestVersionCodeText = nextCode.toString()
          latestVersionNameText = "1.$nextCode"
        }
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PureWhite)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Increment to Next Version", fontSize = 12.sp, color = PureWhite)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Title & Description
    Text(
      text = "CONTENT & DISPLAY",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = titleText,
      onValueChange = { titleText = it },
      label = { Text("Update Title") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedBorderColor = PureWhite,
        unfocusedBorderColor = GlassBorder,
        focusedContainerColor = GlassSurface,
        unfocusedContainerColor = GlassSurface
      ),
      shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = descriptionText,
      onValueChange = { descriptionText = it },
      label = { Text("Changelog / What's New") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 3,
      maxLines = 5,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedBorderColor = PureWhite,
        unfocusedBorderColor = GlassBorder,
        focusedContainerColor = GlassSurface,
        unfocusedContainerColor = GlassSurface
      ),
      shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Update URL
    Text(
      text = "TARGET DOWNLOAD LINK",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = updateUrlText,
      onValueChange = { updateUrlText = it },
      label = { Text("Download Link / Play Store / APK URL") },
      placeholder = { Text("https://example.com/download/app-latest.apk") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedBorderColor = PureWhite,
        unfocusedBorderColor = GlassBorder,
        focusedContainerColor = GlassSurface,
        unfocusedContainerColor = GlassSurface
      ),
      shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Banner Image
    Text(
      text = "BANNER IMAGE (OPTIONAL)",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = imageUrlText,
        onValueChange = { imageUrlText = it },
        label = { Text("Image URL or Upload Below") },
        modifier = Modifier.weight(1f),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        shape = RoundedCornerShape(12.dp)
      )

      Button(
        onClick = {
          imagePickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        enabled = !isUploadingImage,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = GlassSurfaceElevated,
          contentColor = PureWhite
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.height(52.dp)
      ) {
        if (isUploadingImage) {
          CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Upload", fontSize = 13.sp)
        }
      }
    }

    if (imageUrlText.isNotBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(PureBlack)
          .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
      ) {
        AsyncImage(
          model = imageUrlText,
          contentDescription = "Preview",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        IconButton(
          onClick = { imageUrlText = "" },
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(28.dp)
            .background(PureBlack.copy(alpha = 0.7f), CircleShape)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Clear", tint = PureWhite, modifier = Modifier.size(16.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Actions Row: Preview & Save
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedButton(
        onClick = { showPreviewModal = true },
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderFocused),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
      ) {
        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Preview Dialog", fontWeight = FontWeight.Bold)
      }

      Button(
        onClick = {
          val targetCode = latestVersionCodeText.toIntOrNull()
          if (targetCode == null || targetCode < 1) {
            Toast.makeText(context, "Please enter a valid version code number.", Toast.LENGTH_SHORT).show()
            return@Button
          }
          if (latestVersionNameText.isBlank()) {
            Toast.makeText(context, "Please enter a version name.", Toast.LENGTH_SHORT).show()
            return@Button
          }
          if (updateUrlText.isBlank()) {
            Toast.makeText(context, "Please provide an update download link.", Toast.LENGTH_SHORT).show()
            return@Button
          }

          scope.launch {
            isSaving = true
            val config = AppUpdateConfig(
              enabled = isEnabled,
              minVersionCode = minVersionCodeText.toIntOrNull() ?: 1,
              latestVersionCode = targetCode,
              latestVersionName = latestVersionNameText.trim(),
              title = titleText.trim(),
              description = descriptionText.trim(),
              imageUrl = imageUrlText.trim(),
              updateUrl = updateUrlText.trim(),
              isForceUpdate = isForceUpdate,
              updatedAt = System.currentTimeMillis()
            )
            val res = FirebaseService.saveAppUpdateConfig(config, adminUid)
            isSaving = false
            if (res.isSuccess) {
              Toast.makeText(context, "App update settings published successfully!", Toast.LENGTH_LONG).show()
            } else {
              Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
          }
        },
        enabled = !isSaving,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = PureWhite,
          contentColor = PureBlack
        ),
        modifier = Modifier
          .weight(1.2f)
          .height(50.dp)
      ) {
        if (isSaving) {
          CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Publish Update", fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(36.dp))
  }

  // Preview Dialog
  if (showPreviewModal) {
    AppUpdateDialog(
      updateConfig = AppUpdateConfig(
        enabled = isEnabled,
        latestVersionCode = latestVersionCodeText.toIntOrNull() ?: (currentVersionCode + 1),
        latestVersionName = latestVersionNameText.ifBlank { "1.1" },
        title = titleText.ifBlank { "New Version Available" },
        description = descriptionText.ifBlank { "Sample changelog notes..." },
        imageUrl = imageUrlText,
        updateUrl = updateUrlText.ifBlank { "https://example.com" },
        isForceUpdate = isForceUpdate
      ),
      currentVersionName = currentVersionName,
      currentVersionCode = currentVersionCode,
      onDismiss = { showPreviewModal = false }
    )
  }
}
