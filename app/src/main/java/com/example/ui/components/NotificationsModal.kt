package com.example.ui.components

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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.firebase.FirebaseService
import com.example.data.models.AppNotification
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsModal(
  currentUserId: String,
  onDismiss: () -> Unit,
  onUserClick: (String) -> Unit,
  onPostClick: ((String) -> Unit)? = null
) {
  var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  val scope = rememberCoroutineScope()

  // Listen to realtime notifications
  LaunchedEffect(currentUserId) {
    FirebaseService.getNotificationsFlow(currentUserId).collectLatest { list ->
      notifications = list
      isLoading = false
    }
  }

  // Mark all unread notifications as read upon opening
  LaunchedEffect(Unit) {
    FirebaseService.markNotificationsAsRead(currentUserId)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = DarkBackground
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(DarkBackground)
      ) {
        // Status bar space
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
          IconButton(onClick = onDismiss) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = PureWhite)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Notifications",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
        }

        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

        if (isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp)
          }
        } else if (notifications.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Outlined.Notifications, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
              Spacer(modifier = Modifier.height(14.dp))
              Text("No notifications yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PureWhite)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                "When you get follows, comments, or direct messages, they'll show up here.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(notifications, key = { it.notificationId }) { notif ->
              NotificationItem(
                notif = notif,
                onClick = {
                  if (notif.senderId.isNotBlank()) {
                    onDismiss()
                    onUserClick(notif.senderId)
                  } else if (notif.targetId.isNotBlank() && notif.type == "comment") {
                    onDismiss()
                    onPostClick?.invoke(notif.targetId)
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationItem(
  notif: AppNotification,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (!notif.isRead) DarkSurfaceElevated else GlassSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, if (!notif.isRead) GlassBorderFocused else GlassBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar with notification type badge
      Box(modifier = Modifier.size(44.dp)) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (notif.senderAvatar.isNotBlank()) {
            AsyncImage(
              model = notif.senderAvatar,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(
              imageVector = when (notif.type) {
                "admin_announcement" -> Icons.Default.Campaign
                "plus_approval", "plus_expiry" -> Icons.Default.Star
                else -> Icons.Default.Person
              },
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(22.dp)
            )
          }
        }

        // Mini type badge
        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .border(1.dp, PureWhite, CircleShape)
            .align(Alignment.BottomEnd),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when (notif.type) {
              "follow" -> Icons.Default.PersonAdd
              "comment" -> Icons.Default.Comment
              "message" -> Icons.Default.ChatBubble
              "strug_reply" -> Icons.Default.AutoAwesome
              "plus_approval" -> Icons.Default.Star
              "admin_announcement" -> Icons.Default.Campaign
              else -> Icons.Default.Notifications
            },
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(10.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (notif.senderUsername.isNotBlank()) "@${notif.senderUsername}" else "STRUGX",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = PureWhite
          )

          Text(
            text = formatTimeAgo(notif.createdAt),
            fontSize = 11.sp,
            color = TextMuted
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = notif.text.ifBlank {
            when (notif.type) {
              "follow" -> "started following you"
              "comment" -> "commented on your post"
              "message" -> "sent you a message"
              "strug_reply" -> "replied to your Strug"
              "plus_approval" -> "Your Strugx Plus subscription is active!"
              "admin_announcement" -> "Official Announcement"
              else -> "New notification"
            }
          },
          fontSize = 13.sp,
          color = TextSecondary,
          lineHeight = 17.sp,
          maxLines = 2
        )
      }

      if (!notif.isRead) {
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(PureWhite)
        )
      }
    }
  }
}

private fun formatTimeAgo(time: Long): String {
  val diff = System.currentTimeMillis() - time
  val mins = diff / 60000
  val hours = mins / 60
  val days = hours / 24
  return when {
    mins < 1 -> "just now"
    mins < 60 -> "${mins}m"
    hours < 24 -> "${hours}h"
    days < 7 -> "${days}d"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
  }
}
