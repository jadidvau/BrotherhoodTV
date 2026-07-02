package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader

object M3uParser {
    private const val TAG = "M3uParser"
    private val client = OkHttpClient()

    // Regex patterns for M3U tags
    private val tvgLogoRegex = """tvg-logo="([^"]+)"""".toRegex()
    private val logoRegex = """logo="([^"]+)"""".toRegex()
    private val groupTitleRegex = """group-title="([^"]+)"""".toRegex()
    private val tvgNameRegex = """tvg-name="([^"]+)"""".toRegex()

    suspend fun parseFromUrl(playlistUrl: String, playlistId: Int? = null): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        try {
            val request = Request.Builder()
                .url(playlistUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to download M3U playlist: $response")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                return@withContext parseText(bodyString, playlistId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading or parsing playlist from URL: $playlistUrl", e)
            emptyList()
        }
    }

    fun parseText(m3uText: String, playlistId: Int? = null): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val reader = BufferedReader(StringReader(m3uText))
            var line: String?
            var currentMetadata: String? = null

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("#EXTM3U")) {
                    continue
                }

                if (trimmed.startsWith("#EXTINF:")) {
                    currentMetadata = trimmed
                } else if (!trimmed.startsWith("#")) {
                    // This is a stream URL line
                    val streamUrl = trimmed
                    if (currentMetadata != null) {
                        val channel = parseExtInf(currentMetadata, streamUrl, playlistId)
                        channels.add(channel)
                        currentMetadata = null
                    } else {
                        // Fallback: URL with no preceding EXTINF
                        val name = streamUrl.substringAfterLast("/").substringBefore("?")
                        channels.add(
                            Channel(
                                name = if (name.isNotEmpty()) name else "Unnamed Stream",
                                url = streamUrl,
                                category = "General",
                                playlistId = playlistId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing M3U text", e)
        }
        return channels
    }

    private fun parseExtInf(metadataLine: String, streamUrl: String, playlistId: Int? = null): Channel {
        // Extract logo url
        val logoUrl = tvgLogoRegex.find(metadataLine)?.groupValues?.get(1)
            ?: logoRegex.find(metadataLine)?.groupValues?.get(1)

        // Extract category
        val category = groupTitleRegex.find(metadataLine)?.groupValues?.get(1) ?: "Live TV"

        // Extract name
        val tvgName = tvgNameRegex.find(metadataLine)?.groupValues?.get(1)
        
        // Name is usually after the last comma of the #EXTINF line
        val commaIndex = metadataLine.lastIndexOf(',')
        val displayName = if (commaIndex != -1 && commaIndex < metadataLine.length - 1) {
            metadataLine.substring(commaIndex + 1).trim()
        } else {
            tvgName ?: streamUrl.substringAfterLast("/").substringBefore("?")
        }

        return Channel(
            name = if (displayName.isNotEmpty()) displayName else "Unknown Channel",
            url = streamUrl,
            logoUrl = logoUrl,
            category = category,
            playlistId = playlistId
        )
    }
}
