#!/usr/bin/env python3
"""Static integrity checks for AnimePlay (not a Gradle/Kotlin build)."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
required = [
    "settings.gradle.kts", "build.gradle.kts", "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/example/animeplay/MainActivity.kt",
    "app/src/main/java/com/example/animeplay/AnimeModels.kt",
    "app/src/main/java/com/example/animeplay/AniListRepository.kt",
    "app/src/main/java/com/example/animeplay/KitsuCharacterRepository.kt",
    "app/src/main/java/com/example/animeplay/AiringScheduleRepository.kt",
    "app/src/main/java/com/example/animeplay/LocalStore.kt",
    "app/src/main/java/com/example/animeplay/AnimePlayAppearance.kt",
    "app/src/main/java/com/example/animeplay/AniLibertyRepository.kt",
    "app/src/main/java/com/example/animeplay/Downloads.kt",
    "app/src/main/res/drawable/animeplay_icon_orchid.xml",
    "app/src/main/res/drawable/animeplay_icon_sakura.xml",
    "app/src/main/res/drawable/animeplay_icon_neon.xml",
    "gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.properties",
    "README.md", "DATA_SOURCES.md", "BUILD_STATUS.md",
]
missing = [item for item in required if not (ROOT / item).is_file()]
if missing:
    print("FAIL: missing files:", ", ".join(missing))
    sys.exit(1)

ET.parse(ROOT / "app/src/main/AndroidManifest.xml")
for vector_file in (ROOT / "app/src/main/res/drawable").glob("animeplay_icon_*.xml"):
    ET.parse(vector_file)

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
main = (ROOT / "app/src/main/java/com/example/animeplay/MainActivity.kt").read_text(encoding="utf-8")
models = (ROOT / "app/src/main/java/com/example/animeplay/AnimeModels.kt").read_text(encoding="utf-8")
appearance = (ROOT / "app/src/main/java/com/example/animeplay/AnimePlayAppearance.kt").read_text(encoding="utf-8")
store = (ROOT / "app/src/main/java/com/example/animeplay/LocalStore.kt").read_text(encoding="utf-8")
repo = (ROOT / "app/src/main/java/com/example/animeplay/AniListRepository.kt").read_text(encoding="utf-8")
kitsu = (ROOT / "app/src/main/java/com/example/animeplay/KitsuCharacterRepository.kt").read_text(encoding="utf-8")
airing = (ROOT / "app/src/main/java/com/example/animeplay/AiringScheduleRepository.kt").read_text(encoding="utf-8")
readme = (ROOT / "README.md").read_text(encoding="utf-8")
data_sources = (ROOT / "DATA_SOURCES.md").read_text(encoding="utf-8")
build_status = (ROOT / "BUILD_STATUS.md").read_text(encoding="utf-8")
genre_block = models.split("enum class AnimeGenre", 1)[-1].split("/** Curated directions", 1)[0]
genre_count = len(re.findall(r'^\s+[A-Z0-9_]+\("', genre_block, re.MULTILINE))
checks = {
    "package namespace": "com.example.animeplay" in build,
    "internet permission": "android.permission.INTERNET" in manifest,
    "Compose dependency": "androidx.compose.material3:material3" in build,
    "Media3 player": "androidx.media3:media3-exoplayer" in build,
    "HLS support": "media3-exoplayer-hls" in build,
    "DASH support": "media3-exoplayer-dash" in build,
    "AniList metadata endpoint declared": "https://graphql.anilist.co" in repo,
    "genre query filter": "genre_in:" in repo and "apiName" in repo,
    "18 genres defined": genre_count >= 18,
    "Yuri / GL label": "Girls' Love (GL)" in models and "сёдзё-ай" in models,
    "all genres reachable": "AnimeGenre.entries" in main and "onGenreSelected" in main,
    "no sky-only collection": "SKY_AND_STARS" not in models and "НЕБО" not in main,
    "favorites and downloads screens": all(x in main for x in ["Избранное", "Загрузки", "ProfileScreen"]),
    "Media3 playback UI": "ExoPlayer.Builder" in main,
    "authorized-source warning": "разрешённый источник" in main,
    "three launcher aliases": all(f".Launcher{name}" in manifest for name in ["Orchid", "Sakura", "Neon"]),
    "one launcher alias enabled by default": manifest.count('android:enabled="true"') == 1 and manifest.count('android:enabled="false"') == 2,
    "three vector app icons": all((ROOT / f"app/src/main/res/drawable/animeplay_icon_{name}.xml").is_file() for name in ["orchid", "sakura", "neon"]),
    "launcher icon picker": "onLauncherIconSelected" in main and "applyLauncherIcon" in main,
    "launcher icon switches without killing app": "PackageManager.DONT_KILL_APP" in main,
    "persisted launcher icon": "loadLauncherIconId" in store and "saveLauncherIconId" in store,
    "catalog density options": all(x in appearance for x in ["COMPACT", "STANDARD", "LARGE"]),
    "persisted catalog density": "loadCatalogDensityId" in store and "saveCatalogDensityId" in store,
    "AniLiberty API integration": "searchReleases" in main and "getEpisode" in main and "api/v1" in (ROOT / "app/src/main/java/com/example/animeplay/AniLibertyRepository.kt").read_text(encoding="utf-8"),
    "calendar navigation tab": 'CALENDAR("Календарь"' in models and "MainTab.CALENDAR -> CalendarScreen" in main,
    "future airing query": "airingSchedules(notYetAired: true" in airing and "airingAt_greater" in airing and "airingAt_lesser" in airing,
    "calendar pagination": "MAX_PAGES = 5" in airing and "hasNextPage" in airing,
    "calendar local time": "timeFormat.format(Date(entry.airingAtMillis))" in main and "getUpcomingEpisodes(days = 21)" in main,
    "review model and local storage": "data class AnimeReview" in models and "loadReviews" in store and "saveReviews" in store,
    "review form and star rating": "Отзывы зрителей" in main and "Опубликовать отзыв" in main and "rating = rating" in main,
    "local review transparency": "видны только на этом устройстве" in main,
    "profile frames": "enum class AnimeProfileFrame" in appearance,
    "saved profile frame": "loadProfileFrameId" in store and "saveProfileFrameId" in store,
    "Kitsu API integration": "https://kitsu.io/api/edge" in kitsu and "/characters?" in kitsu and "media.kitsu.io" in kitsu,
    "anime search then character selection": "searchAnime(query)" in main and "getAnimeCharacters(anime)" in main,
    "character search filter": "Найти персонажа" in main and "visibleCharacters" in main,
    "character pages for larger casts": "for (offset in 0..40 step 20)" in kitsu and "page%5Boffset%5D=$offset" in kitsu,
    "character image displayed as profile avatar": "selectedCharacterAvatar.imageUrl" in main,
    "character avatar persisted": "loadCharacterAvatar" in store and "saveCharacterAvatar" in store,
    "no generic generated avatar placeholders": "AnimeProfileAvatar" not in models + main + store,
    "five watchlist statuses": all(x in models for x in ["WATCHING", "COMPLETED", "PLANNED", "PAUSED", "DROPPED"]),
    "watchlist persistence": "loadWatchlist" in store and "saveWatchlist" in store,
    "watch status can be changed from detail": "onWatchStatusChanged(status)" in main and "Добавить в мой список" in main,
    "profile status counts": "watchlist.count" in main and "Мои списки" in main,
    "watchlist cards open filtered lists": "selectedListStatus = status" in main and "WatchlistStatusDialog(" in main,
    "watchlist entry removal": "onRemoveWatchEntry" in main and "removeWatchlistEntry" in main,
    "AniList restriction documented": "конкурирующих" in readme and "разрешение AniList" in data_sources,
    "Kitsu image rights caveat": "права на изображения" in data_sources,
    "dev build status documented": "APK не собран" in build_status,
    "version 0.1.6": 'versionName = "0.1.6"' in build and "0.1.6-dev" in readme,
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + name)
if failed:
    print("Static checks failed:", ", ".join(failed))
    sys.exit(1)
print(f"\nSTATIC CHECKS PASSED ({len(checks)} checks). These checks do not compile Kotlin or build an APK.")
