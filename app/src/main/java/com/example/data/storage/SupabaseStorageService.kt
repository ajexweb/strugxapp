package com.example.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

object SupabaseStorageService {
  private const val SUPABASE_PROJECT_URL = "https://pyubxghaghyiucytnfvu.supabase.co"
  private const val BUCKET_NAME = "zoo"
  private const val PUBLISHABLE_KEY = "sb_publishable_xxDY7I7FGNRumDwhigEPGQ_IvFxbVZi"

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  /**
   * Uploads compressed image bytes to Supabase Storage in the 'zoo' bucket.
   * Path convention: folder/{userId}_{uuid}.jpg
   * Returns the public CDN URL on success.
   */
  suspend fun uploadImage(
    bytes: ByteArray,
    folder: String,
    userId: String
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val fileName = "${folder}/${userId}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
      val uploadUrl = "$SUPABASE_PROJECT_URL/storage/v1/object/$BUCKET_NAME/$fileName"

      val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())

      val request = Request.Builder()
        .url(uploadUrl)
        .addHeader("apikey", PUBLISHABLE_KEY)
        .addHeader("Authorization", "Bearer $PUBLISHABLE_KEY")
        .addHeader("Content-Type", "image/jpeg")
        .addHeader("x-upsert", "true")
        .post(requestBody)
        .build()

      val response = client.newCall(request).execute()
      val responseBody = response.body?.string() ?: ""

      if (response.isSuccessful) {
        val publicUrl = "$SUPABASE_PROJECT_URL/storage/v1/object/public/$BUCKET_NAME/$fileName"
        Result.success(publicUrl)
      } else {
        Result.failure(IOException("Storage upload failed (${response.code}): $responseBody"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
