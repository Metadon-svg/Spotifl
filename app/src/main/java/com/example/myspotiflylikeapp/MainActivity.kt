package com.example.myvibemusic

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage

// --- DATA ---
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val audioUrl: String
)

// Используем реальные HTTPS ссылки на картинки
val sampleSongs = listOf(
    Song("1", "Nightcall", "Kavinsky", "https://picsum.photos/id/10/500/500", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
    Song("2", "Resonance", "Home", "https://picsum.photos/id/11/500/500", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
    Song("3", "Midnight City", "M83", "https://picsum.photos/id/12/500/500", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
    Song("4", "After Dark", "Mr.Kitty", "https://picsum.photos/id/13/500/500", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
    Song("5", "Space Song", "Beach House", "https://picsum.photos/id/14/500/500", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
    Song("6", "Dark Beach", "Pastel Ghost", "https://picsum.photos/id/15/500/500", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"),
    Song("7", "Memory", "Direct", "https://picsum.photos/id/16/500/500", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3"),
    Song("8", "Hope", "Roosevelt", "https://picsum.photos/id/17/500/500", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3")
)

// --- COLORS (YT Music Style) ---
val YTBlack = Color(0xFF0F0F0F) // Почти черный, как в YT Music
val YTDarkGray = Color(0xFF212121)
val YTLightGray = Color(0xFFAAAAAA)
val White = Color.White

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YTMusicCloneApp(
                playMusic = { url -> playAudio(url) },
                pauseMusic = { pauseAudio() },
                resumeMusic = { resumeAudio() }
            )
        }
    }

    private fun playAudio(url: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun pauseAudio() {
        if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
    }
    private fun resumeAudio() {
        mediaPlayer?.start()
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

@Composable
fun YTMusicCloneApp(playMusic: (String) -> Unit, pauseMusic: () -> Unit, resumeMusic: () -> Unit) {
    val navController = rememberNavController()
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }

    val onSongSelect: (Song) -> Unit = { song ->
        currentSong = song
        isPlaying = true
        playMusic(song.audioUrl)
    }

    val onPlayToggle: () -> Unit = {
        if (isPlaying) pauseMusic() else resumeMusic()
        isPlaying = !isPlaying
    }

    MaterialTheme(colorScheme = darkColorScheme(background = YTBlack, primary = White)) {
        // Box позволяет накладывать слои друг на друга (Плеер поверх меню)
        Box(modifier = Modifier.fillMaxSize().background(YTBlack)) {
            
            // Слой 1: Основной контент (Scaffold с меню)
            Scaffold(
                bottomBar = {
                    // Меню скрываем, если открыт полный плеер, чтобы не просвечивало
                    if (!showFullPlayer) {
                        Column {
                             // MINI PLAYER (Всегда над меню)
                            if (currentSong != null) {
                                MiniPlayer(
                                    song = currentSong!!,
                                    isPlaying = isPlaying,
                                    onTogglePlay = onPlayToggle,
                                    onClick = { showFullPlayer = true }
                                )
                            }
                            // NAVIGATION
                            NavigationBar(containerColor = YTBlack) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Rounded.Home, "Home") },
                                    label = { Text("Home") },
                                    selected = navController.currentDestination?.route == "home",
                                    onClick = { navController.navigate("home") },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = YTDarkGray, selectedIconColor = White, unselectedIconColor = YTLightGray, selectedTextColor = White, unselectedTextColor = YTLightGray)
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Rounded.Explore, "Explore") },
                                    label = { Text("Explore") },
                                    selected = navController.currentDestination?.route == "explore",
                                    onClick = { navController.navigate("explore") },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = YTDarkGray, selectedIconColor = White, unselectedIconColor = YTLightGray, selectedTextColor = White, unselectedTextColor = YTLightGray)
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Rounded.LibraryMusic, "Library") },
                                    label = { Text("Library") },
                                    selected = navController.currentDestination?.route == "library",
                                    onClick = { navController.navigate("library") },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = YTDarkGray, selectedIconColor = White, unselectedIconColor = YTLightGray, selectedTextColor = White, unselectedTextColor = YTLightGray)
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
                    composable("home") { HomeScreen(onSongSelect) }
                    composable("explore") { ExploreScreen(onSongSelect) }
                    composable("library") { LibraryScreen(onSongSelect) }
                }
            }

            // Слой 2: FULL PLAYER (Модальное окно поверх всего)
            // Используем AnimatedVisibility для красивого выезда снизу
            AnimatedVisibility(
                visible = showFullPlayer,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentSong != null) {
                    FullPlayer(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        onClose = { showFullPlayer = false },
                        onTogglePlay = onPlayToggle
                    )
                }
            }
        }
    }
}

// --- SCREENS ---

@Composable
fun HomeScreen(onSongClick: (Song) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                Icon(Icons.Default.MusicNote, null, tint = White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Music", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Search, null, tint = White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.AccountCircle, null, tint = White, modifier = Modifier.size(28.dp))
            }
        }

        // Chips (Категории)
        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                listOf("Energize", "Workout", "Relax", "Focus", "Commute").forEach { category ->
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF333333))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(category, color = White, fontSize = 14.sp)
                    }
                }
            }
        }

        // Section: Listen Again (Grid 2 columns horizontal style)
        item {
            Text("Listen again", color = YTLightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            // Имитация сетки из 4 элементов
            Column {
                Row(Modifier.fillMaxWidth()) {
                    HomeGridItem(sampleSongs[0], onSongClick, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    HomeGridItem(sampleSongs[1], onSongClick, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    HomeGridItem(sampleSongs[2], onSongClick, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    HomeGridItem(sampleSongs[3], onSongClick, Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Section: Quick Picks (Horizontal List)
        item {
            Text("Quick picks", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow {
                items(sampleSongs.reversed()) { song ->
                    Column(
                        modifier = Modifier
                            .width(160.dp)
                            .padding(end = 16.dp)
                            .clickable { onSongClick(song) }
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(160.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(song.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = YTLightGray, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
             Spacer(modifier = Modifier.height(100.dp)) // Padding for miniplayer
        }
    }
}

@Composable
fun HomeGridItem(song: Song, onClick: (Song) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(YTDarkGray)
            .clickable { onClick(song) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).background(Color.Gray)
        )
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(song.title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(song.artist, color = YTLightGray, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun ExploreScreen(onSongClick: (Song) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Explore", color = White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp)) }
        item {
            Text("New Albums", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(sampleSongs) { song ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { onSongClick(song) }, verticalAlignment = Alignment.CenterVertically) {
                Text((sampleSongs.indexOf(song) + 1).toString(), color = YTLightGray, modifier = Modifier.width(30.dp))
                AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(song.title, color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(song.artist, color = YTLightGray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(onSongClick: (Song) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Library", color = White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
             items(sampleSongs) { song ->
                 Column(modifier = Modifier.clickable { onSongClick(song) }) {
                     AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color.Gray))
                     Spacer(modifier = Modifier.height(8.dp))
                     Text(song.title, color = White, fontWeight = FontWeight.Medium)
                 }
             }
        }
    }
}

// --- PLAYER COMPONENTS ---

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, onTogglePlay: () -> Unit, onClick: () -> Unit) {
    // В YT Music мини-плеер немного приподнят над навигацией
    Column(modifier = Modifier.fillMaxWidth().background(YTDarkGray).clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(song.title, color = White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = YTLightGray, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onTogglePlay) {
                Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = White)
            }
            IconButton(onClick = { /* Close logic if needed */ }) {
                Icon(Icons.Rounded.SkipNext, null, tint = White)
            }
        }
        // Progress Line at very bottom of miniplayer
        LinearProgressIndicator(progress = 0.4f, modifier = Modifier.fillMaxWidth().height(2.dp), color = White, trackColor = Color.Transparent)
    }
}

@Composable
fun FullPlayer(song: Song, isPlaying: Boolean, onClose: () -> Unit, onTogglePlay: () -> Unit) {
    // Градиентный фон
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3E2723), YTBlack))) // Коричнево-черный градиент
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Header: Chevron Down (Collapse)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, "Close", tint = White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.MoreVert, "Menu", tint = White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Big Artwork
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Info
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(song.artist, color = YTLightGray, fontSize = 18.sp)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ThumbUpOffAlt, "Like", tint = White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress
            Slider(value = 0.4f, onValueChange = {}, colors = SliderDefaults.colors(thumbColor = White, activeTrackColor = White))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1:20", color = YTLightGray, fontSize = 12.sp)
                Text("3:50", color = YTLightGray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Shuffle, null, tint = YTLightGray) }
                IconButton(onClick = {}, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.SkipPrevious, null, tint = White, modifier = Modifier.fillMaxSize()) }
                
                // Play Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(White)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(40.dp))
                }

                IconButton(onClick = {}, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.SkipNext, null, tint = White, modifier = Modifier.fillMaxSize()) }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Repeat, null, tint = YTLightGray) }
            }
        }
    }
}
