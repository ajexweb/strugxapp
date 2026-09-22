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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.FirebaseService
import com.example.data.models.Advertisement
import com.example.data.storage.SupabaseStorageService
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

@Composable
fun AdminAdsTab(
  adminUid: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var ads by remember { mutableStateOf<List<Advertisement>>(emptyList()) }
  var filterStatus by remember { mutableStateOf("all") } // "all", "active", "paused"

  var showCreateDialog by remember { mutableStateOf(false) }
  var adToDelete by remember { mutableStateOf<Advertisement?>(null) }

  LaunchedEffect(Unit) {
    FirebaseService.getAllAdsFlow().collect { list ->
      ads = list
    }
  }

  val filteredAds = remember(ads, filterStatus) {
    if (filterStatus == "all") ads else ads.filter { it.status.equals(filterStatus, ignoreCase = true) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Header with Create button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("CAMPAIGN MANAGER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Text("Sponsored Ads", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      }

      Button(
        onClick = { showCreateDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("New Ad", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(
        "all" to "All (${ads.size})",
        "active" to "Active (${ads.count { it.status == "active" }})",
        "paused" to "Paused (${ads.count { it.status == "paused" }})"
      ).forEach { (status, label) ->
        val selected = filterStatus == status
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PureWhite else GlassSurface)
            .border(1.dp, if (selected) PureWhite else GlassBorder, RoundedCornerShape(8.dp))
            .clickable { filterStatus = status }
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) PureBlack else TextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (filteredAds.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No campaigns in this category.", color = TextMuted, fontSize = 14.sp)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredAds, key = { it.adId }) { ad ->
          AdminAdCard(
            ad = ad,
            onToggleStatus = {
              val newStatus = if (ad.status == "active") "paused" else "active"
              scope.launch {
                val res = FirebaseService.updateAdStatus(ad.adId, newStatus)
                if (res.isSuccess) {
                  Toast.makeText(context, "Ad set to $newStatus.", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onDelete = { adToDelete = ad }
          )
        }
      }
    }
  }

  // Create Ad Dialog
  if (showCreateDialog) {
    CreateAdDialog(
      adminUid = adminUid,
      onDismiss = { showCreateDialog = false },
      onSuccess = {
        showCreateDialog = false
        Toast.makeText(context, "Ad created and published!", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Delete Ad Dialog
  val currentAdToDelete = adToDelete
  if (currentAdToDelete != null) {
    AlertDialog(
      onDismissRequest = { adToDelete = null },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text("Delete Campaign") },
      text = { Text("Remove ad \"${currentAdToDelete.title}\"? This action cannot be undone.") },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              val res = FirebaseService.deleteAd(currentAdToDelete.adId)
              if (res.isSuccess) {
                Toast.makeText(context, "Campaign deleted.", Toast.LENGTH_SHORT).show()
                adToDelete = null
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Delete", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { adToDelete = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun AdminAdCard(
  ad: Advertisement,
  onToggleStatus: () -> Unit,
  onDelete: () -> Unit
) {
  val isActive = ad.status == "active"

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) SuccessGreenLight else DangerRedLight)
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = ad.status.uppercase(Locale.ROOT),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) SuccessGreen else DangerRed
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(ad.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      }

      IconButton(onClick = onDelete) {
        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(20.dp))
      }
    }

    if (ad.imageUrl.isNotBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      AsyncImage(
        model = ad.imageUrl,
        contentDescription = null,
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
          .clip(RoundedCornerShape(8.dp))
          .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
      )
    }

    if (ad.description.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(ad.description, fontSize = 12.sp, color = TextSecondary)
    }

    if (ad.targetUrl.isNotBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text("Target: ${ad.targetUrl}", fontSize = 11.sp, color = TextMuted, maxLines = 1)
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Analytics row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text("Impressions", fontSize = 11.sp, color = TextMuted)
        Text("${ad.currentImpressions} / ${if (ad.isPermanent) "∞" else ad.targetImpressions}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      }

      Column {
        Text("Clicks", fontSize = 11.sp, color = TextMuted)
        Text("${ad.clickCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      }

      Column(horizontalAlignment = Alignment.End) {
        Text("CTR", fontSize = 11.sp, color = TextMuted)
        val ctr = if (ad.currentImpressions > 0) (ad.clickCount.toDouble() / ad.currentImpressions * 100) else 0.0
        Text(String.format(Locale.ROOT, "%.2f%%", ctr), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Toggle status button
    Button(
      onClick = onToggleStatus,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = if (isActive) GlassSurface else SuccessGreen,
        contentColor = if (isActive) PureWhite else PureBlack
      ),
      border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, GlassBorder) else null
    ) {
      Icon(if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(if (isActive) "Pause Campaign" else "Resume Campaign", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun CreateAdDialog(
  adminUid: String,
  onDismiss: () -> Unit,
  onSuccess: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var targetUrl by remember { mutableStateOf("https://") }
  var targetImpressions by remember { mutableStateOf("5000") }
  var isPermanent by remember { mutableStateOf(false) }
  var imageUrl by remember { mutableStateOf("") }
  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var isUploading by remember { mutableStateOf(false) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedImageUri = uri
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurfaceElevated,
    titleContentColor = PureWhite,
    textContentColor = TextSecondary,
    title = { Text("Create Sponsored Ad") },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Ad Title", fontSize = 12.sp) },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = GlassBorder
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description / Tagline", fontSize = 12.sp) },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = GlassBorder
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = targetUrl,
          onValueChange = { targetUrl = it },
          label = { Text("Target URL (https://...)", fontSize = 12.sp) },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = GlassBorder
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Image Selection
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Button(
            onClick = {
              photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface, contentColor = PureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (selectedImageUri != null) "Change Banner" else "Select Banner", fontSize = 12.sp)
          }

          if (selectedImageUri != null) {
            Text("Image selected", fontSize = 11.sp, color = SuccessGreen)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = isPermanent,
            onCheckedChange = { isPermanent = it },
            colors = CheckboxDefaults.colors(checkedColor = PureWhite, checkmarkColor = PureBlack)
          )
          Text("Permanent (Continuous Impression)", fontSize = 12.sp, color = PureWhite)
        }

        if (!isPermanent) {
          OutlinedTextField(
            value = targetImpressions,
            onValueChange = { targetImpressions = it.filter { ch -> ch.isDigit() } },
            label = { Text("Impression Target", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder
            )
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank() || targetUrl.isBlank()) {
            Toast.makeText(context, "Title and URL are required.", Toast.LENGTH_SHORT).show()
            return@Button
          }

          scope.launch {
            isUploading = true
            var uploadedUrl = imageUrl
            if (selectedImageUri != null) {
              try {
                val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
                val bytes = inputStream?.use { it.readBytes() } ?: ByteArray(0)
                val upRes = SupabaseStorageService.uploadImage(bytes, "ads", adminUid)
                if (upRes.isSuccess) {
                  uploadedUrl = upRes.getOrThrow()
                }
              } catch (e: Exception) {
                // Fallback placeholder
                uploadedUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800"
              }
            }

            val target = targetImpressions.toLongOrNull() ?: 5000L
            val res = FirebaseService.createAd(
              title = title,
              description = description,
              imageUrl = uploadedUrl,
              targetUrl = targetUrl,
              targetImpressions = target,
              isPermanent = isPermanent,
              startAt = System.currentTimeMillis(),
              endAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )
            isUploading = false
            if (res.isSuccess) {
              onSuccess()
            } else {
              Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
          }
        },
        enabled = !isUploading,
        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
      ) {
        if (isUploading) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PureBlack, strokeWidth = 2.dp)
        } else {
          Text("Publish Ad", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
