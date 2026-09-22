package com.example.ui.screens.plus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.FirebaseService
import com.example.data.models.PaymentConfig
import com.example.data.models.PaymentRequest
import com.example.data.models.User
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlusScreen(
  currentUser: User,
  onBack: () -> Unit
) {
  var selectedPlan by remember { mutableIntStateOf(1) } // 1 = 1 month (₹25), 2 = 6 months (₹75)
  var utrNumber by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var myRequests by remember { mutableStateOf<List<PaymentRequest>>(emptyList()) }
  var paymentConfig by remember { mutableStateOf(PaymentConfig(upiId = "6375862443@ibl", visibility = "BOTH")) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val upiId = paymentConfig.upiId.ifBlank { "6375862443@ibl" }

  LaunchedEffect(currentUser.uid) {
    FirebaseService.getUserPaymentRequestsFlow(currentUser.uid).collectLatest { list ->
      myRequests = list
    }
  }

  LaunchedEffect(Unit) {
    FirebaseService.getPaymentConfigFlow().collectLatest { cfg ->
      paymentConfig = cfg
    }
  }

  val hasPendingRequest = myRequests.any { it.status == "pending" }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Black status bar strip
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
      }
      Text(
        text = "STRUGX PLUS",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = PureWhite
      )
    }

    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(0.5.dp)
        .background(GlassBorder)
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp),
      contentPadding = PaddingValues(vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Futuristic Glass Status Banner
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(1.5.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(PureWhite),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = null,
                  tint = PureBlack,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "STRUGX PLUS",
                  color = PureWhite,
                  fontWeight = FontWeight.Black,
                  fontSize = 18.sp,
                  letterSpacing = 1.sp
                )
                Text(
                  text = "Creator privileges & higher platform limits",
                  color = TextSecondary,
                  fontSize = 12.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Status Pill
            Surface(
              color = GlassSurface,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = when {
                    currentUser.isPlus -> Icons.Default.CheckCircle
                    hasPendingRequest -> Icons.Default.HourglassTop
                    else -> Icons.Default.Info
                  },
                  contentDescription = null,
                  tint = when {
                    currentUser.isPlus -> SuccessGreen
                    hasPendingRequest -> PureWhite
                    else -> TextSecondary
                  },
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = when {
                      currentUser.isPlus -> "Active Plus Member"
                      hasPendingRequest -> "Payment Verification Pending"
                      else -> "Standard Free Tier"
                    },
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = when {
                      currentUser.isPlus -> "Expires: ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(currentUser.plusExpiresAt))}"
                      hasPendingRequest -> "An administrator will review your UTR shortly"
                      else -> "Limited to 20 new follows per day"
                    },
                    color = TextSecondary,
                    fontSize = 11.sp
                  )
                }
              }
            }
          }
        }
      }

      // 2. Subscription Benefits
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Plus Privileges",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = PureWhite
          )

          PlusBenefitRow("100 new follows per day (vs 20 for standard users)")
          PlusBenefitRow("Exclusive verified PLUS badge on your profile & posts")
          PlusBenefitRow("Priority visibility in search and community discovery")
          PlusBenefitRow("Direct support for the platform's continuous development")
        }
      }

      // 3. Plan Selection (EXACTLY 2 PLANS)
      item {
        Text(
          text = "Select Subscription Plan",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = PureWhite
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Plan 1: ₹25 / 1 Month
          DarkPlanSelectionCard(
            title = "1 Month",
            price = "₹25",
            subtitle = "₹25 billed once",
            isSelected = selectedPlan == 1,
            modifier = Modifier.weight(1f),
            onSelect = { selectedPlan = 1 }
          )

          // Plan 2: ₹75 / 6 Months
          DarkPlanSelectionCard(
            title = "6 Months",
            price = "₹75",
            subtitle = "Save 50% vs monthly",
            badge = "BEST VALUE",
            isSelected = selectedPlan == 2,
            modifier = Modifier.weight(1f),
            onSelect = { selectedPlan = 2 }
          )
        }
      }

      // 4. Manual UPI Payment Flow & UTR Submission
      item {
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Manual UPI Payment",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = PureWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "1. Pay exactly ${if (selectedPlan == 1) "₹25" else "₹75"} using any UPI app (Google Pay, PhonePe, Paytm, BHIM).",
              fontSize = 13.sp,
              color = TextSecondary,
              lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Payment Methods (QR / UPI ID based on config)
            if (paymentConfig.visibility == "NEITHER") {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(GlassSurface)
                  .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                  .padding(14.dp)
              ) {
                Text(
                  text = "Manual UPI & QR payments are temporarily paused for maintenance. Please check back shortly.",
                  fontSize = 13.sp,
                  color = WarningAmber,
                  lineHeight = 18.sp
                )
              }
            } else {
              // Show QR Code if enabled and set
              if (paymentConfig.visibility in listOf("QR_ONLY", "BOTH") && paymentConfig.qrImageUrl.isNotBlank()) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = "Scan QR Code to Pay",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Pay exactly ${if (selectedPlan == 1) "₹25" else "₹75"} with any UPI App",
                    fontSize = 12.sp,
                    color = TextSecondary
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  AsyncImage(
                    model = paymentConfig.qrImageUrl,
                    contentDescription = "Payment QR Code",
                    modifier = Modifier
                      .size(160.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(Color.White)
                      .padding(8.dp),
                    contentScale = ContentScale.Fit
                  )
                }
                Spacer(modifier = Modifier.height(12.dp))
              }

              // Official UPI ID Box with Copy Button (if enabled)
              if (paymentConfig.visibility in listOf("UPI_ONLY", "BOTH") && upiId.isNotBlank()) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text("Official Strugx UPI ID", fontSize = 11.sp, color = TextMuted)
                    Text(upiId, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PureWhite)
                  }

                  Button(
                    onClick = {
                      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                      clipboard.setPrimaryClip(ClipData.newPlainText("Strugx UPI ID", upiId))
                      Toast.makeText(context, "UPI ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                  ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = PureBlack)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "2. Enter the 12-digit UPI UTR / Reference number from your payment receipt:",
              fontSize = 13.sp,
              color = TextSecondary,
              lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
              value = utrNumber,
              onValueChange = { utrNumber = it.filter { ch -> ch.isLetterOrDigit() } },
              placeholder = { Text("e.g. 423456789012", fontSize = 13.sp, color = TextMuted) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PureWhite,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface,
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite
              ),
              singleLine = true,
              enabled = !hasPendingRequest
            )

            if (hasPendingRequest) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "You already have a payment request under review. Duplicate requests are disabled while verification is in progress.",
                fontSize = 12.sp,
                color = WarningAmber,
                lineHeight = 16.sp
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = {
                if (utrNumber.trim().length < 6) {
                  Toast.makeText(context, "Please enter a valid 12-digit UTR reference number.", Toast.LENGTH_SHORT).show()
                  return@Button
                }
                isSubmitting = true
                scope.launch {
                  val planName = if (selectedPlan == 1) "₹25 / 1 Month" else "₹75 / 6 Months"
                  val amountStr = if (selectedPlan == 1) "25" else "75"
                  val res = FirebaseService.submitPaymentRequest(
                    userId = currentUser.uid,
                    username = currentUser.username,
                    plan = planName,
                    amount = amountStr,
                    utr = utrNumber.trim()
                  )
                  isSubmitting = false
                  if (res.isSuccess) {
                    utrNumber = ""
                    Toast.makeText(context, "Payment request submitted! Admin will verify and activate your subscription.", Toast.LENGTH_LONG).show()
                  } else {
                    Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                  }
                }
              },
              enabled = utrNumber.isNotBlank() && !isSubmitting && !hasPendingRequest,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = PureWhite,
                contentColor = PureBlack,
                disabledContainerColor = GlassSurface,
                disabledContentColor = TextMuted
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              } else {
                Text("Submit UTR for Verification", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Note: UTR submission does not immediately activate Plus. An administrator verifies the transaction manually before approval.",
              fontSize = 11.sp,
              color = TextMuted,
              lineHeight = 15.sp
            )
          }
        }
      }

      // 5. User's Payment Requests History
      if (myRequests.isNotEmpty()) {
        item {
          Text(
            text = "Payment Request History",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = PureWhite
          )
        }

        items(myRequests, key = { it.requestId }) { req ->
          Surface(
            color = DarkSurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = req.plan.ifBlank { "Plan: ₹${req.amount}" },
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = PureWhite
                )

                // Status Badge
                val (badgeColor, badgeTextColor) = when (req.status) {
                  "approved" -> Pair(SuccessGreen.copy(alpha = 0.2f), SuccessGreen)
                  "rejected" -> Pair(DangerRed.copy(alpha = 0.2f), DangerRed)
                  else -> Pair(GlassSurface, PureWhite)
                }

                Surface(
                  color = badgeColor,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.border(0.5.dp, badgeTextColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                  Text(
                    text = req.status.uppercase(Locale.ROOT),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text(text = "UTR: ${req.utr}", fontSize = 12.sp, color = TextSecondary)
              Text(
                text = "Submitted: ${SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(req.submittedAt))}",
                fontSize = 11.sp,
                color = TextMuted
              )

              if (req.adminNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Admin Note: ${req.adminNote}",
                  fontSize = 12.sp,
                  color = if (req.status == "rejected") DangerRed else TextSecondary
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PlusBenefitRow(text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(18.dp)
        .clip(CircleShape)
        .background(GlassSurface)
        .border(1.dp, PureWhite, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(12.dp))
    }
    Spacer(modifier = Modifier.width(10.dp))
    Text(text, fontSize = 13.sp, color = PureWhite)
  }
}

@Composable
private fun DarkPlanSelectionCard(
  title: String,
  price: String,
  subtitle: String,
  badge: String? = null,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onSelect: () -> Unit
) {
  Surface(
    onClick = onSelect,
    color = if (isSelected) DarkSurfaceElevated else DarkBackground,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier
      .border(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) PureWhite else GlassBorder,
        shape = RoundedCornerShape(16.dp)
      )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (badge != null) {
        Surface(
          color = PureWhite,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            badge,
            color = PureBlack,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
      }

      Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isSelected) PureWhite else TextSecondary)
      Spacer(modifier = Modifier.height(4.dp))
      Text(price, fontWeight = FontWeight.Black, fontSize = 22.sp, color = PureWhite)
      Spacer(modifier = Modifier.height(2.dp))
      Text(subtitle, fontSize = 11.sp, color = TextMuted)
    }
  }
}
