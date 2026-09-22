package com.example.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.BuildConfig
import com.example.data.firebase.FirebaseService
import com.example.data.models.AppUpdateConfig
import com.example.data.models.Post
import com.example.data.models.User
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.PostViewerModal
import com.example.ui.components.StrugxEmblem
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.BannedScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.ProfileSetupScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.feed.HomeScreen
import com.example.ui.screens.legal.LegalPolicyScreen
import com.example.ui.screens.messages.ChatScreen
import com.example.ui.screens.messages.MessagesScreen
import com.example.ui.screens.plus.PlusScreen
import com.example.ui.screens.profile.OtherProfileScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val iconFilled: androidx.compose.ui.graphics.vector.ImageVector, val iconOutlined: androidx.compose.ui.graphics.vector.ImageVector) {
  data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
  data object Search : Screen("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
  data object Messages : Screen("messages", "Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline)
  data object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline)
}

@Composable
fun StrugxApp() {
  val auth = FirebaseService.auth
  var showSplash by rememberSaveable { mutableStateOf(true) }
  var authUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
  var currentUserProfile by remember { mutableStateOf<User?>(null) }
  var isAuthLoading by remember { mutableStateOf(true) }
  var authScreenMode by rememberSaveable { mutableStateOf("login") }
  val scope = rememberCoroutineScope()

  // App Update State
  val currentInstalledVersionCode = BuildConfig.VERSION_CODE
  val currentInstalledVersionName = BuildConfig.VERSION_NAME
  var appUpdateConfig by remember { mutableStateOf<AppUpdateConfig?>(null) }
  var isUpdateDialogDismissedForSession by rememberSaveable { mutableStateOf(false) }

  // Listen to remote update config in real-time
  LaunchedEffect(Unit) {
    FirebaseService.getAppUpdateConfigFlow().collectLatest { config ->
      appUpdateConfig = config
    }
  }

  // Track Firebase auth state changes
  DisposableEffect(Unit) {
    val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
      val newAuthUser = firebaseAuth.currentUser
      authUser = newAuthUser
      if (newAuthUser == null) {
        currentUserProfile = null
        isAuthLoading = false
      }
    }
    auth.addAuthStateListener(listener)
    onDispose { auth.removeAuthStateListener(listener) }
  }

  // Real-time listener for current user's profile document
  LaunchedEffect(authUser?.uid) {
    val uid = authUser?.uid
    if (uid != null) {
      isAuthLoading = true
      var user = FirebaseService.getUser(uid)
      if (user == null) {
        val email = authUser?.email ?: ""
        user = FirebaseService.ensureUserProfile(uid, email)
      }
      currentUserProfile = user
      isAuthLoading = false

      FirebaseService.getUserProfileFlow(uid).collectLatest { updatedUser ->
        if (updatedUser != null) {
          currentUserProfile = updatedUser
        }
      }
    } else {
      currentUserProfile = null
      isAuthLoading = false
    }
  }

  // 1. Initial Splash Screen
  if (showSplash) {
    SplashScreen(
      onFinishSplash = {
        showSplash = false
      }
    )
    return
  }

  // 2. Loading Profile State
  if (isAuthLoading && authUser != null && currentUserProfile == null) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StrugxEmblem(size = 48.dp, animateGlow = true)
        Spacer(modifier = Modifier.height(18.dp))
        CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
      }
    }
    return
  }

  // 3. Not logged in -> Show Auth Flow (Login / Register)
  if (authUser == null) {
    if (authScreenMode == "register") {
      RegisterScreen(
        onRegisterAccount = { email, pass, onError ->
          scope.launch {
            val res = FirebaseService.registerAccountOnly(email, pass)
            if (res.isFailure) {
              onError(res.exceptionOrNull()?.message ?: "Registration failed")
            } else {
              val uid = res.getOrThrow()
              val profile = FirebaseService.getUser(uid)
              currentUserProfile = profile
            }
          }
        },
        onNavigateToLogin = { authScreenMode = "login" }
      )
    } else {
      LoginScreen(
        onLogin = { identifier, pass, onError ->
          scope.launch {
            val res = FirebaseService.signInWithEmail(identifier, pass)
            if (res.isFailure) {
              onError(res.exceptionOrNull()?.message ?: "Invalid email or password")
            } else {
              val uid = res.getOrThrow()
              val profile = FirebaseService.getUser(uid) ?: FirebaseService.ensureUserProfile(uid, identifier)
              currentUserProfile = profile
            }
          }
        },
        onNavigateToRegister = { authScreenMode = "register" },
        onForgotPassword = { email, onSuccess, onError ->
          scope.launch {
            val res = FirebaseService.sendPasswordReset(email)
            if (res.isSuccess) onSuccess() else onError(res.exceptionOrNull()?.message ?: "Failed to send reset email")
          }
        }
      )
    }
    return
  }

  // 4. If logged in but profile needs completion (username is empty or unset)
  val profile = currentUserProfile
  if (profile != null && profile.username.isBlank()) {
    ProfileSetupScreen(
      currentUid = profile.uid,
      email = profile.email,
      initialUser = profile,
      onProfileCompleted = { completedUser ->
        currentUserProfile = completedUser
      }
    )
    return
  }

  if (profile == null) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(color = PureWhite)
    }
    return
  }

  // 5. Check if user is banned
  if (profile.isBanned) {
    BannedScreen(
      user = profile,
      onLogout = { FirebaseService.signOut() }
    )
    return
  }

  // 6. Authenticated & Active -> Main Application Flow
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  var viewingPost by remember { mutableStateOf<Post?>(null) }

  val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Messages,
    Screen.Profile
  )

  val showBottomBar = bottomNavItems.any { it.route == currentRoute }

  Scaffold(
    bottomBar = {
      if (showBottomBar) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .navigationBarsPadding()
        ) {
          Spacer(
            modifier = Modifier
              .fillMaxWidth()
              .height(0.5.dp)
              .background(GlassBorder)
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(60.dp)
              .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            bottomNavItems.forEach { screen ->
              val selected = currentRoute == screen.route
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                  ) {
                    if (currentRoute != screen.route) {
                      navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                          saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  },
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) GlassSurfaceElevated else Color.Transparent)
                    .border(
                      width = if (selected) 0.8.dp else 0.dp,
                      color = if (selected) GlassBorder else Color.Transparent,
                      shape = RoundedCornerShape(14.dp)
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (selected) screen.iconFilled else screen.iconOutlined,
                    contentDescription = screen.title,
                    tint = if (selected) PureWhite else TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = Screen.Home.route,
      modifier = Modifier.padding(innerPadding)
    ) {
      // Home Tab
      composable(Screen.Home.route) {
        HomeScreen(
          currentUser = profile,
          isAdmin = FirebaseService.isAdmin(profile.uid),
          onNavigateToAdmin = { navController.navigate("admin") },
          onNavigateToUserProfile = { uid ->
            if (uid == profile.uid) {
              navController.navigate(Screen.Profile.route)
            } else {
              navController.navigate("user/$uid")
            }
          },
          onNavigateToChat = { rId, rName, rAvatar ->
            val convId = FirebaseService.conversationIdFor(profile.uid, rId)
            val encName = Uri.encode(rName)
            val encAvatar = Uri.encode(rAvatar.ifBlank { "none" })
            navController.navigate("chat/$convId/$rId/$encName/$encAvatar")
          },
          onShowPlus = { navController.navigate("plus") }
        )
      }

      // Search Tab
      composable(Screen.Search.route) {
        SearchScreen(
          currentUserId = profile.uid,
          currentUser = profile,
          onUserClick = { targetUid ->
            if (targetUid == profile.uid) {
              navController.navigate(Screen.Profile.route)
            } else {
              navController.navigate("user/$targetUid")
            }
          },
          onMessageClick = { rId, rName, rAvatar ->
            val convId = FirebaseService.conversationIdFor(profile.uid, rId)
            val encName = Uri.encode(rName)
            val encAvatar = Uri.encode(rAvatar.ifBlank { "none" })
            navController.navigate("chat/$convId/$rId/$encName/$encAvatar")
          },
          onShowPlus = { navController.navigate("plus") }
        )
      }

      // Messages Tab
      composable(Screen.Messages.route) {
        MessagesScreen(
          currentUserId = profile.uid,
          onOpenConversation = { convId, rId, rName, rAvatar ->
            val encName = Uri.encode(rName)
            val encAvatar = Uri.encode(rAvatar.ifBlank { "none" })
            navController.navigate("chat/$convId/$rId/$encName/$encAvatar")
          }
        )
      }

      // Profile Tab
      composable(Screen.Profile.route) {
        ProfileScreen(
          currentUser = profile,
          onNavigateToPlus = { navController.navigate("plus") },
          onNavigateToLegal = { title, type ->
            val encTitle = Uri.encode(title)
            navController.navigate("legal/$encTitle/$type")
          },
          onLogout = {
            FirebaseService.signOut()
            authUser = null
            currentUserProfile = null
            authScreenMode = "login"
          },
          onPostClick = { post -> viewingPost = post },
          onNavigateToAdmin = { navController.navigate("admin") }
        )
      }

      // Direct Message Thread Screen
      composable(
        route = "chat/{convId}/{recipientId}/{recipientUsername}/{recipientAvatar}",
        arguments = listOf(
          navArgument("convId") { type = NavType.StringType },
          navArgument("recipientId") { type = NavType.StringType },
          navArgument("recipientUsername") { type = NavType.StringType },
          navArgument("recipientAvatar") { type = NavType.StringType }
        )
      ) { backStackEntry ->
        val convId = backStackEntry.arguments?.getString("convId") ?: ""
        val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
        val recipientUsername = Uri.decode(backStackEntry.arguments?.getString("recipientUsername") ?: "")
        val rawAvatar = Uri.decode(backStackEntry.arguments?.getString("recipientAvatar") ?: "")
        val recipientAvatar = if (rawAvatar == "none") "" else rawAvatar

        ChatScreen(
          conversationId = convId,
          currentUser = profile,
          recipientId = recipientId,
          recipientUsername = recipientUsername,
          recipientAvatar = recipientAvatar,
          onBack = { navController.popBackStack() },
          onUserClick = { uid ->
            if (uid == profile.uid) navController.navigate(Screen.Profile.route) else navController.navigate("user/$uid")
          }
        )
      }

      // Other User's Profile Screen
      composable(
        route = "user/{targetUid}",
        arguments = listOf(navArgument("targetUid") { type = NavType.StringType })
      ) { backStackEntry ->
        val targetUid = backStackEntry.arguments?.getString("targetUid") ?: ""
        OtherProfileScreen(
          targetUserId = targetUid,
          currentUser = profile,
          onBack = { navController.popBackStack() },
          onOpenChat = { rId, rName, rAvatar ->
            val convId = FirebaseService.conversationIdFor(profile.uid, rId)
            val encName = Uri.encode(rName)
            val encAvatar = Uri.encode(rAvatar.ifBlank { "none" })
            navController.navigate("chat/$convId/$rId/$encName/$encAvatar")
          },
          onShowPlus = { navController.navigate("plus") },
          onPostClick = { post -> viewingPost = post }
        )
      }

      // Plus Subscription Screen
      composable("plus") {
        PlusScreen(
          currentUser = profile,
          onBack = { navController.popBackStack() }
        )
      }

      // Admin Dashboard Screen
      composable("admin") {
        AdminDashboardScreen(
          currentUserId = profile.uid,
          onBack = { navController.popBackStack() }
        )
      }

      // Legal & Info Pages
      composable(
        route = "legal/{title}/{type}",
        arguments = listOf(
          navArgument("title") { type = NavType.StringType },
          navArgument("type") { type = NavType.StringType }
        )
      ) { backStackEntry ->
        val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
        val type = backStackEntry.arguments?.getString("type") ?: ""
        LegalPolicyScreen(
          title = title,
          type = type,
          onBack = { navController.popBackStack() }
        )
      }
    }
  }

  // Global Detailed Post Viewer Modal
  viewingPost?.let { post ->
    PostViewerModal(
      post = post,
      currentUser = profile,
      onDismiss = { viewingPost = null },
      onUserClick = { targetUid ->
        viewingPost = null
        if (targetUid == profile.uid) {
          navController.navigate(Screen.Profile.route)
        } else {
          navController.navigate("user/$targetUid")
        }
      },
      onShowPlus = {
        viewingPost = null
        navController.navigate("plus")
      },
      onPostDeleted = {
        viewingPost = null
      }
    )
  }

  // In-App Update Dialog Check
  val currentConfig = appUpdateConfig
  if (currentConfig != null && currentConfig.enabled) {
    val isOldVersion = currentInstalledVersionCode < currentConfig.latestVersionCode
    val shouldShowUpdate = isOldVersion && (!isUpdateDialogDismissedForSession || currentConfig.isForceUpdate)

    if (shouldShowUpdate) {
      AppUpdateDialog(
        updateConfig = currentConfig,
        currentVersionName = currentInstalledVersionName,
        currentVersionCode = currentInstalledVersionCode,
        onDismiss = {
          isUpdateDialogDismissedForSession = true
        }
      )
    }
  }
}
