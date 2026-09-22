package com.example.ui.components

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import com.example.data.models.User
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListSheet(
  userId: String,
  isFollowers: Boolean,
  currentUserId: String,
  onDismiss: () -> Unit,
  onUserClick: (String) -> Unit,
  onShowPlus: () -> Unit
) {
  var users by remember { mutableStateOf<List<User>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var searchQuery by remember { mutableStateOf("") }
  var followingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  LaunchedEffect(userId, isFollowers) {
    isLoading = true
    val fetched = if (isFollowers) {
      FirebaseService.getFollowers(userId)
    } else {
      FirebaseService.getFollowing(userId)
    }
    users = fetched

    // Check which users current user is following
    val currentFollowing = mutableSetOf<String>()
    for (u in fetched) {
      if (u.uid != currentUserId) {
        if (FirebaseService.isFollowing(currentUserId, u.uid)) {
          currentFollowing.add(u.uid)
        }
      }
    }
    followingIds = currentFollowing
    isLoading = false
  }

  val filteredUsers = remember(users, searchQuery) {
    if (searchQuery.isBlank()) users
    else {
      val q = searchQuery.trim().lowercase(Locale.ROOT)
      users.filter {
        it.username.lowercase(Locale.ROOT).contains(q) ||
          it.displayName.lowercase(Locale.ROOT).contains(q)
      }
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
          .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (isFollowers) "Followers" else "Following",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search...", fontSize = 14.sp, color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
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

      HorizontalDivider(color = GlassBorder, thickness = 0.5.dp, modifier = Modifier.padding(top = 8.dp))

      if (isLoading) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(28.dp))
        }
      } else if (filteredUsers.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (searchQuery.isNotBlank()) "No users found matching \"$searchQuery\""
            else if (isFollowers) "No followers yet"
            else "Not following anyone yet",
            color = TextSecondary,
            fontSize = 14.sp
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredUsers, key = { it.uid }) { user ->
            val isCurrent = user.uid == currentUserId
            val isFollowed = followingIds.contains(user.uid)

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  onDismiss()
                  onUserClick(user.uid)
                }
                .padding(vertical = 6.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkBackground)
                    .border(1.dp, GlassBorder, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  if (user.profileImageUrl.isNotBlank()) {
                    AsyncImage(
                      model = user.profileImageUrl,
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = null,
                      tint = TextSecondary,
                      modifier = Modifier.size(22.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = user.displayName.ifBlank { user.username },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PureWhite
                  )
                  Text(
                    text = "@${user.username}",
                    fontSize = 12.sp,
                    color = TextSecondary
                  )
                }
              }

              if (!isCurrent) {
                Button(
                  onClick = {
                    scope.launch {
                      if (isFollowed) {
                        FirebaseService.unfollowUser(currentUserId, user.uid)
                        followingIds = followingIds - user.uid
                      } else {
                        val res = FirebaseService.followUser(currentUserId, user.uid)
                        if (res.isSuccess) {
                          followingIds = followingIds + user.uid
                        } else {
                          val msg = res.exceptionOrNull()?.message ?: "Failed to follow"
                          Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                          if (msg.contains("limit")) {
                            onShowPlus()
                          }
                        }
                      }
                    }
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowed) GlassSurface else PureWhite,
                    contentColor = if (isFollowed) PureWhite else PureBlack
                  ),
                  shape = RoundedCornerShape(20.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                  modifier = Modifier
                    .height(32.dp)
                    .border(1.dp, if (isFollowed) GlassBorder else Color.Transparent, RoundedCornerShape(20.dp))
                ) {
                  Text(
                    text = if (isFollowed) "Following" else "Follow",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
