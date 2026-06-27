/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import com.furkqn.music.innertube.YouTube
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SongLinks(
    val youtubeUrl: String,
    val spotifyUrl: String?,
)

data class ResolvedYouTubeTarget(
    val videoId: String? = null,
    val playlistId: String? = null,
)

@Serializable
private data class SongLinkResponse(
    @SerialName("linksByPlatform")
    val linksByPlatform: Map<String, PlatformLink>? = null,
    @SerialName("entitiesByUniqueId")
    val entitiesByUniqueId: Map<String, EntityDetail>? = null,
)

@Serializable
private data class PlatformLink(
    val url: String? = null,
)

@Serializable
private data class EntityDetail(
    val id: String? = null,
    val title: String? = null,
    @SerialName("artistName")
    val artistName: List<String>? = null,
)

@Serializable
private data class DeezerSearchResponse(
    val data: List<DeezerTrackSummary> = emptyList(),
)

@Serializable
private data class DeezerTrackSummary(
    val id: Long = 0,
)

@Serializable
private data class DeezerTrackDetail(
    val isrc: String? = null,
)

@Serializable
private data class MusicBrainzRecordingSearchResponse(
    val recordings: List<MusicBrainzRecording> = emptyList(),
)

@Serializable
private data class MusicBrainzRecording(
    val relations: List<MusicBrainzRelation>? = null,
)

@Serializable
private data class MusicBrainzRelation(
    val type: String? = null,
    val url: MusicBrainzUrlResource? = null,
)

@Serializable
private data class MusicBrainzUrlResource(
    val resource: String? = null,
)

@Serializable
private data class MusicBrainzIsrcResponse(
    val recordings: List<MusicBrainzRecording> = emptyList(),
)

@Serializable
private data class SpotifyOEmbedResponse(
    val title: String? = null,
)

object SongLinkResolver {
    private val cache = LinkedHashMap<String, SongLinks>(64, 0.75f, true)

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 12_000
                connectTimeoutMillis = 12_000
                socketTimeoutMillis = 12_000
            }
        }
    }

    suspend fun resolve(
        youtubeVideoId: String,
        title: String? = null,
        artist: String? = null,
    ): SongLinks =
        withContext(Dispatchers.IO) {
            val cacheKey = listOf(youtubeVideoId, title.orEmpty(), artist.orEmpty()).joinToString("|")
            cache[cacheKey]?.let { return@withContext it }

            val youtubeUrl = "https://music.youtube.com/watch?v=$youtubeVideoId"
            val sourceUrls =
                listOf(
                    "https://www.youtube.com/watch?v=$youtubeVideoId",
                    youtubeUrl,
                    "https://youtu.be/$youtubeVideoId",
                )

            var spotifyUrl: String? = null
            var resolvedTitle = title
            var resolvedArtist = artist

            for (sourceUrl in sourceUrls) {
                val odesli = fetchOdesliResponse(sourceUrl) ?: continue
                spotifyUrl = extractSpotifyUrl(odesli)
                if (spotifyUrl.isNullOrBlank()) {
                    extractMetadata(odesli)?.let { (t, a) ->
                        if (resolvedTitle.isNullOrBlank()) resolvedTitle = t
                        if (resolvedArtist.isNullOrBlank()) resolvedArtist = a
                    }
                } else {
                    break
                }
            }

            if (spotifyUrl.isNullOrBlank() && !resolvedTitle.isNullOrBlank()) {
                spotifyUrl = fetchSpotifyByMetadata(resolvedTitle!!, resolvedArtist.orEmpty())
            }

            SongLinks(youtubeUrl = youtubeUrl, spotifyUrl = spotifyUrl?.takeIf { it.isNotBlank() }).also {
                if (cache.size >= 128) {
                    cache.entries.firstOrNull()?.let { entry -> cache.remove(entry.key) }
                }
                cache[cacheKey] = it
            }
        }

    suspend fun resolveToYouTube(url: String): ResolvedYouTubeTarget? =
        withContext(Dispatchers.IO) {
            val parsed = StreamingUrlParser.parseDirectUrl(url) ?: return@withContext null

            when (parsed) {
                is StreamingUrlParser.ParsedStreamingUrl.SpotifyTrack -> {
                    resolveYouTubeFromOdesli(parsed.url)
                        ?: searchYouTubeFromSpotifyOEmbed(parsed.url)
                }
                is StreamingUrlParser.ParsedStreamingUrl.SpotifyAlbum -> {
                    resolveYouTubeFromOdesli("https://open.spotify.com/album/${parsed.id}")
                }
                is StreamingUrlParser.ParsedStreamingUrl.SpotifyPlaylist -> {
                    resolveYouTubeFromOdesli("https://open.spotify.com/playlist/${parsed.id}")
                }
                is StreamingUrlParser.ParsedStreamingUrl.YouTubeVideo ->
                    ResolvedYouTubeTarget(videoId = parsed.id)
                is StreamingUrlParser.ParsedStreamingUrl.YouTubePlaylist,
                is StreamingUrlParser.ParsedStreamingUrl.YouTubeAlbum,
                -> {
                    val id =
                        when (parsed) {
                            is StreamingUrlParser.ParsedStreamingUrl.YouTubePlaylist -> parsed.id
                            is StreamingUrlParser.ParsedStreamingUrl.YouTubeAlbum -> parsed.id
                            else -> return@withContext null
                        }
                    ResolvedYouTubeTarget(playlistId = id)
                }
                else -> null
            }
        }

    private suspend fun resolveYouTubeFromOdesli(sourceUrl: String): ResolvedYouTubeTarget? {
        val odesli = fetchOdesliResponse(sourceUrl) ?: return null
        extractYouTubeVideoId(odesli)?.let { return ResolvedYouTubeTarget(videoId = it) }
        extractYouTubePlaylistId(odesli)?.let { return ResolvedYouTubeTarget(playlistId = it) }
        return null
    }

    private suspend fun searchYouTubeFromSpotifyOEmbed(spotifyUrl: String): ResolvedYouTubeTarget? {
        val (title, artist) = fetchSpotifyOEmbedMetadata(spotifyUrl) ?: return null
        val query = listOfNotNull(title, artist).joinToString(" ").trim()
        if (query.isBlank()) return null

        return YouTube
            .search(query, YouTube.SearchFilter.FILTER_SONG)
            .getOrNull()
            ?.items
            ?.firstOrNull()
            ?.id
            ?.let { ResolvedYouTubeTarget(videoId = it) }
    }

    private suspend fun fetchSpotifyByMetadata(
        title: String,
        artist: String,
    ): String? {
        searchMusicBrainzRecording(title, artist)?.let { return it }

        val isrc = fetchIsrcFromDeezer(title, artist) ?: return null
        searchMusicBrainzByIsrc(isrc)?.let { return it }

        return null
    }

    private suspend fun fetchOdesliResponse(sourceUrl: String): SongLinkResponse? {
        return try {
            val encoded = URLEncoder.encode(sourceUrl, StandardCharsets.UTF_8.toString())
            val response =
                client.get("https://api.song.link/v1-alpha.1/links?url=$encoded&userCountry=TR") {
                    header("User-Agent", "Music/1.0 (Android)")
                }
            if (!response.status.isSuccess()) return null
            response.body<SongLinkResponse>()
        } catch (_: Exception) {
            null
        }
    }

    private fun extractSpotifyUrl(body: SongLinkResponse): String? {
        body.linksByPlatform
            ?.entries
            ?.firstOrNull { it.key.equals("spotify", ignoreCase = true) }
            ?.value
            ?.url
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return body.entitiesByUniqueId
            ?.entries
            ?.firstOrNull { it.key.startsWith("SPOTIFY_", ignoreCase = true) }
            ?.value
            ?.id
            ?.let { trackId -> "https://open.spotify.com/track/$trackId" }
    }

    private fun extractYouTubeVideoId(body: SongLinkResponse): String? {
        val platformUrl =
            body.linksByPlatform
                ?.entries
                ?.firstOrNull { it.key.equals("youtubeMusic", ignoreCase = true) || it.key.equals("youtube", ignoreCase = true) }
                ?.value
                ?.url
                ?: return body.entitiesByUniqueId
                    ?.entries
                    ?.firstOrNull { it.key.startsWith("YOUTUBE_", ignoreCase = true) }
                    ?.value
                    ?.id
                    ?.let { id -> if (id.length == 11) id else null }

        return StreamingUrlParser.parse(platformUrl)?.let { parsed ->
            when (parsed) {
                is StreamingUrlParser.ParsedStreamingUrl.YouTubeVideo -> parsed.id
                else -> null
            }
        }
    }

    private fun extractYouTubePlaylistId(body: SongLinkResponse): String? {
        val platformUrl =
            body.linksByPlatform
                ?.entries
                ?.firstOrNull { it.key.equals("youtubeMusic", ignoreCase = true) || it.key.equals("youtube", ignoreCase = true) }
                ?.value
                ?.url
                ?: return null

        return StreamingUrlParser.parse(platformUrl)?.let { parsed ->
            when (parsed) {
                is StreamingUrlParser.ParsedStreamingUrl.YouTubePlaylist -> parsed.id
                is StreamingUrlParser.ParsedStreamingUrl.YouTubeAlbum -> parsed.id
                else -> null
            }
        }
    }

    private fun extractMetadata(body: SongLinkResponse): Pair<String, String>? {
        val entity =
            body.entitiesByUniqueId?.values?.firstOrNull { !it.title.isNullOrBlank() }
                ?: return null
        val title = entity.title ?: return null
        val artist = entity.artistName?.joinToString(", ").orEmpty()
        return title to artist
    }

    private suspend fun searchMusicBrainzRecording(
        title: String,
        artist: String,
    ): String? {
        val query =
            buildString {
                append("recording:\"${title.escapeLucene()}\"")
                if (artist.isNotBlank()) append(" AND artist:\"${artist.escapeLucene()}\"")
            }
        return fetchMusicBrainzSpotifyUrl(query)
    }

    private suspend fun searchMusicBrainzByIsrc(isrc: String): String? {
        return try {
            val response =
                client.get("https://musicbrainz.org/ws/2/isrc/$isrc") {
                    parameter("fmt", "json")
                    parameter("inc", "recordings")
                    header("User-Agent", "Music/1.0 (contact@metrolist.cc)")
                }
            if (!response.status.isSuccess()) return null
            val body: MusicBrainzIsrcResponse = response.body()
            body.recordings
                .asSequence()
                .mapNotNull { recording -> spotifyUrlFromRelations(recording.relations) }
                .firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchMusicBrainzSpotifyUrl(query: String): String? {
        if (query.isBlank()) return null
        return try {
            val response =
                client.get("https://musicbrainz.org/ws/2/recording/") {
                    parameter("query", query)
                    parameter("fmt", "json")
                    parameter("limit", 5)
                    parameter("inc", "url-rels")
                    header("User-Agent", "Music/1.0 (contact@metrolist.cc)")
                }
            if (!response.status.isSuccess()) return null
            val body: MusicBrainzRecordingSearchResponse = response.body()
            body.recordings
                .asSequence()
                .mapNotNull { recording -> spotifyUrlFromRelations(recording.relations) }
                .firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun spotifyUrlFromRelations(relations: List<MusicBrainzRelation>?): String? =
        relations
            ?.asSequence()
            ?.filter { relation ->
                relation.type.equals("streaming music", ignoreCase = true) ||
                    relation.url?.resource?.contains("spotify.com", ignoreCase = true) == true
            }
            ?.mapNotNull { it.url?.resource }
            ?.firstOrNull { it.contains("open.spotify.com", ignoreCase = true) }

    private suspend fun fetchIsrcFromDeezer(
        title: String,
        artist: String,
    ): String? {
        return fetchIsrcFromDeezerQuery(buildDeezerQuery(title, artist))
            ?: fetchIsrcFromDeezerQuery(listOf(artist, title).filter { it.isNotBlank() }.joinToString(" "))
    }

    private fun buildDeezerQuery(
        title: String,
        artist: String,
    ): String =
        buildString {
            if (artist.isNotBlank()) append("artist:\"$artist\" ")
            append("track:\"$title\"")
        }.trim()

    private suspend fun fetchIsrcFromDeezerQuery(query: String): String? {
        if (query.isBlank()) return null
        return try {
            val response =
                client.get("https://api.deezer.com/search/track") {
                    parameter("q", query)
                    parameter("limit", 5)
                }
            if (!response.status.isSuccess()) return null
            val search: DeezerSearchResponse = response.body()
            val trackId = search.data.firstOrNull()?.id ?: return null
            val detailResponse = client.get("https://api.deezer.com/track/$trackId")
            if (!detailResponse.status.isSuccess()) return null
            detailResponse.body<DeezerTrackDetail>().isrc?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchSpotifyOEmbedMetadata(spotifyUrl: String): Pair<String, String>? {
        return try {
            val response =
                client.get("https://open.spotify.com/oembed") {
                    parameter("url", spotifyUrl)
                    header("User-Agent", "Music/1.0 (Android)")
                }
            if (!response.status.isSuccess()) return null
            val title = response.body<SpotifyOEmbedResponse>().title ?: return null
            val parts = title.split(" · ", " - ", limit = 2)
            when (parts.size) {
                2 -> parts[0].trim() to parts[1].trim()
                else -> title.trim() to ""
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun String.escapeLucene(): String = replace("\"", "\\\"")
}
