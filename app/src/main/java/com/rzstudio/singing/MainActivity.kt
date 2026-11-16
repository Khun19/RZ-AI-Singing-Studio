package com.rzstudio.singing

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val StudioBackground = Color(0xFF090B10)
private val StudioSurface = Color(0xFF171B24)
private val StudioAccent = Color(0xFFB9A2FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = StudioAccent,
                background = StudioBackground,
                surface = StudioSurface
            )) {
                SingingStudioApp()
            }
        }
    }
}

@Composable
private fun SingingStudioApp() {
    val context = LocalContext.current
    var selectedAudio by remember { mutableStateOf<Uri?>(null) }
    var audioName by remember { mutableStateOf("No song selected") }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            player = null
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            player?.release()
            player = null
            selectedAudio = uri
            audioName = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported audio"
            isPlaying = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StudioBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "RZ AI",
                    color = StudioAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Singing Studio",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your song. Your voice. Your studio.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = StudioAccent
                        )
                        Text(
                            "  YOUR SONG",
                            color = StudioAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        audioName,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Import an MP3 or WAV track to start your session.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = { audioPicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedAudio == null) "Import song" else "Choose another song")
                    }
                    if (selectedAudio != null) {
                        Button(
                            onClick = {
                                try {
                                    if (isPlaying) {
                                        player?.pause()
                                        isPlaying = false
                                    } else {
                                        val activePlayer = player ?: MediaPlayer.create(context, selectedAudio!!)?.also {
                                            player = it
                                        }
                                        if (activePlayer != null) {
                                            activePlayer.start()
                                            isPlaying = true
                                        }
                                    }
                                } catch (_: IllegalStateException) {
                                    player?.release()
                                    player = null
                                    isPlaying = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Icon(
                                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Text(if (isPlaying) "  Pause preview" else "  Play preview")
                        }
                    }
                }
            }

            Text(
                "START CREATING",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            StudioModeCard(
                icon = { androidx.compose.material3.Icon(Icons.Default.GraphicEq, null, tint = StudioAccent) },
                title = "Easy Mode",
                description = "Import a song, create karaoke backing, then record your voice.",
                status = "IN DEVELOPMENT"
            )
            StudioModeCard(
                icon = { androidx.compose.material3.Icon(Icons.Default.Mic, null, tint = StudioAccent) },
                title = "Recording Studio",
                description = "Record vocals, capture takes and build your own performance.",
                status = "COMING NEXT"
            )

            Spacer(modifier = Modifier.weight(1f))
            Text(
                "MVP 0.1.0  •  Android 12+",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun StudioModeCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    status: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF29243A), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) { icon() }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(description, color = Color.LightGray, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(status, color = StudioAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
