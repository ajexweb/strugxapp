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
import androidx.compose.material.icons.filled.Delete
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
import com.example.data.models.Comment
import com.example.data.models.Post
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSheet(
  post: Post,
  comments: List<Comment>,
  currentUserId: String,
  isAdmin: Boolean,
  onDismiss: () -> Unit,
  onAddComment: (String) -> Unit,
  onDeleteComment: (String) -> Unit,
  onUserClick: (String) -> Unit = {}
) {
  var commentText by remember { mutableStateOf("") }
  var isSending by remember { mutableStateOf(false) }

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
      // Sheet Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Comments (${comments.size})",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )

        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = TextSecondary
          )
        }
      }

      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(0.5.dp)
          .background(GlassBorder)
      )

      // Comments List
      if (comments.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No comments yet.\nBe the first to share your thoughts!",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(comments, key = { it.commentId }) { comment ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Top
            ) {
              // Avatar
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(GlassSurface)
                  .border(1.dp, GlassBorder, CircleShape)
                  .clickable {
                    onDismiss()
                    onUserClick(comment.userId)
                  },
                contentAlignment = Alignment.Center
              ) {
                if (comment.userProfileImageUrl.isNotBlank()) {
                  AsyncImage(
                    model = comment.userProfileImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.clickable {
                    onDismiss()
                    onUserClick(comment.userId)
                  }
                ) {
                  Text(
                    text = comment.username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PureWhite
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = formatCommentTime(comment.createdAt),
                    fontSize = 11.sp,
                    color = TextMuted
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                MentionText(
                  text = comment.text,
                  fontSize = 13.sp,
                  color = PureWhite.copy(alpha = 0.9f),
                  lineHeight = 18.sp,
                  onUserClick = { uid ->
                    onDismiss()
                    onUserClick(uid)
                  }
                )
              }

              // Delete button if own comment or admin
              if (comment.userId == currentUserId || isAdmin) {
                IconButton(
                  onClick = { onDeleteComment(comment.commentId) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete comment",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
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

      // Comment Input Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = commentText,
          onValueChange = { commentText = it },
          placeholder = { Text("Add a comment...", fontSize = 13.sp, color = TextMuted) },
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.weight(1f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = GlassBorder,
            focusedContainerColor = GlassSurface,
            unfocusedContainerColor = GlassSurface
          ),
          maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = {
            if (commentText.isNotBlank() && !isSending) {
              isSending = true
              onAddComment(commentText)
              commentText = ""
              isSending = false
            }
          },
          enabled = commentText.isNotBlank() && !isSending,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (commentText.isNotBlank()) PureWhite else GlassSurface)
        ) {
          Icon(
            imageVector = Icons.Default.Send,
            contentDescription = "Send",
            tint = if (commentText.isNotBlank()) PureBlack else TextMuted,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

private fun formatCommentTime(time: Long): String {
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
