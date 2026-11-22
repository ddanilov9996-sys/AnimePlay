package com.example.animeplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** User-selectable purple/pink color themes. */
enum class AnimePlayPalette(
    val id: String,
    val title: String,
    val background: Color,
    val surfaceOne: Color,
    val surfaceTwo: Color,
    val accent: Color,
    val accentSecondary: Color,
    val muted: Color
) {
    ORCHID(
        "orchid", "Орхидея", Color(0xFF100D1C), Color(0xFF21182E), Color(0xFF30203F),
        Color(0xFFFF63C3), Color(0xFFB18CFF), Color(0xFFBEB4CA)
    ),
    SAKURA(
        "sakura", "Сакура", Color(0xFF1A1020), Color(0xFF2C1B35), Color(0xFF43264D),
        Color(0xFFFF91C8), Color(0xFFD2A7FF), Color(0xFFD6BDD8)
    ),
    NEON(
        "neon", "Неоновая фуксия", Color(0xFF0D0A19), Color(0xFF211332), Color(0xFF35204D),
        Color(0xFFFF4FCB), Color(0xFF9874FF), Color(0xFFC5B8D9)
    ),
    LAVENDER(
        "lavender", "Лавандовый вечер", Color(0xFF151126), Color(0xFF24203A), Color(0xFF34304F),
        Color(0xFFFFA6D5), Color(0xFFBCA8FF), Color(0xFFC6C2DD)
    )
    ;

    companion object {
        fun fromId(id: String?): AnimePlayPalette = entries.firstOrNull { it.id == id } ?: ORCHID
    }
}

enum class AnimePlayIconStyle(val id: String, val title: String) {
    OUTLINE("outline", "Контурные"),
    NEON("neon", "Неоновые"),
    PASTEL("pastel", "Пастельные")
    ;

    companion object {
        fun fromId(id: String?): AnimePlayIconStyle = entries.firstOrNull { it.id == id } ?: NEON
    }
}

enum class AnimePlayCatalogDensity(
    val id: String,
    val title: String,
    val description: String,
    val columns: Int,
    val homePosterWidth: Int
) {
    COMPACT("compact", "Компактно", "Больше постеров на экране", 3, 116),
    STANDARD("standard", "Стандартно", "Удобный баланс размера и количества", 2, 142),
    LARGE("large", "Крупные карточки", "Постеры крупнее, меньше элементов", 1, 178);

    companion object {
        fun fromId(id: String?): AnimePlayCatalogDensity = entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

enum class AnimePlayLauncherIcon(val id: String, val title: String, val componentName: String) {
    ORCHID("orchid", "Орхидея", "com.example.animeplay.LauncherOrchid"),
    SAKURA("sakura", "Сакура", "com.example.animeplay.LauncherSakura"),
    NEON("neon", "Неон", "com.example.animeplay.LauncherNeon");

    companion object {
        fun fromId(id: String?): AnimePlayLauncherIcon = entries.firstOrNull { it.id == id } ?: ORCHID
    }
}

object AnimePlayAppearance {
    var palette by mutableStateOf(AnimePlayPalette.ORCHID)
    var iconStyle by mutableStateOf(AnimePlayIconStyle.NEON)
    var catalogDensity by mutableStateOf(AnimePlayCatalogDensity.STANDARD)
}


enum class AnimeProfileFrame(
    val id: String,
    val title: String,
    val symbol: String,
    val color: Color
) {
    NONE("none", "Без рамки", "○", Color.Transparent),
    SAKURA("sakura", "Сакура", "🌸", Color(0xFFFF8FCB)),
    LAVENDER("lavender", "Лаванда", "✦", Color(0xFFB99CFF)),
    NEON("neon", "Неон", "⚡", Color(0xFFFF55CF)),
    HEART("heart", "Сердечки", "♡", Color(0xFFFF6FAF));

    companion object {
        fun fromId(id: String?): AnimeProfileFrame = entries.firstOrNull { it.id == id } ?: SAKURA
    }
}
