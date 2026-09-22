package com.example.ui.screens.admin

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseService
import com.example.data.models.PaymentRequest
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminPaymentsTab(
  adminUid: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var requests by remember { mutableStateOf<List<PaymentRequest>>(emptyList()) }
  var statusFilter by remember { mutableStateOf("pending") } // "pending", "approved", "rejected", "all"

  // Dialog States
  var requestToApprove by remember { mutableStateOf<PaymentRequest?>(null) }
  var requestToReject by remember { mutableStateOf<PaymentRequest?>(null) }
  var rejectReason by remember { mutableStateOf("Invalid UTR or payment not received.") }

  LaunchedEffect(Unit) {
    FirebaseService.getAllPaymentRequestsFlow().collect { list ->
      requests = list
    }
  }

  val filteredRequests = remember(requests, statusFilter) {
    if (statusFilter == "all") requests else requests.filter { it.status.equals(statusFilter, ignoreCase = true) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Filter Tabs
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(
        "pending" to "Pending",
        "approved" to "Approved",
        "rejected" to "Rejected",
        "all" to "All"
      ).forEach { (key, label) ->
        val selected = statusFilter == key
        val count = if (key == "all") requests.size else requests.count { it.status.equals(key, ignoreCase = true) }
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PureWhite else GlassSurface)
            .border(1.dp, if (selected) PureWhite else GlassBorder, RoundedCornerShape(8.dp))
            .clickable { statusFilter = key }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "$label ($count)",
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) PureBlack else TextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (filteredRequests.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No payment requests in this category.", color = TextMuted, fontSize = 14.sp)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredRequests, key = { it.requestId }) { req ->
          PaymentRequestAdminCard(
            req = req,
            onCopyUtr = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("UTR", req.utr))
              Toast.makeText(context, "UTR ${req.utr} copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onApprove = { requestToApprove = req },
            onReject = {
              rejectReason = "Invalid UTR or payment not received."
              requestToReject = req
            }
          )
        }
      }
    }
  }

  // Approve Dialog
  val approveReq = requestToApprove
  if (approveReq != null) {
    AlertDialog(
      onDismissRequest = { requestToApprove = null },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text("Approve Strugx Plus Payment") },
      text = {
        Column {
          Text(
            text = "Activate Plus for @${approveReq.username}?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text("Plan: ${approveReq.plan}", fontSize = 13.sp, color = TextSecondary)
          Text("UTR Number: ${approveReq.utr}", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = WarningAmber)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "This will instantly grant Plus badge, 100 follows/day limit, and notify the user.",
            fontSize = 12.sp,
            color = TextMuted
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              val months = if (approveReq.plan.contains("6 Month", ignoreCase = true) || approveReq.plan == "6_months") 6 else 1
              val res = FirebaseService.approvePaymentRequest(approveReq.requestId, approveReq.userId, months, adminUid)
              if (res.isSuccess) {
                Toast.makeText(context, "Payment approved! @${approveReq.username} is now Plus.", Toast.LENGTH_SHORT).show()
                requestToApprove = null
              } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureBlack)
        ) {
          Text("Confirm Approval", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { requestToApprove = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Reject Dialog
  val rejectReq = requestToReject
  if (rejectReq != null) {
    AlertDialog(
      onDismissRequest = { requestToReject = null },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text("Reject Payment Request") },
      text = {
        Column {
          Text(
            text = "Rejecting request for @${rejectReq.username} (UTR: ${rejectReq.utr}).",
            fontSize = 13.sp,
            color = PureWhite
          )
          Spacer(modifier = Modifier.height(10.dp))

          Text("Select or enter reason:", fontSize = 12.sp, color = TextMuted)
          Spacer(modifier = Modifier.height(6.dp))

          listOf(
            "Invalid UTR or payment not received.",
            "Incorrect transfer amount.",
            "Duplicate UTR submission.",
            "Payment reversed / refunded."
          ).forEach { preset ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { rejectReason = preset }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = rejectReason == preset,
                onClick = { rejectReason = preset },
                colors = RadioButtonDefaults.colors(selectedColor = PureWhite, unselectedColor = TextMuted)
              )
              Text(preset, fontSize = 12.sp, color = PureWhite)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = rejectReason,
            onValueChange = { rejectReason = it },
            label = { Text("Rejection Note (Sent to user)", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              val res = FirebaseService.rejectPaymentRequest(rejectReq.requestId, rejectReason, adminUid)
              if (res.isSuccess) {
                Toast.makeText(context, "Payment request rejected.", Toast.LENGTH_SHORT).show()
                requestToReject = null
              } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Reject Request", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { requestToReject = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun PaymentRequestAdminCard(
  req: PaymentRequest,
  onCopyUtr: () -> Unit,
  onApprove: () -> Unit,
  onReject: () -> Unit
) {
  val statusColor = when (req.status.lowercase(Locale.ROOT)) {
    "approved" -> SuccessGreen
    "rejected" -> DangerRed
    else -> WarningAmber
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "@${req.username}",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(statusColor.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = req.status.uppercase(Locale.ROOT),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
          )
        }
      }

      Text(
        text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(req.submittedAt)),
        fontSize = 11.sp,
        color = TextMuted
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Plan & Amount details
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text("Plan Requested", fontSize = 11.sp, color = TextMuted)
        Text(req.plan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
      }
      Column(horizontalAlignment = Alignment.End) {
        Text("Amount", fontSize = 11.sp, color = TextMuted)
        Text("₹${req.amount.ifBlank { if (req.plan.contains("6 Month", true)) "75" else "25" }}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // UTR Highlight Box with Copy Button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(GlassSurface)
        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("UTR / TRANSACTION REFERENCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Text(
          text = req.utr,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = WarningAmber
        )
      }

      IconButton(
        onClick = onCopyUtr,
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(GlassBorder)
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Copy UTR", tint = PureWhite, modifier = Modifier.size(16.dp))
      }
    }

    if (req.adminNote.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Note: ${req.adminNote}",
        fontSize = 11.sp,
        color = if (req.status == "rejected") DangerRed else TextSecondary
      )
    }

    // Actions if Pending
    if (req.status.equals("pending", ignoreCase = true)) {
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onApprove,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureBlack),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Approve Plus", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Button(
          onClick = onReject,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = DangerRedLight, contentColor = DangerRed),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Reject", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }
  }
}
