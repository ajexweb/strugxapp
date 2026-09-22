package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.models.Post
import com.example.data.music.GlobalAudioPlayer
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostCard(
  post: Post,
  currentUserId: String,
  isFollowing: Boolean,
  onFollowToggle: () -> Unit,
  onUserClick: (String) -> Unit,
  onCommentClick: (Post) -> Unit,
  onShareClick: (Post) -> Unit,
  onEditClick: ((Post) -> Unit)? = null,
  onDeleteClick: ((Post) -> Unit)? = null,
  onReportClick: ((Post) -> Unit)? = null,
  canAddMusic: Boolean = false,
  onAddMusicClick: ((Post) -> Unit)? = null
) {
  val isOwnPost = post.userId == currentUserId
  var showMenu by remember { mutableStateOf(false) }
  val cacheVersion by com.example.data.cache.UserProfileCache.version.collectAsState()
  val displayUsername = com.example.data.cache.UserProfileCache.getUsername(post.userId, post.username.ifBlank { "user" })
  val displayAvatar = com.example.data.cache.UserProfileCache.getAvatarUrl(post.userId, post.userProfileImageUrl)
  val activeAudioUrl by GlobalAudioPlayer.activeTrackUrl.collectAsState()
  val isAudioPlaying by GlobalAudioPlayer.isPlaying.collectAsState()
  val isPostAudioPlaying = post.musicAudioUrl.isNotBlank() && activeAudioUrl == post.musicAudioUrl && isAudioPlaying

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(DarkBackground)
      .padding(vertical = 4.dp)
  ) {
    // 1. Author Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .weight(1f)
          .clickable { onUserClick(post.userId) }
      ) {
        // Avatar
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (displayAvatar.isNotBlank()) {
            AsyncImage(
              model = displayAvatar,
              contentDescription = "$displayUsername's avatar",
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
              text = formatRelativeTime(post.createdAt),
              fontSize = 11.sp,
              color = TextMuted
            )
            if (post.musicTitle.isNotBlank()) {
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
                text = post.musicTitle,
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

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Follow / Following Button (if not own post)
        if (!isOwnPost) {
          Button(
            onClick = onFollowToggle,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isFollowing) GlassSurface else PureWhite,
              contentColor = if (isFollowing) PureWhite else PureBlack
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            modifier = Modifier
              .height(30.dp)
              .border(
                width = 1.dp,
                color = if (isFollowing) GlassBorder else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
              )
          ) {
            Text(
              text = if (isFollowing) "Following" else "Follow",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
        }

        // Overflow 3-dot Menu
        Box {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Post options",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        ) {
          if (isOwnPost) {
            if (canAddMusic && post.musicAudioUrl.isBlank() && onAddMusicClick != null) {
              DropdownMenuItem(
                text = { Text("Add Music", color = PureWhite) },
                onClick = {
                  showMenu = false
                  onAddMusicClick(post)
                },
                leadingIcon = {
                  Icon(Icons.Default.MusicNote, contentDescription = null, tint = PureWhite)
                }
              )
            }
            if (onEditClick != null) {
              DropdownMenuItem(
                text = { Text("Edit Caption", color = PureWhite) },
                onClick = {
                  showMenu = false
                  onEditClick(post)
                },
                leadingIcon = {
                  Icon(Icons.Outlined.Edit, contentDescription = null, tint = PureWhite)
                }
              )
            }
            if (onDeleteClick != null) {
              DropdownMenuItem(
                text = { Text("Delete Post", color = DangerRed, fontWeight = FontWeight.SemiBold) },
                onClick = {
                  showMenu = false
                  onDeleteClick(post)
                },
                leadingIcon = {
                  Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                }
              )
            }
          } else if (onReportClick != null) {
            DropdownMenuItem(
              text = { Text("Report Post", color = PureWhite) },
              onClick = {
                showMenu = false
                onReportClick(post)
              },
              leadingIcon = {
                Icon(Icons.Outlined.Flag, contentDescription = null, tint = TextSecondary)
              }
            )
          }
        }
      }
      }
    }

    // 2. Post Content: Image Post OR Text Post (clean presentation)
    if (!post.isTextOnly && post.imageUrl.isNotBlank()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .background(DarkSurface)
      ) {
        AsyncImage(
          model = post.imageUrl,
          contentDescription = "Post image by ${post.username}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        // Instagram-style Mute/Unmute Audio Button on bottom-right of post image
        if (post.musicAudioUrl.isNotBlank()) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(12.dp)
              .size(32.dp)
              .clip(CircleShape)
              .background(PureBlack.copy(alpha = 0.65f))
              .border(1.dp, GlassBorder, CircleShape)
              .clickable {
                GlobalAudioPlayer.togglePlayPause(post.musicAudioUrl)
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
      // Text Post: Modern stylized dark glass card
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(GlassSurface)
          .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
          .padding(20.dp)
      ) {
        MentionText(
          text = post.caption,
          fontSize = 16.sp,
          lineHeight = 23.sp,
          color = PureWhite,
          onUserClick = onUserClick
        )

        // Mute/Unmute button for Text post with music on bottom-right
        if (post.musicAudioUrl.isNotBlank()) {
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
                  GlobalAudioPlayer.togglePlayPause(post.musicAudioUrl)
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

    // 3. Action Bar (Views, Comments, Share - NO LIKE SYSTEM!)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: Views Indicator & Comments Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Views Badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Visibility,
            contentDescription = "Views",
            tint = TextSecondary,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "${post.viewCount} views",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
          )
        }

        // Comments Button
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCommentClick(post) }
            .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = "Comments",
            tint = PureWhite,
            modifier = Modifier.size(19.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (post.commentCount > 0) "${post.commentCount}" else "Comment",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PureWhite
          )
        }
      }

      // Right: In-App Share Button
      IconButton(
        onClick = { onShareClick(post) },
        modifier = Modifier.size(34.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.Send,
          contentDescription = "Share to messages",
          tint = PureWhite,
          modifier = Modifier.size(19.dp)
        )
      }
    }

    // 4. Caption for Image Posts (if caption exists)
    if (!post.isTextOnly && post.caption.isNotBlank()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 2.dp)
      ) {
        Text(
          text = displayUsername,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = PureWhite,
          modifier = Modifier.clickable { onUserClick(post.userId) }
        )
        Spacer(modifier = Modifier.width(6.dp))
        MentionText(
          text = post.caption,
          fontSize = 13.sp,
          color = PureWhite.copy(alpha = 0.88f),
          onUserClick = onUserClick,
          modifier = Modifier.weight(1f)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    // Divider between posts
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(0.5.dp)
        .background(GlassBorder)
    )
  }
}

