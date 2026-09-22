package com.example.data.firebase

import android.content.Context
import com.example.data.models.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FirebaseService {
  const val ADMIN_UID = "UQEIi8Qm8dae1vywlSRnuSnQIpF2"
  const val UPI_ID = "6375862443@ibl"

  private var initialized = false

  fun init(context: Context) {
    if (initialized) return
    try {
      if (FirebaseApp.getApps(context).isEmpty()) {
        val fromRes = FirebaseOptions.fromResource(context)
        if (fromRes != null) {
          FirebaseApp.initializeApp(context, fromRes)
        } else {
          val options = FirebaseOptions.Builder()
            .setApiKey("AIzaSyDv5XpW3oD5gzpXwiDosBSSU99VynA8ecs")
            .setApplicationId("1:175982808701:android:dc4c4400028066a21deb14")
            .setProjectId("hello-zeb")
            .setDatabaseUrl("https://hello-zeb-default-rtdb.asia-southeast1.firebasedatabase.app")
            .setStorageBucket("hello-zeb.firebasestorage.app")
            .setGcmSenderId("175982808701")
            .build()
          FirebaseApp.initializeApp(context, options)
        }
      }
      initialized = true
    } catch (e: Exception) {
      // Already initialized or caught
      initialized = true
    }
  }

  val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
  val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

  val currentUserId: String? get() = auth.currentUser?.uid
  fun isAdmin(uid: String? = currentUserId): Boolean = uid == ADMIN_UID

  suspend fun signInWithEmail(email: String, pass: String): Result<String> = login(email, pass)
  suspend fun signUpWithEmail(email: String, pass: String, username: String, displayName: String): Result<String> = register(email, pass, username, displayName)
  fun signOut() = logout()

  fun getUserProfileFlow(uid: String): Flow<User?> = getUserFlow(uid)
  suspend fun getUserProfile(uid: String): User? = getUser(uid)

  fun getUserPostsFlow(userId: String): Flow<List<Post>> = callbackFlow {
    val listener = firestore.collection("posts")
      .whereEqualTo("userId", userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val posts = snapshot?.toPostsSafe()?.sortedByDescending { it.createdAt } ?: emptyList()
        trySend(posts)
      }
    awaitClose { listener.remove() }
  }

  suspend fun updateUserProfile(
    uid: String,
    displayName: String,
    bio: String,
    avatarUrl: String? = null
  ): Result<Unit> = updateProfile(uid, displayName, bio, avatarUrl)

  fun getReportsFlow(): Flow<List<Report>> = getAllReportsFlow()
  suspend fun deleteAdvertisement(adId: String) = deleteAd(adId)

  suspend fun updateAdStatus(adId: String, isActive: Boolean): Result<Unit> {
    return updateAdStatus(adId, if (isActive) "active" else "paused")
  }

  suspend fun createAdvertisement(
    title: String,
    description: String,
    imageUrl: String,
    targetUrl: String,
    impressionTarget: Long,
    expiresAt: Long
  ): Result<Advertisement> = createAd(
    title = title,
    description = description,
    imageUrl = imageUrl,
    targetUrl = targetUrl,
    targetImpressions = impressionTarget,
    isPermanent = expiresAt == 0L,
    startAt = System.currentTimeMillis(),
    endAt = expiresAt
  )

  suspend fun submitPaymentRequest(
    userId: String,
    username: String,
    userEmail: String,
    plan: String,
    amount: Double,
    utr: String
  ): Result<Unit> {
    return try {
      val cleanUtr = utr.trim()
      if (cleanUtr.length < 6) {
        return Result.failure(IllegalArgumentException("Please enter a valid UTR / transaction reference number."))
      }
      val requestId = UUID.randomUUID().toString()
      val req = PaymentRequest(
        requestId = requestId,
        userId = userId,
        username = username,
        userEmail = userEmail,
        plan = plan,
        amount = amount.toInt().toString(),
        utr = cleanUtr,
        status = "pending",
        submittedAt = System.currentTimeMillis()
      )
      firestore.collection("paymentRequests").document(requestId).set(req.toFirestoreMap()).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun approvePaymentRequest(
    requestId: String,
    userId: String,
    months: Int,
    adminId: String = ADMIN_UID
  ): Result<Unit> {
    val planName = if (months == 6) "6_months" else "1_month"
    return approvePaymentRequest(requestId, userId, planName, adminId)
  }

  // ===================== AUTHENTICATION =====================

  suspend fun login(identifier: String, pass: String): Result<String> {
    return try {
      val trimmed = identifier.trim()
      if (trimmed.isBlank() || pass.isBlank()) {
        return Result.failure(IllegalArgumentException("Please enter your email/username and password."))
      }

      val emailToUse: String = if (!trimmed.contains("@")) {
        val cleanUser = trimmed.lowercase(Locale.ROOT)
        val snapshot = firestore.collection("users")
          .whereEqualTo("usernameLowercase", cleanUser)
          .limit(1)
          .get()
          .await()
        if (snapshot.isEmpty) {
          return Result.failure(IllegalArgumentException("No account found with username '@$trimmed'."))
        }
        val foundUser = snapshot.documents.first().toUserSafe()
        val userEmail = foundUser.email.ifBlank { null }
        userEmail ?: return Result.failure(IllegalArgumentException("No email address found for '@$trimmed'."))
      } else {
        trimmed
      }

      val res = auth.signInWithEmailAndPassword(emailToUse, pass).await()
      val uid = res.user?.uid ?: throw IllegalStateException("No user returned")
      ensureUserProfile(uid, emailToUse)
      Result.success(uid)
    } catch (e: Exception) {
      Result.failure(mapAuthException(e))
    }
  }

  suspend fun register(email: String, pass: String, username: String, displayName: String): Result<String> {
    return try {
      val cleanEmail = email.trim()
      val cleanUsername = username.trim().lowercase(Locale.ROOT)
      val cleanDisplayName = displayName.trim().ifBlank { cleanUsername }

      if (cleanUsername.length < 3) {
        return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
      }
      if (!cleanUsername.matches(Regex("^[a-z0-9._]{3,24}$"))) {
        return Result.failure(IllegalArgumentException("Username can only contain lowercase letters, numbers, underscores, and dots."))
      }
      if (pass.length < 6) {
        return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
      }
      if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
        return Result.failure(IllegalArgumentException("Please enter a valid email address."))
      }

      // Check username uniqueness
      val existing = firestore.collection("users")
        .whereEqualTo("usernameLowercase", cleanUsername)
        .limit(1)
        .get()
        .await()

      if (!existing.isEmpty) {
        return Result.failure(IllegalArgumentException("Username '@$cleanUsername' is already taken. Please choose another."))
      }

      val res = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
      val uid = res.user?.uid ?: throw IllegalStateException("Registration failed")

      val user = User(
        uid = uid,
        email = cleanEmail,
        username = cleanUsername,
        usernameLowercase = cleanUsername,
        displayName = cleanDisplayName,
        bio = "",
        profileImageUrl = "",
        createdAt = System.currentTimeMillis()
      )

      firestore.collection("users").document(uid).set(user.toFirestoreMap()).await()
      Result.success(uid)
    } catch (e: Exception) {
      Result.failure(mapAuthException(e))
    }
  }

  suspend fun ensureUserProfile(
    uid: String,
    email: String,
    username: String? = null,
    displayName: String? = null
  ): User {
    val existing = getUser(uid)
    if (existing != null) return existing

    val baseUser = if (!username.isNullOrBlank()) {
      username.trim().lowercase(Locale.ROOT)
    } else {
      "" // Blank so onboarding knows profile completion is required
    }
    val name = displayName?.ifBlank { "" } ?: ""

    val newUser = User(
      uid = uid,
      email = email.trim(),
      username = baseUser,
      usernameLowercase = baseUser,
      displayName = name.ifBlank { email.substringBefore("@") },
      bio = "",
      profileImageUrl = "",
      createdAt = System.currentTimeMillis()
    )
    try {
      firestore.collection("users").document(uid).set(newUser.toFirestoreMap()).await()
    } catch (_: Exception) {
      // Offline fallback
    }
    return newUser
  }

  private fun mapAuthException(e: Exception): Exception {
    val msg = e.message ?: ""
    return when {
      msg.contains("The email address is already in use", ignoreCase = true) ||
      msg.contains("email-already-in-use", ignoreCase = true) ->
        IllegalArgumentException("This email is already registered. Please log in instead.")
      msg.contains("password is invalid", ignoreCase = true) ||
      msg.contains("wrong-password", ignoreCase = true) ||
      msg.contains("invalid-credential", ignoreCase = true) ||
      msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
        IllegalArgumentException("Incorrect password or credentials. Please try again.")
      msg.contains("no user record", ignoreCase = true) ||
      msg.contains("user-not-found", ignoreCase = true) ->
        IllegalArgumentException("No account found with this email. Please sign up.")
      msg.contains("badly formatted", ignoreCase = true) ||
      msg.contains("invalid-email", ignoreCase = true) ->
        IllegalArgumentException("Please enter a valid email address.")
      msg.contains("network error", ignoreCase = true) ||
      msg.contains("A network error", ignoreCase = true) ->
        IllegalArgumentException("Network error. Please check your internet connection.")
      else -> e
    }
  }

  suspend fun sendPasswordReset(email: String): Result<Unit> {
    return try {
      auth.sendPasswordResetEmail(email.trim()).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(mapAuthException(e))
    }
  }

  fun logout() {
    auth.signOut()
  }

  suspend fun deleteAccount(userId: String): Result<Unit> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    if (currentAuthUid != userId && !isAdmin(currentAuthUid)) {
      return Result.failure(SecurityException("Unauthorized: Cannot delete another user's account."))
    }
    return try {
      val batch = firestore.batch()
      val userRef = firestore.collection("users").document(userId)
      // Anonymize user profile so references in comments/reports/messages remain consistent without leaking identity
      batch.update(
        userRef,
        mapOf(
          "displayName" to "Deleted User",
          "username" to "deleted_${userId.take(8)}",
          "usernameLowercase" to "deleted_${userId.take(8)}",
          "bio" to "",
          "profileImageUrl" to "",
          "isDeleted" to true,
          "deletedAt" to System.currentTimeMillis()
        )
      )

      // Delete active strugs created by this user
      val activeStrugs = firestore.collection("strugs")
        .whereEqualTo("userId", userId)
        .get()
        .await()
      for (strugDoc in activeStrugs.documents) {
        batch.delete(strugDoc.reference)
      }

      batch.commit().await()

      // Delete the Firebase Auth user if it's the current user
      if (currentAuthUid == userId) {
        try {
          auth.currentUser?.delete()?.await()
        } catch (_: Exception) {
          // If auth deletion requires recent re-auth, proceed with sign-out
        }
      }
      logout()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ===================== USER PROFILES =====================

  suspend fun getUser(uid: String): User? {
    return try {
      val doc = firestore.collection("users").document(uid).get().await()
      if (doc.exists()) {
        val u = doc.toUserSafe()
        if (u != null) {
          com.example.data.cache.UserProfileCache.update(u.uid, u.username, u.displayName, u.profileImageUrl)
        }
        u
      } else null
    } catch (e: Exception) {
      null
    }
  }

  fun getUserFlow(uid: String): Flow<User?> = callbackFlow {
    val listener = firestore.collection("users").document(uid)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(null)
          return@addSnapshotListener
        }
        val user = if (snapshot != null && snapshot.exists()) snapshot.toUserSafe() else null
        if (user != null) {
          com.example.data.cache.UserProfileCache.update(user.uid, user.username, user.displayName, user.profileImageUrl)
        }
        trySend(user)
      }
    awaitClose { listener.remove() }
  }

  suspend fun checkUsernameAvailable(username: String, currentUid: String? = null): Boolean {
    val clean = username.trim().lowercase(Locale.ROOT)
    if (clean.length < 3 || !clean.matches(Regex("^[a-z0-9._]{3,24}$"))) return false
    return try {
      val snapshot = firestore.collection("users")
        .whereEqualTo("usernameLowercase", clean)
        .limit(1)
        .get()
        .await()
      if (snapshot.isEmpty) true
      else {
        val foundUid = snapshot.documents.first().id
        foundUid == currentUid
      }
    } catch (e: Exception) {
      true
    }
  }

  suspend fun registerAccountOnly(email: String, pass: String): Result<String> {
    return try {
      val cleanEmail = email.trim()
      if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
        return Result.failure(IllegalArgumentException("Please enter a valid email address."))
      }
      if (pass.length < 6) {
        return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
      }

      val res = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
      val uid = res.user?.uid ?: throw IllegalStateException("Registration failed")

      // Initial draft user document
      val baseUser = cleanEmail.substringBefore("@")
        .replace(Regex("[^a-zA-Z0-9_]"), "_")
        .take(18)
        .lowercase(Locale.ROOT)
        .ifBlank { "user_${uid.take(5)}" }

      val initialUser = User(
        uid = uid,
        email = cleanEmail,
        username = "", // Blank so onboarding knows profile completion is required
        usernameLowercase = "",
        displayName = baseUser,
        bio = "",
        profileImageUrl = "",
        createdAt = System.currentTimeMillis()
      )
      firestore.collection("users").document(uid).set(initialUser.toFirestoreMap()).await()
      Result.success(uid)
    } catch (e: Exception) {
      Result.failure(mapAuthException(e))
    }
  }

  suspend fun completeProfile(
    uid: String,
    username: String,
    displayName: String,
    bio: String,
    profileImageUrl: String
  ): Result<User> {
    val currentAuthUid = auth.currentUser?.uid
    if (currentAuthUid != uid) {
      return Result.failure(SecurityException("Unauthorized: Cannot modify another user's profile."))
    }
    return try {
      val cleanUser = username.trim().lowercase(Locale.ROOT)
      val cleanName = displayName.trim().ifBlank { cleanUser }

      if (cleanUser.length < 3) {
        return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
      }
      if (!cleanUser.matches(Regex("^[a-z0-9._]{3,24}$"))) {
        return Result.failure(IllegalArgumentException("Username can only contain lowercase letters, numbers, '.', and '_'."))
      }

      // Verify uniqueness
      val isAvail = checkUsernameAvailable(cleanUser, uid)
      if (!isAvail) {
        return Result.failure(IllegalArgumentException("Username '@$cleanUser' is already taken."))
      }

      val updates = mapOf(
        "username" to cleanUser,
        "usernameLowercase" to cleanUser,
        "displayName" to cleanName,
        "bio" to bio.trim(),
        "profileImageUrl" to profileImageUrl.trim(),
        "updatedAt" to System.currentTimeMillis()
      )

      firestore.collection("users").document(uid).update(updates).await()
      val updatedUser = getUser(uid) ?: throw IllegalStateException("Failed to retrieve updated user profile")
      Result.success(updatedUser)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateProfile(
    uid: String,
    displayName: String,
    bio: String,
    profileImageUrl: String? = null,
    newUsername: String? = null
  ): Result<Unit> {
    val currentAuthUid = auth.currentUser?.uid
    if (currentAuthUid != uid) {
      return Result.failure(SecurityException("Unauthorized: Cannot modify another user's profile."))
    }
    return try {
      val updates = mutableMapOf<String, Any>(
        "displayName" to displayName.trim(),
        "bio" to bio.trim(),
        "updatedAt" to System.currentTimeMillis()
      )
      if (profileImageUrl != null) {
        updates["profileImageUrl"] = profileImageUrl
      }
      if (!newUsername.isNullOrBlank()) {
        val cleanUser = newUsername.trim().lowercase(Locale.ROOT)
        if (cleanUser.length < 3) {
          return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        }
        if (!cleanUser.matches(Regex("^[a-z0-9._]{3,24}$"))) {
          return Result.failure(IllegalArgumentException("Username can only contain lowercase letters, numbers, '.', and '_'."))
        }
        val isAvail = checkUsernameAvailable(cleanUser, uid)
        if (!isAvail) {
          return Result.failure(IllegalArgumentException("Username '@$cleanUser' is already taken."))
        }
        updates["username"] = cleanUser
        updates["usernameLowercase"] = cleanUser
      }
      firestore.collection("users").document(uid).update(updates).await()

      val finalUsername = updates["username"] as? String
      val finalDisplayName = (updates["displayName"] as? String) ?: displayName
      val finalAvatar = (updates["profileImageUrl"] as? String) ?: profileImageUrl ?: ""

      if (finalUsername != null) {
        com.example.data.cache.UserProfileCache.update(uid, finalUsername, finalDisplayName, finalAvatar)
      } else {
        val curr = com.example.data.cache.UserProfileCache.get(uid)
        val uName = curr?.username ?: ""
        com.example.data.cache.UserProfileCache.update(uid, uName, finalDisplayName, finalAvatar)
      }

      // Propagate updated identity to all existing posts, strugs, and comments authored by this user
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
          val postUpdates = mutableMapOf<String, Any>()
          if (finalUsername != null) postUpdates["username"] = finalUsername
          if (finalAvatar.isNotBlank()) postUpdates["userProfileImageUrl"] = finalAvatar

          if (postUpdates.isNotEmpty()) {
            val userPosts = firestore.collection("posts")
              .whereEqualTo("userId", uid)
              .get()
              .await()
            for (chunk in userPosts.documents.chunked(400)) {
              val batch = firestore.batch()
              for (doc in chunk) {
                batch.update(doc.reference, postUpdates)
              }
              batch.commit().await()
            }

            val userStrugs = firestore.collection("strugs")
              .whereEqualTo("userId", uid)
              .get()
              .await()
            for (chunk in userStrugs.documents.chunked(400)) {
              val batch = firestore.batch()
              for (doc in chunk) {
                batch.update(doc.reference, postUpdates)
              }
              batch.commit().await()
            }

            val userComments = firestore.collection("comments")
              .whereEqualTo("userId", uid)
              .get()
              .await()
            for (chunk in userComments.documents.chunked(400)) {
              val batch = firestore.batch()
              for (doc in chunk) {
                batch.update(doc.reference, postUpdates)
              }
              batch.commit().await()
            }
          }
        } catch (_: Exception) {}
      }

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun searchUsers(query: String): List<User> {
    val q = query.trim().lowercase(Locale.ROOT)
    if (q.isBlank()) return emptyList()
    return try {
      val snapshot = firestore.collection("users")
        .whereGreaterThanOrEqualTo("usernameLowercase", q)
        .whereLessThanOrEqualTo("usernameLowercase", q + "\uf8ff")
        .limit(25)
        .get()
        .await()
      snapshot.toUsersSafe()
    } catch (e: Exception) {
      emptyList()
    }
  }

  // ===================== FOLLOW SYSTEM =====================

  private fun followDocId(followerId: String, followingId: String) = "${followerId}_${followingId}"

  suspend fun isFollowing(followerId: String, followingId: String): Boolean {
    return try {
      val doc = firestore.collection("follows").document(followDocId(followerId, followingId)).get().await()
      doc.exists()
    } catch (e: Exception) {
      false
    }
  }

  suspend fun followUser(followerId: String, followingId: String): Result<Unit> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    if (currentAuthUid != followerId && !isAdmin(currentAuthUid)) {
      return Result.failure(SecurityException("Unauthorized follow action"))
    }
    if (followerId == followingId) {
      return Result.failure(IllegalArgumentException("You cannot follow yourself."))
    }

    if (isBlocked(followerId, followingId)) {
      return Result.failure(IllegalStateException("Cannot follow this user."))
    }

    return try {
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      val followerDoc = firestore.collection("users").document(followerId).get().await()
      val follower = if (followerDoc.exists()) followerDoc.toUserSafe() else throw IllegalStateException("User not found")

      // Check Plus status
      val isPlusActive = follower.isPlus && (follower.plusExpiresAt == 0L || follower.plusExpiresAt > System.currentTimeMillis())
      val maxDailyFollows = if (isPlusActive) 100L else 20L

      val currentDailyDate = follower.dailyFollowDate
      val currentDailyCount = if (currentDailyDate == today) follower.dailyFollowCount else 0L

      if (currentDailyCount >= maxDailyFollows) {
        val tierMsg = if (isPlusActive) "100" else "20"
        val upgradeHint = if (!isPlusActive) " Upgrade to Strugx Plus for up to 100 follows/day!" else ""
        return Result.failure(IllegalStateException("Daily follow limit reached ($tierMsg follows/day).$upgradeHint"))
      }

      val followRef = firestore.collection("follows").document(followDocId(followerId, followingId))
      val existing = followRef.get().await()
      if (existing.exists()) {
        return Result.success(Unit) // Already following
      }

      // Write follow relation & update counts atomically in batch
      val batch = firestore.batch()
      val followData = mapOf(
        "followerId" to followerId,
        "followingId" to followingId,
        "createdAt" to System.currentTimeMillis()
      )
      batch.set(followRef, followData)

      // Increment following count and daily follow count for follower
      val followerRef = firestore.collection("users").document(followerId)
      batch.update(
        followerRef,
        mapOf(
          "followingCount" to FieldValue.increment(1),
          "dailyFollowCount" to (currentDailyCount + 1),
          "dailyFollowDate" to today
        )
      )

      // Increment follower count for target
      val targetRef = firestore.collection("users").document(followingId)
      batch.update(targetRef, "followersCount", FieldValue.increment(1))

      // Notification for target user: @username started following you.
      val notifId = UUID.randomUUID().toString()
      val notifRef = firestore.collection("notifications").document(notifId)
      val notif = AppNotification(
        notificationId = notifId,
        recipientId = followingId,
        senderId = followerId,
        senderUsername = follower.username,
        senderAvatar = follower.profileImageUrl,
        type = "follow",
        text = "@${follower.username} started following you.",
        createdAt = System.currentTimeMillis()
      )
      batch.set(notifRef, notif.toFirestoreMap())

      batch.commit().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    if (currentAuthUid != followerId && !isAdmin(currentAuthUid)) {
      return Result.failure(SecurityException("Unauthorized unfollow action"))
    }
    return try {
      val followRef = firestore.collection("follows").document(followDocId(followerId, followingId))
      val doc = followRef.get().await()
      if (!doc.exists()) return Result.success(Unit)

      val batch = firestore.batch()
      batch.delete(followRef)

      val followerRef = firestore.collection("users").document(followerId)
      batch.update(followerRef, "followingCount", FieldValue.increment(-1))

      val targetRef = firestore.collection("users").document(followingId)
      batch.update(targetRef, "followersCount", FieldValue.increment(-1))

      batch.commit().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getFollowers(userId: String): List<User> {
    return try {
      val snapshot = firestore.collection("follows")
        .whereEqualTo("followingId", userId)
        .limit(50)
        .get()
        .await()
      val followerIds = snapshot.documents.mapNotNull { it.getString("followerId") }
      if (followerIds.isEmpty()) return emptyList()
      val users = mutableListOf<User>()
      for (chunk in followerIds.chunked(10)) {
        val uSnap = firestore.collection("users")
          .whereIn(FieldPath.documentId(), chunk)
          .get()
          .await()
        users.addAll(uSnap.toUsersSafe())
      }
      users
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun getFollowing(userId: String): List<User> {
    return try {
      val snapshot = firestore.collection("follows")
        .whereEqualTo("followerId", userId)
        .limit(50)
        .get()
        .await()
      val followingIds = snapshot.documents.mapNotNull { it.getString("followingId") }
      if (followingIds.isEmpty()) return emptyList()
      val users = mutableListOf<User>()
      for (chunk in followingIds.chunked(10)) {
        val uSnap = firestore.collection("users")
          .whereIn(FieldPath.documentId(), chunk)
          .get()
          .await()
        users.addAll(uSnap.toUsersSafe())
      }
      users
    } catch (e: Exception) {
      emptyList()
    }
  }

  // ===================== POSTS SYSTEM =====================

  suspend fun createPost(
    userId: String,
    username: String,
    userProfileImageUrl: String,
    imageUrl: String?,
    caption: String,
    isTextOnly: Boolean,
    musicTitle: String = "",
    musicArtist: String = "",
    musicAudioUrl: String = "",
    musicCoverUrl: String = ""
  ): Result<Post> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    if (currentAuthUid != userId && !isAdmin(currentAuthUid)) {
      return Result.failure(SecurityException("Cannot create post on behalf of another user"))
    }
    return try {
      val postId = UUID.randomUUID().toString()
      val post = Post(
        postId = postId,
        userId = userId,
        username = username,
        userProfileImageUrl = userProfileImageUrl,
        imageUrl = imageUrl ?: "",
        caption = caption.trim(),
        createdAt = System.currentTimeMillis(),
        viewCount = 0,
        commentCount = 0,
        isTextOnly = isTextOnly,
        musicTitle = musicTitle,
        musicArtist = musicArtist,
        musicAudioUrl = musicAudioUrl,
        musicCoverUrl = musicCoverUrl
      )

      val batch = firestore.batch()
      val postRef = firestore.collection("posts").document(postId)
      batch.set(postRef, post.toFirestoreMap())

      val userRef = firestore.collection("users").document(userId)
      batch.update(userRef, "postsCount", FieldValue.increment(1))

      batch.commit().await()
      Result.success(post)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deletePost(postId: String, userId: String): Result<Unit> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    return try {
      val postRef = firestore.collection("posts").document(postId)
      val postDoc = postRef.get().await()
      val ownerId = postDoc.getString("userId")
      if (ownerId != currentAuthUid && !isAdmin(currentAuthUid)) {
        return Result.failure(IllegalAccessException("Not authorized to delete this post"))
      }

      val batch = firestore.batch()
      batch.delete(postRef)
      if (ownerId != null) {
        val userRef = firestore.collection("users").document(ownerId)
        batch.update(userRef, "postsCount", FieldValue.increment(-1))
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updatePostCaption(postId: String, newCaption: String, userId: String): Result<Unit> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    return try {
      val postRef = firestore.collection("posts").document(postId)
      val doc = postRef.get().await()
      if (!doc.exists()) return Result.failure(IllegalStateException("Post not found"))
      val ownerId = doc.getString("userId")
      if (ownerId != currentAuthUid && !isAdmin(currentAuthUid)) {
        return Result.failure(IllegalAccessException("Not authorized to edit this post"))
      }
      postRef.update(
        mapOf(
          "caption" to newCaption.trim(),
          "updatedAt" to System.currentTimeMillis()
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updatePostMusic(
    postId: String,
    musicAudioUrl: String,
    musicTitle: String,
    musicArtist: String,
    musicCoverUrl: String,
    userId: String
  ): Result<Unit> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    return try {
      val postRef = firestore.collection("posts").document(postId)
      val doc = postRef.get().await()
      if (!doc.exists()) return Result.failure(IllegalStateException("Post not found"))
      val ownerId = doc.getString("userId")
      val isAdminUser = isAdmin(currentAuthUid)
      val isPlusUser = getUser(currentAuthUid)?.isPlus == true
      
      // Constraint: Must be post owner or admin, and only admin or plus plan users can add music to existing posts
      if (ownerId != currentAuthUid && !isAdminUser) {
        return Result.failure(IllegalAccessException("Not authorized to edit this post"))
      }
      if (!isAdminUser && !isPlusUser) {
        return Result.failure(IllegalAccessException("Adding music to existing posts is exclusive to Plus plan users"))
      }

      postRef.update(
        mapOf(
          "musicAudioUrl" to musicAudioUrl,
          "musicTitle" to musicTitle,
          "musicArtist" to musicArtist,
          "musicCoverUrl" to musicCoverUrl,
          "updatedAt" to System.currentTimeMillis()
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getPost(postId: String): Post? {
    return try {
      val doc = firestore.collection("posts").document(postId).get().await()
      if (doc.exists()) doc.toPostSafe() else null
    } catch (e: Exception) {
      null
    }
  }

  fun getFeedPostsFlow(limit: Long = 50): Flow<List<Post>> = callbackFlow {
    val listener = firestore.collection("posts")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(limit)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val posts = snapshot?.toPostsSafe()?.sortedByDescending { it.createdAt } ?: emptyList()
        trySend(posts)
      }
    awaitClose { listener.remove() }
  }

  suspend fun getUserPosts(userId: String): List<Post> {
    return try {
      val snapshot = firestore.collection("posts")
        .whereEqualTo("userId", userId)
        .get()
        .await()
      snapshot.toPostsSafe().sortedByDescending { it.createdAt }
    } catch (e: Exception) {
      emptyList()
    }
  }

  private val viewedPostsThisSession = mutableSetOf<String>()

  suspend fun recordPostView(postId: String, viewerId: String) {
    val key = "${postId}_$viewerId"
    if (viewedPostsThisSession.contains(key)) return
    viewedPostsThisSession.add(key)

    try {
      firestore.collection("posts").document(postId)
        .update("viewCount", FieldValue.increment(1))
        .await()
    } catch (_: Exception) {}
  }

  // ===================== COMMENTS =====================

  fun getCommentsFlow(postId: String): Flow<List<Comment>> = callbackFlow {
    val listener = firestore.collection("posts").document(postId)
      .collection("comments")
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val comments = snapshot?.toCommentsSafe()?.sortedBy { it.createdAt } ?: emptyList()
        trySend(comments)
      }
    awaitClose { listener.remove() }
  }

  suspend fun addComment(
    postId: String,
    userId: String,
    username: String,
    avatar: String,
    text: String
  ): Result<Comment> {
    return try {
      val commentId = UUID.randomUUID().toString()
      val comment = Comment(
        commentId = commentId,
        postId = postId,
        userId = userId,
        username = username,
        userProfileImageUrl = avatar,
        text = text.trim(),
        createdAt = System.currentTimeMillis()
      )

      val batch = firestore.batch()
      val commentRef = firestore.collection("posts").document(postId)
        .collection("comments").document(commentId)
      batch.set(commentRef, comment.toFirestoreMap())

      val postRef = firestore.collection("posts").document(postId)
      batch.update(postRef, "commentCount", FieldValue.increment(1))

      // Notification for post author if different user
      val postDoc = postRef.get().await()
      val postAuthorId = postDoc.getString("userId")
      if (postAuthorId != null && postAuthorId != userId) {
        val notifId = UUID.randomUUID().toString()
        val notifRef = firestore.collection("notifications").document(notifId)
        val notif = AppNotification(
          notificationId = notifId,
          recipientId = postAuthorId,
          senderId = userId,
          senderUsername = username,
          senderAvatar = avatar,
          type = "comment",
          text = "@$username commented on your post: \"${text.take(30)}\"",
          targetId = postId,
          createdAt = System.currentTimeMillis()
        )
        batch.set(notifRef, notif.toFirestoreMap())
      }

      batch.commit().await()
      Result.success(comment)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteComment(postId: String, commentId: String, userId: String): Result<Unit> {
    return try {
      val commentRef = firestore.collection("posts").document(postId)
        .collection("comments").document(commentId)
      val commentDoc = commentRef.get().await()
      val comment = if (commentDoc.exists()) commentDoc.toCommentSafe() else null

      val postDoc = firestore.collection("posts").document(postId).get().await()
      val postOwnerId = postDoc.getString("userId")
      val isPostOwner = postOwnerId == userId

      if (comment?.userId != userId && !isPostOwner && !isAdmin(userId)) {
        return Result.failure(IllegalAccessException("Not authorized to delete this comment"))
      }

      val batch = firestore.batch()
      batch.delete(commentRef)
      val postRef = firestore.collection("posts").document(postId)
      batch.update(postRef, "commentCount", FieldValue.increment(-1))
      batch.commit().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ===================== STRUGS (STORIES) =====================

  suspend fun createStrug(
    userId: String,
    username: String,
    avatar: String,
    imageUrl: String,
    caption: String,
    musicTitle: String = "",
    musicArtist: String = "",
    musicAudioUrl: String = "",
    musicCoverUrl: String = ""
  ): Result<Strug> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    if (currentAuthUid != userId && !isAdmin(currentAuthUid)) {
      return Result.failure(SecurityException("Cannot post Strug on behalf of another user"))
    }
    return try {
      val strugId = UUID.randomUUID().toString()
      val strug = Strug(
        strugId = strugId,
        userId = userId,
        username = username,
        userProfileImageUrl = avatar,
        imageUrl = imageUrl,
        caption = caption.trim(),
        createdAt = System.currentTimeMillis(),
        musicTitle = musicTitle,
        musicArtist = musicArtist,
        musicAudioUrl = musicAudioUrl,
        musicCoverUrl = musicCoverUrl
      )
      firestore.collection("strugs").document(strugId).set(strug.toFirestoreMap()).await()
      Result.success(strug)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteStrug(strugId: String, userId: String): Result<Unit> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    return try {
      val doc = firestore.collection("strugs").document(strugId).get().await()
      val authorUid = doc.getString("userId") ?: ""
      if (authorUid != currentAuthUid && !isAdmin(currentAuthUid)) {
        return Result.failure(IllegalAccessException("Not authorized to delete this Strug"))
      }
      firestore.collection("strugs").document(strugId).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getActiveStrugsFlow(): Flow<List<Strug>> = callbackFlow {
    // Last 24 hours
    val listener = firestore.collection("strugs")
      .limit(100)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val cutoff = System.currentTimeMillis() - (24L * 60 * 60 * 1000)
        val strugs = snapshot?.toStrugsSafe()
          ?.filter { it.createdAt >= cutoff }
          ?.sortedByDescending { it.createdAt }
          ?: emptyList()
        trySend(strugs)
      }
    awaitClose { listener.remove() }
  }

  // ===================== MESSAGING =====================

  fun conversationIdFor(u1: String, u2: String): String {
    return if (u1 < u2) "${u1}_${u2}" else "${u2}_${u1}"
  }

  fun getConversationsFlow(userId: String): Flow<List<Conversation>> = callbackFlow {
    val listener = firestore.collection("conversations")
      .whereArrayContains("participants", userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.toConversationsSafe()
          ?.filter { !it.deletedFor.contains(userId) }
          ?.sortedByDescending { it.lastMessageAt } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  fun getMessagesFlow(convId: String): Flow<List<Message>> = callbackFlow {
    val listener = firestore.collection("conversations").document(convId)
      .collection("messages")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.toMessagesSafe()?.sortedBy { it.createdAt } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  suspend fun sendMessage(
    conversationId: String,
    senderId: String,
    receiverId: String,
    senderUsername: String,
    senderAvatar: String,
    receiverUsername: String,
    receiverAvatar: String,
    text: String,
    sharedPostId: String = "",
    sharedPostCaption: String = "",
    sharedPostImage: String = "",
    strugId: String = "",
    strugImageUrl: String = "",
    sharedProfileUserId: String = "",
    sharedProfileUsername: String = "",
    sharedProfileName: String = "",
    sharedProfileAvatar: String = ""
  ): Result<Unit> {
    return try {
      if (isBlocked(senderId, receiverId)) {
        return Result.failure(IllegalStateException("You cannot message this user."))
      }

      val messageId = UUID.randomUUID().toString()
      val now = System.currentTimeMillis()
      val message = Message(
        messageId = messageId,
        conversationId = conversationId,
        senderId = senderId,
        receiverId = receiverId,
        text = text.trim(),
        sharedPostId = sharedPostId,
        sharedPostCaption = sharedPostCaption,
        sharedPostImage = sharedPostImage,
        strugId = strugId,
        strugImageUrl = strugImageUrl,
        sharedProfileUserId = sharedProfileUserId,
        sharedProfileUsername = sharedProfileUsername,
        sharedProfileName = sharedProfileName,
        sharedProfileAvatar = sharedProfileAvatar,
        createdAt = now
      )

      val convRef = firestore.collection("conversations").document(conversationId)
      val msgRef = convRef.collection("messages").document(messageId)

      val lastText = when {
        text.isNotBlank() -> text.trim()
        sharedPostId.isNotBlank() -> "[Shared Post]"
        sharedProfileUserId.isNotBlank() -> "[Shared Profile]"
        strugId.isNotBlank() -> "[Strug Reply]"
        else -> "New message"
      }

      val convData = mapOf(
        "conversationId" to conversationId,
        "participants" to listOf(senderId, receiverId),
        "participantUsernames" to mapOf(senderId to senderUsername, receiverId to receiverUsername),
        "participantAvatars" to mapOf(senderId to senderAvatar, receiverId to receiverAvatar),
        "lastMessage" to lastText,
        "lastMessageAt" to now,
        "lastSenderId" to senderId,
        "deletedFor" to emptyList<String>()
      )

      val batch = firestore.batch()
      batch.set(convRef, convData, SetOptions.merge())
      batch.set(msgRef, message.toFirestoreMap())
      batch.commit().await()

      // Send in-app notification to receiver
      sendAppNotification(
        recipientId = receiverId,
        senderId = senderId,
        senderUsername = senderUsername,
        senderAvatar = senderAvatar,
        type = if (strugId.isNotBlank()) "strug_reply" else "message",
        text = if (text.isNotBlank()) text.trim() else lastText,
        targetId = conversationId
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateConversationCategory(
    conversationId: String,
    userId: String,
    category: String
  ): Result<Unit> {
    return try {
      firestore.collection("conversations").document(conversationId)
        .update("categories.$userId", category)
        .await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteConversationForUser(
    conversationId: String,
    userId: String
  ): Result<Unit> {
    return try {
      firestore.collection("conversations").document(conversationId)
        .update("deletedFor", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
        .await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun blockUser(currentUserId: String, targetUserId: String): Result<Unit> {
    return try {
      val blockId = "${currentUserId}_${targetUserId}"
      firestore.collection("blocks").document(blockId).set(
        mapOf(
          "blockerId" to currentUserId,
          "blockedId" to targetUserId,
          "createdAt" to com.google.firebase.Timestamp.now()
        )
      ).await()

      // Unfollow in both directions
      unfollowUser(currentUserId, targetUserId)
      unfollowUser(targetUserId, currentUserId)

      // Hide conversation for current user
      val convId = conversationIdFor(currentUserId, targetUserId)
      deleteConversationForUser(convId, currentUserId)

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun unblockUser(currentUserId: String, targetUserId: String): Result<Unit> {
    return try {
      firestore.collection("blocks").document("${currentUserId}_${targetUserId}").delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun isBlocked(u1: String, u2: String): Boolean {
    return try {
      val b1 = firestore.collection("blocks").document("${u1}_${u2}").get().await().exists()
      if (b1) return true
      firestore.collection("blocks").document("${u2}_${u1}").get().await().exists()
    } catch (e: Exception) {
      false
    }
  }

  suspend fun getUserIdByUsername(username: String): String? {
    val clean = username.trim().lowercase(Locale.ROOT)
    return try {
      val query = firestore.collection("users")
        .whereEqualTo("usernameLowercase", clean)
        .limit(1)
        .get()
        .await()
      if (!query.isEmpty) {
        query.documents.first().id
      } else {
        null
      }
    } catch (e: Exception) {
      null
    }
  }

  suspend fun getPopularPosts(limit: Long = 30): List<Post> {
    return try {
      val snapshot = firestore.collection("posts")
        .orderBy("viewCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
        .limit(limit)
        .get()
        .await()
      snapshot.toPostsSafe()
    } catch (e: Exception) {
      try {
        val fallback = firestore.collection("posts").limit(limit).get().await()
        fallback.toPostsSafe().sortedByDescending { it.viewCount }
      } catch (ex: Exception) {
        emptyList()
      }
    }
  }

  suspend fun getOrCreateOfficialAccount(): User {
    return try {
      val query = firestore.collection("users")
        .whereEqualTo("usernameLowercase", "strugx")
        .limit(1)
        .get()
        .await()
      if (!query.isEmpty) {
        return query.documents.first().toUserSafe()
      }
      val doc = firestore.collection("users").document("strugx_official").get().await()
      if (doc.exists()) {
        return doc.toUserSafe()
      }
      val official = User(
        uid = "strugx_official",
        email = "official@strugx.app",
        username = "strugx",
        usernameLowercase = "strugx",
        displayName = "STRUGX",
        bio = "Official STRUGX account. App updates, announcements, and support information.",
        isPlus = true,
        createdAt = System.currentTimeMillis(),
        hasCompletedOnboarding = true
      )
      firestore.collection("users").document("strugx_official").set(official.toFirestoreMap()).await()
      official
    } catch (e: Exception) {
      User(
        uid = "strugx_official",
        email = "official@strugx.app",
        username = "strugx",
        usernameLowercase = "strugx",
        displayName = "STRUGX",
        bio = "Official STRUGX account. App updates, announcements, and support information.",
        isPlus = true
      )
    }
  }

  suspend fun followOfficialAccount(userId: String): Result<Unit> {
    val official = getOrCreateOfficialAccount()
    val followRes = followUser(userId, official.uid)
    completeOnboarding(userId)
    return followRes
  }

  suspend fun completeOnboarding(userId: String): Result<Unit> {
    return try {
      firestore.collection("users").document(userId).update("hasCompletedOnboarding", true).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getNotificationsFlow(recipientId: String): Flow<List<AppNotification>> = callbackFlow {
    val listener = firestore.collection("notifications")
      .whereEqualTo("recipientId", recipientId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.toNotificationsSafe()?.sortedByDescending { it.createdAt } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  fun getUnreadNotificationCountFlow(recipientId: String): Flow<Int> = callbackFlow {
    val listener = firestore.collection("notifications")
      .whereEqualTo("recipientId", recipientId)
      .whereEqualTo("isRead", false)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(0)
          return@addSnapshotListener
        }
        val count = snapshot?.size() ?: 0
        trySend(count)
      }
    awaitClose { listener.remove() }
  }

  suspend fun markNotificationsAsRead(recipientId: String): Result<Unit> {
    return try {
      val unreadDocs = firestore.collection("notifications")
        .whereEqualTo("recipientId", recipientId)
        .whereEqualTo("isRead", false)
        .limit(50)
        .get()
        .await()
      if (!unreadDocs.isEmpty) {
        val batch = firestore.batch()
        for (doc in unreadDocs.documents) {
          batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ===================== PLUS SUBSCRIPTION =====================

  suspend fun submitPaymentRequest(
    userId: String,
    username: String,
    plan: String,
    amount: String,
    utr: String
  ): Result<Unit> {
    val currentAuthUid = auth.currentUser?.uid ?: return Result.failure(SecurityException("Not authenticated"))
    if (currentAuthUid != userId) {
      return Result.failure(SecurityException("Cannot submit payment request on behalf of another user"))
    }
    return try {
      val cleanUtr = utr.trim().uppercase(Locale.ROOT)
      if (cleanUtr.length < 6) {
        return Result.failure(IllegalArgumentException("Please enter a valid UTR / transaction reference number (at least 6 characters)."))
      }

      // 1. Check for duplicate UTR number across payment requests
      val existingUtr = firestore.collection("paymentRequests")
        .whereEqualTo("utr", cleanUtr)
        .limit(1)
        .get()
        .await()
      if (!existingUtr.isEmpty) {
        return Result.failure(IllegalStateException("A payment request with UTR '$cleanUtr' has already been submitted."))
      }

      // 2. Prevent spam submissions (max 2 pending requests per user)
      val userPending = firestore.collection("paymentRequests")
        .whereEqualTo("userId", userId)
        .whereEqualTo("status", "pending")
        .limit(3)
        .get()
        .await()
      if (userPending.size() >= 2) {
        return Result.failure(IllegalStateException("You already have pending payment requests under review. Please allow admin time to verify."))
      }

      val requestId = UUID.randomUUID().toString()
      val req = PaymentRequest(
        requestId = requestId,
        userId = userId,
        username = username,
        plan = plan,
        amount = amount,
        utr = cleanUtr,
        status = "pending",
        submittedAt = System.currentTimeMillis()
      )
      firestore.collection("paymentRequests").document(requestId).set(req.toFirestoreMap()).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getPendingPaymentRequestsFlow(): Flow<List<PaymentRequest>> = callbackFlow {
    val listener = firestore.collection("paymentRequests")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.toPaymentRequestsSafe()?.sortedByDescending { it.submittedAt } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  fun getUserPaymentRequestsFlow(userId: String): Flow<List<PaymentRequest>> = callbackFlow {
    val listener = firestore.collection("paymentRequests")
      .whereEqualTo("userId", userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.toPaymentRequestsSafe()?.sortedByDescending { it.submittedAt } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  suspend fun approvePaymentRequest(
    requestId: String,
    userId: String,
    plan: String,
    adminId: String
  ): Result<Unit> {
    if (!isAdmin(adminId)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      val now = System.currentTimeMillis()
      // Plan 1: 1 Month (30 days) -> 2592000000 ms
      // Plan 2: 6 Months (180 days) -> 15552000000 ms
      val durationMs = if (plan.contains("6 Month", ignoreCase = true)) {
        180L * 24 * 60 * 60 * 1000
      } else {
        30L * 24 * 60 * 60 * 1000
      }
      val expiresAt = now + durationMs

      val batch = firestore.batch()
      val reqRef = firestore.collection("paymentRequests").document(requestId)
      batch.update(
        reqRef,
        mapOf(
          "status" to "approved",
          "reviewedAt" to now,
          "reviewedBy" to adminId
        )
      )

      val userRef = firestore.collection("users").document(userId)
      batch.update(
        userRef,
        mapOf(
          "isPlus" to true,
          "plusPlan" to plan,
          "plusStartedAt" to now,
          "plusExpiresAt" to expiresAt
        )
      )

      batch.commit().await()

      // Notify user and record audit log
      sendAppNotification(
        recipientId = userId,
        senderId = adminId,
        senderUsername = "STRUGX ADMIN",
        type = "plus_approved",
        text = "Your Strugx Plus subscription ($plan) is active! You now have the Plus badge and 100 follows/day."
      )
      logAdminAction(
        adminUid = adminId,
        action = "APPROVE_PAYMENT",
        targetId = userId,
        targetType = "user",
        reason = "Approved payment request $requestId for plan $plan (Expires: ${Date(expiresAt)})"
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun rejectPaymentRequest(requestId: String, reason: String = "", adminId: String = ADMIN_UID): Result<Unit> {
    if (!isAdmin(adminId)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      val reqDoc = firestore.collection("paymentRequests").document(requestId).get().await()
      val userId = reqDoc.getString("userId") ?: ""
      firestore.collection("paymentRequests").document(requestId).update(
        mapOf(
          "status" to "rejected",
          "adminNote" to reason,
          "reviewedAt" to System.currentTimeMillis(),
          "reviewedBy" to adminId
        )
      ).await()
      if (userId.isNotBlank()) {
        sendAppNotification(
          recipientId = userId,
          senderId = adminId,
          senderUsername = "STRUGX ADMIN",
          type = "plus_rejected",
          text = "Your payment request was rejected: ${reason.ifBlank { "Transaction reference unverified" }}"
        )
      }
      logAdminAction(
        adminUid = adminId,
        action = "REJECT_PAYMENT",
        targetId = requestId,
        targetType = "payment_request",
        reason = "Rejected payment request: $reason"
      )
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ===================== ADVERTISEMENTS =====================

  fun getActiveAdsFlow(): Flow<List<Advertisement>> = callbackFlow {
    val listener = firestore.collection("ads")
      .whereEqualTo("status", "active")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.toAdvertisementsSafe()?.sortedByDescending { it.createdAt } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  fun getAllAdsFlow(): Flow<List<Advertisement>> = callbackFlow {
    val listener = firestore.collection("ads")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.toAdvertisementsSafe()?.sortedByDescending { it.createdAt } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  suspend fun createAd(
    title: String,
    description: String,
    imageUrl: String,
    targetUrl: String,
    targetImpressions: Long,
    isPermanent: Boolean,
    startAt: Long,
    endAt: Long
  ): Result<Advertisement> {
    return try {
      val adId = UUID.randomUUID().toString()
      val ad = Advertisement(
        adId = adId,
        title = title.trim(),
        description = description.trim(),
        imageUrl = imageUrl,
        targetUrl = targetUrl.trim(),
        status = "active",
        startAt = startAt,
        endAt = endAt,
        isPermanent = isPermanent,
        targetImpressions = targetImpressions,
        currentImpressions = 0,
        clickCount = 0,
        createdAt = System.currentTimeMillis()
      )
      firestore.collection("ads").document(adId).set(ad.toFirestoreMap()).await()
      Result.success(ad)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateAdStatus(adId: String, status: String): Result<Unit> {
    return try {
      firestore.collection("ads").document(adId).update("status", status).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteAd(adId: String): Result<Unit> {
    return try {
      firestore.collection("ads").document(adId).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private val recordedAdImpressions = mutableSetOf<String>()

  suspend fun recordAdImpression(adId: String) {
    if (recordedAdImpressions.contains(adId)) return
    recordedAdImpressions.add(adId)

    try {
      val adRef = firestore.collection("ads").document(adId)
      firestore.runTransaction { transaction ->
        val snapshot = transaction.get(adRef)
        val current = snapshot.getLong("currentImpressions") ?: 0L
        val target = snapshot.getLong("targetImpressions") ?: 1000L
        val isPerm = snapshot.getBoolean("isPermanent") ?: true
        val next = current + 1

        transaction.update(adRef, "currentImpressions", next)
        if (!isPerm && next >= target) {
          transaction.update(adRef, "status", "paused")
        }
      }.await()
    } catch (_: Exception) {}
  }

  suspend fun recordAdClick(adId: String) {
    try {
      firestore.collection("ads").document(adId)
        .update("clickCount", FieldValue.increment(1))
        .await()
    } catch (_: Exception) {}
  }

  // ===================== REPORTS & MODERATION =====================

  suspend fun submitReport(
    reporterId: String,
    reporterUsername: String,
    targetType: String,
    targetId: String,
    reason: String,
    details: String
  ): Result<Unit> {
    return try {
      val reportId = UUID.randomUUID().toString()
      val report = Report(
        reportId = reportId,
        reporterId = reporterId,
        reporterUsername = reporterUsername,
        targetType = targetType,
        targetId = targetId,
        reason = reason,
        details = details.trim(),
        createdAt = System.currentTimeMillis(),
        status = "pending"
      )
      firestore.collection("reports").document(reportId).set(report.toFirestoreMap()).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getAllReportsFlow(): Flow<List<Report>> = callbackFlow {
    val listener = firestore.collection("reports")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.toReportsSafe()?.sortedByDescending { it.createdAt } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  suspend fun resolveReport(reportId: String): Result<Unit> {
    return try {
      firestore.collection("reports").document(reportId).update("status", "resolved").await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateReport(
    reportId: String,
    newStatus: String,
    adminNote: String,
    adminUid: String = ADMIN_UID
  ): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      firestore.collection("reports").document(reportId).update(
        mapOf(
          "status" to newStatus,
          "adminNote" to adminNote.trim(),
          "reviewedAt" to System.currentTimeMillis(),
          "reviewedBy" to adminUid
        )
      ).await()
      logAdminAction(adminUid, "REPORT_${newStatus.uppercase(Locale.ROOT)}", reportId, "report", adminNote)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun banUser(userId: String, reason: String, banUntil: Long, adminUid: String = ADMIN_UID): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      firestore.collection("users").document(userId).update(
        mapOf(
          "isBanned" to true,
          "banReason" to reason.trim(),
          "banUntil" to banUntil
        )
      ).await()
      val type = if (banUntil > 0) "TEMP_BAN_USER" else "PERM_BAN_USER"
      val durationInfo = if (banUntil > 0) "until ${Date(banUntil)}" else "Permanent"
      logAdminAction(adminUid, type, userId, "user", "$durationInfo: $reason")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun unbanUser(userId: String, adminUid: String = ADMIN_UID): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      firestore.collection("users").document(userId).update(
        mapOf(
          "isBanned" to false,
          "banReason" to "",
          "banUntil" to 0L
        )
      ).await()
      logAdminAction(adminUid, "UNBAN_USER", userId, "user", "Account access restored")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ===================== ADMIN CONTENT MODERATION =====================

  fun getAllPostsFlow(limit: Long = 100): Flow<List<Post>> = callbackFlow {
    val listener = firestore.collection("posts")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(limit)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        trySend(snapshot?.toPostsSafe() ?: emptyList())
      }
    awaitClose { listener.remove() }
  }

  fun getAllStrugsFlow(limit: Long = 100): Flow<List<Strug>> = callbackFlow {
    val listener = firestore.collection("strugs")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(limit)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        trySend(snapshot?.toStrugsSafe() ?: emptyList())
      }
    awaitClose { listener.remove() }
  }

  fun getAllUsersFlow(limit: Long = 100): Flow<List<User>> = callbackFlow {
    val listener = firestore.collection("users")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(limit)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        trySend(snapshot?.toUsersSafe() ?: emptyList())
      }
    awaitClose { listener.remove() }
  }

  fun getAllPlusUsersFlow(limit: Long = 100): Flow<List<User>> = callbackFlow {
    val listener = firestore.collection("users")
      .whereEqualTo("isPlus", true)
      .limit(limit)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        trySend(snapshot?.toUsersSafe() ?: emptyList())
      }
    awaitClose { listener.remove() }
  }

  fun getAllPaymentRequestsFlow(): Flow<List<PaymentRequest>> = callbackFlow {
    val listener = firestore.collection("paymentRequests")
      .orderBy("submittedAt", Query.Direction.DESCENDING)
      .limit(200)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        trySend(snapshot?.toPaymentRequestsSafe() ?: emptyList())
      }
    awaitClose { listener.remove() }
  }

  suspend fun adminDeletePost(postId: String, authorUid: String, reason: String, adminUid: String = ADMIN_UID): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      firestore.collection("posts").document(postId).delete().await()
      if (authorUid.isNotBlank()) {
        firestore.collection("users").document(authorUid)
          .update("postsCount", FieldValue.increment(-1))
          .await()
        sendAppNotification(
          recipientId = authorUid,
          senderId = adminUid,
          senderUsername = "STRUGX ADMIN",
          type = "admin_announcement",
          text = "Your post was removed for violating community guidelines: $reason"
        )
      }
      logAdminAction(adminUid, "DELETE_POST", postId, "post", reason)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun adminDeleteStrug(strugId: String, reason: String, adminUid: String = ADMIN_UID): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      val doc = firestore.collection("strugs").document(strugId).get().await()
      val authorUid = doc.getString("userId") ?: ""
      firestore.collection("strugs").document(strugId).delete().await()
      if (authorUid.isNotBlank()) {
        sendAppNotification(
          recipientId = authorUid,
          senderId = adminUid,
          senderUsername = "STRUGX ADMIN",
          type = "admin_announcement",
          text = "Your Strug was removed for violating community guidelines: $reason"
        )
      }
      logAdminAction(adminUid, "DELETE_STRUG", strugId, "strug", reason)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ===================== MANUAL PLUS OVERRIDES =====================

  suspend fun adminGrantPlus(userId: String, months: Int, reason: String, adminUid: String = ADMIN_UID): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      val now = System.currentTimeMillis()
      val durationMs = months.toLong() * 30L * 24 * 60 * 60 * 1000
      val expiresAt = now + durationMs
      val plan = if (months == 6) "₹75 / 6 Months" else "₹25 / 1 Month"

      firestore.collection("users").document(userId).update(
        mapOf(
          "isPlus" to true,
          "plusPlan" to plan,
          "plusStartedAt" to now,
          "plusExpiresAt" to expiresAt
        )
      ).await()

      sendAppNotification(
        recipientId = userId,
        senderId = adminUid,
        senderUsername = "STRUGX ADMIN",
        type = "plus_approval",
        text = "Strugx Plus has been granted to your account ($plan) by administration."
      )
      logAdminAction(adminUid, "GRANT_PLUS", userId, "user", "Granted $months mo. Reason: $reason")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun adminExtendPlus(userId: String, extraDays: Long, reason: String, adminUid: String = ADMIN_UID): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      val user = getUser(userId) ?: return Result.failure(Exception("User not found"))
      val now = System.currentTimeMillis()
      val currentExpiry = if (user.plusExpiresAt > now) user.plusExpiresAt else now
      val newExpiry = currentExpiry + (extraDays * 24L * 60 * 60 * 1000)

      firestore.collection("users").document(userId).update(
        mapOf(
          "isPlus" to true,
          "plusExpiresAt" to newExpiry
        )
      ).await()

      sendAppNotification(
        recipientId = userId,
        senderId = adminUid,
        senderUsername = "STRUGX ADMIN",
        type = "plus_approval",
        text = "Your Strugx Plus subscription has been extended by $extraDays days."
      )
      logAdminAction(adminUid, "EXTEND_PLUS", userId, "user", "Extended $extraDays days. Reason: $reason")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun adminRevokePlus(userId: String, reason: String, adminUid: String = ADMIN_UID): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      firestore.collection("users").document(userId).update(
        mapOf(
          "isPlus" to false,
          "plusExpiresAt" to 0L
        )
      ).await()

      sendAppNotification(
        recipientId = userId,
        senderId = adminUid,
        senderUsername = "STRUGX ADMIN",
        type = "plus_expiry",
        text = "Your Strugx Plus subscription was revoked by administration: $reason"
      )
      logAdminAction(adminUid, "REVOKE_PLUS", userId, "user", "Reason: $reason")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ===================== PAYMENT METHODS (UPI & QR) =====================

  fun getPaymentConfigFlow(): Flow<PaymentConfig> = callbackFlow {
    val listener = firestore.collection("settings").document("paymentConfig")
      .addSnapshotListener { snapshot, error ->
        if (error != null || snapshot == null || !snapshot.exists()) {
          trySend(PaymentConfig())
          return@addSnapshotListener
        }
        trySend(snapshot.toPaymentConfigSafe())
      }
    awaitClose { listener.remove() }
  }

  suspend fun getPaymentConfig(): PaymentConfig {
    return try {
      val doc = firestore.collection("settings").document("paymentConfig").get().await()
      if (doc.exists()) doc.toPaymentConfigSafe() else PaymentConfig()
    } catch (e: Exception) {
      PaymentConfig()
    }
  }

  suspend fun updatePaymentConfig(config: PaymentConfig, adminUid: String = ADMIN_UID): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      firestore.collection("settings").document("paymentConfig").set(config.toFirestoreMap()).await()
      logAdminAction(adminUid, "UPDATE_PAYMENT_CONFIG", "paymentConfig", "settings", "UPI: ${config.upiId}, Mode: ${config.visibility}")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ===================== ADMIN AUDIT LOGS =====================

  suspend fun logAdminAction(
    adminUid: String,
    action: String,
    targetId: String,
    targetType: String,
    reason: String
  ): Result<Unit> {
    return try {
      val logId = UUID.randomUUID().toString()
      val log = AdminAuditLog(
        logId = logId,
        adminUid = adminUid,
        action = action,
        targetId = targetId,
        targetType = targetType,
        reason = reason.trim(),
        timestamp = System.currentTimeMillis()
      )
      firestore.collection("adminAuditLogs").document(logId).set(log.toFirestoreMap()).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getAdminAuditLogsFlow(limit: Long = 100): Flow<List<AdminAuditLog>> = callbackFlow {
    val listener = firestore.collection("adminAuditLogs")
      .orderBy("timestamp", Query.Direction.DESCENDING)
      .limit(limit)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(emptyList())
          return@addSnapshotListener
        }
        trySend(snapshot?.toAdminAuditLogsSafe() ?: emptyList())
      }
    awaitClose { listener.remove() }
  }

  // ===================== ANNOUNCEMENTS & BROADCAST =====================

  suspend fun broadcastAdminAnnouncement(
    title: String,
    message: String,
    adminUid: String = ADMIN_UID
  ): Result<Unit> {
    if (!isAdmin(adminUid)) return Result.failure(SecurityException("Unauthorized: Admin access required."))
    return try {
      val usersSnapshot = firestore.collection("users").limit(300).get().await()
      val batch = firestore.batch()
      val now = System.currentTimeMillis()
      val content = if (title.isNotBlank()) "[$title] $message" else message
      for (doc in usersSnapshot.documents) {
        val notifId = UUID.randomUUID().toString()
        val notif = AppNotification(
          notificationId = notifId,
          recipientId = doc.id,
          senderId = adminUid,
          senderUsername = "STRUGX ADMIN",
          senderAvatar = "",
          type = "admin_announcement",
          text = content,
          createdAt = now
        )
        batch.set(firestore.collection("notifications").document(notifId), notif.toFirestoreMap())
      }
      batch.commit().await()
      logAdminAction(adminUid, "BROADCAST_ANNOUNCEMENT", "all_users", "announcement", "$title - $message")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun sendAppNotification(
    recipientId: String,
    senderId: String,
    senderUsername: String,
    senderAvatar: String = "",
    type: String,
    text: String,
    targetId: String = ""
  ): Result<Unit> {
    return try {
      val notifId = UUID.randomUUID().toString()
      val notif = AppNotification(
        notificationId = notifId,
        recipientId = recipientId,
        senderId = senderId,
        senderUsername = senderUsername,
        senderAvatar = senderAvatar,
        type = type,
        text = text,
        targetId = targetId,
        createdAt = System.currentTimeMillis()
      )
      firestore.collection("notifications").document(notifId).set(notif.toFirestoreMap()).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // ===================== ADMIN DASHBOARD STATS =====================

  suspend fun getAdminDashboardStats(): Map<String, Long> {
    return try {
      val usersCount = firestore.collection("users").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val postsCount = firestore.collection("posts").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val strugsCount = firestore.collection("strugs").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val adsCount = firestore.collection("ads").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val reportsCount = firestore.collection("reports").whereEqualTo("status", "pending").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val pendingPayments = firestore.collection("paymentRequests").whereEqualTo("status", "pending").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val plusUsersCount = firestore.collection("users").whereEqualTo("isPlus", true).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count

      mapOf(
        "totalUsers" to usersCount,
        "totalPosts" to postsCount,
        "totalStrugs" to strugsCount,
        "totalAds" to adsCount,
        "pendingReports" to reportsCount,
        "pendingPayments" to pendingPayments,
        "plusUsers" to plusUsersCount
      )
    } catch (e: Exception) {
      emptyMap()
    }
  }

  suspend fun getAdminDetailedStats(): AdminDashboardData {
    return try {
      val totalUsers = firestore.collection("users").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val bannedUsers = firestore.collection("users").whereEqualTo("isBanned", true).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val plusUsers = firestore.collection("users").whereEqualTo("isPlus", true).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val totalPosts = firestore.collection("posts").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val textPosts = firestore.collection("posts").whereEqualTo("isTextOnly", true).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val activeStrugs = firestore.collection("strugs").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val pendingReports = firestore.collection("reports").whereEqualTo("status", "pending").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val resolvedReports = firestore.collection("reports").whereEqualTo("status", "resolved").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val pendingPayments = firestore.collection("paymentRequests").whereEqualTo("status", "pending").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val approvedPayments = firestore.collection("paymentRequests").whereEqualTo("status", "approved").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val rejectedPayments = firestore.collection("paymentRequests").whereEqualTo("status", "rejected").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val activeAds = firestore.collection("ads").whereEqualTo("status", "active").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
      val pausedAds = firestore.collection("ads").whereEqualTo("status", "paused").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count

      AdminDashboardData(
        totalUsers = totalUsers,
        bannedUsers = bannedUsers,
        plusUsers = plusUsers,
        totalPosts = totalPosts,
        textPosts = textPosts,
        imagePosts = (totalPosts - textPosts).coerceAtLeast(0),
        activeStrugs = activeStrugs,
        pendingReports = pendingReports,
        resolvedReports = resolvedReports,
        pendingPayments = pendingPayments,
        approvedPayments = approvedPayments,
        rejectedPayments = rejectedPayments,
        activeAds = activeAds,
        pausedAds = pausedAds
      )
    } catch (e: Exception) {
      AdminDashboardData()
    }
  }

  // ===================== APP UPDATE MANAGER =====================

  fun getAppUpdateConfigFlow(): Flow<AppUpdateConfig> = callbackFlow {
    val listener = firestore.collection("app_config")
      .document("app_update")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(AppUpdateConfig())
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val config = AppUpdateConfig(
            enabled = snapshot.getBoolean("enabled") ?: false,
            minVersionCode = (snapshot.getLong("minVersionCode") ?: 1L).toInt(),
            latestVersionCode = (snapshot.getLong("latestVersionCode") ?: 1L).toInt(),
            latestVersionName = snapshot.getString("latestVersionName") ?: "1.0",
            title = snapshot.getString("title") ?: "Update Available",
            description = snapshot.getString("description") ?: "A new update with exciting features and bug fixes is ready.",
            imageUrl = snapshot.getString("imageUrl") ?: "",
            updateUrl = snapshot.getString("updateUrl") ?: "",
            isForceUpdate = snapshot.getBoolean("isForceUpdate") ?: false,
            updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
          )
          trySend(config)
        } else {
          trySend(AppUpdateConfig())
        }
      }
    awaitClose { listener.remove() }
  }

  suspend fun getAppUpdateConfig(): AppUpdateConfig {
    return try {
      val snapshot = firestore.collection("app_config").document("app_update").get().await()
      if (snapshot.exists()) {
        AppUpdateConfig(
          enabled = snapshot.getBoolean("enabled") ?: false,
          minVersionCode = (snapshot.getLong("minVersionCode") ?: 1L).toInt(),
          latestVersionCode = (snapshot.getLong("latestVersionCode") ?: 1L).toInt(),
          latestVersionName = snapshot.getString("latestVersionName") ?: "1.0",
          title = snapshot.getString("title") ?: "Update Available",
          description = snapshot.getString("description") ?: "A new update with exciting features and bug fixes is ready.",
          imageUrl = snapshot.getString("imageUrl") ?: "",
          updateUrl = snapshot.getString("updateUrl") ?: "",
          isForceUpdate = snapshot.getBoolean("isForceUpdate") ?: false,
          updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
        )
      } else {
        AppUpdateConfig()
      }
    } catch (e: Exception) {
      AppUpdateConfig()
    }
  }

  suspend fun saveAppUpdateConfig(config: AppUpdateConfig, adminUid: String): Result<Unit> {
    if (!isAdmin(adminUid)) {
      return Result.failure(IllegalAccessException("Only admins can manage app update configurations"))
    }
    return try {
      firestore.collection("app_config")
        .document("app_update")
        .set(config.toFirestoreMap(), SetOptions.merge())
        .await()

      // Log audit
      logAdminAction(
        adminUid = adminUid,
        action = "APP_UPDATE_CONFIG_UPDATED",
        targetId = "app_update",
        targetType = "config",
        reason = "Set latest version to ${config.latestVersionName} (Code: ${config.latestVersionCode}, enabled: ${config.enabled}, force: ${config.isForceUpdate})"
      )
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

