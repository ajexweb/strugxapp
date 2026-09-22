package com.example.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class CachedUserProfile(
  val userId: String,
  val username: String,
  val displayName: String,
  val avatarUrl: String
)

object UserProfileCache {
  private val cache = ConcurrentHashMap<String, CachedUserProfile>()
  private val _version = MutableStateFlow(0L)
  val version: StateFlow<Long> = _version.asStateFlow()

  fun update(userId: String, username: String, displayName: String = "", avatarUrl: String = "") {
    if (userId.isBlank()) return
    cache[userId] = CachedUserProfile(
      userId = userId,
      username = username,
      displayName = displayName,
      avatarUrl = avatarUrl
    )
    _version.value = System.currentTimeMillis()
  }

  fun get(userId: String): CachedUserProfile? = cache[userId]

  fun getUsername(userId: String, fallback: String): String {
    return cache[userId]?.username?.ifBlank { null } ?: fallback
  }

  fun getAvatarUrl(userId: String, fallback: String): String {
    return cache[userId]?.avatarUrl?.ifBlank { null } ?: fallback
  }
}
