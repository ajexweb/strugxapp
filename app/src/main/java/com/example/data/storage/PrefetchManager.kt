package com.example.data.storage

import android.content.Context
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PrefetchManager {
  private val prefetchedUrls = java.util.Collections.synchronizedSet(LinkedHashSet<String>())
  private const val MAX_PREFETCH_CACHE = 500

  /**
   * Intelligently prefetches the current image + next [count] images in the background
   * ahead of the user's visible position. Caches in memory and on disk.
   */
  fun prefetchNextImages(
    context: Context,
    imageUrls: List<String>,
    currentIndex: Int,
    count: Int = 4
  ) {
    if (imageUrls.isEmpty() || currentIndex < 0) return

    val startIndex = maxOf(0, currentIndex)
    val endIndex = minOf(currentIndex + 1 + count, imageUrls.size)

    if (startIndex >= imageUrls.size) return

    val targetBatch = imageUrls.subList(startIndex, endIndex)
    val imageLoader = coil.Coil.imageLoader(context)

    CoroutineScope(Dispatchers.IO).launch {
      for (url in targetBatch) {
        if (url.isNotBlank() && !prefetchedUrls.contains(url)) {
          if (prefetchedUrls.size >= MAX_PREFETCH_CACHE) {
            val iterator = prefetchedUrls.iterator()
            if (iterator.hasNext()) {
              iterator.next()
              iterator.remove()
            }
          }
          prefetchedUrls.add(url)
          val request = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
          imageLoader.enqueue(request)
        }
      }
    }
  }
}
