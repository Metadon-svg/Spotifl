package com.example.myvibemusic

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope // ВАЖНЫЙ ИМПОРТ
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// --- DATA ---
data class Song(
    val id: String, // YOUTUBE VIDEO ID
    val title: String,
    val artist: String,
    val coverUrl: String
)

// СПИСОК РЕАЛЬНЫХ ВИДЕО С YOUTUBE
val sampleSongs = listOf(
    Song("MV_3Dpw-BRY", "Nightcall", "Kavinsky", "https://i.ytimg.com/vi/MV_3Dpw-BRY/hqdefault.jpg"),
    Song("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"),
    Song("mUkfiLjooxs", "Heat Waves", "Glass Animals", "https://i.ytimg.com/vi/mUkfiLjooxs/hqdefault.jpg"),
    Song("9HDEHj2yzew", "Space Song", "Beach House", "https://i.ytimg.com/vi/9HDEHj2yzew/hqdefault.jpg"),
    Song("SDTZ7iX4vTQ", "After Dark", "Mr.Kitty", "https://i.ytimg.com/vi/SDTZ7iX4vTQ/hqdefault.jpg"),
    Song("1-xGerv5FOk", "Summertime Sadness", "Lana Del Rey", "https://i.ytimg.com/vi/1-xGerv5FOk/hqdefault.jpg"),
    Song("fHI8X4OXluQ", "Blinding Lights (Synthwave)", "The Weeknd", "https://i.ytimg.com/vi/fHI8X4OXluQ/hqdefault.jpg"),
    Song("L_jWHffIx5E", "All Star", "Smash Mouth", "https://i.ytimg.com/vi/L_jWHffIx5E/hqdefault.jpg")
)

// --- COLORS ---
val YTBlack = Color(0xFF0F0F0F)
val YTDarkGray = Color(0xFF212121)
val YTLightGray = Color(0xFFAAAAAA)
val White = Color.White
val AccentRed = Color(0xFFFF0000)

// --- REPOSITORY: Добываем прямую ссылку на аудио ---
object YouTubeRepository {
    // Публичный API Piped
    private const val API_URL = "https://pipedapi.kavin.rocks/streams/"

    suspend fun getAudioStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(API_URL + videoId)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = Gson().fromJson(jsonStr, JsonObject::class.java)
                val audioStreams = jsonObject.getAsJsonArray("audioStreams")

                var bestUrl: String? = null
                var maxBitrate = 0

                // Ищем лучший поток m4a
                for (i in 0 until audioStreams.size()) {
                    val stream = audioStreams.get(i).asJsonObject
                    val bitrate = stream.get("bitrate").asInt
                    val mimeType = stream.get("mimeType").asString
                    
                    if (mimeType.contains("mp4") || mimeType.contains("m4a")) {
                        if (bitrate > maxBitrate) {
                            maxBitrate = bitrate
                            bestUrl = stream.get("url").asString
                        }
                    }
                }
                bestUrl
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YTMusicCloneApp(
                playMusic = { videoId, onReady -> playAudio(videoId, onReady) },
                pauseMusic = { pauseAudio() },
                resumeMusic = { resumeAudio() }
            )
        }
    }

    private fun playAudio(videoId: String, onReady: (Boolean) -> Unit) {
        // ИСПРАВЛЕНИЕ ЗДЕСЬ: используем lifecycleScope напрямую
        lifecycleScope.launch {
            val streamUrl = YouTubeRepository.getAudioStreamUrl(videoId)
            if (streamUrl != null) {
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        setDataSource(streamUrl)
                        prepareAsync()
                        setOnPreparedListener { 
                            start() 
                            onReady(true)
                        }
                        setOnErrorListener { _, _, _ ->
                            onReady(false)
                            false
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onReady(false)
                }
            } else {
                onReady(false)
            }
        }
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
fun YTMusicCloneApp(
    playMusic: (String, (Boolean) -> Unit) -> Unit, 
    pauseMusic: () -> Unit, 
    resumeMusic: () -> Unit
) {
    val navController = rememberNavController()
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val onSongSelect: (Song) -> Unit = { song ->
        currentSong = song
        isLoading = true
        isPlaying = false
        
        playMusic(song.id) { success ->
            isLoading = false
            if (success) {
                isPlaying = true
            } else {
                Toast.makeText(context, "Failed to load. API might be limited.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onPlayToggle: () -> Unit = {
        if (isPlaying) pauseMusic() else resumeMusic()
        isPlaying = !isPlaying
    }

    MaterialTheme(colorScheme = darkColorScheme(background = YTBlack, primary = White)) {
        Box(modifier = Modifier.fillMaxSize().background(YTBlack)) {
            Scaffold(
                bottomBar = {
                    if (!showFullPlayer) {
                        Column {
                            if (currentSong != null) {
                                MiniPlayer(
                                    song = currentSong!!,
                                    isPlaying = isPlaying,
                                    isLoading = isLoading,
                                    onTogglePlay = onPlayToggle,
                                    onClick = { showFullPlayer = true }
                                )
                            }
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
                        isLoading = isLoading,
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
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                Text("Music", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start=4.dp))
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Search, null, tint = White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.AccountCircle, null, tint = White, modifier = Modifier.size(28.dp))
            }
        }

        item {
            Text("Listen again", color = YTLightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow {
                items(sampleSongs) { song ->
                    Column(modifier = Modifier.width(120.dp).padding(end=16.dp).clickable { onSongClick(song) }) {
                        AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.size(120.dp).clip(RoundedCornerShape(4.dp)).background(Color.DarkGray))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(song.title, color = White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = YTLightGray, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        item {
            Text("Recommended Music Videos", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.height(400.dp)) {
                items(sampleSongs.reversed()) { song ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { onSongClick(song) }, verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)).background(Color.DarkGray), contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(song.title, color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(song.artist, color = YTLightGray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExploreScreen(onSongClick: (Song) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Explore", color = White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp)) }
        items(sampleSongs.shuffled()) { song ->
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

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, isLoading: Boolean, onTogglePlay: () -> Unit, onClick: () -> Unit) {
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
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = White, strokeWidth = 2.dp)
                } else {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = White)
                }
            }
        }
        LinearProgressIndicator(progress = if(isLoading) 0f else 0.4f, modifier = Modifier.fillMaxWidth().height(2.dp), color = AccentRed, trackColor = Color.Transparent)
    }
}

@Composable
fun FullPlayer(song: Song, isPlaying: Boolean, isLoading: Boolean, onClose: () -> Unit, onTogglePlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3E2723), YTBlack)))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, "Close", tint = White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

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

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(song.artist, color = YTLightGray, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Slider(value = 0.4f, onValueChange = {}, colors = SliderDefaults.colors(thumbColor = White, activeTrackColor = White))
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Shuffle, null, tint = YTLightGray) }
                IconButton(onClick = {}, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.SkipPrevious, null, tint = White, modifier = Modifier.fillMaxSize()) }
                
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(White)
                        .clickable { if(!isLoading) onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                     if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), color = YTBlack)
                    } else {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(40.dp))
                    }
                }

                IconButton(onClick = {}, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.SkipNext, null, tint = White, modifier = Modifier.fillMaxSize()) }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Repeat, null, tint = YTLightGray) }
            }
        }
    }
}
