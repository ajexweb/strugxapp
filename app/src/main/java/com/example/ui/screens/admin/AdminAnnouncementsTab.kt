package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminAnnouncementsTab(
  adminUid: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var title by remember { mutableStateOf("") }
  var body by remember { mutableStateOf("") }
  var targetUid by remember { mutableStateOf("") } // empty = all users
  var isBroadcastToAll by remember { mutableStateOf(true) }
  var isSending by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp)
  ) {
    Text(
      text = "SYSTEM BROADCAST",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
    Text(
      text = "Announcements & Alerts",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = PureWhite
    )
    Text(
      text = "Send real-time alerts directly to users' notification inboxes.",
      fontSize = 13.sp,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Form
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
        .padding(16.dp)
    ) {
      Text("Notification Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title / Header", fontSize = 12.sp) },
        placeholder = { Text("e.g. Scheduled Maintenance or New Features", fontSize = 12.sp, color = TextMuted) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = body,
        onValueChange = { body = it },
        label = { Text("Message Body", fontSize = 12.sp) },
        placeholder = { Text("Write message...", fontSize = 12.sp, color = TextMuted) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Target selection
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
          checked = isBroadcastToAll,
          onCheckedChange = {
            isBroadcastToAll = it
            if (it) targetUid = ""
          },
          colors = CheckboxDefaults.colors(checkedColor = PureWhite, checkmarkColor = PureBlack)
        )
        Text("Broadcast to all platform users", fontSize = 13.sp, color = PureWhite)
      }

      if (!isBroadcastToAll) {
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = targetUid,
          onValueChange = { targetUid = it.trim() },
          label = { Text("Target User UID", fontSize = 12.sp) },
          placeholder = { Text("Paste exact user UID", fontSize = 12.sp, color = TextMuted) },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = GlassBorder
          )
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          if (title.isBlank() || body.isBlank()) {
            Toast.makeText(context, "Title and message body are required.", Toast.LENGTH_SHORT).show()
            return@Button
          }

          if (!isBroadcastToAll && targetUid.isBlank()) {
            Toast.makeText(context, "Specify target UID or check broadcast to all.", Toast.LENGTH_SHORT).show()
            return@Button
          }

          scope.launch {
            isSending = true
            val res = if (isBroadcastToAll) {
              FirebaseService.broadcastAdminAnnouncement(title, body, adminUid)
            } else {
              FirebaseService.sendAppNotification(
                recipientId = targetUid,
                senderId = adminUid,
                senderUsername = "Admin",
                senderAvatar = "",
                type = "announcement",
                text = "$title: $body",
                targetId = "admin_broadcast"
              )
            }
            isSending = false

            if (res.isSuccess) {
              Toast.makeText(context, "Announcement sent successfully!", Toast.LENGTH_SHORT).show()
              title = ""
              body = ""
              targetUid = ""
            } else {
              Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
          }
        },
        enabled = !isSending,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
      ) {
        if (isSending) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PureBlack, strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isBroadcastToAll) "Broadcast to All Users" else "Send Direct Notification", fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Guidelines Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(GlassSurface)
        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        .padding(14.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Broadcast Policy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "• Broadcasts appear in each active user's notifications feed.\n• Use for important platform downtime, security advisories, or major version updates.\n• Avoid excessive messaging to prevent notification fatigue.",
        fontSize = 12.sp,
        color = TextMuted,
        lineHeight = 18.sp
      )
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}
