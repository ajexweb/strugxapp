package com.example.data.music

data class MusicTrack(
  val trackId: String,
  val trackName: String,
  val artistName: String,
  val collectionName: String = "",
  val previewUrl: String,
  val artworkUrl: String,
  val durationMs: Long = 30000L
)
