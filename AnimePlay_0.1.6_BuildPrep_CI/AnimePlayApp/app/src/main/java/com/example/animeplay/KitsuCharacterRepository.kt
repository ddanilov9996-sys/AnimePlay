package com.example.animeplay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Reads the public Kitsu JSON:API for anime title lookup and character relationships.
 * This repository never downloads/copies artwork into the APK; images remain remote.
 */
object KitsuCharacterRepository {
    private const val BASE = "https://kitsu.io/api/edge"

    suspend fun searchAnime(query: String): List<KitsuAnimeChoice> = withContext(Dispatchers.IO) {
        val safeQuery = query.trim()
        require(safeQuery.length >= 2) { "Введи хотя бы две буквы названия аниме." }
        val url = "$BASE/anime?filter%5Btext%5D=${encode(safeQuery)}&page%5Blimit%5D=12"
        val root = getJson(url)
        val rows = root.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val attrs = row.optJSONObject("attributes") ?: JSONObject()
                val title = attrs.optString("canonicalTitle").ifBlank {
                    val titles = attrs.optJSONObject("titles") ?: JSONObject()
                    titles.optString("en").ifBlank { titles.optString("en_jp") }
                }
                if (title.isBlank()) continue
                val poster = attrs.optJSONObject("posterImage") ?: JSONObject()
                add(KitsuAnimeChoice(
                    id = row.optString("id"),
                    title = title,
                    coverUrl = safeImageUrl(poster.optString("medium").ifBlank { poster.optString("small") })
                ))
            }
        }.distinctBy { it.id }
    }

    suspend fun getAnimeCharacters(anime: KitsuAnimeChoice): List<AnimeCharacterAvatar> = withContext(Dispatchers.IO) {
        require(anime.id.matches(Regex("[0-9]+"))) { "Некорректный идентификатор аниме." }

        val includedCharacters = mutableMapOf<String, JSONObject>()
        val relations = mutableListOf<JSONObject>()
        // Kitsu limits collection pages; load up to three pages so users can find supporting characters too.
        for (offset in 0..40 step 20) {
            val root = getJson(
                "$BASE/anime/${anime.id}/characters?page%5Blimit%5D=20&page%5Boffset%5D=$offset&include=character"
            )
            val included = root.optJSONArray("included") ?: JSONArray()
            for (i in 0 until included.length()) {
                val item = included.optJSONObject(i) ?: continue
                if (item.optString("type").contains("character", ignoreCase = true)) {
                    includedCharacters[item.optString("id")] = item
                }
            }
            val rows = root.optJSONArray("data") ?: JSONArray()
            for (i in 0 until rows.length()) {
                rows.optJSONObject(i)?.let(relations::add)
            }
            if (rows.length() < 20) break
        }

        val result = mutableListOf<AnimeCharacterAvatar>()
        for (relation in relations) {
            val directType = relation.optString("type")
            val relationships = relation.optJSONObject("relationships") ?: JSONObject()
            val characterRel = relationships.optJSONObject("character") ?: JSONObject()
            val characterId = characterRel.optJSONObject("data")?.optString("id").orEmpty()
            val character = includedCharacters[characterId]
                ?: if (directType.contains("character", ignoreCase = true)) relation else null
                ?: continue
            val attrs = character.optJSONObject("attributes") ?: character
            val name = attrs.optString("name").ifBlank { attrs.optString("canonicalName") }
            val image = attrs.opt("image")
            val imageUrl = when (image) {
                is JSONObject -> safeImageUrl(
                    image.optString("original").ifBlank {
                        image.optString("large").ifBlank { image.optString("medium") }
                    }
                )
                is String -> safeImageUrl(image)
                else -> ""
            }
            if (name.isNotBlank() && imageUrl.isNotBlank()) {
                result += AnimeCharacterAvatar(
                    id = character.optString("id").ifBlank { characterId.ifBlank { name } },
                    characterName = name,
                    animeTitle = anime.title,
                    imageUrl = imageUrl
                )
            }
        }
        result.distinctBy { it.id }.take(60)
    }

    private fun getJson(address: String): JSONObject {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 18000
            setRequestProperty("Accept", "application/vnd.api+json")
            setRequestProperty("Content-Type", "application/vnd.api+json")
            setRequestProperty("User-Agent", "AnimePlay/0.1.6 Android")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 429) throw IllegalStateException("Kitsu временно ограничил запросы. Попробуй позже.")
            if (code !in 200..299) throw IllegalStateException("Сервис персонажей ответил: HTTP $code")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun safeImageUrl(value: String): String {
        val candidate = value.trim()
        if (!candidate.startsWith("https://")) return ""
        val host = runCatching { URL(candidate).host.lowercase() }.getOrDefault("")
        return if (host == "media.kitsu.io" || host.endsWith(".media.kitsu.io")) candidate else ""
    }
}
