package com.example.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.FirebaseService
import com.example.data.models.Conversation
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(
  currentUserId: String,
  onOpenConversation: (convId: String, recipientId: String, recipientUsername: String, recipientAvatar: String) -> Unit
) {
  var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Primary, 1 = General, 2 = Requests
  val scope = rememberCoroutineScope()

  LaunchedEffect(currentUserId) {
    FirebaseService.getConversationsFlow(currentUserId).collectLatest { list ->
      conversations = list
    }
  }

  // Filter conversations by category
  val filteredConversations = remember(conversations, selectedTab) {
    when (selectedTab) {
      0 -> conversations.filter {
        val cat = it.categories[currentUserId] ?: "primary"
        cat == "primary"
      }
      1 -> conversations.filter {
        val cat = it.categories[currentUserId] ?: "primary"
        cat == "general"
      }
      else -> conversations.filter {
        val cat = it.categories[currentUserId] ?: "primary"
        cat == "requests"
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Black status bar safe strip
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    // Header
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
      Text(
        text = "Messages",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = PureWhite
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = "Private direct messages with friends & followers",
        fontSize = 12.sp,
        color = TextSecondary
      )
    }

    // Tabs: Primary, General, Requests
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = DarkBackground,
      contentColor = PureWhite,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = PureWhite
        )
      },
      divider = { HorizontalDivider(color = GlassBorder, thickness = 0.5.dp) }
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Primary", fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("General", fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("Requests", fontSize = 13.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
      )
    }

    if (filteredConversations.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Chat, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = when (selectedTab) {
              0 -> "No primary messages"
              1 -> "No general messages"
              else -> "No message requests"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PureWhite
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Send a message from someone's profile or start connecting with people.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
      ) {
        items(filteredConversations, key = { it.conversationId }) { conv ->
          val recipientId = conv.participants.firstOrNull { it != currentUserId } ?: ""
          val recipientUsername = conv.participantUsernames[recipientId] ?: "User"
          val recipientAvatar = conv.participantAvatars[recipientId] ?: ""
          val recipientName = conv.participantNames[recipientId] ?: recipientUsername
          var showItemMenu by remember { mutableStateOf(false) }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onOpenConversation(conv.conversationId, recipientId, recipientUsername, recipientAvatar)
              }
              .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Avatar
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .border(1.dp, GlassBorder, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              if (recipientAvatar.isNotBlank()) {
                AsyncImage(
                  model = recipientAvatar,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(28.dp))
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
                  text = recipientName.ifBlank { recipientUsername },
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = PureWhite
                )

                if (conv.lastMessageAt > 0) {
                  Text(
                    text = formatMessageTime(conv.lastMessageAt),
                    fontSize = 11.sp,
                    color = TextMuted
                  )
                }
              }

              Spacer(modifier = Modifier.height(2.dp))

              Text(
                text = conv.lastMessage.ifBlank { "Started a conversation" },
                fontSize = 13.sp,
                color = if (conv.lastSenderId != currentUserId) PureWhite else TextSecondary,
                fontWeight = if (conv.lastSenderId != currentUserId) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
              )
            }

            // Options menu
            Box {
              IconButton(
                onClick = { showItemMenu = true },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Conversation options", tint = TextMuted, modifier = Modifier.size(18.dp))
              }

              DropdownMenu(
                expanded = showItemMenu,
                onDismissRequest = { showItemMenu = false },
                modifier = Modifier
                  .background(DarkSurfaceElevated)
                  .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
              ) {
                if (selectedTab != 0) {
                  DropdownMenuItem(
                    text = { Text("Move to Primary", color = PureWhite) },
                    onClick = {
                      showItemMenu = false
                      scope.launch {
                        FirebaseService.updateConversationCategory(conv.conversationId, currentUserId, "primary")
                      }
                    },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = PureWhite) }
                  )
                }
                if (selectedTab != 1) {
                  DropdownMenuItem(
                    text = { Text("Move to General", color = PureWhite) },
                    onClick = {
                      showItemMenu = false
                      scope.launch {
                        FirebaseService.updateConversationCategory(conv.conversationId, currentUserId, "general")
                      }
                    },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = PureWhite) }
                  )
                }
                DropdownMenuItem(
                  text = { Text("Delete / Hide", color = DangerRed) },
                  onClick = {
                    showItemMenu = false
                    scope.launch {
                      FirebaseService.deleteConversationForUser(conv.conversationId, currentUserId)
                    }
                  },
                  leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) }
                )
              }
            }
          }

          Spacer(
            modifier = Modifier
              .fillMaxWidth()
              .height(0.5.dp)
              .background(GlassBorder)
          )
        }
      }
    }
  }
}

private fun formatMessageTime(time: Long): String {
  val diff = System.currentTimeMillis() - time
  val mins = diff / 60000
  val hours = mins / 60
  val days = hours / 24
  return when {
    mins < 1 -> "now"
    mins < 60 -> "${mins}m"
    hours < 24 -> "${hours}h"
    days < 7 -> "${days}d"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
  }
}
