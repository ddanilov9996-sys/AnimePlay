package com.example.animeplay

import android.net.Uri
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Html
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

private val Background: Color get() = AnimePlayAppearance.palette.background
private val SurfaceOne: Color get() = AnimePlayAppearance.palette.surfaceOne
private val SurfaceTwo: Color get() = AnimePlayAppearance.palette.surfaceTwo
private val Accent: Color get() = AnimePlayAppearance.palette.accent
private val AccentPurple: Color get() = AnimePlayAppearance.palette.accentSecondary
private val Muted: Color get() = AnimePlayAppearance.palette.muted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 13, 28)
        window.navigationBarColor = android.graphics.Color.rgb(16, 13, 28)
        setContent {
            val palette = AnimePlayAppearance.palette
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = window.statusBarColor
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = palette.accent,
                    secondary = palette.accentSecondary,
                    background = palette.background,
                    surface = palette.surfaceOne,
                    onPrimary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = palette.background) {
                    AnimePlayApp()
                }
            }
        }
    }
}

@Composable
private fun AnimePlayApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var activeCollection by remember { mutableStateOf(AnimeCollection.ALL) }
    var selectedGenre by remember { mutableStateOf<AnimeGenre?>(null) }
    var selectedAnime by remember { mutableStateOf<Anime?>(null) }
    var playback by remember { mutableStateOf<PlaybackRequest?>(null) }
    var searchText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var downloadRefresh by remember { mutableStateOf(0) }
    val catalog = remember { mutableStateListOf<Anime>() }
    val favorites = remember { mutableStateListOf<Int>() }
    val sources = remember { mutableStateListOf<MediaSource>() }
    val downloads = remember { mutableStateListOf<DownloadRecord>() }
    val reviews = remember { mutableStateListOf<AnimeReview>() }
    val upcomingEpisodes = remember { mutableStateListOf<UpcomingEpisode>() }
    var scheduleLoading by remember { mutableStateOf(false) }
    var scheduleError by remember { mutableStateOf<String?>(null) }
    var scheduleLoaded by remember { mutableStateOf(false) }
    var selectedCharacterAvatar by remember { mutableStateOf<AnimeCharacterAvatar?>(null) }
    val watchlist = remember { mutableStateListOf<WatchlistEntry>() }
    var profileFrame by remember { mutableStateOf(AnimeProfileFrame.SAKURA) }
    var launcherIcon by remember { mutableStateOf(AnimePlayLauncherIcon.ORCHID) }

    fun loadCatalog(
        search: String? = null,
        collection: AnimeCollection = activeCollection,
        genre: AnimeGenre? = selectedGenre
    ) {
        scope.launch {
            isLoading = true
            errorText = null
            try {
                val result = AniListRepository.getAnime(search, collection = collection, genre = genre)
                catalog.clear()
                catalog.addAll(result)
                if (result.isEmpty()) errorText = "По этому запросу ничего не найдено."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorText = error.message ?: "Не удалось загрузить каталог. Проверь интернет."
            } finally {
                isLoading = false
            }
        }
    }

    fun loadUpcomingEpisodes() {
        scope.launch {
            scheduleLoading = true
            scheduleError = null
            try {
                val freshSchedule = AiringScheduleRepository.getUpcomingEpisodes(days = 21)
                upcomingEpisodes.clear()
                upcomingEpisodes.addAll(freshSchedule)
                scheduleLoaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                scheduleError = error.message ?: "Не удалось загрузить расписание. Проверь интернет."
            } finally {
                scheduleLoading = false
            }
        }
    }

    fun saveAnimeReview(review: AnimeReview) {
        val index = reviews.indexOfFirst { it.animeId == review.animeId && it.author == "Ты" }
        if (index >= 0) reviews[index] = review else reviews.add(review)
        LocalStore.saveReviews(context, reviews.toList())
    }

    fun saveWatchStatus(anime: Anime, status: WatchStatus?) {
        watchlist.removeAll { it.animeId == anime.id }
        if (status != null) {
            watchlist.add(WatchlistEntry(
                animeId = anime.id,
                animeTitle = anime.title,
                coverUrl = anime.cover,
                status = status,
                updatedAtMillis = System.currentTimeMillis()
            ))
        }
        LocalStore.saveWatchlist(context, watchlist.toList())
    }

    fun removeWatchlistEntry(entry: WatchlistEntry) {
        watchlist.removeAll { it.animeId == entry.animeId }
        LocalStore.saveWatchlist(context, watchlist.toList())
    }

    LaunchedEffect(Unit) {
        AnimePlayAppearance.palette = AnimePlayPalette.fromId(LocalStore.loadThemeId(context))
        AnimePlayAppearance.iconStyle = AnimePlayIconStyle.fromId(LocalStore.loadIconStyleId(context))
        AnimePlayAppearance.catalogDensity = AnimePlayCatalogDensity.fromId(LocalStore.loadCatalogDensityId(context))
        launcherIcon = AnimePlayLauncherIcon.fromId(LocalStore.loadLauncherIconId(context))
        applyLauncherIcon(context, launcherIcon)
        favorites.addAll(LocalStore.loadFavorites(context))
        sources.addAll(LocalStore.loadSources(context))
        downloads.addAll(LocalStore.loadDownloads(context))
        reviews.addAll(LocalStore.loadReviews(context))
        selectedCharacterAvatar = LocalStore.loadCharacterAvatar(context)
        watchlist.addAll(LocalStore.loadWatchlist(context))
        profileFrame = AnimeProfileFrame.fromId(LocalStore.loadProfileFrameId(context))
        loadCatalog()
    }

    LaunchedEffect(currentTab) {
        if (currentTab == MainTab.CALENDAR && !scheduleLoaded && !scheduleLoading) {
            loadUpcomingEpisodes()
        }
    }

    fun toggleFavorite(animeId: Int) {
        if (favorites.contains(animeId)) favorites.remove(animeId) else favorites.add(animeId)
        LocalStore.saveFavorites(context, favorites.toSet())
    }

    fun addMediaSource(source: MediaSource) {
        sources.removeAll { it.id == source.id }
        sources.add(source)
        LocalStore.saveSources(context, sources.toList())
    }

    fun startDownload(anime: Anime, source: MediaSource) {
        runCatching {
            val id = enqueueAuthorizedDownload(context, anime, source)
            val record = DownloadRecord(id, anime.id, anime.title, source.label, source.url)
            downloads.add(record)
            LocalStore.saveDownloads(context, downloads.toList())
        }.onFailure {
            errorText = it.message ?: "Не удалось начать загрузку."
        }
    }

    if (playback != null) {
        PlayerScreen(
            request = playback!!,
            onBack = { playback = null }
        )
        return
    }

    if (selectedAnime != null) {
        AnimeDetailScreen(
            anime = selectedAnime!!,
            isFavorite = favorites.contains(selectedAnime!!.id),
            watchStatus = watchlist.firstOrNull { it.animeId == selectedAnime!!.id }?.status,
            onWatchStatusChanged = { status -> saveWatchStatus(selectedAnime!!, status) },
            sources = sources.filter { it.animeId == selectedAnime!!.id },
            reviews = reviews.filter { it.animeId == selectedAnime!!.id },
            onBack = { selectedAnime = null },
            onToggleFavorite = { toggleFavorite(selectedAnime!!.id) },
            onSaveReview = ::saveAnimeReview,
            onAddSource = ::addMediaSource,
            onWatch = { source -> playback = PlaybackRequest(selectedAnime!!.title, source.url, source.subtitleUrl, source.label) },
            onDownload = { source -> startDownload(selectedAnime!!, source) }
        )
        return
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = SurfaceOne, contentColor = Color.White) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Text(tab.icon(AnimePlayAppearance.iconStyle), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                        label = { Text(tab.title, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppTopBar(onRefresh = {
                if (currentTab == MainTab.CALENDAR) loadUpcomingEpisodes()
                else loadCatalog(searchText.takeIf { it.isNotBlank() })
            })
            when (currentTab) {
                MainTab.HOME -> HomeScreen(
                    catalog = catalog,
                    catalogDensity = AnimePlayAppearance.catalogDensity,
                    isLoading = isLoading,
                    errorText = errorText,
                    onAnimeClick = { selectedAnime = it },
                    onRetry = { loadCatalog() },
                    onCatalogClick = { currentTab = MainTab.CATALOG },
                    onCollectionClick = { collection ->
                        activeCollection = collection
                        selectedGenre = null
                        searchText = ""
                        currentTab = MainTab.CATALOG
                        loadCatalog(collection = collection, genre = null)
                    },
                    onGenreClick = { genre ->
                        activeCollection = AnimeCollection.ALL
                        selectedGenre = genre
                        searchText = ""
                        currentTab = MainTab.CATALOG
                        loadCatalog(collection = AnimeCollection.ALL, genre = genre)
                    }
                )
                MainTab.CATALOG -> CatalogScreen(
                    catalog = catalog,
                    catalogDensity = AnimePlayAppearance.catalogDensity,
                    query = searchText,
                    onQueryChange = { searchText = it },
                    onSearch = { loadCatalog(searchText.takeIf { it.isNotBlank() }) },
                    isLoading = isLoading,
                    errorText = errorText,
                    onRetry = { loadCatalog(searchText.takeIf { it.isNotBlank() }) },
                    onAnimeClick = { selectedAnime = it },
                    selectedCollection = activeCollection,
                    onCollectionSelected = { collection ->
                        activeCollection = collection
                        selectedGenre = null
                        loadCatalog(searchText.takeIf { it.isNotBlank() }, collection, null)
                    },
                    selectedGenre = selectedGenre,
                    onGenreSelected = { genre ->
                        selectedGenre = genre
                        activeCollection = AnimeCollection.ALL
                        loadCatalog(searchText.takeIf { it.isNotBlank() }, AnimeCollection.ALL, genre)
                    },
                    russianVoiceIds = sources.filter { source ->
                        source.label.contains("рус", ignoreCase = true) || source.label.contains("russian", ignoreCase = true)
                    }.map { it.animeId }.toSet()
                )
                MainTab.CALENDAR -> CalendarScreen(
                    episodes = upcomingEpisodes,
                    isLoading = scheduleLoading,
                    errorText = scheduleError,
                    onRefresh = { loadUpcomingEpisodes() }
                )
                MainTab.FAVORITES -> CollectionScreen(
                    title = "Избранное",
                    subtitle = "Тайтлы, которые ты сохранил",
                    items = catalog.filter { favorites.contains(it.id) },
                    onAnimeClick = { selectedAnime = it },
                    emptyMessage = "Пока ничего нет. Открой аниме и нажми на звёздочку.",
                    catalogDensity = AnimePlayAppearance.catalogDensity
                )
                MainTab.DOWNLOADS -> DownloadsScreen(
                    downloads = downloads,
                    onPlay = { record ->
                        val uri = downloadedUri(context, record.downloadId)
                        if (uri != null) playback = PlaybackRequest(record.animeTitle, uri.toString(), "", record.voiceLabel)
                    },
                    onRefresh = { downloadRefresh++ },
                    refreshToken = downloadRefresh
                )
                MainTab.PROFILE -> ProfileScreen(
                    favoritesCount = favorites.size,
                    downloadsCount = downloads.size,
                    sourceCount = sources.size,
                    selectedPalette = AnimePlayAppearance.palette,
                    selectedIconStyle = AnimePlayAppearance.iconStyle,
                    selectedCatalogDensity = AnimePlayAppearance.catalogDensity,
                    selectedLauncherIcon = launcherIcon,
                    selectedCharacterAvatar = selectedCharacterAvatar,
                    watchlist = watchlist.toList(),
                    selectedFrame = profileFrame,
                    onRemoveWatchEntry = ::removeWatchlistEntry,
                    onCharacterAvatarSelected = { avatar ->
                        selectedCharacterAvatar = avatar
                        LocalStore.saveCharacterAvatar(context, avatar)
                    },
                    onFrameSelected = { frame ->
                        profileFrame = frame
                        LocalStore.saveProfileFrameId(context, frame.id)
                    },
                    onPaletteSelected = { preset ->
                        AnimePlayAppearance.palette = preset
                        LocalStore.saveThemeId(context, preset.id)
                    },
                    onIconStyleSelected = { style ->
                        AnimePlayAppearance.iconStyle = style
                        LocalStore.saveIconStyleId(context, style.id)
                    },
                    onCatalogDensitySelected = { density ->
                        AnimePlayAppearance.catalogDensity = density
                        LocalStore.saveCatalogDensityId(context, density.id)
                    },
                    onLauncherIconSelected = { icon ->
                        launcherIcon = icon
                        LocalStore.saveLauncherIconId(context, icon.id)
                        applyLauncherIcon(context, icon)
                    },
                    onOpenStudioBand = { openExternalPage(context, STUDIO_BAND_URL) }
                )
            }
        }
    }
}

private const val STUDIO_BAND_URL = "https://www.studiobandme.ru/"

private fun openExternalPage(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun applyLauncherIcon(context: android.content.Context, selected: AnimePlayLauncherIcon) {
    val packageManager = context.packageManager
    // Enable the new launcher entry first so the system never temporarily has no icon.
    (listOf(selected) + AnimePlayLauncherIcon.entries.filter { it != selected }).forEach { icon ->
        runCatching {
            packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, icon.componentName),
                if (icon == selected) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

private data class PlaybackRequest(
    val title: String,
    val mediaUrl: String,
    val subtitleUrl: String,
    val voiceLabel: String
)

@Composable
private fun AppTopBar(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("ANIMEPLAY", color = Accent, fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text("Твоя аниме-библиотека", color = Muted, fontSize = 11.sp)
        }
        TextButton(onClick = onRefresh) {
            Text("↻", fontSize = 24.sp, color = Color.White)
        }
    }
}

@Composable
private fun HomeScreen(
    catalog: List<Anime>,
    catalogDensity: AnimePlayCatalogDensity,
    isLoading: Boolean,
    errorText: String?,
    onAnimeClick: (Anime) -> Unit,
    onRetry: () -> Unit,
    onCatalogClick: () -> Unit,
    onCollectionClick: (AnimeCollection) -> Unit,
    onGenreClick: (AnimeGenre) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text("Привет, сэмпай! ✨", modifier = Modifier.padding(horizontal = 20.dp), fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Найди историю под своё настроение", modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 16.dp), color = Muted, fontSize = 13.sp)

        SectionHeader("Направления", "Отдельные подборки внутри общего каталога")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(
                    modifier = Modifier.width(260.dp).clickable { onCollectionClick(AnimeCollection.YURI) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF47213F))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("♡  РОМАНТИКА МЕЖДУ ДЕВУШКАМИ", color = Accent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text("Юри / Girls' Love (GL)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp))
                        Text("Также встречается название «сёдзё-ай»", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }

        val featured = catalog.firstOrNull()
        if (featured != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onAnimeClick(featured) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceTwo)
            ) {
                Box(Modifier.fillMaxWidth().height(190.dp)) {
                    AsyncImage(
                        model = featured.banner.ifBlank { featured.cover },
                        contentDescription = featured.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE100D1C)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                        Text("ПОПУЛЯРНОЕ СЕЙЧАС", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(featured.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("★ ${featured.score ?: "—"}   •   ${featured.year ?: "Год неизвестен"}", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        } else if (isLoading) {
            LoadingBlock("Загружаем аниме-каталог…")
        } else if (errorText != null) {
            ErrorBlock(errorText, onRetry)
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Популярные тайтлы", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = onCatalogClick) { Text("Весь каталог →", color = Accent) }
        }
        if (catalog.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(catalog.take(12), key = { it.id }) { anime ->
                    AnimePosterCard(anime = anime, modifier = Modifier.width(catalogDensity.homePosterWidth.dp), onClick = { onAnimeClick(anime) })
                }
            }
        }
        SectionHeader("Все жанры", "Нажми на жанр, чтобы отфильтровать каталог")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AnimeGenre.entries.toList(), key = { it.name }) { genre ->
                Surface(
                    modifier = Modifier.clickable { onGenreClick(genre) },
                    color = SurfaceTwo,
                    shape = CircleShape
                ) {
                    Text("${genre.icon} ${genre.title}", modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Text("Каталог и постеры берутся из AniList. Видеоисточники подключаются отдельно.", modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp), color = Muted, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogScreen(
    catalog: List<Anime>,
    catalogDensity: AnimePlayCatalogDensity,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean,
    errorText: String?,
    onRetry: () -> Unit,
    onAnimeClick: (Anime) -> Unit,
    selectedCollection: AnimeCollection,
    onCollectionSelected: (AnimeCollection) -> Unit,
    selectedGenre: AnimeGenre?,
    onGenreSelected: (AnimeGenre?) -> Unit,
    russianVoiceIds: Set<Int>
) {
    var onlyRussianVoice by remember { mutableStateOf(false) }
    val displayedCatalog = if (onlyRussianVoice) catalog.filter { it.id in russianVoiceIds } else catalog
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Каталог аниме", fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Название аниме…", color = Muted) },
            leadingIcon = { Text("⌕", fontSize = 24.sp, color = Accent) },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        Button(onClick = onSearch, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
            Text("Найти аниме")
        }
        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AnimeCollection.entries.toList(), key = { it.name }) { collection ->
                val selected = selectedCollection == collection
                Surface(
                    modifier = Modifier.clickable { onCollectionSelected(collection) },
                    color = if (selected) AccentPurple.copy(alpha = 0.25f) else SurfaceTwo,
                    shape = CircleShape
                ) {
                    Text(
                        collection.title,
                        color = if (selected) AccentPurple else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                    )
                }
            }
        }
        Text("Жанры", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 5.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item {
                Surface(
                    modifier = Modifier.clickable { onGenreSelected(null) },
                    color = if (selectedGenre == null) Accent.copy(alpha = 0.22f) else SurfaceTwo,
                    shape = CircleShape
                ) {
                    Text("Все", color = if (selectedGenre == null) Accent else Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            items(AnimeGenre.entries.toList(), key = { it.name }) { genre ->
                val selected = selectedGenre == genre
                Surface(
                    modifier = Modifier.clickable { onGenreSelected(if (selected) null else genre) },
                    color = if (selected) AccentPurple.copy(alpha = 0.25f) else SurfaceTwo,
                    shape = CircleShape
                ) {
                    Text("${genre.icon} ${genre.title}", color = if (selected) AccentPurple else Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { onlyRussianVoice = !onlyRussianVoice },
            color = if (onlyRussianVoice) Accent.copy(alpha = 0.20f) else SurfaceOne,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (onlyRussianVoice) "☑" else "□", color = Accent, fontSize = 16.sp)
                Text("  Только с добавленной русской озвучкой", color = Color.White, fontSize = 11.sp)
            }
        }
        if (isLoading) {
            LoadingBlock("Загружаем каталог…")
        } else if (errorText != null) {
            ErrorBlock(errorText, onRetry)
        } else if (displayedCatalog.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(catalogDensity.columns),
                modifier = Modifier.weight(1f).padding(top = 14.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(displayedCatalog, key = { it.id }) { anime ->
                    AnimePosterCard(anime = anime, modifier = Modifier.fillMaxWidth(), onClick = { onAnimeClick(anime) })
                }
            }
        } else if (onlyRussianVoice) {
            EmptyState("Пока нет тайтлов с добавленной русской озвучкой. Источник можно добавить на странице аниме.")
        } else if (errorText == null && !isLoading) {
            EmptyState("В этой подборке пока нет результатов. Попробуй обновить каталог.")
        }
    }
}

@Composable
private fun CollectionScreen(
    title: String,
    subtitle: String,
    items: List<Anime>,
    onAnimeClick: (Anime) -> Unit,
    emptyMessage: String,
    catalogDensity: AnimePlayCatalogDensity
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        if (items.isEmpty()) {
            EmptyState(emptyMessage)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(catalogDensity.columns),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(items, key = { it.id }) { anime ->
                    AnimePosterCard(anime, Modifier.fillMaxWidth()) { onAnimeClick(anime) }
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(
    episodes: List<UpcomingEpisode>,
    isLoading: Boolean,
    errorText: String?,
    onRefresh: () -> Unit
) {
    val dayFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale("ru")) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val groups = remember(episodes) {
        episodes.sortedBy { it.airingAtMillis }
            .groupBy { dayFormat.format(Date(it.airingAtMillis)).replaceFirstChar { it.uppercase() } }
            .toList()
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Календарь серий", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Что и когда выходит в ближайшие 3 недели", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
            OutlinedButton(onClick = onRefresh, enabled = !isLoading) { Text("↻", color = Accent, fontSize = 18.sp) }
        }
        Text("Время указано по часовому поясу телефона • источник: AniList", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        when {
            isLoading && episodes.isEmpty() -> LoadingBlock("Загружаем расписание выхода серий…")
            errorText != null && episodes.isEmpty() -> ErrorBlock(errorText, onRefresh)
            episodes.isEmpty() -> EmptyState("На ближайшие 3 недели нет опубликованных дат выхода. Обнови календарь позже.")
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { (day, itemsForDay) ->
                    item(key = "day_$day") {
                        Text(day, color = AccentPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
                    }
                    items(itemsForDay, key = { "${it.animeId}_${it.episode}_${it.airingAtMillis}" }) { entry ->
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(16.dp)) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = entry.cover,
                                    contentDescription = entry.animeTitle,
                                    modifier = Modifier.width(58.dp).height(78.dp).clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                                    Text(entry.animeTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("Серия ${entry.episode}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                                    Text("${timeFormat.format(Date(entry.airingAtMillis))} • по местному времени", color = Accent, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                                Text("◷", color = AccentPurple, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 5.dp))
                            }
                        }
                    }
                }
            }
        }
        if (isLoading && episodes.isNotEmpty()) {
            Text("Обновляем расписание…", color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(8.dp))
        }
        if (errorText != null && episodes.isNotEmpty()) {
            Text("Не удалось обновить: $errorText", color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(8.dp))
        }
    }
}



@Composable
private fun ReviewsSection(
    anime: Anime,
    reviews: List<AnimeReview>,
    onSaveReview: (AnimeReview) -> Unit
) {
    val ownReview = reviews.firstOrNull { it.author == "Ты" }
    var rating by remember(anime.id) { mutableStateOf(ownReview?.rating ?: 5) }
    var reviewText by remember(anime.id) { mutableStateOf(ownReview?.text.orEmpty()) }
    var notice by remember(anime.id) { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    SectionHeader("Отзывы зрителей", "Оцени аниме и поделись впечатлением")
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(if (ownReview == null) "Твой отзыв" else "Изменить свой отзыв", fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                (1..5).forEach { value ->
                    TextButton(onClick = { rating = value }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(if (value <= rating) "★" else "☆", color = if (value <= rating) Accent else Muted, fontSize = 25.sp)
                    }
                }
                Text("$rating/5", color = Muted, fontSize = 11.sp)
            }
            OutlinedTextField(
                value = reviewText,
                onValueChange = { reviewText = it.take(1200); notice = null },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                label = { Text("Что думаешь об этом аниме?") },
                minLines = 3,
                maxLines = 5
            )
            Button(
                onClick = {
                    onSaveReview(AnimeReview(
                        id = ownReview?.id ?: System.currentTimeMillis(),
                        animeId = anime.id,
                        author = "Ты",
                        rating = rating,
                        text = reviewText.trim(),
                        createdAtMillis = System.currentTimeMillis()
                    ))
                    notice = "Отзыв сохранён на этом устройстве"
                },
                enabled = reviewText.trim().length >= 3,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text(if (ownReview == null) "Опубликовать отзыв" else "Сохранить изменения") }
            notice?.let { Text(it, color = Accent, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)) }
            Text("Отзывы пока видны только на этом устройстве. Для общей ленты между пользователями понадобится сервер аккаунтов.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
        }
    }

    Text("Отзывы на этом устройстве (${reviews.size})", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp))
    if (reviews.isEmpty()) {
        Text("Пока нет отзывов. Стань первым зрителем, который поделится мнением!", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
    } else {
        reviews.sortedByDescending { it.createdAtMillis }.forEach { review ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceTwo), shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌸  ${review.author}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(dateFormat.format(Date(review.createdAtMillis)), color = Muted, fontSize = 10.sp)
                    }
                    Text("★".repeat(review.rating) + "☆".repeat(5 - review.rating), color = Accent, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(review.text, color = Color(0xFFE4DDEB), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun AnimePosterCard(anime: Anime, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceOne)
    ) {
        Column {
            AsyncImage(
                model = anime.cover,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.70f).background(SurfaceTwo),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(10.dp)) {
                Text(anime.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text("★ ${anime.score ?: "—"}   •   ${anime.year ?: "—"}", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun LoadingBlock(message: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Accent, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
        Text(message, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Не получилось загрузить", fontWeight = FontWeight.Bold)
        Text(message, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        OutlinedButton(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✦", color = Accent, fontSize = 34.sp)
        Text(message, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun AnimeDetailScreen(
    anime: Anime,
    isFavorite: Boolean,
    watchStatus: WatchStatus?,
    onWatchStatusChanged: (WatchStatus?) -> Unit,
    sources: List<MediaSource>,
    reviews: List<AnimeReview>,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSaveReview: (AnimeReview) -> Unit,
    onAddSource: (MediaSource) -> Unit,
    onWatch: (MediaSource) -> Unit,
    onDownload: (MediaSource) -> Unit
) {
    val context = LocalContext.current
    var showSourceDialog by remember { mutableStateOf(false) }
    var showAniLibertyPicker by remember { mutableStateOf(false) }
    var showWatchStatusDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Назад", color = Color.White) }
            TextButton(onClick = onToggleFavorite) { Text(if (isFavorite) "★ В избранном" else "☆ В избранное", color = Accent) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            OutlinedButton(
                onClick = { showWatchStatusDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Text(if (watchStatus == null) "＋ Добавить в мой список" else "✓ ${watchStatus.title}", color = Accent)
            }
            AsyncImage(
                model = anime.banner.ifBlank { anime.cover },
                contentDescription = anime.title,
                modifier = Modifier.fillMaxWidth().height(205.dp),
                contentScale = ContentScale.Crop
            )
            Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AsyncImage(
                    model = anime.cover,
                    contentDescription = null,
                    modifier = Modifier.width(112.dp).height(158.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f)) {
                    Text(anime.title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    if (anime.nativeTitle.isNotBlank()) Text(anime.nativeTitle, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                    Text("★ ${anime.score ?: "—"}   •   ${anime.year ?: "—"}", color = Accent, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                    Text("${anime.format.ifBlank { "Аниме" }}   •   ${anime.episodes?.let { "$it сер." } ?: "серии уточняются"}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                    Text(anime.genres.take(4).joinToString(" • "), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
                    if (anime.tags.any { it.equals("Yuri", true) || it.equals("Shoujo Ai", true) || it.equals("Girls' Love", true) }) {
                        Text("♡ Юри / Girls' Love (GL) · сёдзё-ай", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            Text("ОПИСАНИЕ", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            val cleanDescription = Html.fromHtml(anime.description, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            Text(cleanDescription.ifBlank { "Описание пока отсутствует в каталоге." }, fontSize = 13.sp, color = Color(0xFFD9D9E4), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))

            ReviewsSection(anime = anime, reviews = reviews, onSaveReview = onSaveReview)

            SectionHeader("Русская озвучка", "Ищи версии от студий и выбирай доступный источник")
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎙️", fontSize = 24.sp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text("AniLibria / AniLiberty", fontWeight = FontWeight.Bold)
                            Text("Поиск релиза и серий через API", color = Muted, fontSize = 10.sp)
                        }
                        Text("API", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text("Если источник показывает гео- или правообладательскую блокировку, AnimePlay не будет её обходить.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                    Button(onClick = { showAniLibertyPicker = true }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                        Text("Найти русскую озвучку")
                    }
                }
            }
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎧", fontSize = 24.sp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text("Студийная Банда / Studio Band", fontWeight = FontWeight.Bold)
                            Text("Официальный сайт студии", color = Muted, fontSize = 10.sp)
                        }
                        Text("WEB", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text("У студии найден официальный сайт с каталогом. Публичный API для встраивания плеера не опубликован, поэтому откроем сам сайт студии.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                    OutlinedButton(onClick = { openExternalPage(context, STUDIO_BAND_URL) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Открыть Studio Band", color = AccentPurple)
                    }
                }
            }
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Добавить свой источник", fontWeight = FontWeight.SemiBold)
                    Text("Только HTTPS-ссылки на видео, которое разрешено воспроизводить и скачивать.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Button(onClick = { showSourceDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)) {
                        Text("＋ Добавить источник / озвучку")
                    }
                }
            }

            if (sources.isNotEmpty()) {
                SectionHeader("Доступные озвучки", "Выбери дорожку, которую добавили для этого аниме")
                sources.forEach { source ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceTwo), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(13.dp)) {
                            Text(source.label, fontWeight = FontWeight.SemiBold)
                            Text(if (source.url.contains(".m3u8", true)) "Потоковое видео HLS" else "Прямая ссылка на медиа", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onWatch(source) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("▶ Смотреть") }
                                OutlinedButton(
                                    onClick = { onDownload(source) },
                                    enabled = isDirectDownloadUrl(source.url),
                                    modifier = Modifier.weight(1f)
                                ) { Text("⇩ Скачать") }
                            }
                            if (!isDirectDownloadUrl(source.url)) {
                                Text("Офлайн-загрузка в этой версии доступна для прямых MP4/M4V/WebM-ссылок.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                            }
                        }
                    }
                }
            } else {
                EmptyState("Источники ещё не добавлены. После подключения легального видео здесь появятся кнопки просмотра и загрузки.")
            }
        }
    }

    if (showSourceDialog) {
        AddSourceDialog(
            animeId = anime.id,
            onDismiss = { showSourceDialog = false },
            onSave = {
                onAddSource(it)
                showSourceDialog = false
            }
        )
    }
    if (showAniLibertyPicker) {
        AniLibertyPickerDialog(
            anime = anime,
            onDismiss = { showAniLibertyPicker = false },
            onPlay = { source ->
                onAddSource(source)
                onWatch(source)
                showAniLibertyPicker = false
            }
        )
    }
    if (showWatchStatusDialog) {
        AlertDialog(
            onDismissRequest = { showWatchStatusDialog = false },
            containerColor = SurfaceOne,
            title = { Text("Мой список") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    WatchStatus.entries.forEach { status ->
                        OutlinedButton(
                            onClick = {
                                onWatchStatusChanged(status)
                                showWatchStatusDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text((if (watchStatus == status) "✓  " else "") + status.title, color = if (watchStatus == status) Accent else Color.White)
                        }
                    }
                    if (watchStatus != null) {
                        TextButton(onClick = {
                            onWatchStatusChanged(null)
                            showWatchStatusDialog = false
                        }) { Text("Убрать из списка", color = Color(0xFFFF8C9D)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWatchStatusDialog = false }) { Text("Закрыть") } }
        )
    }
}

@Composable
private fun AddSourceDialog(
    animeId: Int,
    onDismiss: () -> Unit,
    onSave: (MediaSource) -> Unit
) {
    var label by remember { mutableStateOf("Русская озвучка") }
    var url by remember { mutableStateOf("") }
    var subtitleUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить видеоисточник") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Название озвучки") }, singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("HTTPS-ссылка на видео") }, singleLine = true)
                OutlinedTextField(value = subtitleUrl, onValueChange = { subtitleUrl = it }, label = { Text("Ссылка на субтитры (необязательно)") }, singleLine = true)
                Text("Вставляй только свой или официально разрешённый источник. Для офлайн-загрузки сейчас поддерживаются прямые MP4/M4V/WebM.", color = Muted, fontSize = 10.sp)
                if (error != null) Text(error!!, color = Color(0xFFFF8181), fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = runCatching { Uri.parse(url.trim()) }.getOrNull()
                if (label.isBlank()) error = "Укажи название озвучки."
                else if (parsed?.scheme != "https" || parsed.host.isNullOrBlank()) error = "Нужна корректная HTTPS-ссылка."
                else if (subtitleUrl.isNotBlank() && !subtitleUrl.startsWith("https://")) error = "Субтитры тоже должны иметь HTTPS-ссылку."
                else onSave(MediaSource(System.currentTimeMillis(), animeId, label.trim(), url.trim(), subtitleUrl.trim()))
            }) { Text("Сохранить", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun AniLibertyPickerDialog(
    anime: Anime,
    onDismiss: () -> Unit,
    onPlay: (MediaSource) -> Unit
) {
    val scope = rememberCoroutineScope()
    var matches by remember { mutableStateOf<List<AniLibertyRepository.ReleaseMatch>>(emptyList()) }
    var selectedRelease by remember { mutableStateOf<AniLibertyRepository.ReleaseDetails?>(null) }
    var selectedEpisodeId by remember { mutableStateOf<String?>(null) }
    var episodeStreams by remember { mutableStateOf<AniLibertyRepository.EpisodeStreams?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(anime.id) {
        isLoading = true
        errorText = null
        try {
            var found = AniLibertyRepository.searchReleases(anime.title)
            if (found.isEmpty() && anime.nativeTitle.isNotBlank() && anime.nativeTitle != anime.title) {
                found = AniLibertyRepository.searchReleases(anime.nativeTitle)
            }
            matches = found
            if (found.isEmpty()) errorText = "AniLibria / AniLiberty не нашёл подходящий релиз. Можно проверить официальный сайт вручную."
        } catch (error: Exception) {
            errorText = error.message ?: "Не удалось выполнить поиск источника."
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("AniLibria / AniLiberty")
                Text(anime.title, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                if (isLoading) {
                    LoadingBlock("Ищем релизы и озвучку…")
                } else if (selectedRelease == null) {
                    if (errorText != null) Text(errorText!!, color = Muted, fontSize = 12.sp)
                    matches.forEach { match ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !match.isBlocked) {
                                scope.launch {
                                    isLoading = true
                                    errorText = null
                                    try {
                                        selectedRelease = AniLibertyRepository.getRelease(match.id.toString())
                                    } catch (error: Exception) {
                                        errorText = error.message ?: "Не удалось открыть релиз."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = SurfaceTwo),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (match.posterUrl.isNotBlank()) {
                                    AsyncImage(model = match.posterUrl, contentDescription = null, modifier = Modifier.width(52.dp).height(72.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                }
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(match.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${match.year ?: "Год неизвестен"} · ${match.episodesTotal ?: "?"} сер.", color = Muted, fontSize = 10.sp)
                                    if (match.isBlocked) Text(AniLibertyRepository.blockMessage(match), color = Color(0xFFFF9AAE), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                                    else Text("Открыть список серий →", color = Accent, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                } else {
                    TextButton(onClick = {
                        selectedRelease = null
                        selectedEpisodeId = null
                        episodeStreams = null
                    }) { Text("← К результатам поиска", color = Accent) }
                    Text(selectedRelease!!.match.title, fontWeight = FontWeight.Bold)
                    Text("Выбери серию. Потоки доступны только в качестве, которое вернул источник.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(vertical = 6.dp))
                    if (selectedRelease!!.episodes.isEmpty()) {
                        EmptyState("В API пока нет списка серий для этого релиза.")
                    }
                    selectedRelease!!.episodes.forEach { episode ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = SurfaceTwo), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(9.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Серия ${episode.number}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        if (episode.name.isNotBlank()) Text(episode.name, color = Muted, fontSize = 10.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                errorText = null
                                                try {
                                                    episodeStreams = AniLibertyRepository.getEpisode(episode.id)
                                                    selectedEpisodeId = episode.id
                                                } catch (error: Exception) {
                                                    errorText = error.message ?: "Не удалось получить видеопоток серии."
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    ) { Text(if (selectedEpisodeId == episode.id) "Обновить" else "Качество", fontSize = 10.sp) }
                                }
                                if (selectedEpisodeId == episode.id && episodeStreams != null) {
                                    val streamChoices = listOf(
                                        "1080p" to episodeStreams!!.hls1080,
                                        "720p" to episodeStreams!!.hls720,
                                        "480p" to episodeStreams!!.hls480
                                    ).filter { it.second.startsWith("https://") }
                                    if (streamChoices.isEmpty()) {
                                        Text("Для серии нет доступного HLS-потока.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                                    } else {
                                        Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            streamChoices.forEach { (quality, url) ->
                                                Button(
                                                    onClick = {
                                                        val source = MediaSource(
                                                            id = System.currentTimeMillis() + quality.hashCode().toLong(),
                                                            animeId = anime.id,
                                                            label = "AniLibria / AniLiberty · Русская озвучка · серия ${episode.number} · $quality",
                                                            url = url
                                                        )
                                                        onPlay(source)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 5.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                                                ) { Text("▶ $quality", fontSize = 10.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (isLoading) CircularProgressIndicator(color = Accent, modifier = Modifier.size(22.dp).align(Alignment.CenterHorizontally))
                    if (errorText != null) Text(errorText!!, color = Color(0xFFFF9AAE), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = Accent) } }
    )
}

@Composable
private fun DownloadsScreen(
    downloads: List<DownloadRecord>,
    onPlay: (DownloadRecord) -> Unit,
    onRefresh: () -> Unit,
    refreshToken: Int
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Загрузки", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Серии, сохранённые на этом устройстве", fontSize = 11.sp, color = Muted)
            }
            TextButton(onClick = onRefresh) { Text("Обновить", color = Accent) }
        }
        if (downloads.isEmpty()) {
            EmptyState("Здесь будут серии, скачанные для просмотра без интернета.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(downloads, key = { "${it.downloadId}-$refreshToken" }) { record ->
                    val status = downloadStatus(LocalContext.current, record.downloadId)
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceTwo), contentAlignment = Alignment.Center) {
                                Text("⇩", color = Accent, fontSize = 21.sp)
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(record.animeTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(record.voiceLabel, color = Muted, fontSize = 11.sp)
                                Text(status, color = if (status == "Готово офлайн") Color(0xFF7BE0A3) else AccentPurple, fontSize = 10.sp)
                            }
                            if (status == "Готово офлайн") {
                                TextButton(onClick = { onPlay(record) }) { Text("▶", color = Accent) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    favoritesCount: Int,
    downloadsCount: Int,
    sourceCount: Int,
    selectedPalette: AnimePlayPalette,
    selectedIconStyle: AnimePlayIconStyle,
    selectedCatalogDensity: AnimePlayCatalogDensity,
    selectedLauncherIcon: AnimePlayLauncherIcon,
    selectedCharacterAvatar: AnimeCharacterAvatar?,
    watchlist: List<WatchlistEntry>,
    selectedFrame: AnimeProfileFrame,
    onRemoveWatchEntry: (WatchlistEntry) -> Unit,
    onCharacterAvatarSelected: (AnimeCharacterAvatar?) -> Unit,
    onFrameSelected: (AnimeProfileFrame) -> Unit,
    onPaletteSelected: (AnimePlayPalette) -> Unit,
    onIconStyleSelected: (AnimePlayIconStyle) -> Unit,
    onCatalogDensitySelected: (AnimePlayCatalogDensity) -> Unit,
    onLauncherIconSelected: (AnimePlayLauncherIcon) -> Unit,
    onOpenStudioBand: () -> Unit
) {
    var showCharacterPicker by remember { mutableStateOf(false) }
    var selectedListStatus by remember { mutableStateOf<WatchStatus?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Профиль", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Твоя личная аниме-библиотека", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(84.dp).border(4.dp, selectedFrame.color, CircleShape).padding(3.dp).clip(CircleShape)) {
                        if (selectedCharacterAvatar != null) {
                            AsyncImage(model = selectedCharacterAvatar.imageUrl, contentDescription = selectedCharacterAvatar.characterName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Box(
                                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(SurfaceTwo, AccentPurple.copy(alpha = 0.35f)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✦", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Твой аниме-профиль", fontWeight = FontWeight.Bold)
                        Text(selectedCharacterAvatar?.let { "${it.characterName} • ${it.animeTitle}" } ?: "Пока нет выбранного персонажа", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Рамка: ${selectedFrame.title}", color = Accent, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                }
                SectionHeader("Аватар из любимого аниме", "Найди тайтл и выбери его персонажа")
                Button(onClick = { showCharacterPicker = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                    Text(if (selectedCharacterAvatar == null) "Выбрать персонажа" else "Поменять персонажа")
                }
                if (selectedCharacterAvatar != null) {
                    Text("Источник изображения: ${selectedCharacterAvatar.source}. Изображение загружается по сети.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                    TextButton(onClick = { onCharacterAvatarSelected(null) }) { Text("Сбросить персонажа", color = Muted) }
                } else {
                    Text("Аватар появится здесь после выбора персонажа из любимого аниме.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                }
                SectionHeader("Рамки аватара", "Сохраняются в профиле")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AnimeProfileFrame.entries, key = { it.id }) { frame ->
                        val active = selectedFrame == frame
                        Card(Modifier.clickable { onFrameSelected(frame) }, colors = CardDefaults.cardColors(containerColor = if (active) SurfaceTwo else Background), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(frame.symbol, color = if (frame == AnimeProfileFrame.NONE) Muted else frame.color, fontSize = 19.sp)
                                Text(frame.title, color = if (active) Accent else Muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Избранное", favoritesCount.toString(), Modifier.weight(1f))
            StatCard("Загрузки", downloadsCount.toString(), Modifier.weight(1f))
            StatCard("Источники", sourceCount.toString(), Modifier.weight(1f))
        }
        SectionHeader("Мои списки", "Твой прогресс просмотра хранится на устройстве")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WatchStatus.entries.toList(), key = { it.id }) { status ->
                val count = watchlist.count { it.status == status }
                Card(
                    modifier = Modifier.clickable { selectedListStatus = status },
                    colors = CardDefaults.cardColors(containerColor = SurfaceTwo),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                        Text(count.toString(), color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(status.title, color = Color.White, fontSize = 10.sp)
                        Text("Открыть список →", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
        if (watchlist.isNotEmpty()) {
            Text("Последние изменения", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            watchlist.sortedByDescending { it.updatedAtMillis }.take(5).forEach { entry ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = entry.coverUrl, contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(entry.animeTitle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.status.title, color = AccentPurple, fontSize = 10.sp)
                    }
                }
            }
        }

        SectionHeader("Оформление", "Выбери настроение AnimePlay")
        AnimePlayPalette.entries.forEach { preset ->
            val selected = selectedPalette == preset
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onPaletteSelected(preset) },
                colors = CardDefaults.cardColors(containerColor = preset.surfaceOne),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(preset.background, preset.surfaceTwo, preset.accent, preset.accentSecondary).forEach { color ->
                            Box(Modifier.size(22.dp).clip(CircleShape).background(color))
                        }
                    }
                    Text(preset.title, modifier = Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
                    Text(if (selected) "✓ Выбрано" else "Выбрать", color = if (selected) preset.accent else Muted, fontSize = 11.sp)
                }
            }
        }

        SectionHeader("Стиль иконок", "Можно менять значки нижнего меню")
        AnimePlayIconStyle.entries.forEach { style ->
            val selected = selectedIconStyle == style
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onIconStyleSelected(style) },
                colors = CardDefaults.cardColors(containerColor = SurfaceOne),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (style) {
                            AnimePlayIconStyle.OUTLINE -> "⌂   ▤   ▣   ♡   ⇩   ◉"
                            AnimePlayIconStyle.NEON -> "✦   ▦   ◷   ♥   ⇩   ☾"
                            AnimePlayIconStyle.PASTEL -> "🏠   🎬   🗓️   💗   📥   🌸"
                        },
                        color = Accent,
                        fontSize = 19.sp
                    )
                    Text(style.title, modifier = Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
                    Text(if (selected) "✓" else "", color = AccentPurple, fontSize = 18.sp)
                }
            }
        }

        SectionHeader("Размер каталога", "Выбери, сколько постеров помещается на экране")
        AnimePlayCatalogDensity.entries.forEach { density ->
            val selected = selectedCatalogDensity == density
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onCatalogDensitySelected(density) },
                colors = CardDefaults.cardColors(containerColor = SurfaceOne),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (density == AnimePlayCatalogDensity.COMPACT) "▦ ▦ ▦" else if (density == AnimePlayCatalogDensity.STANDARD) "▦ ▦" else "▣", color = Accent, fontSize = 18.sp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(density.title, fontWeight = FontWeight.SemiBold)
                        Text(density.description, color = Muted, fontSize = 10.sp)
                    }
                    Text(if (selected) "✓" else "", color = AccentPurple, fontSize = 18.sp)
                }
            }
        }

        SectionHeader("Иконка приложения", "Измени значок AnimePlay на рабочем столе телефона")
        AnimePlayLauncherIcon.entries.forEach { icon ->
            val selected = selectedLauncherIcon == icon
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onLauncherIconSelected(icon) },
                colors = CardDefaults.cardColors(containerColor = SurfaceOne),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    val preview = when (icon) {
                        AnimePlayLauncherIcon.ORCHID -> "💜▶"
                        AnimePlayLauncherIcon.SAKURA -> "🌸▶"
                        AnimePlayLauncherIcon.NEON -> "💗◆"
                    }
                    Text(preview, fontSize = 22.sp)
                    Text(icon.title, modifier = Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
                    Text(if (selected) "✓ Выбрано" else "Выбрать", color = if (selected) Accent else Muted, fontSize = 11.sp)
                }
            }
        }

        SectionHeader("Источники русской озвучки", "Переход к площадкам и встроенный поиск")
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("AniLibria / AniLiberty", fontWeight = FontWeight.Bold)
                Text("Поиск релизов, эпизоды и HLS-потоки через API V1. Недоступные по региону или авторским ограничениям релизы не разблокируются.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                TextButton(onClick = { /* source is searched from each anime detail */ }) {
                    Text("Поиск доступен на странице каждого аниме", color = Accent)
                }
            }
        }
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("Студийная Банда / Studio Band", fontWeight = FontWeight.Bold)
                Text("Официальный сайт студии с собственным каталогом дубляжа.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                Button(onClick = onOpenStudioBand, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                    Text("Открыть сайт студии")
                }
            }
        }

        SectionHeader("Возможности", "Что уже предусмотрено")
        listOf(
            "Каталог и поиск аниме по подключённому каталогу метаданных",
            "Постеры, описания, жанры и рейтинги",
            "Юри / Girls' Love как направление внутри общего каталога",
            "Настраиваемые цвета, наборы значков, иконка приложения и размер каталога",
            "Избранное хранится на устройстве",
            "Календарь будущих серий на 21 день с данными AniList",
            "Выбор портрета персонажа из найденного аниме через Kitsu и 5 рамок профиля",
            "Личные списки: смотрю, просмотрено, в планах, отложено и брошено; можно открыть каждый список и убрать тайтл",
            "Оценки и отзывы к аниме (локальное хранение на устройстве)",
            "Видеоплеер Android Media3 с HLS",
            "Поиск релизов и серий AniLibria / AniLiberty",
            "Переход на официальный сайт Studio Band",
            "Офлайн-загрузка прямых MP4/M4V/WebM-ссылок"
        ).forEach { line ->
            Text("•  $line", color = Color(0xFFDADAE5), fontSize = 13.sp, modifier = Modifier.padding(vertical = 5.dp))
        }
        Text("AniList предоставляет метаданные, а не видео. Для публичного выпуска необходимо соблюдать условия каждого источника и иметь разрешение на встраивание/распространение контента.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Версия приложения: 0.1.6-dev • AnimePlay", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 18.dp))
    }
    if (selectedListStatus != null) {
        WatchlistStatusDialog(
            status = selectedListStatus!!,
            entries = watchlist.filter { it.status == selectedListStatus },
            onDismiss = { selectedListStatus = null },
            onRemove = onRemoveWatchEntry
        )
    }
    if (showCharacterPicker) {
        CharacterAvatarPickerDialog(
            onDismiss = { showCharacterPicker = false },
            onSelect = { avatar ->
                onCharacterAvatarSelected(avatar)
                showCharacterPicker = false
            }
        )
    }
}

@Composable
private fun WatchlistStatusDialog(
    status: WatchStatus,
    entries: List<WatchlistEntry>,
    onDismiss: () -> Unit,
    onRemove: (WatchlistEntry) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceOne,
        title = { Text(status.title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            if (entries.isEmpty()) {
                Text("В этом списке пока пусто. Добавляй аниме со страницы тайтла.", color = Muted)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.animeId }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceTwo).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = entry.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                                Text(entry.animeTitle, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(status.description, color = Muted, fontSize = 10.sp)
                            }
                            TextButton(onClick = { onRemove(entry) }) { Text("Убрать", color = Color(0xFFFF9CA9), fontSize = 10.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово", color = Accent) } }
    )
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = Accent, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text(title, color = Muted, fontSize = 10.sp)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(request: PlaybackRequest, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(request.mediaUrl) {
        ExoPlayer.Builder(context).build().apply {
            val itemBuilder = MediaItem.Builder().setUri(Uri.parse(request.mediaUrl))
            if (request.subtitleUrl.isNotBlank()) {
                val mime = if (request.subtitleUrl.substringBefore('?').endsWith(".srt", true)) {
                    MimeTypes.APPLICATION_SUBRIP
                } else {
                    MimeTypes.TEXT_VTT
                }
                val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.parse(request.subtitleUrl))
                    .setMimeType(mime)
                    .setLanguage("ru")
                    .setLabel("Русские субтитры")
                    .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                    .build()
                itemBuilder.setSubtitleConfigurations(listOf(subtitle))
            }
            setMediaItem(itemBuilder.build())
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().background(Background).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад", color = Color.White) }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(request.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(request.voiceLabel, color = Muted, fontSize = 10.sp)
            }
        }
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = true
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
        )
        Text("Выбор озвучки (если поток передаёт языковые дорожки):", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 5.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setPreferredAudioLanguage("ru").build() }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Русская", fontSize = 10.sp) }
            OutlinedButton(onClick = { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setPreferredAudioLanguage("ja").build() }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Японская", fontSize = 10.sp) }
            OutlinedButton(onClick = { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setPreferredAudioLanguage("en").build() }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Английская", fontSize = 10.sp) }
        }
        Text("Можно также выбрать доступные дорожки в меню плеера. Если у видео нет нужного языка, он не появится.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(16.dp))
        Spacer(Modifier.weight(1f))
        Text("AnimePlay • воспроизводится предоставленный источник", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(18.dp))
    }
}


@Composable
private fun CharacterAvatarPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (AnimeCharacterAvatar) -> Unit
) {
    val scope = rememberCoroutineScope()
    val animeResults = remember { mutableStateListOf<KitsuAnimeChoice>() }
    val characters = remember { mutableStateListOf<AnimeCharacterAvatar>() }
    var query by remember { mutableStateOf("") }
    var selectedAnime by remember { mutableStateOf<KitsuAnimeChoice?>(null) }
    var characterFilter by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceOne,
        title = {
            Column {
                Text("Аватар из любимого аниме", fontWeight = FontWeight.Bold)
                Text("Сначала найди аниме, затем выбери персонажа", color = Muted, fontSize = 11.sp)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                if (selectedAnime == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Название аниме") },
                        placeholder = { Text("Например, Naruto или Frieren") }
                    )
                    Text("Если русское название не находится, попробуй английское или японское.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                animeResults.clear()
                                try {
                                    animeResults.addAll(KitsuCharacterRepository.searchAnime(query))
                                    if (animeResults.isEmpty()) error = "Ничего не найдено. Попробуй другое название."
                                } catch (e: Exception) {
                                    error = e.message ?: "Не удалось найти аниме. Проверь подключение к интернету."
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        enabled = query.trim().length >= 2 && !loading,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) { Text("Найти аниме") }
                    if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(12.dp), color = Accent)
                    error?.let { Text(it, color = Color(0xFFFF9CA9), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
                    LazyColumn(Modifier.heightIn(max = 360.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(animeResults, key = { it.id }) { anime ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceTwo).clickable {
                                    selectedAnime = anime
                                    characterFilter = ""
                                    scope.launch {
                                        loading = true
                                        error = null
                                        characters.clear()
                                        try {
                                            characters.addAll(KitsuCharacterRepository.getAnimeCharacters(anime))
                                            if (characters.isEmpty()) error = "Для этого тайтла пока не найден список персонажей с изображениями."
                                        } catch (e: Exception) {
                                            error = e.message ?: "Не удалось загрузить персонажей."
                                        } finally {
                                            loading = false
                                        }
                                    }
                                }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(model = anime.coverUrl, contentDescription = null, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(7.dp)), contentScale = ContentScale.Crop)
                                Text(anime.title, modifier = Modifier.weight(1f).padding(start = 10.dp), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("→", color = Accent, fontSize = 18.sp)
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            selectedAnime = null
                            characterFilter = ""
                            characters.clear()
                            error = null
                        }) { Text("← Аниме", color = Accent) }
                        Text(selectedAnime!!.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(12.dp), color = Accent)
                    error?.let { Text(it, color = Color(0xFFFF9CA9), fontSize = 11.sp, modifier = Modifier.padding(vertical = 6.dp)) }
                    OutlinedTextField(
                        value = characterFilter,
                        onValueChange = { characterFilter = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        singleLine = true,
                        label = { Text("Найти персонажа") },
                        placeholder = { Text("Имя героя") }
                    )
                    val visibleCharacters = characters.filter { it.characterName.contains(characterFilter.trim(), ignoreCase = true) }
                    if (!loading && visibleCharacters.isEmpty() && characters.isNotEmpty()) {
                        Text("Персонаж не найден в этом списке. Попробуй другое имя.", color = Muted, fontSize = 11.sp)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(300.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleCharacters, key = { it.id }) { character ->
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceTwo).clickable { onSelect(character) }.padding(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = character.imageUrl,
                                    contentDescription = character.characterName,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(character.characterName, color = Color.White, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                            }
                        }
                    }
                    Text("Изображения подгружаются из Kitsu; доступность зависит от источника.", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = Accent) } }
    )
}
