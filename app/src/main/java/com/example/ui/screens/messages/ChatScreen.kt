package com.example.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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
import coil.compose.AsyncImage
import com.example.data.firebase.FirebaseService
import com.example.data.models.Message
import com.example.data.models.User
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
  conversationId: String,
  currentUser: User,
  recipientId: String,
  recipientUsername: String,
  recipientAvatar: String,
  onBack: () -> Unit,
  onUserClick: (String) -> Unit
) {
  var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
  var inputText by remember { mutableStateOf("") }
  var showMenu by remember { mutableStateOf(false) }
  var isBlockedByUser by remember { mutableStateOf(false) }
  var hasBlockedUser by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  LaunchedEffect(recipientId) {
    isBlockedByUser = FirebaseService.isBlocked(recipientId, currentUser.uid)
    hasBlockedUser = FirebaseService.isBlocked(currentUser.uid, recipientId)
  }

  LaunchedEffect(conversationId) {
    FirebaseService.getMessagesFlow(conversationId).collectLatest { list ->
      messages = list
      if (list.isNotEmpty()) {
        listState.animateScrollToItem(list.size - 1)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Top black status bar safe strip
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    // Chat Header
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

      Row(
        modifier = Modifier
          .weight(1f)
          .clickable { onUserClick(recipientId) },
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
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
            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(text = recipientUsername, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PureWhite)
          Text(text = "Direct Message", fontSize = 11.sp, color = TextSecondary)
        }
      }

      // Overflow Menu
      Box {
        IconButton(onClick = { showMenu = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "Chat Options", tint = PureWhite)
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        ) {
          DropdownMenuItem(
            text = { Text("View Profile", color = PureWhite) },
            onClick = {
              showMenu = false
              onUserClick(recipientId)
            },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PureWhite) }
          )

          DropdownMenuItem(
            text = { Text(if (hasBlockedUser) "Unblock User" else "Block User", color = if (hasBlockedUser) PureWhite else DangerRed) },
            onClick = {
              showMenu = false
              scope.launch {
                if (hasBlockedUser) {
                  FirebaseService.unblockUser(currentUser.uid, recipientId)
                  hasBlockedUser = false
                } else {
                  FirebaseService.blockUser(currentUser.uid, recipientId)
                  hasBlockedUser = true
                }
              }
            },
            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = if (hasBlockedUser) PureWhite else DangerRed) }
          )

          DropdownMenuItem(
            text = { Text("Delete Conversation", color = DangerRed) },
            onClick = {
              showMenu = false
              scope.launch {
                FirebaseService.deleteConversationForUser(conversationId, currentUser.uid)
                onBack()
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

    // Blocking warning banner if blocked
    if (hasBlockedUser || isBlockedByUser) {
      Surface(
        color = GlassSurface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Block, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (hasBlockedUser) "You have blocked @$recipientUsername" else "@$recipientUsername has blocked you",
            color = PureWhite,
            fontSize = 12.sp
          )
        }
      }
    }

    // Message Bubbles List
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(messages, key = { it.messageId }) { msg ->
        val isMe = msg.senderId == currentUser.uid

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
          Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
          ) {
            // Shared Profile Card Preview
            if (msg.sharedProfileUserId.isNotBlank()) {
              Surface(
                color = GlassSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .padding(bottom = 4.dp)
                  .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                  .clickable { onUserClick(msg.sharedProfileUserId) }
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(42.dp)
                      .clip(CircleShape)
                      .background(DarkSurfaceElevated)
                      .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    if (msg.sharedProfileAvatar.isNotBlank()) {
                      AsyncImage(
                        model = msg.sharedProfileAvatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                      )
                    } else {
                      Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(text = "Shared Profile", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text(text = "@${msg.sharedProfileUsername}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Text(text = "View Profile →", fontSize = 11.sp, color = PureWhite)
                  }
                }
              }
            }

            // Shared Post Card Preview
            if (msg.sharedPostId.isNotBlank()) {
              Surface(
                color = GlassSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                  .padding(bottom = 4.dp)
                  .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
              ) {
                Column(modifier = Modifier.padding(8.dp)) {
                  Text(
                    text = "Shared Post",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = PureWhite
                  )
                  if (msg.sharedPostImage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AsyncImage(
                      model = msg.sharedPostImage,
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                    )
                  }
                  if (msg.sharedPostCaption.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = msg.sharedPostCaption, fontSize = 12.sp, color = PureWhite, maxLines = 2)
                  }
                }
              }
            }

            // Strug Reply Preview
            if (msg.strugId.isNotBlank()) {
              Surface(
                color = GlassSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                  .padding(bottom = 4.dp)
                  .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
              ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                  if (msg.strugImageUrl.isNotBlank()) {
                    AsyncImage(
                      model = msg.strugImageUrl,
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                  }
                  Text(text = "Replied to Strug", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                }
              }
            }

            // Regular text bubble
            if (msg.text.isNotBlank()) {
              Surface(
                color = if (isMe) PureWhite else DarkSurfaceElevated,
                shape = RoundedCornerShape(
                  topStart = 18.dp,
                  topEnd = 18.dp,
                  bottomStart = if (isMe) 18.dp else 4.dp,
                  bottomEnd = if (isMe) 4.dp else 18.dp
                ),
                modifier = Modifier.border(
                  width = if (isMe) 0.dp else 1.dp,
                  color = GlassBorder,
                  shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMe) 18.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 18.dp
                  )
                )
              ) {
                Text(
                  text = msg.text,
                  color = if (isMe) PureBlack else PureWhite,
                  fontSize = 14.sp,
                  lineHeight = 19.sp,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
              }
            }

            // Timestamp
            Text(
              text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.createdAt)),
              fontSize = 10.sp,
              color = TextMuted,
              modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
          }
        }
      }
    }

    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(0.5.dp)
        .background(GlassBorder)
    )

    // Input Bar (Disabled if blocked)
    if (!hasBlockedUser && !isBlockedByUser) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp)
          .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("Message...", fontSize = 14.sp, color = TextMuted) },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedContainerColor = GlassSurface,
            unfocusedContainerColor = GlassSurface,
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = GlassBorder
          ),
          maxLines = 4
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = {
            if (inputText.isNotBlank()) {
              val textToSend = inputText.trim()
              inputText = ""
              scope.launch {
                FirebaseService.sendMessage(
                  conversationId = conversationId,
                  senderId = currentUser.uid,
                  receiverId = recipientId,
                  senderUsername = currentUser.username,
                  senderAvatar = currentUser.profileImageUrl,
                  receiverUsername = recipientUsername,
                  receiverAvatar = recipientAvatar,
                  text = textToSend
                )
              }
            }
          },
          enabled = inputText.isNotBlank(),
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (inputText.isNotBlank()) PureWhite else GlassSurface)
        ) {
          Icon(
            imageVector = Icons.Default.Send,
            contentDescription = "Send Message",
            tint = if (inputText.isNotBlank()) PureBlack else TextMuted,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        contentAlignment = Alignment.Center
      ) {
        Text("You cannot send messages in this conversation.", color = TextMuted, fontSize = 13.sp)
      }
    }
  }
}
