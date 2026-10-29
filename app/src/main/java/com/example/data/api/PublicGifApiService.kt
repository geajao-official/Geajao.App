package com.example.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class PublicGifItem(
    val id: String,
    val title: String,
    val gifUrl: String,
    val previewUrl: String,
    val tag: String,
    val fallbackEmoji: String = "🎬"
) {
    /**
     * Serializes the GIF item into a compact string stored in `mediaDataBase64` when `mediaType == "GIF"`.
     */
    fun toMediaPayload(): String {
        val safeTitle = title.replace("|", " ").take(60).ifBlank { "Reaction GIF" }
        val safeTag = tag.replace("|", " ").take(24).ifBlank { "GIF" }
        return if (gifUrl.isNotBlank()) {
            "URL|$gifUrl|$safeTitle|$safeTag|$fallbackEmoji"
        } else {
            id
        }
    }

    companion object {
        fun fromMediaPayload(payload: String): PublicGifItem? {
            if (payload.startsWith("URL|")) {
                val parts = payload.split("|")
                val url = parts.getOrNull(1).orEmpty()
                val title = parts.getOrNull(2).orEmpty().ifBlank { "Animated GIF" }
                val tag = parts.getOrNull(3).orEmpty().ifBlank { "LIVE" }
                val emoji = parts.getOrNull(4).orEmpty().ifBlank { "🎬" }
                return PublicGifItem(
                    id = url,
                    title = title,
                    gifUrl = url,
                    previewUrl = url,
                    tag = tag,
                    fallbackEmoji = emoji
                )
            }
            if (payload.startsWith("https://") || payload.startsWith("http://")) {
                return PublicGifItem(
                    id = payload,
                    title = "Animated Reaction",
                    gifUrl = payload,
                    previewUrl = payload,
                    tag = "GIF",
                    fallbackEmoji = "🎬"
                )
            }
            return null
        }
    }
}

object PublicGifApiService {
    private const val TAG = "PublicGifApiService"

    // Public Tenor v1 anonymous discovery key for open client GIF search & trending reactions
    private const val TENOR_BASE_URL = "https://g.tenor.com/v1"
    private const val TENOR_PUBLIC_KEY = "LIVDSRZULELA"

    val REACTION_CATEGORIES = listOf(
        "Trending" to "",
        "Salute" to "military salute respect",
        "Celebrate" to "celebrate victory cheer",
        "Respect" to "respect applause",
        "Laugh" to "laughing funny reaction",
        "Thumbs Up" to "thumbs up approved",
        "Bravo" to "bravo well done",
        "Workout" to "pushups fitness training"
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchGifs(query: String, limit: Int = 24): Result<List<PublicGifItem>> =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = query.trim()
                val endpoint = if (trimmed.isEmpty()) {
                    "$TENOR_BASE_URL/trending?key=$TENOR_PUBLIC_KEY&limit=$limit&media_filter=minimal&contentfilter=high"
                } else {
                    val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
                    "$TENOR_BASE_URL/search?q=$encodedQuery&key=$TENOR_PUBLIC_KEY&limit=$limit&media_filter=minimal&contentfilter=high"
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Public GIF API returned HTTP ${response.code}, falling back to curated GIFs")
                        return@withContext Result.success(filterCuratedFallback(trimmed))
                    }

                    val bodyStr = response.body?.string().orEmpty()
                    if (bodyStr.isBlank()) {
                        return@withContext Result.success(filterCuratedFallback(trimmed))
                    }

                    val parsed = parseTenorResponse(bodyStr, trimmed.ifBlank { "TRENDING" })
                    if (parsed.isNotEmpty()) {
                        Result.success(parsed)
                    } else {
                        Result.success(filterCuratedFallback(trimmed))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Public GIF API fetch failed, using fallback reaction GIFs", e)
                Result.success(filterCuratedFallback(query))
            }
        }

    private fun parseTenorResponse(jsonStr: String, defaultTag: String): List<PublicGifItem> {
        val items = mutableListOf<PublicGifItem>()
        val root = JSONObject(jsonStr)
        val results = root.optJSONArray("results") ?: return emptyList()

        for (i in 0 until results.length()) {
            val itemObj = results.optJSONObject(i) ?: continue
            val id = itemObj.optString("id", "gif_$i")
            val rawTitle = itemObj.optString("title")
                .ifBlank { itemObj.optString("content_description") }
                .ifBlank { itemObj.optString("h1_title") }
                .replace(" GIF", "", ignoreCase = true)
                .trim()
                .ifBlank { "Expressive Reaction #${i + 1}" }

            val mediaArray = itemObj.optJSONArray("media")
            val firstMedia = mediaArray?.optJSONObject(0)
            val tinyGifObj = firstMedia?.optJSONObject("tinygif")
            val gifObj = firstMedia?.optJSONObject("gif")
            val mediumGifObj = firstMedia?.optJSONObject("mediumgif")

            val previewUrl = tinyGifObj?.optString("url")
                ?.ifBlank { mediumGifObj?.optString("url").orEmpty() }
                ?.ifBlank { gifObj?.optString("url").orEmpty() }
                .orEmpty()

            val fullGifUrl = tinyGifObj?.optString("url")
                ?.ifBlank { gifObj?.optString("url").orEmpty() }
                .orEmpty()

            if (fullGifUrl.isNotBlank()) {
                items.add(
                    PublicGifItem(
                        id = id,
                        title = rawTitle.take(45),
                        gifUrl = fullGifUrl,
                        previewUrl = previewUrl.ifBlank { fullGifUrl },
                        tag = defaultTag.uppercase().take(16),
                        fallbackEmoji = pickEmojiForTitle(rawTitle)
                    )
                )
            }
        }
        return items
    }

    private fun pickEmojiForTitle(title: String): String {
        val lower = title.lowercase()
        return when {
            "salute" in lower || "respect" in lower -> "🫡"
            "celebrat" in lower || "party" in lower || "cheer" in lower || "win" in lower -> "🎉"
            "laugh" in lower || "lol" in lower || "haha" in lower || "funny" in lower -> "😂"
            "thumb" in lower || "approve" in lower || "good" in lower -> "👍"
            "clap" in lower || "applause" in lower || "bravo" in lower -> "👏"
            "fire" in lower || "lit" in lower -> "🔥"
            else -> "🎬"
        }
    }

    private fun filterCuratedFallback(query: String): List<PublicGifItem> {
        val all = listOf(
            PublicGifItem(
                id = "GIF_SALUTE_RESPECT",
                title = "Sharp Cadet Salute",
                gifUrl = "https://media.tenor.com/images/2b633c18a8f4162c8e8d31d198a3b45f/tenor.gif",
                previewUrl = "https://media.tenor.com/images/2b633c18a8f4162c8e8d31d198a3b45f/tenor.gif",
                tag = "SALUTE",
                fallbackEmoji = "🫡"
            ),
            PublicGifItem(
                id = "GIF_VICTORY_CHEER",
                title = "Squadron Victory Cheer",
                gifUrl = "https://media.tenor.com/images/0f3b1e5f7c91e6e1d3b5c49d29f2304c/tenor.gif",
                previewUrl = "https://media.tenor.com/images/0f3b1e5f7c91e6e1d3b5c49d29f2304c/tenor.gif",
                tag = "CELEBRATE",
                fallbackEmoji = "🏆"
            ),
            PublicGifItem(
                id = "GIF_MARCHING_DRILL",
                title = "Synchronized Drill March",
                gifUrl = "https://media.tenor.com/images/7a11f48e6f402e23594a64891f0a1234/tenor.gif",
                previewUrl = "https://media.tenor.com/images/7a11f48e6f402e23594a64891f0a1234/tenor.gif",
                tag = "DRILL",
                fallbackEmoji = "🥁"
            ),
            PublicGifItem(
                id = "GIF_MISSION_GO",
                title = "Thumbs Up Approved",
                gifUrl = "https://media.tenor.com/images/5e8b21a4b26f8e928416244b3c02b36e/tenor.gif",
                previewUrl = "https://media.tenor.com/images/5e8b21a4b26f8e928416244b3c02b36e/tenor.gif",
                tag = "APPROVED",
                fallbackEmoji = "👍"
            ),
            PublicGifItem(
                id = "GIF_PUSHUP_CHALLENGE",
                title = "50 Pushups Now!",
                gifUrl = "https://media.tenor.com/images/8d912f53a4e1320987654321fedcba98/tenor.gif",
                previewUrl = "https://media.tenor.com/images/8d912f53a4e1320987654321fedcba98/tenor.gif",
                tag = "WORKOUT",
                fallbackEmoji = "💪"
            ),
            PublicGifItem(
                id = "GIF_NIGHT_RADAR",
                title = "Standing Ovation",
                gifUrl = "https://media.tenor.com/images/4c2a1b3d5e6f7a8b9c0d1e2f3a4b5c6d/tenor.gif",
                previewUrl = "https://media.tenor.com/images/4c2a1b3d5e6f7a8b9c0d1e2f3a4b5c6d/tenor.gif",
                tag = "RESPECT",
                fallbackEmoji = "👏"
            )
        )
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        val filtered = all.filter {
            it.title.lowercase().contains(q) || it.tag.lowercase().contains(q)
        }
        return filtered.ifEmpty { all }
    }
}
