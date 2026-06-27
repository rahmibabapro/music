/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import android.content.Context
import com.furkqn.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object BpmLookupService {
    private const val BASE_URL = "https://api.getsongbpm.com/search/"
    private val concurrency = Semaphore(3)

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    suspend fun lookupWithCache(
        context: Context,
        songId: String,
        title: String,
        artist: String,
    ): Float? =
        withContext(Dispatchers.IO) {
            BpmCache.get(context, songId)?.let { return@withContext it }
            val bpm = lookup(title, artist) ?: return@withContext null
            BpmCache.put(context, songId, bpm)
            bpm
        }

    suspend fun lookup(title: String, artist: String): Float? =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GETSONGBPM_API_KEY
            if (apiKey.isBlank()) return@withContext null

            concurrency.withPermit {
                val lookup = "$title $artist".trim()
                if (lookup.isBlank()) return@withPermit null

                val response =
                    runCatching {
                        client.get(BASE_URL) {
                            parameter("api_key", apiKey)
                            parameter("type", "both")
                            parameter("lookup", lookup)
                            parameter("limit", 5)
                        }
                    }.getOrNull() ?: return@withPermit null

                if (!response.status.isSuccess()) return@withPermit null

                val body = runCatching { response.body<GetSongBpmSearchResponse>() }.getOrNull()
                    ?: return@withPermit null

                body.search
                    ?.firstOrNull()
                    ?.tempo
                    ?.toFloatOrNull()
            }
        }
}

@Serializable
private data class GetSongBpmSearchResponse(
    val search: List<GetSongBpmTrack>? = null,
)

@Serializable
private data class GetSongBpmTrack(
    val tempo: String? = null,
    @SerialName("title") val title: String? = null,
)
