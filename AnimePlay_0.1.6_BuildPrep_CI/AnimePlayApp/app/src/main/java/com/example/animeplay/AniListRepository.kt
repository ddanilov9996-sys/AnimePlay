package com.example.animeplay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Anime metadata only. AniList does not provide episode video files. */
object AniListRepository {
    private const val ENDPOINT = "https://graphql.anilist.co"

    suspend fun getAnime(
        search: String? = null,
        page: Int = 1,
        collection: AnimeCollection = AnimeCollection.ALL,
        genre: AnimeGenre? = null
    ): List<Anime> = withContext(Dispatchers.IO) {
        val query = """
            query (${ '$' }page: Int, ${ '$' }search: String, ${ '$' }tags: [String], ${ '$' }genres: [String]) {
              Page(page: ${ '$' }page, perPage: 30) {
                media(type: ANIME, sort: POPULARITY_DESC, isAdult: false, search: ${ '$' }search, tag_in: ${ '$' }tags, genre_in: ${ '$' }genres) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large }
                  bannerImage
                  description(asHtml: false)
                  averageScore
                  episodes
                  seasonYear
                  format
                  status
                  genres
                  tags { name }
                }
              }
            }
        """

        val payload = JSONObject()
            .put("query", query)
            .put("variables", JSONObject()
                .put("page", page)
                .put("search", search?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("tags", if (collection.tags.isEmpty()) JSONObject.NULL else org.json.JSONArray(collection.tags))
                .put("genres", genre?.let { org.json.JSONArray(listOf(it.apiName)) } ?: JSONObject.NULL))

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "AnimePlay/0.1.4 (Android)")
        }

        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                ?: throw IllegalStateException("Сервис каталога не вернул ответ.")
            val responseText = stream.bufferedReader().use { it.readText() }
            if (status == 429) throw IllegalStateException("Каталог временно ограничил частоту запросов. Попробуй чуть позже.")
            if (status !in 200..299) throw IllegalStateException("Сервис каталога ответил: HTTP $status")

            val root = JSONObject(responseText)
            val errors = root.optJSONArray("errors")
            if (errors != null && errors.length() > 0) {
                throw IllegalStateException(errors.optJSONObject(0)?.optString("message") ?: "Ошибка каталога")
            }
            val rows = root.optJSONObject("data")
                ?.optJSONObject("Page")
                ?.optJSONArray("media") ?: return@withContext emptyList()

            buildList {
                for (index in 0 until rows.length()) {
                    val item = rows.optJSONObject(index) ?: continue
                    val titles = item.optJSONObject("title") ?: JSONObject()
                    val covers = item.optJSONObject("coverImage") ?: JSONObject()
                    val english = titles.optString("english").takeIf(String::isNotBlank)
                    val romaji = titles.optString("romaji").takeIf(String::isNotBlank)
                    val native = titles.optString("native")
                    val title = english ?: romaji ?: native.takeIf(String::isNotBlank) ?: "Без названия"
                    val genresArray = item.optJSONArray("genres")
                    val genres = buildList {
                        if (genresArray != null) for (i in 0 until genresArray.length()) {
                            val genre = genresArray.optString(i)
                            if (genre.isNotBlank()) add(genre)
                        }
                    }
                    val tagsArray = item.optJSONArray("tags")
                    val tags = buildList {
                        if (tagsArray != null) for (i in 0 until tagsArray.length()) {
                            val tag = tagsArray.optJSONObject(i)?.optString("name").orEmpty()
                            if (tag.isNotBlank()) add(tag)
                        }
                    }
                    add(
                        Anime(
                            id = item.optInt("id"),
                            title = title,
                            nativeTitle = native,
                            cover = covers.optString("extraLarge").ifBlank { covers.optString("large") },
                            banner = item.optString("bannerImage"),
                            description = item.optString("description"),
                            score = item.optInt("averageScore").takeIf { it > 0 },
                            episodes = item.optInt("episodes").takeIf { it > 0 },
                            year = item.optInt("seasonYear").takeIf { it > 0 },
                            format = item.optString("format").replace('_', ' '),
                            status = item.optString("status").replace('_', ' '),
                            genres = genres,
                            tags = tags
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
