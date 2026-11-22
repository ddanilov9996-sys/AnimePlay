package com.example.animeplay

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.util.Locale

fun isDirectDownloadUrl(rawUrl: String): Boolean {
    val path = runCatching { Uri.parse(rawUrl).path.orEmpty().lowercase(Locale.ROOT) }.getOrDefault("")
    return path.endsWith(".mp4") || path.endsWith(".m4v") || path.endsWith(".webm")
}

fun enqueueAuthorizedDownload(
    context: Context,
    anime: Anime,
    source: MediaSource
): Long {
    require(source.url.startsWith("https://")) { "Нужна защищённая HTTPS-ссылка." }
    require(isDirectDownloadUrl(source.url)) { "Для скачивания в этой версии нужна прямая ссылка на MP4, M4V или WebM." }

    val safeTitle = anime.title
        .replace(Regex("[^A-Za-zА-Яа-я0-9._-]"), "_")
        .trim('_')
        .take(48)
        .ifBlank { "anime_${anime.id}" }
    val extension = runCatching { Uri.parse(source.url).path.orEmpty().substringAfterLast('.', "mp4") }
        .getOrDefault("mp4").lowercase(Locale.ROOT).let { if (it in setOf("mp4", "m4v", "webm")) it else "mp4" }
    val request = DownloadManager.Request(Uri.parse(source.url)).apply {
        setTitle("${anime.title} — ${source.label}")
        setDescription("Загрузка в AnimePlay")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setAllowedOverRoaming(false)
        setDestinationInExternalFilesDir(
            context,
            Environment.DIRECTORY_MOVIES,
            "${safeTitle}_${source.id}.$extension"
        )
    }
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return manager.enqueue(request)
}

fun downloadStatus(context: Context, id: Long): String {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return runCatching {
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (cursor == null || !cursor.moveToFirst()) return@use "Запись не найдена"
            when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_PENDING -> "Ожидает"
                DownloadManager.STATUS_RUNNING -> "Скачивается"
                DownloadManager.STATUS_PAUSED -> "Пауза"
                DownloadManager.STATUS_SUCCESSFUL -> "Готово офлайн"
                DownloadManager.STATUS_FAILED -> "Ошибка загрузки"
                else -> "Неизвестно"
            }
        }
    }.getOrDefault("Недоступно")
}

fun downloadedUri(context: Context, id: Long): Uri? {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return runCatching { manager.getUriForDownloadedFile(id) }.getOrNull()
}
