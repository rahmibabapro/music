/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.utils

import android.content.Context
import android.content.res.Configuration
import com.furkqn.music.R
import java.util.Locale

const val DEFAULT_ARTIST_NAME = "Furkqn"

fun getArtistSeparator(context: Context): String = " ${context.getString(R.string.and)} "

fun formatArtistDisplay(
    names: List<String>,
    conjunction: String = ", ",
): String {
    val filtered = names.filter { it.isNotBlank() }
    if (filtered.isEmpty()) return DEFAULT_ARTIST_NAME
    return when (filtered.size) {
        1 -> filtered[0]
        2 -> "${filtered[0]}$conjunction${filtered[1]}"
        else -> filtered.dropLast(1).joinToString(", ") + "$conjunction${filtered.last()}"
    }
}

fun <T> List<T>.joinToArtistString(
    conjunction: String,
    transform: (T) -> String,
): String = formatArtistDisplay(map(transform), conjunction)

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

@Suppress("DEPRECATION")
fun setAppLocale(context: Context, locale: Locale) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}
