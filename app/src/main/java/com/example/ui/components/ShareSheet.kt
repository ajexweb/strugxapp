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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.Conversation
import com.example.data.models.Post
import com.example.data.models.User
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
  post: Post,
  conversations: List<Conversation>,
  currentUserId: String,
  onDismiss: () -> Unit,
  onSearchUsers: suspend (String) -> List<User>,
  onShareToUser: (targetUserId: String, targetUsername: String, targetAvatar: String) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
  var isSearching by remember { mutableStateOf(false) }

  LaunchedEffect(searchQuery) {
    if (searchQuery.isNotBlank()) {
      isSearching = true
      searchResults = onSearchUsers(searchQuery)
      isSearching = false
    } else {
      searchResults = emptyList()
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = DarkSurfaceElevated,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.75f)
        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Share Post to Friend",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }

      // Mini Post Preview Card
      Surface(
        color = GlassSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (!post.isTextOnly && post.imageUrl.isNotBlank()) {
            AsyncImage(
              model = post.imageUrl,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Post by @${post.username}",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = PureWhite
            )
            if (post.caption.isNotBlank()) {
              Text(
                text = post.caption,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1
              )
            }
          }
        }
      }

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search friends...", fontSize = 14.sp, color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder
        ),
        singleLine = true
      )

      HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

      // Recipient list: Search results if query active, otherwise recent conversations
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (searchQuery.isNotBlank()) {
          if (isSearching) {
            item {
              Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
              }
            }
          } else if (searchResults.isEmpty()) {
            item {
              Text(
                text = "No users found matching '$searchQuery'",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
              )
            }
          } else {
            items(searchResults) { user ->
              if (user.uid != currentUserId) {
                RecipientRow(
                  username = user.username,
                  displayName = user.displayName,
                  avatarUrl = user.profileImageUrl,
                  onSend = {
                    onShareToUser(user.uid, user.username, user.profileImageUrl)
                    onDismiss()
                  }
                )
              }
            }
          }
        } else {
          // Recent conversations
          if (conversations.isEmpty()) {
            item {
              Text(
                text = "Search for a user above to send this post directly into your chat.",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
              )
            }
          } else {
            items(conversations) { conv ->
              val otherUid = conv.participants.firstOrNull { it != currentUserId } ?: ""
              val otherUsername = conv.participantUsernames[otherUid] ?: "User"
              val otherAvatar = conv.participantAvatars[otherUid] ?: ""
              val otherName = conv.participantNames[otherUid] ?: otherUsername

              RecipientRow(
                username = otherUsername,
                displayName = otherName,
                avatarUrl = otherAvatar,
                onSend = {
                  onShareToUser(otherUid, otherUsername, otherAvatar)
                  onDismiss()
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
private fun RecipientRow(
  username: String,
  displayName: String,
  avatarUrl: String,
  onSend: () -> Unit
) {
  var sent by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(DarkSurfaceElevated)
          .border(1.dp, GlassBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        if (avatarUrl.isNotBlank()) {
          AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(text = displayName.ifBlank { username }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PureWhite)
        Text(text = "@$username", fontSize = 12.sp, color = TextSecondary)
      }
    }

    Button(
      onClick = {
        sent = true
        onSend()
      },
      enabled = !sent,
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = if (sent) GlassSurface else PureWhite,
        contentColor = if (sent) TextSecondary else PureBlack
      ),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
      modifier = Modifier
        .height(34.dp)
        .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
    ) {
      Text(
        text = if (sent) "Sent" else "Send",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
