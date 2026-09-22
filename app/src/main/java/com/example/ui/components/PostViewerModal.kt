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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.models.Comment
import com.example.data.models.Post
import com.example.data.models.User
import com.example.data.music.GlobalAudioPlayer
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostViewerModal(
  post: Post,
  currentUser: User,
  onDismiss: () -> Unit,
  onUserClick: (String) -> Unit,
  onShowPlus: () -> Unit,
  onPostDeleted: ((String) -> Unit)? = null
) {
  var currentPost by remember { mutableStateOf(post) }
  var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
  var newCommentText by remember { mutableStateOf("") }
  var isFollowing by remember { mutableStateOf(false) }
  var showMenu by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  var showShareSheet by remember { mutableStateOf(false) }
  var showMusicPicker by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isOwnPost = currentPost.userId == currentUser.uid
  val isAdmin = remember(currentUser.uid) { FirebaseService.isAdmin(currentUser.uid) }
  val canAddMusic = isOwnPost && (currentUser.isPlus || isAdmin) && currentPost.musicAudioUrl.isBlank()

  // Listen to comments real-time
  LaunchedEffect(currentPost.postId) {
    FirebaseService.recordPostView(currentPost.postId, currentUser.uid)
    FirebaseService.getCommentsFlow(currentPost.postId).collectLatest {
      comments = it
    }
  }

  // Check following state
  LaunchedEffect(currentPost.userId) {
    if (!isOwnPost) {
      isFollowing = FirebaseService.isFollowing(currentUser.uid, currentPost.userId)
    }
  }

  val cacheVersion by com.example.data.cache.UserProfileCache.version.collectAsState()
  val displayUsername = com.example.data.cache.UserProfileCache.getUsername(currentPost.userId, currentPost.username.ifBlank { "user" })
  val displayAvatar = com.example.data.cache.UserProfileCache.getAvatarUrl(currentPost.userId, currentPost.userProfileImageUrl)
  val activeAudioUrl by GlobalAudioPlayer.activeTrackUrl.collectAsState()
  val isAudioPlaying by GlobalAudioPlayer.isPlaying.collectAsState()
  val isPostAudioPlaying = currentPost.musicAudioUrl.isNotBlank() && activeAudioUrl == currentPost.musicAudioUrl && isAudioPlaying

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = DarkBackground,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()
        .imePadding()
    ) {
      // Top Navigation Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onDismiss) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
          }
          Text(
            text = "Post",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
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
            if (isOwnPost) {
              if (canAddMusic) {
                DropdownMenuItem(
                  text = { Text("Add Music", color = PureWhite) },
                  onClick = {
                    showMenu = false
                    showMusicPicker = true
                  },
                  leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = PureWhite) }
                )
              }
              DropdownMenuItem(
                text = { Text("Edit Caption", color = PureWhite) },
                onClick = {
                  showMenu = false
                  showEditDialog = true
                },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = PureWhite) }
              )
              DropdownMenuItem(
                text = { Text("Delete Post", color = DangerRed, fontWeight = FontWeight.SemiBold) },
                onClick = {
                  showMenu = false
                  showDeleteDialog = true
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) }
              )
            } else {
              DropdownMenuItem(
                text = { Text("Report Post", color = PureWhite) },
                onClick = {
                  showMenu = false
                  showReportDialog = true
                },
                leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = TextSecondary) }
              )
            }
          }
        }
        }
      }

      HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

      // Post Content + Comments List
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        // Author Header
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f)
                .clickable {
                  onDismiss()
                  onUserClick(currentPost.userId)
                }
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(DarkSurfaceElevated)
                  .border(1.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                if (displayAvatar.isNotBlank()) {
                  AsyncImage(
                    model = displayAvatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                } else {
                  Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
                }
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column {
                Text(
                  text = displayUsername,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = PureWhite
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = formatRelativeTime(currentPost.createdAt),
                    fontSize = 11.sp,
                    color = TextMuted
                  )
                  if (currentPost.musicTitle.isNotBlank()) {
                    Text(
                      text = "•",
                      fontSize = 11.sp,
                      color = TextMuted
                    )
                    Icon(
                      imageVector = Icons.Default.MusicNote,
                      contentDescription = null,
                      tint = PureWhite.copy(alpha = 0.85f),
                      modifier = Modifier.size(11.dp)
                    )
                    Text(
                      text = currentPost.musicTitle,
                      fontSize = 11.sp,
                      color = PureWhite.copy(alpha = 0.85f),
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                      modifier = Modifier.widthIn(max = 140.dp)
                    )
                  }
                }
              }
            }

            if (!isOwnPost) {
              Button(
                onClick = {
                  scope.launch {
                    if (isFollowing) {
                      FirebaseService.unfollowUser(currentUser.uid, currentPost.userId)
                      isFollowing = false
                    } else {
                      val res = FirebaseService.followUser(currentUser.uid, currentPost.userId)
                      if (res.isSuccess) {
                        isFollowing = true
                      } else {
                        val msg = res.exceptionOrNull()?.message ?: "Failed to follow"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        if (msg.contains("limit")) onShowPlus()
                      }
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isFollowing) GlassSurface else PureWhite,
                  contentColor = if (isFollowing) PureWhite else PureBlack
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier
                  .height(30.dp)
                  .border(1.dp, if (isFollowing) GlassBorder else Color.Transparent, RoundedCornerShape(20.dp))
              ) {
                Text(
                  text = if (isFollowing) "Following" else "Follow",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        // Image or Text Post
        item {
          if (!currentPost.isTextOnly && currentPost.imageUrl.isNotBlank()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(DarkSurface)
            ) {
              AsyncImage(
                model = currentPost.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )

              // Instagram-style Mute/Unmute Audio Button on bottom-right of post image
              if (currentPost.musicAudioUrl.isNotBlank()) {
                Box(
                  modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PureBlack.copy(alpha = 0.65f))
                    .border(1.dp, GlassBorder, CircleShape)
                    .clickable {
                      GlobalAudioPlayer.togglePlayPause(currentPost.musicAudioUrl)
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (isPostAudioPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = if (isPostAudioPlaying) "Mute music" else "Unmute music",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          } else {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
            ) {
              Text(
                text = currentPost.caption,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = PureWhite
              )

              // Mute/Unmute button for Text post with music on bottom-right
              if (currentPost.musicAudioUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End
                ) {
                  Box(
                    modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(PureBlack.copy(alpha = 0.65f))
                      .border(1.dp, GlassBorder, CircleShape)
                      .clickable {
                        GlobalAudioPlayer.togglePlayPause(currentPost.musicAudioUrl)
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = if (isPostAudioPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                      contentDescription = if (isPostAudioPlaying) "Mute music" else "Unmute music",
                      tint = PureWhite,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }
        }

        // Action Bar (Views, Comments, Share - NO LIKE)
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              // Views Badge
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(GlassSurface)
                  .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Outlined.Visibility, contentDescription = "Views", tint = TextSecondary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("${currentPost.viewCount} views", fontSize = 12.sp, color = TextSecondary)
              }

              // Comments indicator
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(GlassSurface)
                  .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("${comments.size} comments", fontSize = 12.sp, color = PureWhite)
              }
            }

            IconButton(onClick = { showShareSheet = true }, modifier = Modifier.size(34.dp)) {
              Icon(Icons.Outlined.Send, contentDescription = "Share", tint = PureWhite, modifier = Modifier.size(19.dp))
            }
          }
        }

        // Caption for image post
        if (!currentPost.isTextOnly && currentPost.caption.isNotBlank()) {
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
              Text(text = displayUsername, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PureWhite)
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = currentPost.caption, fontSize = 13.sp, color = PureWhite.copy(alpha = 0.88f))
            }
          }
        }

        item {
          HorizontalDivider(color = GlassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
          Text(
            text = "Comments",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = PureWhite,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
          )
        }

        // Comments items
        if (comments.isEmpty()) {
          item {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
              Text("No comments yet. Be the first to comment!", fontSize = 13.sp, color = TextSecondary)
            }
          }
        } else {
          items(comments, key = { it.commentId }) { comment ->
            val canDelete = comment.userId == currentUser.uid || isOwnPost || FirebaseService.isAdmin(currentUser.uid)
            val commentUser = com.example.data.cache.UserProfileCache.getUsername(comment.userId, comment.username.ifBlank { "user" })
            val commentAvatar = com.example.data.cache.UserProfileCache.getAvatarUrl(comment.userId, comment.userProfileImageUrl)

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              verticalAlignment = Alignment.Top
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(DarkSurfaceElevated)
                  .border(0.5.dp, GlassBorder, CircleShape)
                  .clickable {
                    onDismiss()
                    onUserClick(comment.userId)
                  },
                contentAlignment = Alignment.Center
              ) {
                if (commentAvatar.isNotBlank()) {
                  AsyncImage(
                    model = commentAvatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                } else {
                  Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = commentUser,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PureWhite,
                    modifier = Modifier.clickable {
                      onDismiss()
                      onUserClick(comment.userId)
                    }
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = formatRelativeTime(comment.createdAt), fontSize = 11.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = comment.text, fontSize = 13.sp, color = PureWhite.copy(alpha = 0.9f))
              }

              if (canDelete) {
                IconButton(
                  onClick = {
                    scope.launch {
                      FirebaseService.deleteComment(currentPost.postId, comment.commentId, currentUser.uid)
                    }
                  },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(15.dp))
                }
              }
            }
          }
        }
      }

      // Add Comment Bottom Input Bar
      HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurfaceElevated)
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = newCommentText,
          onValueChange = { newCommentText = it },
          placeholder = { Text("Add a comment...", fontSize = 13.sp, color = TextMuted) },
          shape = RoundedCornerShape(20.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedContainerColor = GlassSurface,
            unfocusedContainerColor = GlassSurface,
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = GlassBorder
          ),
          modifier = Modifier.weight(1f),
          singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = {
            if (newCommentText.isNotBlank()) {
              val txt = newCommentText.trim()
              newCommentText = ""
              scope.launch {
                FirebaseService.addComment(
                  postId = currentPost.postId,
                  userId = currentUser.uid,
                  username = currentUser.username,
                  avatar = currentUser.profileImageUrl,
                  text = txt
                )
              }
            }
          },
          enabled = newCommentText.isNotBlank()
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = if (newCommentText.isNotBlank()) PureWhite else TextMuted
          )
        }
      }
    }
  }

  // Edit Caption Dialog
  if (showEditDialog) {
    var editedCaption by remember { mutableStateOf(currentPost.caption) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
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
              val res = FirebaseService.updatePostCaption(currentPost.postId, editedCaption.trim(), currentUser.uid)
              if (res.isSuccess) {
                currentPost = currentPost.copy(caption = editedCaption.trim())
                showEditDialog = false
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
        TextButton(onClick = { showEditDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Delete Post Confirmation Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Delete Post?", color = PureWhite, fontWeight = FontWeight.Bold) },
      text = { Text("This will permanently remove this post and all its comments.", color = TextSecondary, fontSize = 13.sp) },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              FirebaseService.deletePost(currentPost.postId, currentUser.uid)
              showDeleteDialog = false
              onDismiss()
              onPostDeleted?.invoke(currentPost.postId)
              Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Report Post Dialog
  if (showReportDialog) {
    var reportReason by remember { mutableStateOf("Spam or inappropriate content") }
    var reportDetails by remember { mutableStateOf("") }
    var isReporting by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showReportDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Report Post", color = PureWhite, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Select reason:", fontSize = 13.sp, color = TextSecondary)
          Spacer(modifier = Modifier.height(8.dp))
          listOf("Spam", "Harassment or Hate", "Inappropriate content", "Scam or Fraud").forEach { r ->
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
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = reportDetails,
            onValueChange = { reportDetails = it },
            placeholder = { Text("Additional details...", fontSize = 12.sp, color = TextMuted) },
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
                targetId = currentPost.postId,
                reason = reportReason,
                details = reportDetails
              )
              isReporting = false
              showReportDialog = false
              Toast.makeText(context, "Report submitted. Thank you.", Toast.LENGTH_SHORT).show()
            }
          },
          enabled = !isReporting,
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Report")
        }
      },
      dismissButton = {
        TextButton(onClick = { showReportDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // In-App Share Sheet
  if (showShareSheet) {
    var conversations by remember { mutableStateOf<List<com.example.data.models.Conversation>>(emptyList()) }
    LaunchedEffect(currentUser.uid) {
      FirebaseService.getConversationsFlow(currentUser.uid).collectLatest {
        conversations = it
      }
    }

    ShareSheet(
      post = currentPost,
      conversations = conversations,
      currentUserId = currentUser.uid,
      onDismiss = { showShareSheet = false },
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
            text = "Shared a post by @${currentPost.username}",
            sharedPostId = currentPost.postId,
            sharedPostCaption = currentPost.caption,
            sharedPostImage = currentPost.imageUrl
          )
          Toast.makeText(context, "Post shared with @$targetUsername", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  // Music Picker Sheet to add music to this post
  if (showMusicPicker) {
    MusicSearchSheet(
      onDismiss = { showMusicPicker = false },
      onTrackSelected = { track ->
        showMusicPicker = false
        scope.launch {
          val res = FirebaseService.updatePostMusic(
            postId = currentPost.postId,
            musicAudioUrl = track.previewUrl,
            musicTitle = track.trackName,
            musicArtist = track.artistName,
            musicCoverUrl = track.artworkUrl,
            userId = currentUser.uid
          )
          if (res.isSuccess) {
            currentPost = currentPost.copy(
              musicAudioUrl = track.previewUrl,
              musicTitle = track.trackName,
              musicArtist = track.artistName,
              musicCoverUrl = track.artworkUrl
            )
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

