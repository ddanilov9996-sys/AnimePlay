package com.example.animeplay

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocalStore {
    private const val PREFS = "animeplay_local"
    private const val FAVORITES = "favorite_ids"
    private const val SOURCES = "media_sources"
    private const val DOWNLOADS = "download_records"
    private const val THEME_ID = "theme_id"
    private const val ICON_STYLE_ID = "icon_style_id"
    private const val CATALOG_DENSITY_ID = "catalog_density_id"
    private const val LAUNCHER_ICON_ID = "launcher_icon_id"
    private const val PROFILE_FRAME_ID = "profile_frame_id"
    private const val ANIME_REVIEWS = "anime_reviews"
    private const val CHARACTER_AVATAR = "character_avatar"
    private const val WATCHLIST = "watchlist_entries"

    fun loadFavorites(context: Context): Set<Int> = runCatching {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(FAVORITES, emptySet())
            .orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }.getOrDefault(emptySet())

    fun saveFavorites(context: Context, values: Set<Int>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(FAVORITES, values.map(Int::toString).toSet()).apply()
    }

    fun loadThemeId(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(THEME_ID, AnimePlayPalette.ORCHID.id) ?: AnimePlayPalette.ORCHID.id

    fun saveThemeId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(THEME_ID, id).apply()
    }

    fun loadIconStyleId(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(ICON_STYLE_ID, AnimePlayIconStyle.NEON.id) ?: AnimePlayIconStyle.NEON.id

    fun saveIconStyleId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ICON_STYLE_ID, id).apply()
    }

    fun loadCatalogDensityId(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(CATALOG_DENSITY_ID, AnimePlayCatalogDensity.STANDARD.id) ?: AnimePlayCatalogDensity.STANDARD.id

    fun saveCatalogDensityId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CATALOG_DENSITY_ID, id).apply()
    }

    fun loadLauncherIconId(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(LAUNCHER_ICON_ID, AnimePlayLauncherIcon.ORCHID.id) ?: AnimePlayLauncherIcon.ORCHID.id

    fun saveLauncherIconId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LAUNCHER_ICON_ID, id).apply()
    }

    fun loadSources(context: Context): List<MediaSource> = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SOURCES, "[]") ?: "[]"
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val url = item.optString("url")
                if (url.startsWith("https://")) {
                    add(MediaSource(
                        id = item.optLong("id"),
                        animeId = item.optInt("animeId"),
                        label = item.optString("label", "Оригинал"),
                        url = url,
                        subtitleUrl = item.optString("subtitleUrl")
                    ))
                }
            }
        }
    }.getOrDefault(emptyList())

    fun saveSources(context: Context, values: List<MediaSource>) {
        val array = JSONArray()
        values.forEach { source ->
            array.put(JSONObject()
                .put("id", source.id)
                .put("animeId", source.animeId)
                .put("label", source.label)
                .put("url", source.url)
                .put("subtitleUrl", source.subtitleUrl))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(SOURCES, array.toString()).apply()
    }

    fun loadDownloads(context: Context): List<DownloadRecord> = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(DOWNLOADS, "[]") ?: "[]"
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optLong("downloadId", -1L)
                if (id >= 0) add(DownloadRecord(
                    downloadId = id,
                    animeId = item.optInt("animeId"),
                    animeTitle = item.optString("animeTitle"),
                    voiceLabel = item.optString("voiceLabel"),
                    originalUrl = item.optString("originalUrl")
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveDownloads(context: Context, values: List<DownloadRecord>) {
        val array = JSONArray()
        values.forEach { item ->
            array.put(JSONObject()
                .put("downloadId", item.downloadId)
                .put("animeId", item.animeId)
                .put("animeTitle", item.animeTitle)
                .put("voiceLabel", item.voiceLabel)
                .put("originalUrl", item.originalUrl))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(DOWNLOADS, array.toString()).apply()
    }


// Profile cosmetics and reviews remain on this device; no account server is connected yet.
fun loadProfileFrameId(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    .getString(PROFILE_FRAME_ID, AnimeProfileFrame.SAKURA.id) ?: AnimeProfileFrame.SAKURA.id

fun saveProfileFrameId(context: Context, id: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(PROFILE_FRAME_ID, id).apply()
}

fun loadReviews(context: Context): List<AnimeReview> = runCatching {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ANIME_REVIEWS, "[]") ?: "[]"
    val array = JSONArray(raw)
    buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val text = item.optString("text").trim()
            val rating = item.optInt("rating")
            if (text.isNotBlank() && rating in 1..5) {
                add(AnimeReview(
                    id = item.optLong("id"),
                    animeId = item.optInt("animeId"),
                    author = item.optString("author", "Зритель"),
                    rating = rating,
                    text = text.take(1200),
                    createdAtMillis = item.optLong("createdAtMillis")
                ))
            }
        }
    }
}.getOrDefault(emptyList())

fun saveReviews(context: Context, values: List<AnimeReview>) {
    val array = JSONArray()
    values.takeLast(500).forEach { review ->
        array.put(JSONObject()
            .put("id", review.id)
            .put("animeId", review.animeId)
            .put("author", review.author.take(32))
            .put("rating", review.rating.coerceIn(1, 5))
            .put("text", review.text.take(1200))
            .put("createdAtMillis", review.createdAtMillis))
    }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ANIME_REVIEWS, array.toString()).apply()
}

    fun loadCharacterAvatar(context: Context): AnimeCharacterAvatar? = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHARACTER_AVATAR, null)
            ?: return@runCatching null
        val item = JSONObject(raw)
        val image = item.optString("imageUrl")
        if (!image.startsWith("https://media.kitsu.io/")) return@runCatching null
        AnimeCharacterAvatar(
            id = item.optString("id"),
            characterName = item.optString("characterName"),
            animeTitle = item.optString("animeTitle"),
            imageUrl = image,
            source = item.optString("source", "Kitsu")
        )
    }.getOrNull()

    fun saveCharacterAvatar(context: Context, avatar: AnimeCharacterAvatar?) {
        val edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (avatar == null) {
            edit.remove(CHARACTER_AVATAR)
        } else if (avatar.imageUrl.startsWith("https://media.kitsu.io/")) {
            edit.putString(CHARACTER_AVATAR, JSONObject()
                .put("id", avatar.id.take(120))
                .put("characterName", avatar.characterName.take(100))
                .put("animeTitle", avatar.animeTitle.take(120))
                .put("imageUrl", avatar.imageUrl)
                .put("source", avatar.source.take(30)).toString())
        }
        edit.apply()
    }

    fun loadWatchlist(context: Context): List<WatchlistEntry> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(WATCHLIST, "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val status = WatchStatus.fromId(item.optString("status")) ?: continue
                val id = item.optInt("animeId", -1)
                val title = item.optString("animeTitle").take(150)
                if (id >= 0 && title.isNotBlank()) add(WatchlistEntry(
                    animeId = id,
                    animeTitle = title,
                    coverUrl = item.optString("coverUrl").takeIf { it.startsWith("https://") }.orEmpty(),
                    status = status,
                    updatedAtMillis = item.optLong("updatedAtMillis")
                ))
            }
        }.distinctBy { it.animeId }
    }.getOrDefault(emptyList())

    fun saveWatchlist(context: Context, values: List<WatchlistEntry>) {
        val array = JSONArray()
        values.distinctBy { it.animeId }.take(1000).forEach { item ->
            array.put(JSONObject()
                .put("animeId", item.animeId)
                .put("animeTitle", item.animeTitle.take(150))
                .put("coverUrl", item.coverUrl)
                .put("status", item.status.id)
                .put("updatedAtMillis", item.updatedAtMillis))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(WATCHLIST, array.toString()).apply()
    }

}
