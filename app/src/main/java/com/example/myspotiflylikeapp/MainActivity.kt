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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// --- DATA ---
data class Song(
    val id: String, // Youtube ID
    val title: String,
    val artist: String,
    val coverUrl: String
)

val sampleSongs = listOf(
    Song("MV_3Dpw-BRY", "Nightcall", "Kavinsky", "https://i.ytimg.com/vi/MV_3Dpw-BRY/hqdefault.jpg"),
    Song("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"),
    Song("mUkfiLjooxs", "Heat Waves", "Glass Animals", "https://i.ytimg.com/vi/mUkfiLjooxs/hqdefault.jpg"),
    Song("9HDEHj2yzew", "Space Song", "Beach House", "https://i.ytimg.com/vi/9HDEHj2yzew/hqdefault.jpg"),
    Song("SDTZ7iX4vTQ", "After Dark", "Mr.Kitty", "https://i.ytimg.com/vi/SDTZ7iX4vTQ/hqdefault.jpg"),
    Song("1-xGerv5FOk", "Summertime Sadness", "Lana Del Rey", "https://i.ytimg.com/vi/1-xGerv5FOk/hqdefault.jpg"),
    Song("L_jWHffIx5E", "All Star", "Smash Mouth", "https://i.ytimg.com/vi/L_jWHffIx5E/hqdefault.jpg"),
    Song("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg")
)

val YTBlack = Color(0xFF0F0F0F)
val YTDarkGray = Color(0xFF212121)
val YTLightGray = Color(0xFFAAAAAA)
val White = Color.White
val AccentRed = Color(0xFFFF0000)

// --- SIMP MUSIC REPOSITORY (SMARTER VERSION) ---
object SimpMusicRepository {
    // Резервный список на случай, если не удастся скачать динамический
    private val FALLBACK_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.video",
        "https://pipedapi.drgns.space",
        "https://piped-api.lunar.icu",
        "https://api-piped.mha.fi",
        "https://pipedapi.tokhmi.xyz",
        "https://pa.il.ax",
        "https://api.piped.projectsegfau.lt",
        "https://piped-api.privacy.com.de"
    )
    
    // Сюда мы загрузим свежий список
    private var cachedInstances: List<String> = emptyList()

    suspend fun getStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            // 1. Если кеш пуст, пробуем загрузить свежий список серверов
            if (cachedInstances.isEmpty()) {
                cachedInstances = fetchInstances()
            }
            
            // Объединяем скачанные сервера с резервными (чтобы наверняка)
            val allInstances = (cachedInstances + FALLBACK_INSTANCES).distinct()

            var bestUrl: String? = null
            
            // 2. Пробуем каждый сервер
            for (instance in allInstances) {
                try {
                    // Убираем лишние слеши, если есть
                    val cleanInstance = instance.trimEnd('/')
                    val urlStr = "$cleanInstance/streams/$videoId"
                    
                    val url = URL(urlStr)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 3000 // Тайм-аут 3 сек на сервер
                    connection.readTimeout = 3000
                    
                    if (connection.responseCode == 200) {
                        val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonObject = Gson().fromJson(jsonStr, JsonObject::class.java)
                        
                        if (jsonObject.has("audioStreams")) {
                            val audioStreams = jsonObject.getAsJsonArray("audioStreams")
                            var maxBitrate = 0
                            
                            for (i in 0 until audioStreams.size()) {
                                val stream = audioStreams.get(i).asJsonObject
                                val bitrate = stream.get("bitrate").asInt
                                val mimeType = stream.get("mimeType").asString
                                val format = stream.get("format")?.asString ?: ""
                                
                                // Ищем M4A или MP4 с лучшим качеством
                                if (mimeType.contains("mp4") || mimeType.contains("m4a") || format.equals("M4A", ignoreCase = true)) {
                                    if (bitrate > maxBitrate) {
                                        maxBitrate = bitrate
                                        bestUrl = stream.get("url").asString
                                    }
                                }
                            }
                            
                            // Если нашли ссылку - успех!
                            if (bestUrl != null) {
                                // Продвигаем этот сервер в начало списка (оптимизация)
                                cachedInstances = listOf(cleanInstance) + (cachedInstances - cleanInstance)
                                break 
                            }
                        }
                    }
                } catch (e: Exception) {
                    continue // Сервер мертв, идем к следующему
                }
            }
            bestUrl
        }
    }

    // Функция загрузки списка рабочих серверов из GitHub документации Piped
    private fun fetchInstances(): List<String> {
        return try {
            val url = URL("https://raw.githubusercontent.com/TeamPiped/documentation/main/static/instances.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            
            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JsonParser.parseString(jsonStr).asJsonArray
                val list = mutableListOf<String>()
                
                for (element in jsonArray) {
                    val obj = element.asJsonObject
                    // Берем только те, у которых есть API URL
                    if (obj.has("api_url")) {
                        list.add(obj.get("api_url").asString)
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppContent(
                playMusic = { id, callback -> playAudio(id, callback) },
                pauseMusic = { pauseAudio() },
                resumeMusic = { resumeAudio() }
            )
        }
    }

    private fun playAudio(videoId: String, onReady: (Boolean) -> Unit) {
        lifecycleScope.launch {
            val streamUrl = SimpMusicRepository.getStreamUrl(videoId)
            
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
fun AppContent(
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
                Toast.makeText(context, "Trying other servers... please wait", Toast.LENGTH_LONG).show()
                // Если не получилось с первого раза, можно попробовать еще раз (логика ретрая уже внутри репозитория)
            }
        }
    }

    val onTogglePlay: () -> Unit = {
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
                                MiniPlayer(currentSong!!, isPlaying, isLoading, onTogglePlay) { showFullPlayer = true }
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
                                    icon = { Icon(Icons.Rounded.Search, "Search") },
                                    label = { Text("Search") },
                                    selected = navController.currentDestination?.route == "search",
                                    onClick = { navController.navigate("search") },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = YTDarkGray, selectedIconColor = White, unselectedIconColor = YTLightGray, selectedTextColor = White, unselectedTextColor = YTLightGray)
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
                    composable("home") { HomeScreen(onSongSelect) }
                    composable("search") { ExploreScreen(onSongSelect) }
                }
            }
            
            AnimatedVisibility(
                visible = showFullPlayer,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                if (currentSong != null) {
                    FullPlayer(currentSong!!, isPlaying, isLoading, { showFullPlayer = false }, onTogglePlay)
                }
            }
        }
    }
}

// --- UI COMPONENTS ---
@Composable
fun HomeScreen(onSongClick: (Song) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
             Text("SimpMusic Clone", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=20.dp))
        }
        items(sampleSongs) { song ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { onSongClick(song) }, verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray), contentScale = ContentScale.Crop)
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
fun ExploreScreen(onSongClick: (Song) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Discover", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=20.dp)) }
        items(sampleSongs.shuffled()) { song ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { onSongClick(song) }, verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray), contentScale = ContentScale.Crop)
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
fun MiniPlayer(song: Song, isPlaying: Boolean, isLoading: Boolean, onTogglePlay: () -> Unit, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(YTDarkGray).clickable { onClick() }) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(song.title, color = White, fontSize = 14.sp, maxLines = 1)
                Text(song.artist, color = YTLightGray, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onTogglePlay) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = White)
                else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = White)
            }
        }
        LinearProgressIndicator(progress = if(isLoading) 0f else 0.3f, modifier = Modifier.fillMaxWidth().height(2.dp), color = White, trackColor = Color.Transparent)
    }
}

@Composable
fun FullPlayer(song: Song, isPlaying: Boolean, isLoading: Boolean, onClose: () -> Unit, onTogglePlay: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF424242), YTBlack))).padding(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, null, tint = White, modifier = Modifier.size(32.dp)) }
            }
            Spacer(modifier = Modifier.height(32.dp))
            AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.height(48.dp))
            Text(song.title, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(song.artist, color = YTLightGray, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Slider(value = 0.3f, onValueChange = {}, colors = SliderDefaults.colors(thumbColor = White, activeTrackColor = White))
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(White).clickable { if(!isLoading) onTogglePlay() }, contentAlignment = Alignment.Center) {
                if (isLoading) CircularProgressIndicator(color = YTBlack)
                else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = YTBlack, modifier = Modifier.size(40.dp))
            }
        }
    }
}
