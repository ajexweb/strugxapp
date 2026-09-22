package com.example.data.firebase

import com.example.data.models.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import java.util.Date
import java.util.Locale

fun Any?.toTimestampMillis(default: Long = System.currentTimeMillis()): Long {
  return when (this) {
    is Timestamp -> this.toDate().time
    is Number -> this.toLong()
    is Date -> this.time
    is String -> this.toLongOrNull() ?: default
    else -> default
  }
}

fun DocumentSnapshot.getLongOrTimestamp(field: String, default: Long = 0L): Long {
  val value = get(field) ?: return default
  return value.toTimestampMillis(default)
}

fun DocumentSnapshot.getStringOrEmpty(field: String, default: String = ""): String {
  return getString(field) ?: get(field)?.toString() ?: default
}

fun DocumentSnapshot.getBooleanSafe(field: String, default: Boolean = false): Boolean {
  return getBoolean(field) ?: when (val v = get(field)) {
    is Boolean -> v
    is Number -> v.toInt() != 0
    is String -> v.toBoolean()
    else -> default
  }
}

fun DocumentSnapshot.toUserSafe(): User {
  val rawUsername = getStringOrEmpty("username")
  val rawUsernameLower = getStringOrEmpty("usernameLowercase")
  val resolvedUsernameLower = if (rawUsernameLower.isNotBlank()) rawUsernameLower else rawUsername.lowercase(Locale.ROOT)

  return User(
    uid = getStringOrEmpty("uid", id),
    email = getStringOrEmpty("email"),
    username = rawUsername,
    usernameLowercase = resolvedUsernameLower,
    displayName = getStringOrEmpty("displayName"),
    bio = getStringOrEmpty("bio"),
    profileImageUrl = getStringOrEmpty("profileImageUrl"),
    followersCount = getLongOrTimestamp("followersCount", 0L),
    followingCount = getLongOrTimestamp("followingCount", 0L),
    postsCount = getLongOrTimestamp("postsCount", 0L),
    createdAt = getLongOrTimestamp("createdAt", System.currentTimeMillis()),
    isPlus = getBooleanSafe("isPlus", false),
    plusPlan = getStringOrEmpty("plusPlan"),
    plusStartedAt = getLongOrTimestamp("plusStartedAt", 0L),
    plusExpiresAt = getLongOrTimestamp("plusExpiresAt", 0L),
    dailyFollowCount = getLongOrTimestamp("dailyFollowCount", 0L),
    dailyFollowDate = getStringOrEmpty("dailyFollowDate"),
    isBanned = getBooleanSafe("isBanned", false),
    banReason = getStringOrEmpty("banReason"),
    banUntil = getLongOrTimestamp("banUntil", 0L),
    hasCompletedOnboarding = getBooleanSafe("hasCompletedOnboarding", false)
  )
}

fun DocumentSnapshot.toPostSafe(): Post {
  return Post(
    postId = getStringOrEmpty("postId", id),
    userId = getStringOrEmpty("userId"),
    username = getStringOrEmpty("username"),
    userProfileImageUrl = getStringOrEmpty("userProfileImageUrl"),
    imageUrl = getStringOrEmpty("imageUrl"),
    caption = getStringOrEmpty("caption"),
    createdAt = getLongOrTimestamp("createdAt", System.currentTimeMillis()),
    viewCount = getLongOrTimestamp("viewCount", 0L),
    commentCount = getLongOrTimestamp("commentCount", 0L),
    isTextOnly = getBooleanSafe("isTextOnly", false),
    musicTitle = getStringOrEmpty("musicTitle"),
    musicArtist = getStringOrEmpty("musicArtist"),
    musicAudioUrl = getStringOrEmpty("musicAudioUrl"),
    musicCoverUrl = getStringOrEmpty("musicCoverUrl")
  )
}

fun DocumentSnapshot.toCommentSafe(): Comment {
  return Comment(
    commentId = getStringOrEmpty("commentId", id),
    postId = getStringOrEmpty("postId"),
    userId = getStringOrEmpty("userId"),
    username = getStringOrEmpty("username"),
    userProfileImageUrl = getStringOrEmpty("userProfileImageUrl"),
    text = getStringOrEmpty("text").ifBlank { getStringOrEmpty("content") },
    createdAt = getLongOrTimestamp("createdAt", System.currentTimeMillis())
  )
}

fun DocumentSnapshot.toStrugSafe(): Strug {
  return Strug(
    strugId = getStringOrEmpty("strugId", id),
    userId = getStringOrEmpty("userId"),
    username = getStringOrEmpty("username"),
    userProfileImageUrl = getStringOrEmpty("userProfileImageUrl"),
    imageUrl = getStringOrEmpty("imageUrl"),
    caption = getStringOrEmpty("caption"),
    createdAt = getLongOrTimestamp("createdAt", System.currentTimeMillis()),
    musicTitle = getStringOrEmpty("musicTitle"),
    musicArtist = getStringOrEmpty("musicArtist"),
    musicAudioUrl = getStringOrEmpty("musicAudioUrl"),
    musicCoverUrl = getStringOrEmpty("musicCoverUrl")
  )
}

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toConversationSafe(): Conversation {
  val participantsList = (get("participants") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
  val usernamesMap = (get("participantUsernames") as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
  val avatarsMap = (get("participantAvatars") as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
  val namesMap = (get("participantNames") as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
  val categoriesMap = (get("categories") as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
  val deletedForList = (get("deletedFor") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

  return Conversation(
    conversationId = getStringOrEmpty("conversationId", id),
    participants = participantsList,
    participantUsernames = usernamesMap,
    participantAvatars = avatarsMap,
    participantNames = namesMap,
    lastMessage = getStringOrEmpty("lastMessage"),
    lastMessageAt = getLongOrTimestamp("lastMessageAt", 0L),
    lastSenderId = getStringOrEmpty("lastSenderId"),
    categories = categoriesMap,
    deletedFor = deletedForList
  )
}

fun DocumentSnapshot.toMessageSafe(): Message {
  return Message(
    messageId = getStringOrEmpty("messageId", id),
    conversationId = getStringOrEmpty("conversationId"),
    senderId = getStringOrEmpty("senderId"),
    receiverId = getStringOrEmpty("receiverId"),
    text = getStringOrEmpty("text"),
    sharedPostId = getStringOrEmpty("sharedPostId"),
    sharedPostCaption = getStringOrEmpty("sharedPostCaption"),
    sharedPostImage = getStringOrEmpty("sharedPostImage"),
    strugId = getStringOrEmpty("strugId"),
    strugImageUrl = getStringOrEmpty("strugImageUrl"),
    sharedProfileUserId = getStringOrEmpty("sharedProfileUserId"),
    sharedProfileUsername = getStringOrEmpty("sharedProfileUsername"),
    sharedProfileName = getStringOrEmpty("sharedProfileName"),
    sharedProfileAvatar = getStringOrEmpty("sharedProfileAvatar"),
    createdAt = getLongOrTimestamp("createdAt", System.currentTimeMillis())
  )
}

fun DocumentSnapshot.toPaymentRequestSafe(): PaymentRequest {
  return PaymentRequest(
    requestId = getStringOrEmpty("requestId", id),
    userId = getStringOrEmpty("userId"),
    username = getStringOrEmpty("username"),
    userEmail = getStringOrEmpty("userEmail"),
    plan = getStringOrEmpty("plan"),
    amount = getStringOrEmpty("amount"),
    utr = getStringOrEmpty("utr"),
    status = getStringOrEmpty("status", "pending"),
    adminNote = getStringOrEmpty("adminNote"),
    submittedAt = getLongOrTimestamp("submittedAt", System.currentTimeMillis()),
    reviewedAt = getLongOrTimestamp("reviewedAt", 0L),
    reviewedBy = getStringOrEmpty("reviewedBy")
  )
}

fun DocumentSnapshot.toAdvertisementSafe(): Advertisement {
  return Advertisement(
    adId = getStringOrEmpty("adId", id),
    title = getStringOrEmpty("title"),
    description = getStringOrEmpty("description"),
    imageUrl = getStringOrEmpty("imageUrl"),
    targetUrl = getStringOrEmpty("targetUrl"),
    status = getStringOrEmpty("status", "active"),
    startAt = getLongOrTimestamp("startAt", 0L),
    endAt = getLongOrTimestamp("endAt", 0L),
    isPermanent = getBooleanSafe("isPermanent", true),
    targetImpressions = getLongOrTimestamp("targetImpressions", 1000L),
    currentImpressions = getLongOrTimestamp("currentImpressions", 0L),
    clickCount = getLongOrTimestamp("clickCount", 0L),
    createdAt = getLongOrTimestamp("createdAt", System.currentTimeMillis())
  )
}

fun DocumentSnapshot.toReportSafe(): Report {
  return Report(
    reportId = getStringOrEmpty("reportId", id),
    reporterId = getStringOrEmpty("reporterId"),
    reporterUsername = getStringOrEmpty("reporterUsername"),
    targetType = getStringOrEmpty("targetType"),
    targetId = getStringOrEmpty("targetId"),
    reason = getStringOrEmpty("reason"),
    details = getStringOrEmpty("details"),
    createdAt = getLongOrTimestamp("createdAt", System.currentTimeMillis()),
    status = getStringOrEmpty("status", "pending"),
    adminNote = getStringOrEmpty("adminNote"),
    reviewedAt = getLongOrTimestamp("reviewedAt", 0L),
    reviewedBy = getStringOrEmpty("reviewedBy")
  )
}

fun DocumentSnapshot.toAdminAuditLogSafe(): AdminAuditLog {
  return AdminAuditLog(
    logId = getStringOrEmpty("logId", id),
    adminUid = getStringOrEmpty("adminUid"),
    action = getStringOrEmpty("action"),
    targetId = getStringOrEmpty("targetId"),
    targetType = getStringOrEmpty("targetType"),
    reason = getStringOrEmpty("reason"),
    timestamp = getLongOrTimestamp("timestamp", System.currentTimeMillis())
  )
}

fun DocumentSnapshot.toPaymentConfigSafe(): PaymentConfig {
  return PaymentConfig(
    upiId = getStringOrEmpty("upiId", "6375862443@ibl"),
    qrImageUrl = getStringOrEmpty("qrImageUrl"),
    visibility = getStringOrEmpty("visibility", "BOTH")
  )
}

fun QuerySnapshot?.toAdminAuditLogsSafe(): List<AdminAuditLog> =
  this?.documents?.mapNotNull { doc -> try { doc.toAdminAuditLogSafe() } catch (e: Exception) { null } } ?: emptyList()

fun DocumentSnapshot.toNotificationSafe(): AppNotification {
  return AppNotification(
    notificationId = getStringOrEmpty("notificationId", id),
    recipientId = getStringOrEmpty("recipientId"),
    senderId = getStringOrEmpty("senderId"),
    senderUsername = getStringOrEmpty("senderUsername"),
    senderAvatar = getStringOrEmpty("senderAvatar"),
    type = getStringOrEmpty("type"),
    text = getStringOrEmpty("text"),
    targetId = getStringOrEmpty("targetId"),
    createdAt = getLongOrTimestamp("createdAt", System.currentTimeMillis()),
    isRead = getBoolean("isRead") ?: false
  )
}

fun QuerySnapshot?.toPostsSafe(): List<Post> =
  this?.documents?.mapNotNull { doc -> try { doc.toPostSafe() } catch (e: Exception) { null } } ?: emptyList()

fun QuerySnapshot?.toUsersSafe(): List<User> =
  this?.documents?.mapNotNull { doc -> try { doc.toUserSafe() } catch (e: Exception) { null } } ?: emptyList()

fun QuerySnapshot?.toCommentsSafe(): List<Comment> =
  this?.documents?.mapNotNull { doc -> try { doc.toCommentSafe() } catch (e: Exception) { null } } ?: emptyList()

fun QuerySnapshot?.toStrugsSafe(): List<Strug> =
  this?.documents?.mapNotNull { doc -> try { doc.toStrugSafe() } catch (e: Exception) { null } } ?: emptyList()

fun QuerySnapshot?.toConversationsSafe(): List<Conversation> =
  this?.documents?.mapNotNull { doc -> try { doc.toConversationSafe() } catch (e: Exception) { null } } ?: emptyList()

fun QuerySnapshot?.toMessagesSafe(): List<Message> =
  this?.documents?.mapNotNull { doc -> try { doc.toMessageSafe() } catch (e: Exception) { null } } ?: emptyList()

fun QuerySnapshot?.toPaymentRequestsSafe(): List<PaymentRequest> =
  this?.documents?.mapNotNull { doc -> try { doc.toPaymentRequestSafe() } catch (e: Exception) { null } } ?: emptyList()

fun QuerySnapshot?.toAdvertisementsSafe(): List<Advertisement> =
  this?.documents?.mapNotNull { doc -> try { doc.toAdvertisementSafe() } catch (e: Exception) { null } } ?: emptyList()

fun QuerySnapshot?.toReportsSafe(): List<Report> =
  this?.documents?.mapNotNull { doc -> try { doc.toReportSafe() } catch (e: Exception) { null } } ?: emptyList()

fun QuerySnapshot?.toNotificationsSafe(): List<AppNotification> =
  this?.documents?.mapNotNull { doc -> try { doc.toNotificationSafe() } catch (e: Exception) { null } } ?: emptyList()

// Writing to Firestore helpers with Timestamp
fun Post.toFirestoreMap(): Map<String, Any> = mapOf(
  "postId" to postId,
  "userId" to userId,
  "username" to username,
  "userProfileImageUrl" to userProfileImageUrl,
  "imageUrl" to imageUrl,
  "caption" to caption,
  "createdAt" to Timestamp(Date(createdAt)),
  "viewCount" to viewCount,
  "commentCount" to commentCount,
  "isTextOnly" to isTextOnly,
  "musicTitle" to musicTitle,
  "musicArtist" to musicArtist,
  "musicAudioUrl" to musicAudioUrl,
  "musicCoverUrl" to musicCoverUrl
)

fun Strug.toFirestoreMap(): Map<String, Any> = mapOf(
  "strugId" to strugId,
  "userId" to userId,
  "username" to username,
  "userProfileImageUrl" to userProfileImageUrl,
  "imageUrl" to imageUrl,
  "caption" to caption,
  "createdAt" to Timestamp(Date(createdAt)),
  "musicTitle" to musicTitle,
  "musicArtist" to musicArtist,
  "musicAudioUrl" to musicAudioUrl,
  "musicCoverUrl" to musicCoverUrl
)

fun Comment.toFirestoreMap(): Map<String, Any> = mapOf(
  "commentId" to commentId,
  "postId" to postId,
  "userId" to userId,
  "username" to username,
  "userProfileImageUrl" to userProfileImageUrl,
  "text" to text,
  "createdAt" to Timestamp(Date(createdAt))
)

fun User.toFirestoreMap(): Map<String, Any> = mapOf(
  "uid" to uid,
  "email" to email,
  "username" to username,
  "usernameLowercase" to usernameLowercase,
  "displayName" to displayName,
  "bio" to bio,
  "profileImageUrl" to profileImageUrl,
  "followersCount" to followersCount,
  "followingCount" to followingCount,
  "postsCount" to postsCount,
  "createdAt" to Timestamp(Date(createdAt)),
  "isPlus" to isPlus,
  "plusPlan" to plusPlan,
  "plusStartedAt" to plusStartedAt,
  "plusExpiresAt" to plusExpiresAt,
  "dailyFollowCount" to dailyFollowCount,
  "dailyFollowDate" to dailyFollowDate,
  "isBanned" to isBanned,
  "banReason" to banReason,
  "banUntil" to banUntil,
  "hasCompletedOnboarding" to hasCompletedOnboarding
)

fun Message.toFirestoreMap(): Map<String, Any> = mapOf(
  "messageId" to messageId,
  "conversationId" to conversationId,
  "senderId" to senderId,
  "receiverId" to receiverId,
  "text" to text,
  "sharedPostId" to sharedPostId,
  "sharedPostCaption" to sharedPostCaption,
  "sharedPostImage" to sharedPostImage,
  "strugId" to strugId,
  "strugImageUrl" to strugImageUrl,
  "sharedProfileUserId" to sharedProfileUserId,
  "sharedProfileUsername" to sharedProfileUsername,
  "sharedProfileName" to sharedProfileName,
  "sharedProfileAvatar" to sharedProfileAvatar,
  "createdAt" to Timestamp(Date(createdAt))
)

fun PaymentRequest.toFirestoreMap(): Map<String, Any> = mapOf(
  "requestId" to requestId,
  "userId" to userId,
  "username" to username,
  "userEmail" to userEmail,
  "plan" to plan,
  "amount" to amount,
  "utr" to utr,
  "status" to status,
  "adminNote" to adminNote,
  "submittedAt" to Timestamp(Date(submittedAt)),
  "reviewedAt" to reviewedAt,
  "reviewedBy" to reviewedBy
)

fun Advertisement.toFirestoreMap(): Map<String, Any> = mapOf(
  "adId" to adId,
  "title" to title,
  "description" to description,
  "imageUrl" to imageUrl,
  "targetUrl" to targetUrl,
  "status" to status,
  "startAt" to startAt,
  "endAt" to endAt,
  "isPermanent" to isPermanent,
  "targetImpressions" to targetImpressions,
  "currentImpressions" to currentImpressions,
  "clickCount" to clickCount,
  "createdAt" to Timestamp(Date(createdAt))
)

fun Report.toFirestoreMap(): Map<String, Any> = mapOf(
  "reportId" to reportId,
  "reporterId" to reporterId,
  "reporterUsername" to reporterUsername,
  "targetType" to targetType,
  "targetId" to targetId,
  "reason" to reason,
  "details" to details,
  "createdAt" to Timestamp(Date(createdAt)),
  "status" to status,
  "adminNote" to adminNote,
  "reviewedAt" to reviewedAt,
  "reviewedBy" to reviewedBy
)

fun AdminAuditLog.toFirestoreMap(): Map<String, Any> = mapOf(
  "logId" to logId,
  "adminUid" to adminUid,
  "action" to action,
  "targetId" to targetId,
  "targetType" to targetType,
  "reason" to reason,
  "timestamp" to Timestamp(Date(timestamp))
)

fun PaymentConfig.toFirestoreMap(): Map<String, Any> = mapOf(
  "upiId" to upiId,
  "qrImageUrl" to qrImageUrl,
  "visibility" to visibility
)

fun AppNotification.toFirestoreMap(): Map<String, Any> = mapOf(
  "notificationId" to notificationId,
  "recipientId" to recipientId,
  "senderId" to senderId,
  "senderUsername" to senderUsername,
  "senderAvatar" to senderAvatar,
  "type" to type,
  "text" to text,
  "targetId" to targetId,
  "createdAt" to Timestamp(Date(createdAt)),
  "isRead" to isRead
)
