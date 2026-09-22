package com.example.data.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MusicService {
  private val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  // Curated trending queries for instant suggestions
  val POPULAR_GENRES = listOf("Bollywood", "Punjabi", "Pop", "Hip-Hop", "Lo-Fi", "Romantic", "Rock")

  suspend fun searchTracks(query: String, limit: Int = 25): List<MusicTrack> = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim()
    if (cleanQuery.isEmpty()) return@withContext getTrendingTracks()

    try {
      val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
      val url = "https://itunes.apple.com/search?term=$encoded&entity=song&media=music&limit=$limit"
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "StrugxAndroidApp/1.0")
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@withContext emptyList()
        val body = response.body?.string() ?: return@withContext emptyList()
        parseTracksJson(body)
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun getTrendingTracks(): List<MusicTrack> = withContext(Dispatchers.IO) {
    try {
      val defaultTerms = listOf("Bollywood Trending", "Top Pop Hits", "Arijit Singh", "Diljit Dosanjh", "Trending Viral")
      val term = defaultTerms.random()
      val encoded = URLEncoder.encode(term, "UTF-8")
      val url = "https://itunes.apple.com/search?term=$encoded&entity=song&media=music&limit=20"
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "StrugxAndroidApp/1.0")
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@withContext fallbackTracks()
        val body = response.body?.string() ?: return@withContext fallbackTracks()
        val parsed = parseTracksJson(body)
        if (parsed.isNotEmpty()) parsed else fallbackTracks()
      }
    } catch (e: Exception) {
      fallbackTracks()
    }
  }

  private fun parseTracksJson(jsonString: String): List<MusicTrack> {
    val list = mutableListOf<MusicTrack>()
    try {
      val root = JSONObject(jsonString)
      val results = root.optJSONArray("results") ?: return emptyList()
      for (i in 0 until results.length()) {
        val item = results.getJSONObject(i)
        val previewUrl = item.optString("previewUrl")
        val trackName = item.optString("trackName")
        val artistName = item.optString("artistName")
        val trackId = item.optLong("trackId", 0L).toString()
        val collection = item.optString("collectionName")
        var artwork = item.optString("artworkUrl100")
        if (artwork.isNotBlank()) {
          // Request higher res artwork from iTunes CDN (100x100 -> 600x600)
          artwork = artwork.replace("100x100bb", "600x600bb")
        }

        if (previewUrl.isNotBlank() && trackName.isNotBlank() && artistName.isNotBlank()) {
          list.add(
            MusicTrack(
              trackId = if (trackId != "0") trackId else java.util.UUID.randomUUID().toString(),
              trackName = trackName,
              artistName = artistName,
              collectionName = collection,
              previewUrl = previewUrl,
              artworkUrl = artwork,
              durationMs = item.optLong("trackTimeMillis", 30000L)
            )
          )
        }
      }
    } catch (e: Exception) {
      // return whatever parsed so far
    }
    return list
  }

  private fun fallbackTracks(): List<MusicTrack> {
    return listOf(
      MusicTrack(
        trackId = "f1",
        trackName = "Midnight Groove",
        artistName = "Lofi Vibes",
        collectionName = "Chill Lounge",
        previewUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3",
        artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600"
      ),
      MusicTrack(
        trackId = "f2",
        trackName = "Golden Sunset",
        artistName = "Acoustic Sunset",
        collectionName = "Summer Breeze",
        previewUrl = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=sunset-vibes-10023.mp3",
        artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600"
      )
    )
  }
}
