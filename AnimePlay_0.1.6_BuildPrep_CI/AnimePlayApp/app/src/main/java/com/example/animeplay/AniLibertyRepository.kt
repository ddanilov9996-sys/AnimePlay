package com.example.animeplay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Uses the documented public AniLiberty/AniLibria API v1.
 * The API supplies its own release metadata and HLS playlists. We do not bypass
 * region/copyright blocks and do not turn HLS links into downloadable files.
 */
object AniLibertyRepository {
    private const val API_BASE = "https://anilibria.top/api/v1"
    private const val SITE_BASE = "https://anilibria.top"

    data class ReleaseMatch(
        val id: Int,
        val alias: String,
        val title: String,
        val year: Int?,
        val posterUrl: String,
        val blockedByGeo: Boolean,
        val blockedByCopyrights: Boolean,
        val episodesTotal: Int?
    ) {
        val isBlocked: Boolean get() = blockedByGeo || blockedByCopyrights
    }

    data class EpisodeRef(val id: String, val number: String, val name: String)

    data class ReleaseDetails(
        val match: ReleaseMatch,
        val episodes: List<EpisodeRef>
    )

    data class EpisodeStreams(
        val number: String,
        val name: String,
        val hls480: String,
        val hls720: String,
        val hls1080: String
    )

    suspend fun searchReleases(query: String): List<ReleaseMatch> = withContext(Dispatchers.IO) {
        require(query.isNotBlank()) { "Введи название аниме." }
        val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
        val root = getJson("$API_BASE/app/search/releases?query=$encoded")
        val items = findArray(root, "data", "items", "releases", "results")
        buildList {
            for (index in 0 until items.length()) {
                val row = items.optJSONObject(index) ?: continue
                val release = parseRelease(row)
                if (release.id > 0 && release.title.isNotBlank()) add(release)
            }
        }
    }

    suspend fun getRelease(idOrAlias: String): ReleaseDetails = withContext(Dispatchers.IO) {
        val safe = idOrAlias.trim()
        require(safe.isNotBlank() && !safe.contains("/")) { "Некорректный идентификатор релиза." }
        val root = getJson("$API_BASE/anime/releases/${URLEncoder.encode(safe, Charsets.UTF_8.name())}")
        val data = unwrapObject(root)
        val match = parseRelease(data)
        if (match.isBlocked) {
            throw IllegalStateException(blockMessage(match))
        }
        val episodes = data.optJSONArray("episodes") ?: JSONArray()
        val parsedEpisodes = buildList {
            for (i in 0 until episodes.length()) {
                val row = episodes.optJSONObject(i) ?: continue
                val id = row.optString("id")
                if (id.isBlank()) continue
                val ordinal = row.opt("ordinal")?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: (i + 1).toString()
                add(EpisodeRef(id, ordinal, row.optString("name")))
            }
        }
        ReleaseDetails(match, parsedEpisodes)
    }

    suspend fun getEpisode(episodeId: String): EpisodeStreams = withContext(Dispatchers.IO) {
        require(episodeId.isNotBlank() && !episodeId.contains("/")) { "Некорректный номер серии." }
        val root = getJson("$API_BASE/anime/releases/episodes/${URLEncoder.encode(episodeId, Charsets.UTF_8.name())}")
        val data = unwrapObject(root)
        EpisodeStreams(
            number = data.opt("ordinal")?.toString().orEmpty(),
            name = data.optString("name"),
            hls480 = data.optString("hls_480"),
            hls720 = data.optString("hls_720"),
            hls1080 = data.optString("hls_1080")
        )
    }

    fun blockMessage(match: ReleaseMatch): String = when {
        match.blockedByCopyrights -> "Этот релиз заблокирован правообладателем. AnimePlay не обходит блокировку."
        match.blockedByGeo -> "Этот релиз недоступен в текущем регионе. AnimePlay не обходит региональные ограничения."
        else -> "Источник недоступен."
    }

    private fun parseRelease(row: JSONObject): ReleaseMatch {
        val name = row.optJSONObject("name")
        val title = name?.optString("main")?.takeIf { it.isNotBlank() }
            ?: row.optString("name").takeIf { it.isNotBlank() }
            ?: row.optString("title")
        val poster = row.optJSONObject("poster")
        val optimized = poster?.optJSONObject("optimized")
        val posterPath = optimized?.optString("preview")?.takeIf { it.isNotBlank() }
            ?: poster?.optString("preview")?.takeIf { it.isNotBlank() }
            ?: poster?.optString("src")?.takeIf { it.isNotBlank() }
            ?: ""
        return ReleaseMatch(
            id = row.optInt("id"),
            alias = row.optString("alias"),
            title = title,
            year = row.optInt("year").takeIf { it > 0 },
            posterUrl = toAbsoluteUrl(posterPath),
            blockedByGeo = row.optBoolean("is_blocked_by_geo"),
            blockedByCopyrights = row.optBoolean("is_blocked_by_copyrights"),
            episodesTotal = row.optInt("episodes_total").takeIf { it > 0 }
        )
    }

    private fun toAbsoluteUrl(path: String): String = when {
        path.isBlank() -> ""
        path.startsWith("https://", true) -> path
        path.startsWith("/") -> SITE_BASE + path
        else -> "$SITE_BASE/$path"
    }

    private fun findArray(root: JSONObject, vararg keys: String): JSONArray {
        val data = root.opt("data")
        if (data is JSONArray) return data
        val candidates = listOfNotNull(root, data as? JSONObject)
        for (candidate in candidates) {
            for (key in keys) {
                val array = candidate.optJSONArray(key)
                if (array != null) return array
            }
        }
        return JSONArray()
    }

    private fun unwrapObject(root: JSONObject): JSONObject {
        root.optJSONObject("data")?.let { return it }
        return root
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 18_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "AnimePlay Android/0.1.3")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status == 404) throw IllegalStateException("В AniLibria / AniLiberty такой релиз не найден.")
            if (status == 429) throw IllegalStateException("AniLibria / AniLiberty ограничил частоту запросов. Попробуй позже.")
            if (status !in 200..299) throw IllegalStateException("Сервис озвучки ответил: HTTP $status")
            val json = JSONObject(body)
            val errors = json.optJSONArray("errors")
            if (errors != null && errors.length() > 0) {
                throw IllegalStateException(errors.optJSONObject(0)?.optString("message") ?: "Ошибка API AniLibria / AniLiberty.")
            }
            return json
        } finally {
            connection.disconnect()
        }
    }
}
