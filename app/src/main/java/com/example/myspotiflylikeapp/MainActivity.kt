package com.example.myvibemusic

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage

// --- DATA MODELS ---
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val audioUrl: String,
    var isLiked: Boolean = false
)

// --- MOCK DATA (Simulating YouTube Music) ---
val sampleSongs = listOf(
    Song("1", "Cyberpunk Vibes", "Synthwave Boy", "https://picsum.photos/300/300?random=1", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
    Song("2", "Lo-Fi Study", "Chill Beats", "https://picsum.photos/300/300?random=2", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
    Song("3", "Night Drive", "Retro Future", "https://picsum.photos/300/300?random=3", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
    Song("4", "Gym Phonk", "Bass Killer", "https://picsum.photos/300/300?random=4", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
    Song("5", "Ambient Sleep", "Nature Sounds", "https://picsum.photos/300/300?random=5", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
    Song("6", "Rock Classic", "The Dads", "https://picsum.photos/300/300?random=6", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3")
)

// --- COLORS ---
val SpotifyBlack = Color(0xFF121212)
val SpotifyDarkGray = Color(0xFF212121)
val SpotifyGreen = Color(0xFF1DB954)
val White = Color.White
val LightGray = Color.LightGray

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyVibeApp(
                playMusic = { url -> playAudio(url) },
                pauseMusic = { pauseAudio() },
                resumeMusic = { resumeAudio() }
            )
        }
    }

    private fun playAudio(url: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener { start() }
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
fun MyVibeApp(playMusic: (String) -> Unit, pauseMusic: () -> Unit, resumeMusic: () -> Unit) {
    val navController = rememberNavController()
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var likedSongs by remember { mutableStateOf(setOf<String>()) }
    var showFullPlayer by remember { mutableStateOf(false) }

    val onSongSelected: (Song) -> Unit = { song ->
        currentSong = song
        isPlaying = true
        playMusic(song.audioUrl)
    }

    val onTogglePlay: () -> Unit = {
        if (isPlaying) pauseMusic() else resumeMusic()
        isPlaying = !isPlaying
    }

    val onLikeToggle: (Song) -> Unit = { song ->
        likedSongs = if (likedSongs.contains(song.id)) likedSongs - song.id else likedSongs + song.id
    }

    MaterialTheme(
        colorScheme = darkColorScheme(background = SpotifyBlack, primary = SpotifyGreen)
    ) {
        Scaffold(
            bottomBar = {
                Column {
                    // MINI PLAYER
                    if (currentSong != null) {
                        MiniPlayer(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            onTogglePlay = onTogglePlay,
                            onClick = { showFullPlayer = true }
                        )
                    }
                    // BOTTOM NAVIGATION
                    NavigationBar(containerColor = SpotifyBlack) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = navController.currentDestination?.route == "home",
                            onClick = { navController.navigate("home") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = SpotifyGreen, unselectedIconColor = LightGray)
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            selected = navController.currentDestination?.route == "search",
                            onClick = { navController.navigate("search") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = SpotifyGreen, unselectedIconColor = LightGray)
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Library") },
                            label = { Text("Library") },
                            selected = navController.currentDestination?.route == "library",
                            onClick = { navController.navigate("library") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = SpotifyGreen, unselectedIconColor = LightGray)
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") { HomeScreen(onSongSelected) }
                composable("search") { SearchScreen(onSongSelected) }
                composable("library") { LibraryScreen(likedSongs, onSongSelected) }
            }
            
            // FULL SCREEN PLAYER OVERLAY
            AnimatedVisibility(
                visible = showFullPlayer,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                if (currentSong != null) {
                    FullPlayerScreen(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        isLiked = likedSongs.contains(currentSong!!.id),
                        onClose = { showFullPlayer = false },
                        onTogglePlay = onTogglePlay,
                        onLikeToggle = { onLikeToggle(currentSong!!) }
                    )
                }
            }
        }
    }
}

// --- SCREENS ---

@Composable
fun HomeScreen(onSongClick: (Song) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(SpotifyBlack).padding(16.dp)) {
        item {
            Text("Good Evening", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        }
        item {
            Text("Made For You", color = SpotifyGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }
        items(sampleSongs) { song ->
            SongListItem(song, onSongClick)
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SearchScreen(onSongClick: (Song) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filteredSongs = sampleSongs.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().background(SpotifyBlack).padding(16.dp)) {
        Text("Search", color = White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("What do you want to listen to?") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = White, unfocusedContainerColor = White, focusedTextColor = SpotifyBlack)
        )

        LazyColumn {
            items(filteredSongs) { song ->
                SongListItem(song, onSongClick)
            }
        }
    }
}

@Composable
fun LibraryScreen(likedIds: Set<String>, onSongClick: (Song) -> Unit) {
    val likedSongsList = sampleSongs.filter { likedIds.contains(it.id) }

    Column(modifier = Modifier.fillMaxSize().background(SpotifyBlack).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            AsyncImage(
                model = "https://picsum.photos/200", 
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Liked Songs", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${likedSongsList.size} songs", color = LightGray, fontSize = 14.sp)
            }
        }
        
        if (likedSongsList.isEmpty()) {
            Text("No liked songs yet.", color = LightGray, modifier = Modifier.padding(top = 20.dp))
        } else {
            LazyColumn {
                items(likedSongsList) { song ->
                    SongListItem(song, onSongClick)
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun SongListItem(song: Song, onClick: (Song) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick(song) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = "Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(text = song.artist, color = LightGray, fontSize = 14.sp)
        }
        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = LightGray)
    }
}

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, onTogglePlay: () -> Unit, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        // Progress bar simulation
        LinearProgressIndicator(progress = 0.3f, modifier = Modifier.fillMaxWidth().height(2.dp), color = White, trackColor = SpotifyDarkGray)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(SpotifyDarkGray)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = song.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = song.artist, color = LightGray, fontSize = 12.sp)
            }
            IconButton(onClick = { /* Like */ }) {
                 Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = White)
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = White
                )
            }
        }
    }
}

@Composable
fun FullPlayerScreen(
    song: Song, 
    isPlaying: Boolean, 
    isLiked: Boolean,
    onClose: () -> Unit, 
    onTogglePlay: () -> Unit,
    onLikeToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF444444), SpotifyBlack)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = White) }
            Text("Now Playing", color = White, modifier = Modifier.align(Alignment.CenterVertically))
            IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, contentDescription = "More", tint = White) }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Artwork
        AsyncImage(
            model = song.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(8.dp))
                .shadow(16.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title & Artist
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = song.artist, color = LightGray, fontSize = 18.sp)
            }
            IconButton(onClick = onLikeToggle) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) SpotifyGreen else White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Progress Bar
        Slider(
            value = 0.3f, 
            onValueChange = {}, 
            colors = SliderDefaults.colors(thumbColor = White, activeTrackColor = White, inactiveTrackColor = Color.Gray)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1:15", color = LightGray, fontSize = 12.sp)
            Text("3:45", color = LightGray, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceEvenly, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Shuffle, null, tint = White) }
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
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = SpotifyBlack,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.SkipNext, null, tint = White, modifier = Modifier.fillMaxSize()) }
            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Repeat, null, tint = White) }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

// Helper for shadow
fun Modifier.shadow(
    elevation: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    clip: Boolean = elevation > 0.dp
) = this
