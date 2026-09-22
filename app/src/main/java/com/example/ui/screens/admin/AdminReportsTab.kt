package com.example.ui.screens.admin

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseService
import com.example.data.models.Report
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminReportsTab(
  adminUid: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
  var statusFilter by remember { mutableStateOf("pending") } // "all", "pending", "reviewing", "resolved", "dismissed"
  var selectedReportForAction by remember { mutableStateOf<Report?>(null) }
  var adminActionNote by remember { mutableStateOf("") }
  var actionType by remember { mutableStateOf("resolved") } // "resolved" or "dismissed"

  LaunchedEffect(Unit) {
    FirebaseService.getReportsFlow().collect { list ->
      reports = list
    }
  }

  val filteredReports = remember(reports, statusFilter) {
    if (statusFilter == "all") reports else reports.filter { it.status.equals(statusFilter, ignoreCase = true) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Status Filter Tabs
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf(
        "pending" to "Pending",
        "reviewing" to "Reviewing",
        "resolved" to "Resolved",
        "dismissed" to "Dismissed",
        "all" to "All"
      ).forEach { (key, label) ->
        val selected = statusFilter == key
        val count = if (key == "all") reports.size else reports.count { it.status.equals(key, ignoreCase = true) }
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PureWhite else GlassSurface)
            .border(1.dp, if (selected) PureWhite else GlassBorder, RoundedCornerShape(8.dp))
            .clickable { statusFilter = key }
            .padding(vertical = 6.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "$label ($count)",
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) PureBlack else TextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (filteredReports.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No reports found in this category.", color = TextMuted, fontSize = 14.sp)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredReports, key = { it.reportId }) { report ->
          ReportAdminCard(
            report = report,
            onMarkReviewing = {
              scope.launch {
                val res = FirebaseService.updateReport(report.reportId, "reviewing", "Investigation initiated", adminUid)
                if (res.isSuccess) {
                  Toast.makeText(context, "Report marked as in-review.", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onResolve = {
              selectedReportForAction = report
              actionType = "resolved"
              adminActionNote = "Action taken / verified by moderation team."
            },
            onDismiss = {
              selectedReportForAction = report
              actionType = "dismissed"
              adminActionNote = "False report / no violation detected."
            }
          )
        }
      }
    }
  }

  // Resolve / Dismiss Action Dialog
  val selectedReport = selectedReportForAction
  if (selectedReport != null) {
    AlertDialog(
      onDismissRequest = { selectedReportForAction = null },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text(if (actionType == "resolved") "Resolve Report" else "Dismiss Report") },
      text = {
        Column {
          Text(
            text = "Target: ${selectedReport.targetType.uppercase()} (${selectedReport.targetId})",
            fontSize = 12.sp,
            color = PureWhite
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Provide an internal moderator note:",
            fontSize = 12.sp,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = adminActionNote,
            onValueChange = { adminActionNote = it },
            label = { Text("Admin Note", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
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
              val res = FirebaseService.updateReport(selectedReport.reportId, actionType, adminActionNote, adminUid)
              if (res.isSuccess) {
                Toast.makeText(context, "Report updated to $actionType.", Toast.LENGTH_SHORT).show()
                selectedReportForAction = null
              } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (actionType == "resolved") SuccessGreen else GlassSurface,
            contentColor = if (actionType == "resolved") PureBlack else PureWhite
          )
        ) {
          Text("Confirm", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedReportForAction = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun ReportAdminCard(
  report: Report,
  onMarkReviewing: () -> Unit,
  onResolve: () -> Unit,
  onDismiss: () -> Unit
) {
  val statusColor = when (report.status.lowercase(Locale.ROOT)) {
    "pending" -> WarningAmber
    "reviewing" -> Color(0xFF64B5F6)
    "resolved" -> SuccessGreen
    else -> TextMuted
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
      .padding(14.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(statusColor.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = report.status.uppercase(Locale.ROOT),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(GlassSurface)
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = report.targetType.uppercase(Locale.ROOT),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
        }
      }

      Text(
        text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(report.createdAt)),
        fontSize = 11.sp,
        color = TextMuted
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "Reason: ${report.reason}",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = PureWhite
    )

    if (report.details.isNotBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Details: ${report.details}",
        fontSize = 12.sp,
        color = TextSecondary,
        lineHeight = 16.sp
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Reported by: @${report.reporterUsername} • Target ID: ${report.targetId}",
      fontSize = 11.sp,
      color = TextMuted
    )

    if (report.adminNote.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Admin Note: ${report.adminNote}",
        fontSize = 11.sp,
        color = Color(0xFFA5D6A7)
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Action buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (report.status.equals("pending", ignoreCase = true)) {
        OutlinedButton(
          onClick = onMarkReviewing,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
          border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
          Text("In Review", fontSize = 12.sp)
        }
      }

      if (!report.status.equals("resolved", ignoreCase = true)) {
        Button(
          onClick = onResolve,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureBlack)
        ) {
          Text("Resolve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      if (!report.status.equals("dismissed", ignoreCase = true)) {
        Button(
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GlassSurface, contentColor = TextSecondary),
          border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
          Text("Dismiss", fontSize = 12.sp)
        }
      }
    }
  }
}
