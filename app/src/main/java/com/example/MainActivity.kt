package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.data.firebase.FirebaseService
import com.example.data.music.GlobalAudioPlayer
import com.example.ui.StrugxApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK),
      navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
    )
    FirebaseService.init(applicationContext)

    // Configure fast, optimized image loading with Memory & Disk caching
    val imageLoader = ImageLoader.Builder(applicationContext)
      .memoryCache {
        MemoryCache.Builder(applicationContext)
          .maxSizePercent(0.30) // Up to 30% available memory for instant image rendering
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(applicationContext.cacheDir.resolve("image_cache"))
          .maxSizeBytes(120L * 1024 * 1024) // 120MB disk cache
          .build()
      }
      .crossfade(true)
      .respectCacheHeaders(false)
      .build()
    Coil.setImageLoader(imageLoader)

    setContent {
      MyApplicationTheme {
        StrugxApp()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    GlobalAudioPlayer.stop()
  }
}

