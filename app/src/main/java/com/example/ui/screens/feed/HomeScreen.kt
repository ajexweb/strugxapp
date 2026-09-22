package com.example.ui.screens.feed

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseService
import com.example.data.models.Advertisement
import com.example.data.models.Comment
import com.example.data.models.Post
import com.example.data.models.Strug
import com.example.data.models.User
import com.example.data.storage.PrefetchManager
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  currentUser: User,
  isAdmin: Boolean,
  onNavigateToAdmin: () -> Unit,
  onNavigateToUserProfile: (String) -> Unit,
  onNavigateToChat: (recipientId: String, recipientUsername: String, recipientAvatar: String) -> Unit,
  onShowPlus: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var strugs by remember { mutableStateOf<List<Strug>>(emptyList()) }
  var ads by remember { mutableStateOf<List<Advertisement>>(emptyList()) }
  var followingSet by remember { mutableStateOf<Set<String>>(emptySet()) }
  var unreadNotifCount by remember { mutableIntStateOf(0) }

  // Modals & Sheets state
  var showCreateModal by remember { mutableStateOf(false) }
  var viewingStrug by remember { mutableStateOf<Strug?>(null) }
  var viewingStrugGroups by remember { mutableStateOf<List<List<Strug>>>(emptyList()) }
  var viewingGroupIndex by remember { mutableIntStateOf(0) }
  var showNotificationsModal by remember { mutableStateOf(false) }
  var showOnboardingFollowDialog by remember { mutableStateOf(!currentUser.hasCompletedOnboarding) }
  var commentingPost by remember { mutableStateOf<Post?>(null) }
  var activeComments by remember { mutableStateOf<List<Comment>>(emptyList()) }
  var sharingPost by remember { mutableStateOf<Post?>(null) }
  var reportingPost by remember { mutableStateOf<Post?>(null) }
  var editingPost by remember { mutableStateOf<Post?>(null) }
  var deletingPost by remember { mutableStateOf<Post?>(null) }
  var musicAddingPost by remember { mutableStateOf<Post?>(null) }

  // Real-time unread notifications listener
  LaunchedEffect(currentUser.uid) {
    FirebaseService.getUnreadNotificationCountFlow(currentUser.uid).collectLatest { count ->
      unreadNotifCount = count
    }
  }

  // Real-time Posts Listener
  LaunchedEffect(Unit) {
    FirebaseService.getFeedPostsFlow().collectLatest { fetchedPosts ->
      posts = fetchedPosts
    }
  }

  // Real-time Strugs Listener
  LaunchedEffect(Unit) {
    FirebaseService.getActiveStrugsFlow().collectLatest { fetchedStrugs ->
      strugs = fetchedStrugs
    }
  }

  // Real-time Active Ads Listener
  LaunchedEffect(Unit) {
    FirebaseService.getActiveAdsFlow().collectLatest { fetchedAds ->
      ads = fetchedAds
    }
  }

  // Comments listener when commenting on a post
  LaunchedEffect(commentingPost?.postId) {
    val pid = commentingPost?.postId
    if (pid != null) {
      FirebaseService.getCommentsFlow(pid).collectLatest { fetchedComments ->
        activeComments = fetchedComments
      }
    } else {
      activeComments = emptyList()
    }
  }

  // Intelligent Image Prefetching (Next 4 images ahead of visible item)
  LaunchedEffect(listState.firstVisibleItemIndex, posts) {
    val postImageUrls = posts.mapNotNull { if (!it.isTextOnly && it.imageUrl.isNotBlank()) it.imageUrl else null }
    PrefetchManager.prefetchNextImages(
      context = context,
      imageUrls = postImageUrls,
      currentIndex = listState.firstVisibleItemIndex,
      count = 4
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Top Bar with Black status strip + '+' button + Brand + Admin + Notifications Bell
    StrugxTopBar(
      onAddClick = { showCreateModal = true },
      isAdmin = isAdmin,
      onAdminClick = onNavigateToAdmin,
      unreadNotificationCount = unreadNotifCount,
      onNotificationsClick = { showNotificationsModal = true }
    )

    // Feed content
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      // 1. Strugs Tray (Square with rounded corners, each user separate)
      item {
        StrugTray(
          strugs = strugs,
          currentUserAvatar = currentUser.profileImageUrl,
          currentUsername = currentUser.username,
          onAddStrugClick = { showCreateModal = true },
          onUserGroupClick = { userGroup, groupIndex, allGroups ->
            viewingStrugGroups = allGroups
            viewingGroupIndex = groupIndex
          },
          onStrugClick = { strug -> viewingStrug = strug }
        )
        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(GlassBorder)
        )
      }

      // 2. Feed Posts with Ads inserted every 7 normal posts
      if (posts.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(48.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(54.dp)
              )
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "Your feed is empty",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Tap '+' to share your first post or story!",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
      } else {
        itemsIndexed(posts, key = { _, post -> post.postId }) { index, post ->
          // Trigger post view count & prefetch upcoming images
          LaunchedEffect(post.postId) {
            FirebaseService.recordPostView(post.postId, currentUser.uid)
            val allImageUrls = posts.mapNotNull { if (it.imageUrl.isNotBlank()) it.imageUrl else null }
            val currentImgIndex = allImageUrls.indexOf(post.imageUrl)
            if (currentImgIndex >= 0) {
              PrefetchManager.prefetchNextImages(context, allImageUrls, currentImgIndex, count = 4)
            }
          }

          // Check following state
          var isFollowingThisUser by remember(post.userId) {
            mutableStateOf(followingSet.contains(post.userId))
          }

          LaunchedEffect(post.userId) {
            if (post.userId != currentUser.uid) {
              isFollowingThisUser = FirebaseService.isFollowing(currentUser.uid, post.userId)
              if (isFollowingThisUser) {
                followingSet = followingSet + post.userId
              }
            }
          }

          // Normal Post Card
          PostCard(
            post = post,
            currentUserId = currentUser.uid,
            isFollowing = isFollowingThisUser,
            onFollowToggle = {
              scope.launch {
                if (isFollowingThisUser) {
                  FirebaseService.unfollowUser(currentUser.uid, post.userId)
                  isFollowingThisUser = false
                  followingSet = followingSet - post.userId
                } else {
                  val result = FirebaseService.followUser(currentUser.uid, post.userId)
                  if (result.isSuccess) {
                    isFollowingThisUser = true
                    followingSet = followingSet + post.userId
                  } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Failed to follow"
                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                    if (errMsg.contains("limit")) {
                      onShowPlus()
                    }
                  }
                }
              }
            },
            onUserClick = { targetUid -> onNavigateToUserProfile(targetUid) },
            onCommentClick = { selectedPost -> commentingPost = selectedPost },
            onShareClick = { selectedPost -> sharingPost = selectedPost },
            onEditClick = { postToEdit -> editingPost = postToEdit },
            onDeleteClick = { postToDelete -> deletingPost = postToDelete },
            onReportClick = { postToReport -> reportingPost = postToReport },
            canAddMusic = currentUser.isPlus || isAdmin,
            onAddMusicClick = { targetPost ->
              if (currentUser.isPlus || isAdmin) {
                musicAddingPost = targetPost
              } else {
                onShowPlus()
              }
            }
          )

          // INSERT ADVERTISEMENT AFTER EVERY 7 NORMAL POSTS (indices 6, 13, 20, 27...)
          if ((index + 1) % 7 == 0 && ads.isNotEmpty()) {
            val adIndex = ((index + 1) / 7 - 1) % ads.size
            val ad = ads[adIndex]
            AdCard(
              ad = ad,
              onImpression = { adId ->
                scope.launch { FirebaseService.recordAdImpression(adId) }
              },
              onClick = { adId ->
                scope.launch { FirebaseService.recordAdClick(adId) }
              }
            )
          }
        }
      }
    }
  }

  // Create Modal (+ button)
  if (showCreateModal) {
    CreatePostModal(
      currentUserId = currentUser.uid,
      currentUsername = currentUser.username,
      currentUserAvatar = currentUser.profileImageUrl,
      onDismiss = { showCreateModal = false },
      onCreateImagePost = { imageUrl, caption, musicTrack ->
        scope.launch {
          FirebaseService.createPost(
            userId = currentUser.uid,
            username = currentUser.username,
            userProfileImageUrl = currentUser.profileImageUrl,
            imageUrl = imageUrl,
            caption = caption,
            isTextOnly = false,
            musicTitle = musicTrack?.trackName ?: "",
            musicArtist = musicTrack?.artistName ?: "",
            musicAudioUrl = musicTrack?.previewUrl ?: "",
            musicCoverUrl = musicTrack?.artworkUrl ?: ""
          )
          Toast.makeText(context, "Post published!", Toast.LENGTH_SHORT).show()
        }
      },
      onCreateTextPost = { text, musicTrack ->
        scope.launch {
          FirebaseService.createPost(
            userId = currentUser.uid,
            username = currentUser.username,
            userProfileImageUrl = currentUser.profileImageUrl,
            imageUrl = null,
            caption = text,
            isTextOnly = true,
            musicTitle = musicTrack?.trackName ?: "",
            musicArtist = musicTrack?.artistName ?: "",
            musicAudioUrl = musicTrack?.previewUrl ?: "",
            musicCoverUrl = musicTrack?.artworkUrl ?: ""
          )
          Toast.makeText(context, "Text post published!", Toast.LENGTH_SHORT).show()
        }
      },
      onCreateStrug = { imageUrl, caption, musicTrack ->
        scope.launch {
          FirebaseService.createStrug(
            userId = currentUser.uid,
            username = currentUser.username,
            avatar = currentUser.profileImageUrl,
            imageUrl = imageUrl,
            caption = caption,
            musicTitle = musicTrack?.trackName ?: "",
            musicArtist = musicTrack?.artistName ?: "",
            musicAudioUrl = musicTrack?.previewUrl ?: "",
            musicCoverUrl = musicTrack?.artworkUrl ?: ""
          )
          Toast.makeText(context, "Strug posted!", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  // Strug Viewer Modal (Supports separated user groups & smooth navigation)
  if (viewingStrugGroups.isNotEmpty()) {
    StrugViewerModal(
      allUserGroups = viewingStrugGroups,
      initialUserIndex = viewingGroupIndex,
      currentUserId = currentUser.uid,
      onDismiss = { viewingStrugGroups = emptyList() },
      onReply = { targetStrug, replyMsg ->
        scope.launch {
          val convId = FirebaseService.conversationIdFor(currentUser.uid, targetStrug.userId)
          FirebaseService.sendMessage(
            conversationId = convId,
            senderId = currentUser.uid,
            receiverId = targetStrug.userId,
            senderUsername = currentUser.username,
            senderAvatar = currentUser.profileImageUrl,
            receiverUsername = targetStrug.username,
            receiverAvatar = targetStrug.userProfileImageUrl,
            text = replyMsg,
            strugId = targetStrug.strugId,
            strugImageUrl = targetStrug.imageUrl
          )
          Toast.makeText(context, "Reply sent to ${targetStrug.username}", Toast.LENGTH_SHORT).show()
        }
      }
    )
  } else viewingStrug?.let { strug ->
    val startIndex = strugs.indexOfFirst { it.strugId == strug.strugId }.coerceAtLeast(0)
    StrugViewerModal(
      strugs = if (strugs.isNotEmpty()) strugs else listOf(strug),
      initialIndex = startIndex,
      currentUserId = currentUser.uid,
      onDismiss = { viewingStrug = null },
      onReply = { targetStrug, replyMsg ->
        scope.launch {
          val convId = FirebaseService.conversationIdFor(currentUser.uid, targetStrug.userId)
          FirebaseService.sendMessage(
            conversationId = convId,
            senderId = currentUser.uid,
            receiverId = targetStrug.userId,
            senderUsername = currentUser.username,
            senderAvatar = currentUser.profileImageUrl,
            receiverUsername = targetStrug.username,
            receiverAvatar = targetStrug.userProfileImageUrl,
            text = replyMsg,
            strugId = targetStrug.strugId,
            strugImageUrl = targetStrug.imageUrl
          )
          Toast.makeText(context, "Reply sent to ${targetStrug.username}", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  // Comment Sheet
  commentingPost?.let { targetPost ->
    CommentSheet(
      post = targetPost,
      comments = activeComments,
      currentUserId = currentUser.uid,
      isAdmin = isAdmin,
      onDismiss = { commentingPost = null },
      onUserClick = onNavigateToUserProfile,
      onAddComment = { text ->
        scope.launch {
          FirebaseService.addComment(
            postId = targetPost.postId,
            userId = currentUser.uid,
            username = currentUser.username,
            avatar = currentUser.profileImageUrl,
            text = text
          )
        }
      },
      onDeleteComment = { commentId ->
        scope.launch {
          FirebaseService.deleteComment(targetPost.postId, commentId, currentUser.uid)
        }
      }
    )
  }

  // Notifications Modal
  if (showNotificationsModal) {
    NotificationsModal(
      currentUserId = currentUser.uid,
      onDismiss = { showNotificationsModal = false },
      onUserClick = onNavigateToUserProfile
    )
  }

  // Onboarding: Follow Official Strugx Account Dialog
  if (showOnboardingFollowDialog) {
    FollowStrugxDialog(
      currentUserId = currentUser.uid,
      onDismiss = { showOnboardingFollowDialog = false }
    )
  }

  // Share Sheet (In-App Share to DM)
  sharingPost?.let { postToShare ->
    var recentConversations by remember { mutableStateOf<List<com.example.data.models.Conversation>>(emptyList()) }
    LaunchedEffect(currentUser.uid) {
      FirebaseService.getConversationsFlow(currentUser.uid).collectLatest {
        recentConversations = it
      }
    }

    ShareSheet(
      post = postToShare,
      conversations = recentConversations,
      currentUserId = currentUser.uid,
      onDismiss = { sharingPost = null },
      onSearchUsers = { q -> FirebaseService.searchUsers(q) },
      onShareToUser = { targetUid, targetUsername, targetAvatar ->
        scope.launch {
          val convId = FirebaseService.conversationIdFor(currentUser.uid, targetUid)
          FirebaseService.sendMessage(
            conversationId = convId,
            senderId = currentUser.uid,
            receiverId = targetUid,
            senderUsername = currentUser.username,
            senderAvatar = currentUser.profileImageUrl,
            receiverUsername = targetUsername,
            receiverAvatar = targetAvatar,
            text = "Shared a post by @${postToShare.username}",
            sharedPostId = postToShare.postId,
            sharedPostCaption = postToShare.caption,
            sharedPostImage = postToShare.imageUrl
          )
          Toast.makeText(context, "Post shared with @$targetUsername", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  // Report Post Dialog
  reportingPost?.let { postToReport ->
    var reportReason by remember { mutableStateOf("Spam or inappropriate content") }
    var reportDetails by remember { mutableStateOf("") }
    var isReporting by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { reportingPost = null },
      containerColor = DarkSurfaceElevated,
      title = { Text("Report Post", fontWeight = FontWeight.Bold, color = PureWhite) },
      text = {
        Column {
          Text("Help us keep Strugx safe by reporting violating content:", fontSize = 13.sp, color = TextSecondary)
          Spacer(modifier = Modifier.height(10.dp))
          val reasons = listOf("Spam", "Harassment or Hate", "Inappropriate content", "Scam or Fraud", "Copyright issue")
          reasons.forEach { r ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
              RadioButton(
                selected = reportReason == r,
                onClick = { reportReason = r },
                colors = RadioButtonDefaults.colors(selectedColor = PureWhite, unselectedColor = TextSecondary)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(r, fontSize = 13.sp, color = PureWhite)
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = reportDetails,
            onValueChange = { reportDetails = it },
            placeholder = { Text("Additional details (optional)...", fontSize = 12.sp, color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder,
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface
            ),
            modifier = Modifier.fillMaxWidth()
          )
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
                targetType = "post",
                targetId = postToReport.postId,
                reason = reportReason,
                details = reportDetails
              )
              isReporting = false
              reportingPost = null
              Toast.makeText(context, "Report submitted. Thank you.", Toast.LENGTH_SHORT).show()
            }
          },
          enabled = !isReporting,
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Submit Report")
        }
      },
      dismissButton = {
        TextButton(onClick = { reportingPost = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Edit Caption Dialog
  editingPost?.let { targetPost ->
    var editedCaption by remember { mutableStateOf(targetPost.caption) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { editingPost = null },
      containerColor = DarkSurfaceElevated,
      title = { Text("Edit Caption", color = PureWhite, fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = editedCaption,
          onValueChange = { editedCaption = it },
          placeholder = { Text("Enter caption...", color = TextMuted) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = GlassBorder,
            focusedContainerColor = GlassSurface,
            unfocusedContainerColor = GlassSurface
          ),
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            isSaving = true
            scope.launch {
              val res = FirebaseService.updatePostCaption(targetPost.postId, editedCaption.trim(), currentUser.uid)
              if (res.isSuccess) {
                editingPost = null
                Toast.makeText(context, "Post updated", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(context, "Failed to update post", Toast.LENGTH_SHORT).show()
              }
              isSaving = false
            }
          },
          enabled = !isSaving,
          colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingPost = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Delete Post Confirmation Dialog
  deletingPost?.let { targetPost ->
    AlertDialog(
      onDismissRequest = { deletingPost = null },
      containerColor = DarkSurfaceElevated,
      title = { Text("Delete Post?", color = PureWhite, fontWeight = FontWeight.Bold) },
      text = { Text("This will permanently remove this post and all its comments.", color = TextSecondary, fontSize = 13.sp) },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              FirebaseService.deletePost(targetPost.postId, currentUser.uid)
              deletingPost = null
              Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingPost = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Music Search Sheet to add music to existing post
  musicAddingPost?.let { targetPost ->
    MusicSearchSheet(
      onDismiss = { musicAddingPost = null },
      onTrackSelected = { track ->
        val selectedPostId = targetPost.postId
        musicAddingPost = null
        scope.launch {
          val res = FirebaseService.updatePostMusic(
            postId = selectedPostId,
            musicAudioUrl = track.previewUrl,
            musicTitle = track.trackName,
            musicArtist = track.artistName,
            musicCoverUrl = track.artworkUrl,
            userId = currentUser.uid
          )
          if (res.isSuccess) {
            Toast.makeText(context, "Music added to post!", Toast.LENGTH_SHORT).show()
          } else {
            val err = res.exceptionOrNull()?.message ?: "Failed to add music"
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
          }
        }
      }
    )
  }
}
