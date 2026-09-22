package com.example.data.models

data class User(
  val uid: String = "",
  val email: String = "",
  val username: String = "",
  val usernameLowercase: String = "",
  val displayName: String = "",
  val bio: String = "",
  val profileImageUrl: String = "",
  val followersCount: Long = 0,
  val followingCount: Long = 0,
  val postsCount: Long = 0,
  val createdAt: Long = System.currentTimeMillis(),
  val isPlus: Boolean = false,
  val plusPlan: String = "", // "monthly" or "six_months"
  val plusStartedAt: Long = 0,
  val plusExpiresAt: Long = 0,
  val dailyFollowCount: Long = 0,
  val dailyFollowDate: String = "", // YYYY-MM-DD
  val isBanned: Boolean = false,
  val banReason: String = "",
  val banUntil: Long = 0, // 0 = permanent if isBanned, or timestamp
  val hasCompletedOnboarding: Boolean = false
) {
  val followerCount: Long get() = followersCount
}

data class Post(
  val postId: String = "",
  val userId: String = "",
  val username: String = "",
  val userProfileImageUrl: String = "",
  val imageUrl: String = "",
  val caption: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val viewCount: Long = 0,
  val commentCount: Long = 0,
  val isTextOnly: Boolean = false,
  val musicTitle: String = "",
  val musicArtist: String = "",
  val musicAudioUrl: String = "",
  val musicCoverUrl: String = ""
)

data class Comment(
  val commentId: String = "",
  val postId: String = "",
  val userId: String = "",
  val username: String = "",
  val userProfileImageUrl: String = "",
  val text: String = "",
  val createdAt: Long = System.currentTimeMillis()
)

data class Strug(
  val strugId: String = "",
  val userId: String = "",
  val username: String = "",
  val userProfileImageUrl: String = "",
  val imageUrl: String = "",
  val caption: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val musicTitle: String = "",
  val musicArtist: String = "",
  val musicAudioUrl: String = "",
  val musicCoverUrl: String = ""
)

data class Conversation(
  val conversationId: String = "",
  val participants: List<String> = emptyList(),
  val participantUsernames: Map<String, String> = emptyMap(),
  val participantAvatars: Map<String, String> = emptyMap(),
  val participantNames: Map<String, String> = emptyMap(),
  val lastMessage: String = "",
  val lastMessageAt: Long = 0,
  val lastSenderId: String = "",
  val categories: Map<String, String> = emptyMap(), // userId -> "primary" or "general"
  val deletedFor: List<String> = emptyList() // list of userIds who deleted/hid this conversation
)

data class Message(
  val messageId: String = "",
  val conversationId: String = "",
  val senderId: String = "",
  val receiverId: String = "",
  val text: String = "",
  val sharedPostId: String = "",
  val sharedPostCaption: String = "",
  val sharedPostImage: String = "",
  val strugId: String = "",
  val strugImageUrl: String = "",
  val sharedProfileUserId: String = "",
  val sharedProfileUsername: String = "",
  val sharedProfileName: String = "",
  val sharedProfileAvatar: String = "",
  val createdAt: Long = System.currentTimeMillis()
)

data class PaymentRequest(
  val requestId: String = "",
  val userId: String = "",
  val username: String = "",
  val userEmail: String = "",
  val plan: String = "", // "₹25 / 1 Month" or "₹75 / 6 Months"
  val amount: String = "", // "25" or "75"
  val utr: String = "",
  val status: String = "pending", // "pending", "approved", "rejected"
  val adminNote: String = "",
  val submittedAt: Long = System.currentTimeMillis(),
  val reviewedAt: Long = 0,
  val reviewedBy: String = ""
)

data class Advertisement(
  val adId: String = "",
  val title: String = "",
  val description: String = "",
  val imageUrl: String = "",
  val targetUrl: String = "",
  val status: String = "active", // "active", "paused"
  val startAt: Long = 0,
  val endAt: Long = 0,
  val isPermanent: Boolean = true,
  val targetImpressions: Long = 1000,
  val currentImpressions: Long = 0,
  val clickCount: Long = 0,
  val createdAt: Long = System.currentTimeMillis()
) {
  val impressionsCount: Long get() = currentImpressions
  val clicksCount: Long get() = clickCount
  val isActive: Boolean get() = status == "active"
}

data class Report(
  val reportId: String = "",
  val reporterId: String = "",
  val reporterUsername: String = "",
  val targetType: String = "", // "post", "user", "comment", "strug", "message"
  val targetId: String = "",
  val reason: String = "",
  val details: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val status: String = "pending", // "pending", "reviewing", "resolved", "dismissed"
  val adminNote: String = "",
  val reviewedAt: Long = 0,
  val reviewedBy: String = ""
)

data class AdminAuditLog(
  val logId: String = "",
  val adminUid: String = "",
  val action: String = "", // "BAN_USER", "UNBAN_USER", "DELETE_POST", "DELETE_STRUG", "APPROVE_PAYMENT", "REJECT_PAYMENT", "GRANT_PLUS", "EXTEND_PLUS", "REVOKE_PLUS", "CREATE_AD", "EDIT_AD", "PAUSE_AD", "RESUME_AD", "DELETE_AD", "UPDATE_PAYMENT_CONFIG", "RESOLVE_REPORT", "DISMISS_REPORT"
  val targetId: String = "",
  val targetType: String = "",
  val reason: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

data class PaymentConfig(
  val upiId: String = "6375862443@ibl",
  val qrImageUrl: String = "",
  val visibility: String = "BOTH" // "QR_ONLY", "UPI_ONLY", "BOTH", "NEITHER"
)

data class AdminDashboardData(
  val totalUsers: Long = 0,
  val bannedUsers: Long = 0,
  val plusUsers: Long = 0,
  val totalPosts: Long = 0,
  val textPosts: Long = 0,
  val imagePosts: Long = 0,
  val activeStrugs: Long = 0,
  val pendingReports: Long = 0,
  val resolvedReports: Long = 0,
  val pendingPayments: Long = 0,
  val approvedPayments: Long = 0,
  val rejectedPayments: Long = 0,
  val activeAds: Long = 0,
  val pausedAds: Long = 0
)

data class AppNotification(
  val notificationId: String = "",
  val recipientId: String = "",
  val senderId: String = "",
  val senderUsername: String = "",
  val senderAvatar: String = "",
  val type: String = "", // "follow", "comment", "plus_approval", "plus_expiry", "admin_announcement"
  val text: String = "",
  val targetId: String = "", // postId if comment
  val createdAt: Long = System.currentTimeMillis(),
  val isRead: Boolean = false
)

data class AppUpdateConfig(
  val enabled: Boolean = false,
  val minVersionCode: Int = 1,
  val latestVersionCode: Int = 1,
  val latestVersionName: String = "1.0",
  val title: String = "Update Available",
  val description: String = "A new update with exciting features and bug fixes is ready.",
  val imageUrl: String = "",
  val updateUrl: String = "",
  val isForceUpdate: Boolean = false,
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun toFirestoreMap(): Map<String, Any> {
    return mapOf(
      "enabled" to enabled,
      "minVersionCode" to minVersionCode,
      "latestVersionCode" to latestVersionCode,
      "latestVersionName" to latestVersionName,
      "title" to title,
      "description" to description,
      "imageUrl" to imageUrl,
      "updateUrl" to updateUrl,
      "isForceUpdate" to isForceUpdate,
      "updatedAt" to updatedAt
    )
  }
}

