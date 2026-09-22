package com.example.ui.screens.admin

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
import androidx.compose.material.icons.filled.*
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
import com.example.data.models.Post
import com.example.data.models.Strug
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminContentTab(
  adminUid: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var contentMode by remember { mutableIntStateOf(0) } // 0: Posts, 1: Strugs
  var postFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Images, 2: Text

  var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var strugs by remember { mutableStateOf<List<Strug>>(emptyList()) }

  // For Deletion Dialogs
  var postToDelete by remember { mutableStateOf<Post?>(null) }
  var strugToDelete by remember { mutableStateOf<Strug?>(null) }
  var deletionReason by remember { mutableStateOf("Violation of community safety standards.") }

  LaunchedEffect(Unit) {
    FirebaseService.getAllPostsFlow().collect { list ->
      posts = list
    }
  }

  LaunchedEffect(Unit) {
    FirebaseService.getAllStrugsFlow().collect { list ->
      strugs = list
    }
  }

  val filteredPosts = remember(posts, postFilter) {
    when (postFilter) {
      1 -> posts.filter { !it.isTextOnly && it.imageUrl.isNotBlank() }
      2 -> posts.filter { it.isTextOnly || it.imageUrl.isBlank() }
      else -> posts
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Mode Switcher (Posts vs Strugs)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        .padding(4.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(if (contentMode == 0) PureWhite else Color.Transparent)
          .clickable { contentMode = 0 }
          .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Posts (${posts.size})",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = if (contentMode == 0) PureBlack else TextSecondary
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(if (contentMode == 1) PureWhite else Color.Transparent)
          .clickable { contentMode = 1 }
          .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Strugs (${strugs.size})",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = if (contentMode == 1) PureBlack else TextSecondary
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (contentMode == 0) {
      // Posts Filter Sub-row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("All Posts", "Image Only", "Text Only").forEachIndexed { index, label ->
          val selected = postFilter == index
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (selected) GlassSurface else Color.Transparent)
              .border(1.dp, if (selected) PureWhite else GlassBorder, RoundedCornerShape(8.dp))
              .clickable { postFilter = index }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = label,
              fontSize = 12.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
              color = if (selected) PureWhite else TextSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (filteredPosts.isEmpty()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text("No posts available in this category.", color = TextMuted, fontSize = 14.sp)
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(filteredPosts, key = { it.postId }) { post ->
            AdminPostItemCard(
              post = post,
              onDelete = {
                deletionReason = "Violation of community standards."
                postToDelete = post
              }
            )
          }
        }
      }
    } else {
      // Strugs View
      if (strugs.isEmpty()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text("No active Strugs at this moment.", color = TextMuted, fontSize = 14.sp)
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(strugs, key = { it.strugId }) { strug ->
            AdminStrugItemCard(
              strug = strug,
              onDelete = {
                deletionReason = "Inappropriate story media or caption."
                strugToDelete = strug
              }
            )
          }
        }
      }
    }
  }

  // Delete Post Dialog
  val postToDel = postToDelete
  if (postToDel != null) {
    AlertDialog(
      onDismissRequest = { postToDelete = null },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text("Delete Post") },
      text = {
        Column {
          Text("Remove post by @${postToDel.username}? This action is irreversible.", fontSize = 13.sp)
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = deletionReason,
            onValueChange = { deletionReason = it },
            label = { Text("Reason for Removal", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              val res = FirebaseService.adminDeletePost(postToDel.postId, postToDel.userId, deletionReason, adminUid)
              if (res.isSuccess) {
                Toast.makeText(context, "Post deleted and removed from feed.", Toast.LENGTH_SHORT).show()
                postToDelete = null
              } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Delete Post", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { postToDelete = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Delete Strug Dialog
  val strugToDel = strugToDelete
  if (strugToDel != null) {
    AlertDialog(
      onDismissRequest = { strugToDelete = null },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text("Delete Strug") },
      text = {
        Column {
          Text("Remove temporary Strug from @${strugToDel.username}?", fontSize = 13.sp)
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = deletionReason,
            onValueChange = { deletionReason = it },
            label = { Text("Reason for Removal", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              val res = FirebaseService.adminDeleteStrug(strugToDel.strugId, deletionReason, adminUid)
              if (res.isSuccess) {
                Toast.makeText(context, "Strug deleted.", Toast.LENGTH_SHORT).show()
                strugToDelete = null
              } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Delete Strug", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { strugToDelete = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun AdminPostItemCard(
  post: Post,
  onDelete: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    // Author Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = post.userProfileImageUrl.ifBlank { "https://placehold.co/100x100/111111/ffffff.png?text=${post.username.take(1).uppercase()}" },
        contentDescription = null,
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .border(1.dp, GlassBorder, CircleShape),
        contentScale = ContentScale.Crop
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "@${post.username}",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
        Text(
          text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(post.createdAt)),
          fontSize = 11.sp,
          color = TextMuted
        )
      }

      IconButton(onClick = onDelete) {
        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(20.dp))
      }
    }

    if (post.caption.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = post.caption,
        fontSize = 13.sp,
        color = PureWhite,
        lineHeight = 18.sp
      )
    }

    if (!post.isTextOnly && post.imageUrl.isNotBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      AsyncImage(
        model = post.imageUrl,
        contentDescription = null,
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .clip(RoundedCornerShape(8.dp))
          .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Analytics row (NO LIKES EVER)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "${post.viewCount} views • ${post.commentCount} comments",
        fontSize = 11.sp,
        color = TextSecondary
      )
      Text(
        text = "ID: ${post.postId.take(8)}...",
        fontSize = 10.sp,
        color = TextMuted
      )
    }
  }
}

@Composable
private fun AdminStrugItemCard(
  strug: Strug,
  onDelete: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (strug.imageUrl.isNotBlank()) {
      AsyncImage(
        model = strug.imageUrl,
        contentDescription = null,
        modifier = Modifier
          .size(60.dp)
          .clip(RoundedCornerShape(8.dp))
          .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
      )
      Spacer(modifier = Modifier.width(12.dp))
    }

    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "@${strug.username}",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(GlassSurface)
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Text("24H STRUG", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        }
      }

      if (strug.caption.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = strug.caption,
          fontSize = 12.sp,
          color = TextSecondary,
          maxLines = 2
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
      val hoursLeft = ((strug.createdAt + 24 * 60 * 60 * 1000 - System.currentTimeMillis()) / (60 * 60 * 1000)).coerceAtLeast(0)
      Text(
        text = "Expires in ~$hoursLeft hours",
        fontSize = 11.sp,
        color = WarningAmber
      )
    }

    IconButton(onClick = onDelete) {
      Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(20.dp))
    }
  }
}
