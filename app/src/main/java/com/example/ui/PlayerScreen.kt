package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.Channel
import com.example.ui.theme.NeonRed
import kotlinx.coroutines.delay

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 1. Force landscape mode during video playback
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        // Keep screen on during streaming
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation = originalOrientation
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // AudioManager for system music volume
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // 2. Playback state management
    var isPlaying by remember { mutableStateOf(true) }
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Controls visibility and gesture level indicators
    var controlsVisible by remember { mutableStateOf(true) }
    var volumeIndicatorValue by remember { mutableStateOf<Float?>(null) } // 0f to 1f
    var brightnessIndicatorValue by remember { mutableStateOf<Float?>(null) } // 0f to 1f

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000)
            controlsVisible = false
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Setup and release player lifecycle
    DisposableEffect(channel.url) {
        val mediaItem = MediaItem.fromUri(channel.url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    errorMessage = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlayerScreen", "ExoPlayer Error: ${error.message}", error)
                errorMessage = "Stream unavailable or server offline."
                playbackState = Player.STATE_IDLE
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Layout containing player, swipe handler, and control overlays
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_container")
            .pointerInput(Unit) {
                // Swipe gesture processing
                val screenWidth = size.width
                detectDragGestures(
                    onDragStart = { offset ->
                        // Show controls on initial touch
                        controlsVisible = true
                    },
                    onDragEnd = {
                        // Reset indicators
                        volumeIndicatorValue = null
                        brightnessIndicatorValue = null
                    },
                    onDragCancel = {
                        volumeIndicatorValue = null
                        brightnessIndicatorValue = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val isLeftHalf = change.position.x < (screenWidth / 2)

                        if (isLeftHalf) {
                            // Adjust Brightness
                            val window = activity?.window
                            val attributes = window?.attributes
                            val currentBrightness = if (attributes?.screenBrightness ?: -1f < 0) 0.5f else attributes!!.screenBrightness
                            // Drag amount is negative for up-dragging
                            val delta = -dragAmount.y / 600f
                            val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                            
                            attributes?.screenBrightness = newBrightness
                            window?.attributes = attributes
                            brightnessIndicatorValue = newBrightness
                        } else {
                            // Adjust Music Volume
                            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val delta = -dragAmount.y / 150f
                            val step = (delta * maxVolume).toInt()
                            if (step != 0) {
                                val newVolume = (currentVolume + step).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                volumeIndicatorValue = newVolume.toFloat() / maxVolume.toFloat()
                            }
                        }
                    }
                )
            }
    ) {
        // Video Render View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Custom Compose controllers instead of default Media3 controls
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable { controlsVisible = !controlsVisible }
        )

        // Loading Progress Indicator (Buffering)
        if (playbackState == Player.STATE_BUFFERING && errorMessage == null) {
            CircularProgressIndicator(
                color = NeonRed,
                strokeWidth = 4.dp,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .testTag("player_buffering_indicator")
            )
        }

        // Playback Exception Fallback UI
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .align(Alignment.Center)
                    .testTag("player_error_view")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = "Error signal",
                        tint = NeonRed,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "Streaming Exception",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Format might be unsupported, or connection has timed out.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Exit Player")
                    }
                }
            }
        }

        // Gesture indicators HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left HUD (Brightness)
                AnimatedVisibility(
                    visible = brightnessIndicatorValue != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    brightnessIndicatorValue?.let { level ->
                        LevelHUD(icon = Icons.Default.Brightness5, label = "Brightness", percent = (level * 100).toInt())
                    }
                }

                // Right HUD (Volume)
                AnimatedVisibility(
                    visible = volumeIndicatorValue != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    volumeIndicatorValue?.let { level ->
                        LevelHUD(icon = Icons.Default.VolumeUp, label = "Volume", percent = (level * 100).toInt())
                    }
                }
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .testTag("player_back_button")
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Live IPTV Stream • Category: ${channel.category}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Center Play/Pause quick action
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = NeonRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Bottom control overlay bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeonRed, CircleShape)
                        )
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Inform user of drag gestures
                        Text(
                            text = "Swipe Vertical: Left = Brightness | Right = Volume",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen locked",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelHUD(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    percent: Int
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = NeonRed)
            Column {
                Text(text = label, color = Color.Gray, fontSize = 11.sp)
                Text(text = "$percent%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
