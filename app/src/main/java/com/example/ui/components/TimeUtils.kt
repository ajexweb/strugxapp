package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRelativeTime(timestamp: Long): String {
  val diff = System.currentTimeMillis() - timestamp
  val seconds = diff / 1000
  val minutes = seconds / 60
  val hours = minutes / 60
  val days = hours / 24

  return when {
    seconds < 60 -> "Just now"
    minutes < 60 -> "${minutes}m ago"
    hours < 24 -> "${hours}h ago"
    days < 7 -> "${days}d ago"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
  }
}
