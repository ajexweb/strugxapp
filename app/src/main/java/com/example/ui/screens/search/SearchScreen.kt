package com.example.ui.screens.search

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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
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
import com.example.ui.components.PostViewerModal
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
  currentUserId: String,
  currentUser: User? = null,
  onUserClick: (String) -> Unit,
  onMessageClick: (recipientId: String, recipientUsername: String, recipientAvatar: String) -> Unit,
  onShowPlus: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
  var isSearching by remember { mutableStateOf(false) }
  var followingMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Accounts, 1 = Popular Posts, 2 = Explore Grid
  var popularPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var explorePosts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var viewingPost by remember { mutableStateOf<Post?>(null) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Load Popular & Explore posts
  LaunchedEffect(Unit) {
    scope.launch {
      popularPosts = FirebaseService.getPopularPosts(limit = 30)
    }
    FirebaseService.getFeedPostsFlow().collectLatest { allPosts ->
      explorePosts = allPosts.filter { !it.isTextOnly && it.imageUrl.isNotBlank() }
    }
  }

  // Debounced search
  LaunchedEffect(searchQuery) {
    if (searchQuery.isNotBlank()) {
      isSearching = true
      delay(250) // fast debounce
      val results = FirebaseService.searchUsers(searchQuery)
      searchResults = results.filter { it.uid != currentUserId }

      val fMap = mutableMapOf<String, Boolean>()
      for (u in searchResults) {
        fMap[u.uid] = FirebaseService.isFollowing(currentUserId, u.uid)
      }
      followingMap = fMap
      isSearching = false
    } else {
      searchResults = emptyList()
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

    // Search header
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
      Text(
        text = "Explore & Search",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = PureWhite
      )
      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by username or name...", fontSize = 14.sp, color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
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
    }

    // Discovery Tabs (When not actively searching)
    if (searchQuery.isBlank()) {
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
          text = { Text("Accounts", fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Whatshot, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Popular", fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
            }
          }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("Explore Grid", fontSize = 13.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
        )
      }
    } else {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(0.5.dp)
          .background(GlassBorder)
      )
    }

    // Content Area
    if (isSearching) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PureWhite)
      }
    } else if (searchQuery.isNotBlank()) {
      // Active Search Results
      if (searchResults.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
          Text("No users found matching '$searchQuery'", fontSize = 14.sp, color = TextSecondary)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(searchResults, key = { it.uid }) { user ->
            UserSearchItem(
              user = user,
              isFollowing = followingMap[user.uid] == true,
              onUserClick = { onUserClick(user.uid) },
              onMessageClick = { onMessageClick(user.uid, user.username, user.profileImageUrl) },
              onFollowToggle = {
                scope.launch {
                  val currentFollow = followingMap[user.uid] == true
                  if (currentFollow) {
                    FirebaseService.unfollowUser(currentUserId, user.uid)
                    followingMap = followingMap + (user.uid to false)
                  } else {
                    val result = FirebaseService.followUser(currentUserId, user.uid)
                    if (result.isSuccess) {
                      followingMap = followingMap + (user.uid to true)
                    } else {
                      val err = result.exceptionOrNull()?.message ?: "Failed to follow"
                      Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                      if (err.contains("limit")) {
                        onShowPlus()
                      }
                    }
                  }
                }
              }
            )
          }
        }
      }
    } else {
      // Tab 0: Accounts (Prompt + Suggestions)
      if (selectedTab == 0) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Search Accounts", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Search by username, display name, or handle above.", fontSize = 13.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
          }
        }
      }
      // Tab 1: Popular Posts (Top viewed and engaged posts)
      else if (selectedTab == 1) {
        if (popularPosts.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No trending posts yet. Views and interactions will surface popular posts here.", color = TextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(popularPosts, key = { it.postId }) { post ->
              PopularPostRow(
                post = post,
                onClick = { viewingPost = post }
              )
            }
          }
        }
      }
      // Tab 2: Explore Grid
      else {
        if (explorePosts.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No explore photos yet.", color = TextSecondary, fontSize = 13.sp)
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            items(explorePosts, key = { it.postId }) { post ->
              Box(
                modifier = Modifier
                  .aspectRatio(1f)
                  .background(DarkSurfaceElevated)
                  .clickable { viewingPost = post }
              ) {
                AsyncImage(
                  model = post.imageUrl,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
                // View count badge overlay
                if (post.viewCount > 0) {
                  Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                      .align(Alignment.BottomStart)
                      .padding(4.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                      Icon(Icons.Outlined.Visibility, contentDescription = null, tint = PureWhite, modifier = Modifier.size(11.dp))
                      Spacer(modifier = Modifier.width(3.dp))
                      Text("${post.viewCount}", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Open Post Viewer Modal when post clicked
  viewingPost?.let { targetPost ->
    val activeUser = currentUser ?: User(uid = currentUserId)
    PostViewerModal(
      post = targetPost,
      currentUser = activeUser,
      onDismiss = { viewingPost = null },
      onUserClick = { uid ->
        viewingPost = null
        onUserClick(uid)
      },
      onShowPlus = onShowPlus,
      onPostDeleted = {
        viewingPost = null
        popularPosts = popularPosts.filter { p -> p.postId != targetPost.postId }
        explorePosts = explorePosts.filter { p -> p.postId != targetPost.postId }
      }
    )
  }
}

@Composable
private fun PopularPostRow(
  post: Post,
  onClick: () -> Unit
) {
  Surface(
    color = GlassSurface,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (post.imageUrl.isNotBlank()) {
        AsyncImage(
          model = post.imageUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(12.dp))
        )
      } else {
        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "TEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "@${post.username}",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = PureWhite
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = PureWhite,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "POPULAR",
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = PureBlack,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = post.caption.ifBlank { "View photo post" },
          fontSize = 12.sp,
          color = TextSecondary,
          maxLines = 2
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Visibility, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("${post.viewCount} views", fontSize = 11.sp, color = TextMuted)

          Spacer(modifier = Modifier.width(12.dp))
          Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("${post.commentCount} comments", fontSize = 11.sp, color = TextMuted)
        }
      }
    }
  }
}

@Composable
private fun UserSearchItem(
  user: User,
  isFollowing: Boolean,
  onUserClick: () -> Unit,
  onMessageClick: () -> Unit,
  onFollowToggle: () -> Unit
) {
  Surface(
    color = GlassSurface,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onUserClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(DarkSurfaceElevated)
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
            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = user.displayName.ifBlank { user.username },
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = PureWhite
            )
            if (user.isPlus) {
              Spacer(modifier = Modifier.width(4.dp))
              Surface(
                color = PureWhite,
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = "PLUS",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Black,
                  color = PureBlack,
                  modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
              }
            }
          }
          Text(
            text = "@${user.username}",
            fontSize = 12.sp,
            color = TextSecondary
          )
          if (user.bio.isNotBlank()) {
            Text(
              text = user.bio,
              fontSize = 11.sp,
              color = TextMuted,
              maxLines = 1
            )
          }
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onMessageClick,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = "Message",
            tint = PureWhite,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Button(
          onClick = onFollowToggle,
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isFollowing) GlassSurface else PureWhite,
            contentColor = if (isFollowing) PureWhite else PureBlack
          ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
          modifier = Modifier
            .height(34.dp)
            .border(
              width = if (isFollowing) 1.dp else 0.dp,
              color = if (isFollowing) GlassBorder else Color.Transparent,
              shape = RoundedCornerShape(18.dp)
            )
        ) {
          Text(
            text = if (isFollowing) "Following" else "Follow",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}
