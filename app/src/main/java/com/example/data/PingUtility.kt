package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object PingUtility {
    private const val TAG = "PingUtility"

    // Small, fast timeouts for low-overhead ping checks
    private val client = OkHttpClient.Builder()
        .connectTimeout(2000, TimeUnit.MILLISECONDS)
        .readTimeout(2000, TimeUnit.MILLISECONDS)
        .build()

    suspend fun pingUrl(url: String): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(url)
                .head() // HEAD request is super lightweight
                .build()

            client.newCall(request).execute().use { response ->
                val endTime = System.currentTimeMillis()
                if (response.isSuccessful || response.code in 200..399) {
                    return@withContext endTime - startTime
                } else {
                    // Try with GET with a small range header if HEAD is rejected
                    return@withContext pingWithGet(url)
                }
            }
        } catch (e: Exception) {
            return@withContext pingWithGet(url)
        }
    }

    private fun pingWithGet(url: String): Long {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Range", "bytes=0-0") // Request only the first byte of data
                .build()

            client.newCall(request).execute().use { response ->
                val endTime = System.currentTimeMillis()
                if (response.isSuccessful || response.code in 200..399) {
                    return endTime - startTime
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ping failed for URL: $url - Error: ${e.message}")
        }
        return -1L // Indicates offline
    }
}
