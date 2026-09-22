package com.example.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import com.example.data.models.User
import com.example.data.storage.ImageOptimizer
import com.example.data.storage.SupabaseStorageService
import com.example.ui.components.FollowListSheet
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  currentUser: User,
  onNavigateToPlus: () -> Unit,
  onNavigateToLegal: (title: String, type: String) -> Unit,
  onLogout: () -> Unit,
  onPostClick: (Post) -> Unit,
  onNavigateToAdmin: (() -> Unit)? = null
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Images, 1 = Text
  var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
  var showFullMenuPage by remember { mutableStateOf(false) }
  var showEditProfileModal by remember { mutableStateOf(false) }
  var showReportProblemModal by remember { mutableStateOf(false) }
  var showFollowSheet by remember { mutableStateOf<Boolean?>(null) }
  var showDeleteAccountDialog by remember { mutableStateOf(false) }
  var isDeletingAccount by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val isAdmin = remember(currentUser.uid) { FirebaseService.isAdmin(currentUser.uid) }

  LaunchedEffect(currentUser.uid) {
    FirebaseService.getUserPostsFlow(currentUser.uid).collectLatest { posts ->
      userPosts = posts
    }
  }

  val imagePosts = remember(userPosts) { userPosts.filter { !it.isTextOnly && it.imageUrl.isNotBlank() } }
  val textPosts = remember(userPosts) { userPosts.filter { it.isTextOnly } }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground)
    ) {
      // Top status bar black strip
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsTopHeight(WindowInsets.statusBars)
          .background(Color.Black)
      )

      // Top Header: Username on left, ☰ Hamburger Menu on right
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = currentUser.username.ifBlank { "profile" },
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
          if (currentUser.isPlus) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = GlassSurface,
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
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

        // Three-line ☰ Menu Button
        IconButton(onClick = { showFullMenuPage = true }) {
          Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu Options",
            tint = PureWhite,
            modifier = Modifier.size(26.dp)
          )
        }
      }

    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(0.5.dp)
        .background(GlassBorder)
    )

    // User Profile Stats Header
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
        // Large Avatar
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(DarkSurfaceElevated)
            .border(1.5.dp, GlassBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (currentUser.profileImageUrl.isNotBlank()) {
            AsyncImage(
              model = currentUser.profileImageUrl,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
          }
        }

        // Stats: Posts, Followers, Following
        Row(
          horizontalArrangement = Arrangement.spacedBy(28.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          StatColumn(count = "${userPosts.size}", label = "Posts")
          StatColumn(
            count = "${currentUser.followerCount}",
            label = "Followers",
            onClick = { showFollowSheet = true }
          )
          StatColumn(
            count = "${currentUser.followingCount}",
            label = "Following",
            onClick = { showFollowSheet = false }
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Display Name & Bio
      Text(
        text = currentUser.displayName.ifBlank { currentUser.username },
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = PureWhite
      )
      if (currentUser.bio.isNotBlank()) {
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = currentUser.bio,
          fontSize = 13.sp,
          color = TextSecondary,
          lineHeight = 18.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Action Buttons: Edit Profile & Strugx Plus Banner/Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { showEditProfileModal = true },
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
          Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = onNavigateToPlus,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (currentUser.isPlus) GlassSurface else PureWhite,
            contentColor = if (currentUser.isPlus) PureWhite else PureBlack
          ),
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .border(
              width = if (currentUser.isPlus) 1.dp else 0.dp,
              color = if (currentUser.isPlus) GlassBorder else Color.Transparent,
              shape = RoundedCornerShape(12.dp)
            )
        ) {
          Text(if (currentUser.isPlus) "Plus Active" else "Get Plus", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Tabs: Images (0) | Text Posts (1)
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = DarkBackground,
      contentColor = PureWhite,
      divider = {
        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(GlassBorder)
        )
      }
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Photos (${imagePosts.size})", color = if (selectedTab == 0) PureWhite else TextSecondary, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
        icon = { Icon(Icons.Default.GridOn, contentDescription = null, tint = if (selectedTab == 0) PureWhite else TextSecondary, modifier = Modifier.size(18.dp)) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Text (${textPosts.size})", color = if (selectedTab == 1) PureWhite else TextSecondary, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
        icon = { Icon(Icons.Default.Notes, contentDescription = null, tint = if (selectedTab == 1) PureWhite else TextSecondary, modifier = Modifier.size(18.dp)) }
      )
    }

    // Tab Content
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
                Text(text = "${post.viewCount} views", fontSize = 11.sp, color = TextMuted)
              }
            }
          }
        }
      }
    }
  }

    // Full-Page Menu Screen (Overlays Profile smoothly with slide transition, zero app restarts)
    AnimatedVisibility(
      visible = showFullMenuPage,
      enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
      exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
      ProfileFullMenuPage(
        currentUser = currentUser,
        isAdmin = isAdmin,
        onBack = { showFullMenuPage = false },
        onNavigateToPlus = {
          showFullMenuPage = false
          onNavigateToPlus()
        },
        onEditProfile = {
          showFullMenuPage = false
          showEditProfileModal = true
        },
        onNavigateToLegal = { title, type ->
          showFullMenuPage = false
          onNavigateToLegal(title, type)
        },
        onReportProblem = {
          showFullMenuPage = false
          showReportProblemModal = true
        },
        onNavigateToAdmin = {
          showFullMenuPage = false
          onNavigateToAdmin?.invoke()
        },
        onLogout = {
          showFullMenuPage = false
          onLogout()
        },
        onDeleteAccount = {
          showFullMenuPage = false
          showDeleteAccountDialog = true
        }
      )
    }
  }

  // Delete Account Confirmation Dialog
  if (showDeleteAccountDialog) {
    AlertDialog(
      onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
      title = {
        Text("Delete Account?", color = PureWhite, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          "Are you sure you want to delete your Strugx account? Your profile, stories, and active data will be removed. This action cannot be undone.",
          color = TextSecondary,
          fontSize = 14.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            isDeletingAccount = true
            scope.launch {
              val res = FirebaseService.deleteAccount(currentUser.uid)
              isDeletingAccount = false
              showDeleteAccountDialog = false
              if (res.isSuccess) {
                Toast.makeText(context, "Account successfully deleted.", Toast.LENGTH_SHORT).show()
                onLogout()
              } else {
                Toast.makeText(
                  context,
                  res.exceptionOrNull()?.message ?: "Failed to delete account. Please re-authenticate and try again.",
                  Toast.LENGTH_LONG
                ).show()
              }
            }
          },
          enabled = !isDeletingAccount,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEF4444),
            contentColor = PureWhite
          ),
          shape = RoundedCornerShape(10.dp)
        ) {
          if (isDeletingAccount) {
            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text("Delete Account", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDeleteAccountDialog = false },
          enabled = !isDeletingAccount
        ) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = Color(0xFF161616),
      shape = RoundedCornerShape(16.dp)
    )
  }

  // Edit Profile Modal (Phase 1 Specification: Name, Username, Bio, Photo, uniqueness check)
  if (showEditProfileModal) {
    var editUsername by remember { mutableStateOf(currentUser.username) }
    var editDisplayName by remember { mutableStateOf(currentUser.displayName) }
    var editBio by remember { mutableStateOf(currentUser.bio) }
    var newAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var checkJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(editUsername) {
      val clean = editUsername.trim().lowercase(Locale.ROOT)
      if (clean == currentUser.username.lowercase(Locale.ROOT)) {
        isUsernameAvailable = true
        return@LaunchedEffect
      }
      if (clean.length < 3 || !clean.matches(Regex("^[a-z0-9._]{3,24}$"))) {
        isUsernameAvailable = false
        return@LaunchedEffect
      }
      checkJob?.cancel()
      checkJob = launch {
        delay(350)
        isUsernameAvailable = FirebaseService.checkUsernameAvailable(clean, currentUser.uid)
      }
    }

    val avatarPicker = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        newAvatarUri = uri
      }
    }

    AlertDialog(
      onDismissRequest = { if (!isSaving) showEditProfileModal = false },
      containerColor = DarkSurfaceElevated,
      shape = RoundedCornerShape(20.dp),
      title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = PureWhite) },
      text = {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          // Avatar picker
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(GlassSurface)
              .border(1.5.dp, GlassBorder, CircleShape)
              .clickable {
                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
            contentAlignment = Alignment.Center
          ) {
            if (newAvatarUri != null) {
              AsyncImage(model = newAvatarUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (currentUser.profileImageUrl.isNotBlank()) {
              AsyncImage(model = currentUser.profileImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
              Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PureWhite.copy(alpha = 0.8f))
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text("Tap to change photo", fontSize = 11.sp, color = TextSecondary)

          Spacer(modifier = Modifier.height(14.dp))

          // Username
          OutlinedTextField(
            value = editUsername,
            onValueChange = {
              editUsername = it.filter { ch -> !ch.isWhitespace() }.lowercase(Locale.ROOT)
              errorMsg = null
            },
            label = { Text("Username", color = TextSecondary) },
            prefix = { Text("@", color = TextSecondary, fontWeight = FontWeight.Bold) },
            singleLine = true,
            isError = isUsernameAvailable == false,
            supportingText = {
              if (isUsernameAvailable == false) {
                Text("Username is taken or invalid", color = DangerRed, fontSize = 11.sp)
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder,
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Display Name
          OutlinedTextField(
            value = editDisplayName,
            onValueChange = { editDisplayName = it },
            label = { Text("Profile Name", color = TextSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder,
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Bio
          OutlinedTextField(
            value = editBio,
            onValueChange = { if (it.length <= 160) editBio = it },
            label = { Text("Bio", color = TextSecondary) },
            maxLines = 3,
            supportingText = {
              Text("${editBio.length}/160", color = TextMuted, fontSize = 11.sp)
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder,
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )

          if (errorMsg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMsg!!, color = DangerRed, fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val cleanUser = editUsername.trim().lowercase(Locale.ROOT)
            val cleanName = editDisplayName.trim().ifBlank { cleanUser }

            if (cleanUser.length < 3) {
              errorMsg = "Username must be at least 3 characters."
              return@Button
            }
            if (isUsernameAvailable == false) {
              errorMsg = "Username is unavailable."
              return@Button
            }

            scope.launch {
              isSaving = true
              errorMsg = null
              var avatarUrl: String? = null

              if (newAvatarUri != null) {
                val compRes = ImageOptimizer.compressAndValidate(context, newAvatarUri!!, maxDimension = 512, quality = 85)
                if (compRes.isSuccess) {
                  val upRes = SupabaseStorageService.uploadImage(compRes.getOrThrow(), "profiles", currentUser.uid)
                  if (upRes.isSuccess) {
                    avatarUrl = upRes.getOrThrow()
                  }
                }
              }

              val res = FirebaseService.updateProfile(
                uid = currentUser.uid,
                displayName = cleanName,
                bio = editBio.trim(),
                profileImageUrl = avatarUrl,
                newUsername = if (cleanUser != currentUser.username) cleanUser else null
              )

              isSaving = false
              if (res.isSuccess) {
                showEditProfileModal = false
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
              } else {
                errorMsg = res.exceptionOrNull()?.message ?: "Failed to update profile."
              }
            }
          },
          enabled = !isSaving && isUsernameAvailable != false,
          colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
        ) {
          if (isSaving) {
            CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          } else {
            Text("Save Changes", fontWeight = FontWeight.Bold)
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileModal = false }, enabled = !isSaving) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Report a Problem Modal
  if (showReportProblemModal) {
    var problemText by remember { mutableStateOf("") }
    var isSubmittingProblem by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showReportProblemModal = false },
      containerColor = DarkSurfaceElevated,
      shape = RoundedCornerShape(20.dp),
      title = { Text("Report a Problem", fontWeight = FontWeight.Bold, color = PureWhite) },
      text = {
        Column {
          Text("Encountered a bug or issue? Let our team know:", fontSize = 13.sp, color = TextSecondary)
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = problemText,
            onValueChange = { problemText = it },
            placeholder = { Text("Describe what happened...", fontSize = 13.sp, color = TextMuted) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder,
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (problemText.isBlank()) return@Button
            isSubmittingProblem = true
            scope.launch {
              FirebaseService.submitReport(
                reporterId = currentUser.uid,
                reporterUsername = currentUser.username,
                targetType = "app_issue",
                targetId = "system",
                reason = "Problem Report",
                details = problemText
              )
              isSubmittingProblem = false
              showReportProblemModal = false
              Toast.makeText(context, "Problem reported. Thank you for your feedback!", Toast.LENGTH_LONG).show()
            }
          },
          enabled = problemText.isNotBlank() && !isSubmittingProblem,
          colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
        ) {
          Text("Submit", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showReportProblemModal = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Followers / Following Sheet
  showFollowSheet?.let { isFollowers ->
    FollowListSheet(
      userId = currentUser.uid,
      isFollowers = isFollowers,
      currentUserId = currentUser.uid,
      onDismiss = { showFollowSheet = null },
      onUserClick = { clickedUid ->
        showFollowSheet = null
        // Navigating to profile can be wired if parent supports or same-screen
      },
      onShowPlus = onNavigateToPlus
    )
  }
}

@Composable
private fun StatColumn(count: String, label: String, onClick: (() -> Unit)? = null) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
      .padding(horizontal = 4.dp, vertical = 2.dp)
  ) {
    Text(text = count, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PureWhite)
    Text(text = label, fontSize = 12.sp, color = TextSecondary)
  }
}

@Composable
private fun ProfileFullMenuPage(
  currentUser: User,
  isAdmin: Boolean,
  onBack: () -> Unit,
  onNavigateToPlus: () -> Unit,
  onEditProfile: () -> Unit,
  onNavigateToLegal: (title: String, type: String) -> Unit,
  onReportProblem: () -> Unit,
  onNavigateToAdmin: () -> Unit,
  onLogout: () -> Unit,
  onDeleteAccount: () -> Unit
) {
  BackHandler(onBack = onBack)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Status Bar Spacer
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    // Top Header Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Profile",
          tint = PureWhite
        )
      }
      Text(
        text = "Options & Settings",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = PureWhite
      )
    }

    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Account & Membership Section
      MenuSectionHeader("ACCOUNT & MEMBERSHIP")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FullMenuItem(
          icon = Icons.Default.Star,
          title = "STRUGX PLUS",
          subtitle = if (currentUser.isPlus) "Active Subscription (Expanded 100 follows/day)" else "Upgrade for verified badge & 100 follows/day",
          highlight = true,
          onClick = onNavigateToPlus
        )
        FullMenuItem(
          icon = Icons.Default.Edit,
          title = "Edit Profile",
          subtitle = "Update username, display name, bio, and avatar",
          onClick = onEditProfile
        )
      }

      // 2. Policies & Guidelines Section
      MenuSectionHeader("PLATFORM POLICIES & INFORMATION")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FullMenuItem(
          icon = Icons.Default.Info,
          title = "About Strugx",
          subtitle = "Version ${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE}), architecture & mission",
          onClick = { onNavigateToLegal("About Strugx", "about") }
        )
        FullMenuItem(
          icon = Icons.Default.PrivacyTip,
          title = "Privacy Policy",
          subtitle = "How your information is protected",
          onClick = { onNavigateToLegal("Privacy Policy", "privacy") }
        )
        FullMenuItem(
          icon = Icons.Default.Description,
          title = "Terms & Conditions",
          subtitle = "Platform rules and usage agreement",
          onClick = { onNavigateToLegal("Terms & Conditions", "terms") }
        )
        FullMenuItem(
          icon = Icons.Default.ReceiptLong,
          title = "Refund Policy",
          subtitle = "Cancellation and refund standards",
          onClick = { onNavigateToLegal("Refund Policy", "refund") }
        )
        FullMenuItem(
          icon = Icons.Default.Groups,
          title = "Community Guidelines",
          subtitle = "Standards for respectful interactions",
          onClick = { onNavigateToLegal("Community Guidelines", "guidelines") }
        )
        FullMenuItem(
          icon = Icons.Default.Code,
          title = "Developer Information",
          subtitle = "Ajay Saini, @ajayzsaini, @strugx.app",
          onClick = { onNavigateToLegal("Developer Information", "about") }
        )
      }

      // 3. Support & Feedback
      MenuSectionHeader("SUPPORT & FEEDBACK")
      FullMenuItem(
        icon = Icons.Default.HelpOutline,
        title = "Report a Problem",
        subtitle = "Submit bug reports or feature feedback",
        onClick = onReportProblem
      )

      // 4. Administration (Verified Admin Only)
      if (isAdmin) {
        MenuSectionHeader("ADMINISTRATION")
        FullMenuItem(
          icon = Icons.Default.Shield,
          title = "Admin Dashboard",
          subtitle = "User management, reports, audit logs & payments",
          highlight = true,
          onClick = onNavigateToAdmin
        )
      }

      // 5. Account Actions
      MenuSectionHeader("ACCOUNT ACTIONS")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FullMenuItem(
          icon = Icons.Default.Logout,
          title = "Log Out",
          subtitle = "Sign out from this device",
          onClick = onLogout
        )
        FullMenuItem(
          icon = Icons.Default.DeleteForever,
          title = "Delete Account",
          subtitle = "Permanently remove your profile and data",
          isDanger = true,
          onClick = onDeleteAccount
        )
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

@Composable
private fun FullMenuItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String? = null,
  highlight: Boolean = false,
  isDanger: Boolean = false,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = GlassSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, if (highlight) GlassBorderFocused else GlassBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(
            when {
              isDanger -> DangerRedLight
              highlight -> GlassSurfaceElevated
              else -> GlassSurfaceElevated
            }
          )
          .border(
            0.5.dp,
            if (isDanger) DangerRed.copy(alpha = 0.4f) else GlassBorder,
            RoundedCornerShape(10.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = when {
            isDanger -> DangerRed
            highlight -> PureWhite
            else -> PureWhite
          },
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = if (isDanger) DangerRed else PureWhite
        )
        if (!subtitle.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
          )
        }
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = TextMuted,
        modifier = Modifier.size(14.dp)
      )
    }
  }
}

@Composable
private fun MenuSectionHeader(title: String) {
  Text(
    text = title,
    fontSize = 11.sp,
    fontWeight = FontWeight.Black,
    letterSpacing = 1.sp,
    color = TextSecondary,
    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 6.dp)
  )
}
