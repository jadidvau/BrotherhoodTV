package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.Channel
import com.example.data.ChannelRepository
import com.example.ui.*
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instantiate database and repository
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = ChannelRepository(database.channelDao())

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepBlack
                ) {
                    val navController = rememberNavController()
                    
                    // ViewModel instantiation with Factory inside Compose
                    val viewModel: ChannelViewModel = viewModel(
                        factory = ChannelViewModelFactory(
                            application = application,
                            repository = repository
                        )
                    )

                    var activeStreamingChannel by remember { mutableStateOf<Channel?>(null) }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        // Render Main Navigation
                        NavHost(
                            navController = navController,
                            startDestination = "home"
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onChannelSelected = { channel ->
                                        // Update history and trigger playback
                                        viewModel.markAsWatched(channel)
                                        activeStreamingChannel = channel
                                    },
                                    onNavigateToAddPlaylist = {
                                        navController.navigate("add_playlist")
                                    },
                                    onNavigateToAbout = {
                                        navController.navigate("about")
                                    },
                                    onNavigateToYallaPortal = {
                                        navController.navigate("yalla_portal")
                                    },
                                    onNavigateToKickBdPortal = {
                                        navController.navigate("kickbd_portal")
                                    }
                                )
                            }

                            composable("add_playlist") {
                                AddPlaylistScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("about") {
                                AboutScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigateToYallaPortal = {
                                        navController.navigate("yalla_portal")
                                    }
                                )
                            }

                            composable("yalla_portal") {
                                WebViewScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("kickbd_portal") {
                                KickBdPortalScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // Fullscreen Media Player Container Overlay
                        AnimatedVisibility(
                            visible = activeStreamingChannel != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            activeStreamingChannel?.let { channel ->
                                PlayerScreen(
                                    channel = channel,
                                    onBack = { activeStreamingChannel = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
