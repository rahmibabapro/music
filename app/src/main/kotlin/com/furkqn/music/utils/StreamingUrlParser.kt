/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import com.furkqn.music.innertube.utils.YouTubeUrlParser

object StreamingUrlParser {
    private val URL_IN_TEXT =
        Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)

    private val SPOTIFY_HTTPS_TRACK =
        Regex("""https?://open\.spotify\.com/(?:intl-[a-z]{2}/)?track/([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE)
    private val SPOTIFY_HTTPS_ALBUM =
        Regex("""https?://open\.spotify\.com/(?:intl-[a-z]{2}/)?album/([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE)
    private val SPOTIFY_HTTPS_PLAYLIST =
        Regex("""https?://open\.spotify\.com/(?:intl-[a-z]{2}/)?playlist/([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE)
    private val SPOTIFY_HTTPS_ARTIST =
        Regex("""https?://open\.spotify\.com/(?:intl-[a-z]{2}/)?artist/([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE)

    private val SPOTIFY_URI =
        Regex("""spotify:(track|album|playlist|artist):([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE)

    sealed class ParsedStreamingUrl {
        abstract val id: String

        data class YouTubeVideo(override val id: String) : ParsedStreamingUrl()
        data class YouTubePlaylist(override val id: String) : ParsedStreamingUrl()
        data class YouTubeAlbum(override val id: String) : ParsedStreamingUrl()
        data class YouTubeArtist(override val id: String) : ParsedStreamingUrl()

        data class SpotifyTrack(override val id: String, val url: String) : ParsedStreamingUrl()
        data class SpotifyAlbum(override val id: String, val url: String) : ParsedStreamingUrl()
        data class SpotifyPlaylist(override val id: String, val url: String) : ParsedStreamingUrl()
        data class SpotifyArtist(override val id: String, val url: String) : ParsedStreamingUrl()
    }

    fun extractUrl(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("spotify:", ignoreCase = true) ||
            trimmed.startsWith("music://", ignoreCase = true)
        ) {
            return trimmed.split(Regex("\\s")).firstOrNull()?.trimEnd(',', '.', ')', ']', '>')
        }
        return URL_IN_TEXT.find(trimmed)?.value?.trimEnd(',', '.', ')', ']', '>')
            ?: Regex("""music://[^\s<>"']+""", RegexOption.IGNORE_CASE).find(trimmed)?.value
                ?.trimEnd(',', '.', ')', ']', '>')
    }

    fun parse(input: String): ParsedStreamingUrl? {
        val url = extractUrl(input) ?: return null
        return parseDirectUrl(url)
    }

    fun parseDirectUrl(url: String): ParsedStreamingUrl? {
        val trimmed = url.trim()

        SPOTIFY_URI.find(trimmed)?.let { match ->
            val type = match.groupValues[1].lowercase()
            val id = match.groupValues[2]
            val canonical = "https://open.spotify.com/$type/$id"
            return when (type) {
                "track" -> ParsedStreamingUrl.SpotifyTrack(id, canonical)
                "album" -> ParsedStreamingUrl.SpotifyAlbum(id, canonical)
                "playlist" -> ParsedStreamingUrl.SpotifyPlaylist(id, canonical)
                "artist" -> ParsedStreamingUrl.SpotifyArtist(id, canonical)
                else -> null
            }
        }

        SPOTIFY_HTTPS_TRACK.find(trimmed)?.let { match ->
            val id = match.groupValues[1]
            return ParsedStreamingUrl.SpotifyTrack(id, "https://open.spotify.com/track/$id")
        }
        SPOTIFY_HTTPS_ALBUM.find(trimmed)?.let { match ->
            val id = match.groupValues[1]
            return ParsedStreamingUrl.SpotifyAlbum(id, "https://open.spotify.com/album/$id")
        }
        SPOTIFY_HTTPS_PLAYLIST.find(trimmed)?.let { match ->
            val id = match.groupValues[1]
            return ParsedStreamingUrl.SpotifyPlaylist(id, "https://open.spotify.com/playlist/$id")
        }
        SPOTIFY_HTTPS_ARTIST.find(trimmed)?.let { match ->
            val id = match.groupValues[1]
            return ParsedStreamingUrl.SpotifyArtist(id, "https://open.spotify.com/artist/$id")
        }

        when (val parsed = YouTubeUrlParser.parse(trimmed)) {
            is YouTubeUrlParser.ParsedUrl.Video ->
                return ParsedStreamingUrl.YouTubeVideo(parsed.id)
            is YouTubeUrlParser.ParsedUrl.Playlist ->
                return ParsedStreamingUrl.YouTubePlaylist(parsed.id)
            is YouTubeUrlParser.ParsedUrl.Album ->
                return ParsedStreamingUrl.YouTubeAlbum(parsed.id)
            is YouTubeUrlParser.ParsedUrl.Artist ->
                return ParsedStreamingUrl.YouTubeArtist(parsed.id)
            null -> Unit
        }

        return null
    }
}
