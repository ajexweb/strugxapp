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
import com.example.data.firebase.FirebaseService
import com.example.data.models.PaymentConfig
import com.example.data.storage.SupabaseStorageService
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun AdminPaymentMethodsTab(
  adminUid: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var paymentConfig by remember { mutableStateOf(PaymentConfig()) }
  var upiIdInput by remember { mutableStateOf("") }
  var selectedVisibility by remember { mutableStateOf("BOTH") }
  var qrImageUrl by remember { mutableStateOf("") }
  var selectedQrUri by remember { mutableStateOf<Uri?>(null) }
  var isSaving by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    val current = FirebaseService.getPaymentConfig()
    paymentConfig = current
    upiIdInput = current.upiId
    selectedVisibility = current.visibility
    qrImageUrl = current.qrImageUrl
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedQrUri = uri
    }
  }

  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp)
  ) {
    Text(
      text = "PAYMENT RAILS & VISIBILITY",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
    Text(
      text = "UPI & QR Management",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = PureWhite
    )
    Text(
      text = "Configure the manual payment details displayed on the user Plus subscription screen.",
      fontSize = 13.sp,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    // 1. UPI ID Section
    Text(
      text = "1. Official UPI ID",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = PureWhite
    )
    Spacer(modifier = Modifier.height(6.dp))

    OutlinedTextField(
      value = upiIdInput,
      onValueChange = { upiIdInput = it.trim() },
      placeholder = { Text("e.g. 6375862443@ibl", color = TextMuted) },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PureWhite) },
      trailingIcon = {
        if (upiIdInput.isNotBlank()) {
          IconButton(onClick = { upiIdInput = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
          }
        }
      },
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedBorderColor = PureWhite,
        unfocusedBorderColor = GlassBorder,
        focusedContainerColor = DarkSurfaceElevated,
        unfocusedContainerColor = DarkSurfaceElevated
      )
    )

    Spacer(modifier = Modifier.height(24.dp))

    // 2. QR Code Section
    Text(
      text = "2. Official Payment QR Code",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = PureWhite
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (selectedQrUri != null) {
        AsyncImage(
          model = selectedQrUri,
          contentDescription = "New QR Preview",
          modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
          contentScale = ContentScale.Fit
        )
      } else if (qrImageUrl.isNotBlank()) {
        AsyncImage(
          model = qrImageUrl,
          contentDescription = "Current QR",
          modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
          contentScale = ContentScale.Fit
        )
      } else {
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.QrCode2, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (selectedQrUri != null) "New QR Selected" else if (qrImageUrl.isNotBlank()) "Active QR Uploaded" else "No QR Code Set",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = PureWhite
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface, contentColor = PureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Text(if (qrImageUrl.isNotBlank() || selectedQrUri != null) "Replace QR" else "Upload QR", fontSize = 12.sp)
          }

          if (qrImageUrl.isNotBlank() || selectedQrUri != null) {
            Button(
              onClick = {
                qrImageUrl = ""
                selectedQrUri = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = DangerRedLight, contentColor = DangerRed),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text("Remove", fontSize = 12.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 3. Visibility Selector
    Text(
      text = "3. Payment Display Mode",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = PureWhite
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Choose what users see on the Plus subscription checkout screen:",
      fontSize = 12.sp,
      color = TextMuted
    )
    Spacer(modifier = Modifier.height(10.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      VisibilityOptionRow(
        title = "BOTH QR & UPI ID (Recommended)",
        subtitle = "Users can scan QR or copy UPI ID",
        selected = selectedVisibility == "BOTH",
        onSelect = { selectedVisibility = "BOTH" }
      )
      VisibilityOptionRow(
        title = "UPI ONLY",
        subtitle = "Display only the copyable UPI text box",
        selected = selectedVisibility == "UPI_ONLY",
        onSelect = { selectedVisibility = "UPI_ONLY" }
      )
      VisibilityOptionRow(
        title = "QR ONLY",
        subtitle = "Display only the QR Code to scan",
        selected = selectedVisibility == "QR_ONLY",
        onSelect = { selectedVisibility = "QR_ONLY" }
      )
      VisibilityOptionRow(
        title = "NEITHER (Pause Manual Payments)",
        subtitle = "Temporarily hides checkout and shows maintenance notice",
        selected = selectedVisibility == "NEITHER",
        onSelect = { selectedVisibility = "NEITHER" }
      )
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Save Button
    Button(
      onClick = {
        scope.launch {
          isSaving = true
          var finalQrUrl = qrImageUrl

          // Upload QR to Supabase if newly selected
          if (selectedQrUri != null) {
            try {
              val inputStream = context.contentResolver.openInputStream(selectedQrUri!!)
              val bytes = inputStream?.use { it.readBytes() } ?: ByteArray(0)
              val upRes = SupabaseStorageService.uploadImage(bytes, "payment-methods/qr", adminUid)
              if (upRes.isSuccess) {
                finalQrUrl = upRes.getOrThrow()
              }
            } catch (e: Exception) {
              Toast.makeText(context, "Failed to upload QR image.", Toast.LENGTH_LONG).show()
            }
          }

          val newConfig = PaymentConfig(
            upiId = upiIdInput,
            qrImageUrl = finalQrUrl,
            visibility = selectedVisibility
          )

          val res = FirebaseService.updatePaymentConfig(newConfig, adminUid)
          isSaving = false
          if (res.isSuccess) {
            paymentConfig = newConfig
            qrImageUrl = finalQrUrl
            selectedQrUri = null
            Toast.makeText(context, "Payment configuration saved successfully!", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
          }
        }
      },
      enabled = !isSaving,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
    ) {
      if (isSaving) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureBlack, strokeWidth = 2.dp)
      } else {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save & Publish Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
    }

    Spacer(modifier = Modifier.height(36.dp))
  }
}

@Composable
private fun VisibilityOptionRow(
  title: String,
  subtitle: String,
  selected: Boolean,
  onSelect: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(if (selected) GlassSurface else Color.Transparent)
      .clickable { onSelect() }
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      selected = selected,
      onClick = onSelect,
      colors = RadioButtonDefaults.colors(selectedColor = PureWhite, unselectedColor = TextMuted)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column {
      Text(title, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = PureWhite)
      Text(subtitle, fontSize = 11.sp, color = TextMuted)
    }
  }
}
