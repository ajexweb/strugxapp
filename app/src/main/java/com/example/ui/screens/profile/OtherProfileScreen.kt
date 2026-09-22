package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.models.Post
import com.example.data.models.User
import com.example.ui.components.FollowListSheet
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun OtherProfileScreen(
  targetUserId: String,
  currentUser: User,
  onBack: () -> Unit,
  onOpenChat: (recipientId: String, recipientUsername: String, recipientAvatar: String) -> Unit,
  onShowPlus: () -> Unit,
  onPostClick: (Post) -> Unit
) {
  var targetUser by remember { mutableStateOf<User?>(null) }
  var isFollowing by remember { mutableStateOf(false) }
  var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var selectedTab by remember { mutableIntStateOf(0) }
  var showMenu by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  var showFollowSheet by remember { mutableStateOf<Boolean?>(null) } // true = followers, false = following, null = closed
  var hasBlockedUser by remember { mutableStateOf(false) }
  var isBlockedByUser by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Fetch target user data
  LaunchedEffect(targetUserId) {
    hasBlockedUser = FirebaseService.isBlocked(currentUser.uid, targetUserId)
    isBlockedByUser = FirebaseService.isBlocked(targetUserId, currentUser.uid)
    targetUser = FirebaseService.getUserProfile(targetUserId)
    isFollowing = FirebaseService.isFollowing(currentUser.uid, targetUserId)
    FirebaseService.getUserPostsFlow(targetUserId).collectLatest { posts ->
      userPosts = posts
    }
  }

  val imagePosts = remember(userPosts) { userPosts.filter { !it.isTextOnly && it.imageUrl.isNotBlank() } }
  val textPosts = remember(userPosts) { userPosts.filter { it.isTextOnly } }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Top black status strip
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    // Top Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
        }
        Text(
          text = targetUser?.username ?: "Profile",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
        if (targetUser?.isPlus == true) {
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = GlassSurface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
          ) {
            Text(
              text = "PLUS",
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
          val shareText = "Check out @${targetUser?.username ?: "user"} on Strugx: https://strugx.app/user/${targetUser?.username}"
          val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
          val clip = android.content.ClipData.newPlainText("Strugx Profile", shareText)
          clipboard.setPrimaryClip(clip)
          Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
        }) {
          Icon(Icons.Default.Share, contentDescription = "Share Profile", tint = PureWhite)
        }

        Box {
          IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
          }
          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
              .background(DarkSurfaceElevated)
              .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
          ) {
            DropdownMenuItem(
              text = { Text("Share Profile", color = PureWhite) },
              onClick = {
                showMenu = false
                val shareText = "Check out @${targetUser?.username ?: "user"} on Strugx: https://strugx.app/user/${targetUser?.username}"
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Strugx Profile", shareText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
              },
              leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = PureWhite) }
            )

            DropdownMenuItem(
              text = { Text(if (hasBlockedUser) "Unblock User" else "Block User", color = if (hasBlockedUser) PureWhite else DangerRed) },
              onClick = {
                showMenu = false
                scope.launch {
                  if (hasBlockedUser) {
                    FirebaseService.unblockUser(currentUser.uid, targetUserId)
                    hasBlockedUser = false
                    Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
                  } else {
                    FirebaseService.blockUser(currentUser.uid, targetUserId)
                    hasBlockedUser = true
                    Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = if (hasBlockedUser) PureWhite else DangerRed) }
            )

            DropdownMenuItem(
              text = { Text("Report User", color = DangerRed) },
              onClick = {
                showMenu = false
                showReportDialog = true
              },
              leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = DangerRed) }
            )
          }
        }
      }
    }

    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

    if (hasBlockedUser || isBlockedByUser) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Block, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = if (hasBlockedUser) "You blocked this user" else "User unavailable",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = PureWhite
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = if (hasBlockedUser) "You cannot view their posts or interact while blocked." else "This account's content is not available.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
          if (hasBlockedUser) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = {
                scope.launch {
                  FirebaseService.unblockUser(currentUser.uid, targetUserId)
                  hasBlockedUser = false
                  Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Unblock User", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else if (targetUser == null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PureWhite)
      }
    } else {
      val user = targetUser!!

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Avatar
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(DarkSurfaceElevated)
              .border(1.5.dp, GlassBorder, CircleShape),
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
              Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
            }
          }

          // Stats
          Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${userPosts.size}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PureWhite)
              Text("Posts", fontSize = 12.sp, color = TextSecondary)
            }
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showFollowSheet = true }
                .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
              Text("${user.followerCount}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PureWhite)
              Text("Followers", fontSize = 12.sp, color = TextSecondary)
            }
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showFollowSheet = false }
                .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
              Text("${user.followingCount}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PureWhite)
              Text("Following", fontSize = 12.sp, color = TextSecondary)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = user.displayName.ifBlank { user.username },
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = PureWhite
        )
        Text(
          text = "@${user.username}",
          fontSize = 12.sp,
          color = TextSecondary
        )
        if (user.bio.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = user.bio, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons: Follow / Following & Message
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              scope.launch {
                if (isFollowing) {
                  FirebaseService.unfollowUser(currentUser.uid, user.uid)
                  isFollowing = false
                  targetUser = targetUser?.copy(followersCount = maxOf(0, user.followerCount - 1))
                } else {
                  val result = FirebaseService.followUser(currentUser.uid, user.uid)
                  if (result.isSuccess) {
                    isFollowing = true
                    targetUser = targetUser?.copy(followersCount = user.followerCount + 1)
                  } else {
                    val err = result.exceptionOrNull()?.message ?: "Failed to follow"
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    if (err.contains("limit")) {
                      onShowPlus()
                    }
                  }
                }
              }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isFollowing) GlassSurface else PureWhite,
              contentColor = if (isFollowing) PureWhite else PureBlack
            ),
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
              .border(
                width = 1.dp,
                color = if (isFollowing) GlassBorder else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
              )
          ) {
            Text(if (isFollowing) "Following" else "Follow", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = {
              onOpenChat(user.uid, user.username, user.profileImageUrl)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = GlassSurface,
              contentColor = PureWhite
            ),
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
              .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
          ) {
            Text("Message", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      // Tabs: Photos | Text
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
          text = { Text("Photos (${imagePosts.size})", color = if (selectedTab == 0) PureWhite else TextSecondary) },
          icon = { Icon(Icons.Default.GridOn, contentDescription = null, tint = if (selectedTab == 0) PureWhite else TextSecondary, modifier = Modifier.size(18.dp)) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Text (${textPosts.size})", color = if (selectedTab == 1) PureWhite else TextSecondary) },
          icon = { Icon(Icons.Default.Notes, contentDescription = null, tint = if (selectedTab == 1) PureWhite else TextSecondary, modifier = Modifier.size(18.dp)) }
        )
      }

      if (selectedTab == 0) {
        if (imagePosts.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No photos posted yet.", color = TextSecondary, fontSize = 14.sp)
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            verticalArrangement = Arrangement.spacedBy(1.5.dp)
          ) {
            items(imagePosts, key = { it.postId }) { post ->
              Box(
                modifier = Modifier
                  .aspectRatio(1f)
                  .background(DarkSurfaceElevated)
                  .clickable { onPostClick(post) }
              ) {
                AsyncImage(
                  model = post.imageUrl,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              }
            }
          }
        }
      } else {
        if (textPosts.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No text posts yet.", color = TextSecondary, fontSize = 14.sp)
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(textPosts, key = { it.postId }) { post ->
              Surface(
                color = GlassSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                  .clickable { onPostClick(post) }
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(text = post.caption, fontSize = 14.sp, color = PureWhite, lineHeight = 20.sp)
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${post.viewCount} views", fontSize = 11.sp, color = TextMuted)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Followers / Following Sheet
  showFollowSheet?.let { isFollowers ->
    FollowListSheet(
      userId = targetUserId,
      isFollowers = isFollowers,
      currentUserId = currentUser.uid,
      onDismiss = { showFollowSheet = null },
      onUserClick = { clickedUid ->
        showFollowSheet = null
        if (clickedUid != targetUserId) {
          // If different user, re-trigger fetch
          // Or if parent has navigation, it can handle it
        }
      },
      onShowPlus = onShowPlus
    )
  }

  // Report User Dialog
  if (showReportDialog) {
    var reason by remember { mutableStateOf("Harassment") }
    var isReporting by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showReportDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Report User", color = PureWhite, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Select a reason to report @${targetUser?.username}:", fontSize = 13.sp, color = TextSecondary)
          Spacer(modifier = Modifier.height(10.dp))
          listOf("Harassment or Bullying", "Fake Profile / Impersonation", "Spam", "Inappropriate Content").forEach { r ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { reason = r }
                .padding(vertical = 4.dp)
            ) {
              RadioButton(
                selected = reason == r,
                onClick = { reason = r },
                colors = RadioButtonDefaults.colors(selectedColor = PureWhite, unselectedColor = TextSecondary)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(r, fontSize = 13.sp, color = PureWhite)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            isReporting = true
            scope.launch {
              FirebaseService.submitReport(
                reporterId = currentUser.uid,
                reporterUsername = currentUser.username,
                targetType = "user",
                targetId = targetUserId,
                reason = reason,
                details = "Reported from profile"
              )
              isReporting = false
              showReportDialog = false
              Toast.makeText(context, "User reported. Thank you.", Toast.LENGTH_SHORT).show()
            }
          },
          enabled = !isReporting,
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Submit Report")
        }
      },
      dismissButton = {
        TextButton(onClick = { showReportDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}
