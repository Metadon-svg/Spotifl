package com.example.myspotifylikeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpotifyLikeApp()
        }
    }
}

data class Song(val title: String, val artist: String)

@Composable
fun SpotifyLikeApp() {
    val darkBackground = Color(0xFF121212)
    val spotifyGreen = Color(0xFF1DB954)
    
    val songs = listOf(
        Song("Midnight City", "M83"),
        Song("Blinding Lights", "The Weeknd"),
        Song("Heat Waves", "Glass Animals"),
        Song("Levitating", "Dua Lipa"),
        Song("Stay", "The Kid LAROI"),
        Song("As It Was", "Harry Styles"),
        Song("Bad Habit", "Steve Lacy"),
        Song("Shut Up and Dance", "WALK THE MOON"),
        Song("Wake Me Up", "Avicii"),
        Song("Mr. Brightside", "The Killers")
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
                .padding(16.dp)
        ) {
            Text(
                text = "Good Evening",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Made for You",
                color = spotifyGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(songs) { song ->
                    SongItem(song)
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF282828), shape = RoundedCornerShape(4.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art Placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Gray)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(text = song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = song.artist, color = Color.LightGray, fontSize = 14.sp)
        }
    }
}