package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageOptimizer {

  suspend fun compressAndValidate(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1280,
    quality: Int = 85
  ): Result<ByteArray> = withContext(Dispatchers.IO) {
    try {
      // Validate MIME type
      val mimeType = context.contentResolver.getType(uri) ?: ""
      if (mimeType.startsWith("video/")) {
        return@withContext Result.failure(IllegalArgumentException("Videos are not allowed on Strugx. Please select an image."))
      }

      // Read bounds first
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
      BitmapFactory.decodeStream(inputStream, null, options)
      inputStream?.close()

      if (options.outWidth <= 0 || options.outHeight <= 0) {
        return@withContext Result.failure(IllegalArgumentException("Invalid image file."))
      }

      // Calculate sample size
      var sampleSize = 1
      var width = options.outWidth
      var height = options.outHeight
      while (width / 2 >= maxDimension || height / 2 >= maxDimension) {
        width /= 2
        height /= 2
        sampleSize *= 2
      }

      // Decode downsampled bitmap
      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565
      }
      inputStream = context.contentResolver.openInputStream(uri)
      val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
      inputStream?.close()

      if (bitmap == null) {
        return@withContext Result.failure(IllegalStateException("Failed to decode image."))
      }

      // Scale proportionally if still exceeds max dimension
      val scale = minOf(
        maxDimension.toFloat() / bitmap.width.coerceAtLeast(1),
        maxDimension.toFloat() / bitmap.height.coerceAtLeast(1),
        1.0f
      )
      val scaledBitmap = if (scale < 1.0f) {
        val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
      } else {
        bitmap
      }

      // Compress to JPEG / WebP
      val outputStream = ByteArrayOutputStream()
      scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
      val bytes = outputStream.toByteArray()

      if (scaledBitmap != bitmap) {
        scaledBitmap.recycle()
      }
      bitmap.recycle()

      Result.success(bytes)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
