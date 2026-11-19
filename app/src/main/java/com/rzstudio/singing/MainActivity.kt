package com.rzstudio.singing

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.delay

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
    var takePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingTakePath by remember { mutableStateOf<String?>(null) }
    var isTakePlaying by remember { mutableStateOf(false) }
    var takePositionMs by remember { mutableStateOf(0f) }
    var takeDurationMs by remember { mutableStateOf(0) }
    var isTakeSeeking by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingOutput by remember { mutableStateOf<File?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready") }
    var takes by remember { mutableStateOf(TakeStore.list(context)) }

    LaunchedEffect(takePlayer, isTakePlaying) {
        while (true) {
            val activePlayer = takePlayer
            if (activePlayer != null && !isTakeSeeking) {
                runCatching { takePositionMs = activePlayer.currentPosition.toFloat() }
            }
            delay(200)
        }
    }

    val latestRecorder = rememberUpdatedState(recorder)
    val latestRecordingOutput = rememberUpdatedState(recordingOutput)
    val latestPlayer = rememberUpdatedState(player)
    val latestTakePlayer = rememberUpdatedState(takePlayer)

    DisposableEffect(Unit) {
        onDispose {
            latestRecorder.value?.let { activeRecorder ->
                val stopped = runCatching { activeRecorder.stop() }.isSuccess
                runCatching { activeRecorder.release() }
                if (!stopped) latestRecordingOutput.value?.delete()
            }
            latestPlayer.value?.release()
            latestTakePlayer.value?.release()
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            player?.release()
            player = null
            selectedAudio = uri
            audioName = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported audio"
            isPlaying = false
            status = "Audio selected"
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            status = "Microphone permission granted. Tap Record to begin."
        } else {
            status = "Microphone permission denied"
        }
    }

    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val output = TakeStore.newTakeFile(context)
        var activeRecorder: MediaRecorder? = null
        try {
            val createdRecorder = MediaRecorder(context)
            activeRecorder = createdRecorder
            createdRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(192_000)
                setAudioSamplingRate(44_100)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
            recorder = activeRecorder
            recordingOutput = output
            isRecording = true
            status = "Recording: ${output.name}"
        } catch (error: Exception) {
            activeRecorder?.let { runCatching { it.release() } }
            output.delete()
            recorder = null
            recordingOutput = null
            status = "Recording could not start: ${error.javaClass.simpleName}"
        }
    }

    fun stopRecording() {
        val activeRecorder = recorder ?: return
        val output = recordingOutput
        var saved = false
        try {
            activeRecorder.stop()
            saved = output?.isFile == true && output.length() > 0L
            status = if (saved) "Take saved" else "Recording could not be saved"
        } catch (_: RuntimeException) {
            status = "Recording was too short or could not be saved"
        } finally {
            runCatching { activeRecorder.release() }
            if (!saved) output?.delete()
            recorder = null
            recordingOutput = null
            isRecording = false
            takes = TakeStore.list(context)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = StudioBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("RZ AI", color = StudioAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Singing Studio", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Your song. Your voice. Your studio.", color = Color.LightGray, fontSize = 14.sp)
            }

            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Default.LibraryMusic, null, tint = StudioAccent)
                    Text("  YOUR SONG", color = StudioAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(audioName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Import an audio file you own or are authorized to use.", color = Color.LightGray, fontSize = 13.sp)
                Button(
                    onClick = { audioPicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (selectedAudio == null) "Import song" else "Choose another song") }

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
                                        it.setOnCompletionListener { completed ->
                                            completed.seekTo(0)
                                            isPlaying = false
                                        }
                                    }
                                    if (activePlayer != null) {
                                        activePlayer.start()
                                        isPlaying = true
                                    } else {
                                        status = "This audio format could not be played"
                                    }
                                }
                            } catch (_: Exception) {
                                player?.release()
                                player = null
                                isPlaying = false
                                status = "Playback failed"
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

            Text("RECORDING STUDIO", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Default.Mic, null, tint = StudioAccent)
                    Text("  VOCAL RECORDING", color = StudioAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(
                    "Record a microphone take as an AAC audio file. Headphone monitoring and synchronized multitrack recording are not implemented yet.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Button(
                    onClick = { if (isRecording) stopRecording() else startRecording() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Text(if (isRecording) "  Stop and save take" else "  Record vocal take")
                }
            }

            StudioCard {
                Text("VOCAL TAKES  •  ${takes.size}", color = StudioAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Play back or delete your recorded takes.", color = Color.LightGray, fontSize = 13.sp)
                if (takes.isEmpty()) {
                    Text("No recordings yet.", color = Color.LightGray, fontSize = 13.sp)
                } else {
                    takes.forEach { take ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(take.name, color = Color.White, fontSize = 13.sp)
                                Text("${take.length() / 1024} KB", color = Color.Gray, fontSize = 11.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = {
                                    try {
                                        if (playingTakePath == take.absolutePath && isTakePlaying) {
                                            takePlayer?.pause()
                                            isTakePlaying = false
                                            status = "Take paused"
                                        } else if (playingTakePath == take.absolutePath && takePlayer != null) {
                                            takePlayer?.start()
                                            isTakePlaying = true
                                            status = "Playing recorded take"
                                        } else {
                                            takePlayer?.release()
                                            takePlayer = null
                                            playingTakePath = null
                                            isTakePlaying = false
                                            takePositionMs = 0f
                                            takeDurationMs = 0
                                            val newPlayer = MediaPlayer.create(context, Uri.fromFile(take))
                                            if (newPlayer == null) {
                                                status = "This recording could not be played"
                                            } else {
                                                takePlayer = newPlayer
                                                playingTakePath = take.absolutePath
                                                takeDurationMs = newPlayer.duration
                                                newPlayer.setOnCompletionListener { completedPlayer ->
                                                    if (takePlayer === completedPlayer) {
                                                        takePlayer = null
                                                        isTakePlaying = false
                                                        playingTakePath = null
                                                        takePositionMs = 0f
                                                        takeDurationMs = 0
                                                    }
                                                    completedPlayer.release()
                                                    status = "Take playback completed"
                                                }
                                                newPlayer.start()
                                                isTakePlaying = true
                                                status = "Playing recorded take"
                                            }
                                        }
                                    } catch (_: Exception) {
                                        takePlayer?.release()
                                        takePlayer = null
                                        playingTakePath = null
                                        isTakePlaying = false
                                        status = "Take playback failed"
                                    }
                                }) {
                                    androidx.compose.material3.Icon(
                                        if (playingTakePath == take.absolutePath && isTakePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                    Text(if (playingTakePath == take.absolutePath && isTakePlaying) "Pause" else "Play")
                                }
                                Button(onClick = {
                                    if (playingTakePath == take.absolutePath) {
                                        takePlayer?.release()
                                        takePlayer = null
                                        playingTakePath = null
                                        isTakePlaying = false
                                    }
                                    TakeStore.delete(take, File(context.filesDir, "recordings"))
                                    takes = TakeStore.list(context)
                                    status = "Take deleted"
                                }) { Text("Delete") }
                            }
                            }
                            if (playingTakePath == take.absolutePath && takeDurationMs > 0) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(formatPlaybackTime(takePositionMs.toInt()), color = Color.LightGray, fontSize = 11.sp)
                                        Text(formatPlaybackTime(takeDurationMs), color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Slider(
                                        value = takePositionMs.coerceIn(0f, takeDurationMs.toFloat()),
                                        onValueChange = { value -> isTakeSeeking = true; takePositionMs = value },
                                        onValueChangeFinished = {
                                            runCatching { takePlayer?.seekTo(takePositionMs.toInt()) }
                                            isTakeSeeking = false
                                        },
                                        valueRange = 0f..takeDurationMs.toFloat(),
                                        modifier = Modifier.fillMaxWidth().testTag("take_seek_slider")
                                    )
                                    Text("Drag the timeline to jump to any position.", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Default.GraphicEq, null, tint = StudioAccent)
                    Text("  AI VOCAL SEPARATION", color = StudioAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(
                    "Not available yet. A compatible model, on-device runtime, quality tests and output validation are required before this feature can be enabled.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }

            Text(status, color = StudioAccent, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("MVP 0.2.0  •  Android 12+", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun StudioCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

internal object TakeStore {
    fun newTakeFile(context: android.content.Context): File {
        val directory = File(context.filesDir, "recordings").apply { mkdirs() }
        return File(directory, "take_${System.currentTimeMillis()}.m4a")
    }

    fun list(context: android.content.Context): List<File> =
        File(context.filesDir, "recordings")
            .listFiles { file -> file.isFile && file.extension.equals("m4a", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun delete(file: File, recordingsDirectory: File): Boolean {
        val canonicalDirectory = runCatching { recordingsDirectory.canonicalFile }.getOrNull() ?: return false
        val canonicalFile = runCatching { file.canonicalFile }.getOrNull() ?: return false
        if (canonicalFile.parentFile != canonicalDirectory) return false
        return file.delete()
    }
}

internal fun formatPlaybackTime(milliseconds: Int): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
