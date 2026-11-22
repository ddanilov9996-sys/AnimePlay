package com.example.animeplay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Upcoming episode metadata. This API returns schedules, not video files. */
object AiringScheduleRepository {
    private const val ENDPOINT = "https://graphql.anilist.co"
    private const val PAGE_SIZE = 50
    private const val MAX_PAGES = 5

    suspend fun getUpcomingEpisodes(days: Int = 21): List<UpcomingEpisode> = withContext(Dispatchers.IO) {
        val nowSeconds = System.currentTimeMillis() / 1000L
        val untilSeconds = nowSeconds + days.coerceIn(1, 30) * 24L * 60L * 60L
        val query = """
            query (${'$'}page: Int, ${'$'}from: Int, ${'$'}to: Int) {
              Page(page: ${'$'}page, perPage: $PAGE_SIZE) {
                pageInfo { hasNextPage }
                airingSchedules(notYetAired: true, airingAt_greater: ${'$'}from, airingAt_lesser: ${'$'}to, sort: TIME) {
                  airingAt
                  episode
                  media {
                    id
                    title { romaji english native }
                    coverImage { large }
                  }
                }
              }
            }
        """.trimIndent()

        val results = mutableListOf<UpcomingEpisode>()
        for (page in 1..MAX_PAGES) {
            val payload = JSONObject()
                .put("query", query)
                .put("variables", JSONObject()
                    .put("page", page)
                    .put("from", nowSeconds.toInt())
                    .put("to", untilSeconds.toInt()))
            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "AnimePlay/0.1.4 (Android)")
            }
            val pageHasNext: Boolean
            try {
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    ?: throw IllegalStateException("AniList не вернул данные расписания.")
                val responseText = stream.bufferedReader().use { it.readText() }
                if (status == 429) throw IllegalStateException("AniList временно ограничил запросы. Попробуй позже.")
                if (status !in 200..299) throw IllegalStateException("Не удалось получить расписание: HTTP $status")
                val root = JSONObject(responseText)
                val errors = root.optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    throw IllegalStateException(errors.optJSONObject(0)?.optString("message") ?: "Ошибка расписания AniList")
                }
                val pageObject = root.optJSONObject("data")?.optJSONObject("Page")
                    ?: throw IllegalStateException("AniList вернул пустую страницу расписания.")
                val rows = pageObject.optJSONArray("airingSchedules")
                if (rows != null) {
                    for (index in 0 until rows.length()) {
                        val item = rows.optJSONObject(index) ?: continue
                        val media = item.optJSONObject("media") ?: continue
                        val titles = media.optJSONObject("title") ?: JSONObject()
                        val title = titles.optString("english").takeIf { it.isNotBlank() }
                            ?: titles.optString("romaji").takeIf { it.isNotBlank() }
                            ?: titles.optString("native").takeIf { it.isNotBlank() }
                            ?: "Аниме без названия"
                        val airingAt = item.optLong("airingAt", 0L)
                        val episode = item.optInt("episode", 0)
                        if (airingAt > nowSeconds && episode > 0) {
                            results.add(UpcomingEpisode(
                                animeId = media.optInt("id"),
                                animeTitle = title,
                                cover = media.optJSONObject("coverImage")?.optString("large").orEmpty(),
                                episode = episode,
                                airingAtMillis = airingAt * 1000L
                            ))
                        }
                    }
                }
                pageHasNext = pageObject.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true
            } finally {
                connection.disconnect()
            }
            if (!pageHasNext) break
        }
        results.distinctBy { "${it.animeId}_${it.episode}_${it.airingAtMillis}" }
            .sortedBy { it.airingAtMillis }
    }
}
