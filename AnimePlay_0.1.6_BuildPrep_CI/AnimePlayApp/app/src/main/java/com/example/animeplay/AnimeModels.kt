package com.example.animeplay

data class Anime(
    val id: Int,
    val title: String,
    val nativeTitle: String,
    val cover: String,
    val banner: String,
    val description: String,
    val score: Int?,
    val episodes: Int?,
    val year: Int?,
    val format: String,
    val status: String,
    val genres: List<String>,
    val tags: List<String>
)

data class MediaSource(
    val id: Long,
    val animeId: Int,
    val label: String,
    val url: String,
    val subtitleUrl: String = ""
)

data class DownloadRecord(
    val downloadId: Long,
    val animeId: Int,
    val animeTitle: String,
    val voiceLabel: String,
    val originalUrl: String
)

enum class MainTab(val title: String, val glyph: String) {
    HOME("Главная", "⌂"),
    CATALOG("Каталог", "▦"),
    CALENDAR("Календарь", "▣"),
    FAVORITES("Избранное", "♡"),
    DOWNLOADS("Загрузки", "↓"),
    PROFILE("Профиль", "◉");

    fun icon(style: AnimePlayIconStyle): String = when (style) {
        AnimePlayIconStyle.OUTLINE -> when (this) {
            HOME -> "⌂"; CATALOG -> "▤"; CALENDAR -> "▣"; FAVORITES -> "♡"; DOWNLOADS -> "⇩"; PROFILE -> "◉"
        }
        AnimePlayIconStyle.NEON -> when (this) {
            HOME -> "✦"; CATALOG -> "▦"; CALENDAR -> "◷"; FAVORITES -> "♥"; DOWNLOADS -> "⇩"; PROFILE -> "☾"
        }
        AnimePlayIconStyle.PASTEL -> when (this) {
            HOME -> "🏠"; CATALOG -> "🎬"; CALENDAR -> "🗓️"; FAVORITES -> "💗"; DOWNLOADS -> "📥"; PROFILE -> "🌸"
        }
    }
}


/** Genre names used by AniList's genre_in filter, with Russian labels for the UI. */
enum class AnimeGenre(val title: String, val apiName: String, val icon: String) {
    ACTION("Экшен", "Action", "⚔️"),
    ADVENTURE("Приключения", "Adventure", "🧭"),
    COMEDY("Комедия", "Comedy", "😄"),
    DRAMA("Драма", "Drama", "🎭"),
    ECCHI("Этти", "Ecchi", "✨"),
    FANTASY("Фэнтези", "Fantasy", "🪄"),
    HORROR("Ужасы", "Horror", "👻"),
    MAHOU_SHOUJO("Махо-сёдзё", "Mahou Shoujo", "🌙"),
    MECHA("Меха", "Mecha", "🤖"),
    MUSIC("Музыка", "Music", "🎵"),
    MYSTERY("Мистика и тайна", "Mystery", "🔮"),
    PSYCHOLOGICAL("Психологическое", "Psychological", "🧠"),
    ROMANCE("Романтика", "Romance", "💞"),
    SCI_FI("Фантастика", "Sci-Fi", "🚀"),
    SLICE_OF_LIFE("Повседневность", "Slice of Life", "☕"),
    SPORTS("Спорт", "Sports", "🏅"),
    SUPERNATURAL("Сверхъестественное", "Supernatural", "🌌"),
    THRILLER("Триллер", "Thriller", "🕵️")
}

/** Curated directions based on community tags in AniList. */
enum class AnimeCollection(
    val title: String,
    val subtitle: String,
    val tags: List<String>
) {
    ALL("Весь каталог", "Все доступные аниме", emptyList()),
    YURI(
        "Юри / Girls' Love (GL)",
        "Романтические истории между девушками; также встречается название сёдзё-ай",
        listOf("Yuri")
    )
}


/** Upcoming episode schedule from AniList, stored as Unix milliseconds. */
data class UpcomingEpisode(
    val animeId: Int,
    val animeTitle: String,
    val cover: String,
    val episode: Int,
    val airingAtMillis: Long
)

/** Reviews are currently stored locally on the user's device. */
data class AnimeReview(
    val id: Long,
    val animeId: Int,
    val author: String,
    val rating: Int,
    val text: String,
    val createdAtMillis: Long
)

/** Character portrait metadata returned by Kitsu; the remote artwork is not copied into the APK. */
data class AnimeCharacterAvatar(
    val id: String,
    val characterName: String,
    val animeTitle: String,
    val imageUrl: String,
    val source: String = "Kitsu"
)

data class KitsuAnimeChoice(
    val id: String,
    val title: String,
    val coverUrl: String
)

data class WatchlistEntry(
    val animeId: Int,
    val animeTitle: String,
    val coverUrl: String,
    val status: WatchStatus,
    val updatedAtMillis: Long
)

enum class WatchStatus(val id: String, val title: String, val description: String) {
    WATCHING("watching", "Смотрю", "Сейчас смотрю"),
    COMPLETED("completed", "Просмотрено", "Уже посмотрел"),
    PLANNED("planned", "В планах", "Хочу посмотреть"),
    PAUSED("paused", "Отложено", "Вернусь позже"),
    DROPPED("dropped", "Брошено", "Не планирую продолжать");

    companion object {
        fun fromId(id: String?): WatchStatus? = entries.firstOrNull { it.id == id }
    }
}
