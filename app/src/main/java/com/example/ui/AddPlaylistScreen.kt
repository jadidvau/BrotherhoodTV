package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Playlist
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaylistScreen(
    viewModel: ChannelViewModel,
    onBack: () -> Unit
) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Import M3U Playlist", "Manual Custom Channel")

    val playlists by viewModel.playlists.collectAsState()
    val playlistAddingStatus by viewModel.playlistAddingStatus.collectAsState()

    // Playlist input fields
    var playlistName by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }

    // Manual channel input fields
    var channelName by remember { mutableStateOf("") }
    var channelUrl by remember { mutableStateOf("") }
    var channelCategory by remember { mutableStateOf("") }
    var channelLogoUrl by remember { mutableStateOf("") }

    // Clear status when navigating away or on initial entry
    DisposableEffect(Unit) {
        viewModel.clearPlaylistStatus()
        onDispose {}
    }

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Stream Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button_from_add")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Tab Header
            item {
                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = CardSurface,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                            color = NeonRed
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = {
                                tabIndex = index
                                viewModel.clearPlaylistStatus()
                            },
                            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            selectedContentColor = NeonRed,
                            unselectedContentColor = TextSecondary,
                            modifier = Modifier.testTag("add_tab_$index")
                        )
                    }
                }
            }

            // Status Banner overlays (Loading, Error, Success)
            item {
                when (val status = playlistAddingStatus) {
                    is ChannelViewModel.PlaylistStatus.Loading -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = NeonRed, modifier = Modifier.size(24.dp))
                                Text(
                                    text = "Downloading & parsing M3U stream index...",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    is ChannelViewModel.PlaylistStatus.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x33F44336)),
                            border = BorderStroke(1.dp, PingRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Import Failed", color = PingRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(status.message, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    is ChannelViewModel.PlaylistStatus.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x334CAF50)),
                            border = BorderStroke(1.dp, PingGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Import Successful!", color = PingGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Successfully loaded ${status.channelCount} custom channels to your database.",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }

            // Render form based on selected tab index
            item {
                if (tabIndex == 0) {
                    // M3U Tab
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                            .background(CardSurface)
                            .padding(16.dp)
                    ) {
                        Text("Add Remote M3U Playlist", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Playlist Name", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonRed,
                                unfocusedBorderColor = BorderGray,
                                cursorColor = NeonRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_name_input")
                        )

                        OutlinedTextField(
                            value = playlistUrl,
                            onValueChange = { playlistUrl = it },
                            label = { Text("M3U Playlist URL", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonRed,
                                unfocusedBorderColor = BorderGray,
                                cursorColor = NeonRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_url_input")
                        )

                        // Suggested Playlists quick selector
                        Text(
                            text = "Or select a popular public playlist:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                        
                        val suggestions = listOf(
                            Pair("Global IPTV-Org (General)", "https://iptv-org.github.io/iptv/index.m3u"),
                            Pair("Samsung TV Plus (US Streams)", "https://raw.githubusercontent.com/FazzR/samsung-tv-plus-m3u/main/samsung-tv-plus.m3u"),
                            Pair("Pluto TV Streams", "https://raw.githubusercontent.com/FazzR/iptv-pluto-tv/main/pluto-tv.m3u")
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggestions.forEach { (sName, sUrl) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DeepBlack)
                                        .border(1.dp, if (playlistUrl == sUrl) NeonRed else BorderGray, RoundedCornerShape(8.dp))
                                        .clickable {
                                            playlistName = sName
                                            playlistUrl = sUrl
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = if (playlistUrl == sUrl) NeonRed else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sName,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = sUrl,
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.addPlaylist(playlistName, playlistUrl)
                                // Clear input on attempt
                                playlistName = ""
                                playlistUrl = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_playlist_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download & Import", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Manual Single Channel Tab
                    var manualSuccessTriggered by remember { mutableStateOf(false) }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                            .background(CardSurface)
                            .padding(16.dp)
                    ) {
                        Text("Add Manual Single Stream", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

                        OutlinedTextField(
                            value = channelName,
                            onValueChange = { channelName = it },
                            label = { Text("Channel Name", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonRed,
                                unfocusedBorderColor = BorderGray,
                                cursorColor = NeonRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_name_input")
                        )

                        OutlinedTextField(
                            value = channelUrl,
                            onValueChange = { channelUrl = it },
                            label = { Text("Stream URL (.m3u8, .ts, etc.)", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonRed,
                                unfocusedBorderColor = BorderGray,
                                cursorColor = NeonRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_url_input")
                        )

                        OutlinedTextField(
                            value = channelCategory,
                            onValueChange = { channelCategory = it },
                            label = { Text("Category (e.g. Sports, Movies)", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonRed,
                                unfocusedBorderColor = BorderGray,
                                cursorColor = NeonRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_category_input")
                        )

                        OutlinedTextField(
                            value = channelLogoUrl,
                            onValueChange = { channelLogoUrl = it },
                            label = { Text("Channel Logo URL (Optional)", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonRed,
                                unfocusedBorderColor = BorderGray,
                                cursorColor = NeonRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_logo_input")
                        )

                        if (manualSuccessTriggered) {
                            Text(
                                "Custom stream added successfully!",
                                color = PingGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (channelName.isNotBlank() && channelUrl.isNotBlank()) {
                                    viewModel.addManualChannel(
                                        channelName,
                                        channelUrl,
                                        channelCategory,
                                        channelLogoUrl
                                    )
                                    // Clear manual fields
                                    channelName = ""
                                    channelUrl = ""
                                    channelCategory = ""
                                    channelLogoUrl = ""
                                    manualSuccessTriggered = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_manual_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Tv, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Stream", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Playlist CRUD database listing (Displays active custom playlists)
            item {
                Text(
                    text = "Active Playlists (${playlists.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No custom playlists currently imported.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(playlists) { playlist ->
                    PlaylistItemCard(
                        playlist = playlist,
                        onDelete = { viewModel.deletePlaylist(playlist) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistItemCard(playlist: Playlist, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
            .background(CardSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playlist.name,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playlist.url,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("delete_playlist_${playlist.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete playlist",
                tint = TextSecondary
            )
        }
    }
}
