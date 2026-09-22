package com.example.ui.screens.admin

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseService
import com.example.data.models.AdminAuditLog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminAuditLogsTab() {
  var logs by remember { mutableStateOf<List<AdminAuditLog>>(emptyList()) }
  var searchQuery by remember { mutableStateOf("") }
  var filterCategory by remember { mutableStateOf("ALL") } // ALL, MODERATION, PAYMENTS, CONFIG

  LaunchedEffect(Unit) {
    FirebaseService.getAdminAuditLogsFlow().collect { list ->
      logs = list
    }
  }

  val filteredLogs = remember(logs, searchQuery, filterCategory) {
    logs.filter { log ->
      val matchesQuery = searchQuery.isBlank() ||
        log.action.contains(searchQuery, ignoreCase = true) ||
        log.targetId.contains(searchQuery, ignoreCase = true) ||
        log.reason.contains(searchQuery, ignoreCase = true)

      val matchesCategory = when (filterCategory) {
        "MODERATION" -> log.action.contains("BAN", true) || log.action.contains("DELETE", true) || log.action.contains("REPORT", true)
        "PAYMENTS" -> log.action.contains("PAYMENT", true) || log.action.contains("PLUS", true)
        "CONFIG" -> log.action.contains("CONFIG", true) || log.action.contains("ANNOUNCEMENT", true)
        else -> true
      }
      matchesQuery && matchesCategory
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text(
      text = "SYSTEM AUDIT TRAIL",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
    Text(
      text = "Security & Action Logs",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = PureWhite
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Search Box
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("Filter by action, target, or details...", fontSize = 13.sp, color = TextMuted) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedBorderColor = PureWhite,
        unfocusedBorderColor = GlassBorder,
        focusedContainerColor = DarkSurfaceElevated,
        unfocusedContainerColor = DarkSurfaceElevated
      )
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Category Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("ALL", "MODERATION", "PAYMENTS", "CONFIG").forEach { cat ->
        val selected = filterCategory == cat
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PureWhite else GlassSurface)
            .border(1.dp, if (selected) PureWhite else GlassBorder, RoundedCornerShape(8.dp))
            .clickable { filterCategory = cat }
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = cat,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) PureBlack else TextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (filteredLogs.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No audit logs matching criteria.", color = TextMuted, fontSize = 14.sp)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filteredLogs, key = { it.logId }) { log ->
          AuditLogCard(log = log)
        }
      }
    }
  }
}

@Composable
private fun AuditLogCard(log: AdminAuditLog) {
  val actionColor = when {
    log.action.contains("BAN", true) || log.action.contains("DELETE", true) || log.action.contains("REJECT", true) -> DangerRed
    log.action.contains("APPROVE", true) || log.action.contains("GRANT", true) || log.action.contains("UNBAN", true) -> SuccessGreen
    log.action.contains("CONFIG", true) || log.action.contains("ANNOUNCEMENT", true) -> Color(0xFF64B5F6)
    else -> WarningAmber
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(actionColor.copy(alpha = 0.2f))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = log.action,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = actionColor
        )
      }

      Text(
        text = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
        fontSize = 10.sp,
        color = TextMuted
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (log.reason.isNotBlank()) {
      Text(
        text = log.reason,
        fontSize = 12.sp,
        color = PureWhite,
        lineHeight = 16.sp
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "Target: ${log.targetType} [${log.targetId.take(12)}...]",
        fontSize = 10.sp,
        color = TextSecondary
      )
      Text(
        text = "By: ${log.adminUid.take(8)}...",
        fontSize = 10.sp,
        color = TextMuted
      )
    }
  }
}
