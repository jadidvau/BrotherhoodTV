package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.Channel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChannelViewModel,
    onChannelSelected: (Channel) -> Unit,
    onNavigateToAddPlaylist: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToYallaPortal: () -> Unit,
    onNavigateToKickBdPortal: () -> Unit
) {
    val searchVal by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val recentlyWatched by viewModel.recentlyWatchedChannels.collectAsState()
    val isRefreshingPings by viewModel.isRefreshingPings.collectAsState()

    // Animation for rotation when refreshing pings
    val infiniteTransition = rememberInfiniteTransition(label = "ping_refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ping_refresh_rotate"
    )

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PREMIUM IPTV",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonRed,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "BrotherHood ",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "TV",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = NeonRed,
                                fontSize = 18.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToYallaPortal,
                        modifier = Modifier.testTag("yalla_portal_appbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Yalla Portal",
                            tint = NeonRed
                        )
                    }
                    IconButton(
                        onClick = onNavigateToAbout,
                        modifier = Modifier.testTag("about_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Developer",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onNavigateToAddPlaylist,
                        modifier = Modifier.testTag("add_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Playlist",
                            tint = NeonRed
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepBlack,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.refreshAllPings() },
                containerColor = NeonRed,
                contentColor = Color.White,
                modifier = Modifier
                    .testTag("refresh_pings_fab")
                    .padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Latency Pings",
                    modifier = if (isRefreshingPings) Modifier.rotate(rotation) else Modifier
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Featured Billboard (Static with Premium design & Fallback visual gradients)
            item {
                FeaturedBillboard(
                    onWatchLive = {
                        // Watch the first available seeded channel (Caze TV or similar)
                        val firstChannel = filteredChannels.firstOrNull { it.isSeed } 
                            ?: filteredChannels.firstOrNull()
                        if (firstChannel != null) {
                            onChannelSelected(firstChannel)
                        }
                    }
                )
            }

            // 1.5 Web Portals Section (Yalla Portal & KickBD Live)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "WEB PORTALS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonRed,
                        letterSpacing = 1.5.sp
                    )

                    // Yalla Portal Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToYallaPortal() }
                            .border(
                                BorderStroke(1.dp, Brush.linearGradient(listOf(NeonRed, Color(0xFF330011)))),
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(NeonRed, Color(0xFF800020))))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "YALLA PORTAL",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(NeonRed, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Live marketplace and storefront services.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Navigate",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // KickBD Live Card (Sleek Glassmorphism, Crimson/Neon Accent Glow)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToKickBdPortal() }
                            .border(
                                BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFFFF1E56), Color(0xFF800C2A)))),
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFF880E4F))))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "KICKBD LIVE",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE91E63), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "MEDIA",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Dedicated real-time media portal & live streaming player.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Navigate",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Search & Filter Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Search Text Field
                    TextField(
                        value = searchVal,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search channel name...", color = TextSecondary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = TextSecondary
                            )
                        },
                        trailingIcon = {
                            if (searchVal.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear search",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface,
                            disabledContainerColor = CardSurface,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = NeonRed,
                            unfocusedIndicatorColor = BorderGray,
                            cursorColor = NeonRed
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                            .testTag("channel_search_input")
                    )

                    // Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(categories) { category ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selectedCat == category) NeonRed else CardSurface)
                                    .border(
                                        width = 1.dp,
                                        color = if (selectedCat == category) NeonRed else BorderGray,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { viewModel.setSelectedCategory(category) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("category_chip_$category"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    color = if (selectedCat == category) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Recently Watched Channels (Netflix Row)
            if (recentlyWatched.isNotEmpty()) {
                item {
                    ChannelRow(
                        title = "Recently Watched",
                        channels = recentlyWatched,
                        onChannelClick = onChannelSelected,
                        onFavoriteClick = { viewModel.toggleFavorite(it) }
                    )
                }
            }

            // 4. Favorites (Netflix Row)
            if (favoriteChannels.isNotEmpty()) {
                item {
                    ChannelRow(
                        title = "My Favorites",
                        channels = favoriteChannels,
                        onChannelClick = onChannelSelected,
                        onFavoriteClick = { viewModel.toggleFavorite(it) }
                    )
                }
            }

            // 5. All Channels / Current Filtered Results
            item {
                Text(
                    text = if (selectedCat == "All") "All Channels" else "$selectedCat Streams",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (filteredChannels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = "No channels",
                                tint = TextSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No channels found.",
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Try importing an M3U playlist above.",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                // Render list in grid style
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filteredChannels.forEach { channel ->
                            ChannelGridCard(
                                channel = channel,
                                onClick = { onChannelSelected(channel) },
                                onFavoriteToggle = { viewModel.toggleFavorite(channel) },
                                onDeleteManual = {
                                    if (!channel.isSeed) {
                                        viewModel.deleteChannel(channel.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Extra padding at bottom to prevent FAB covering elements
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun FeaturedBillboard(onWatchLive: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(24.dp))
    ) {
        // Cover Image from HTML Design
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://images.unsplash.com/photo-1504450758481-7338eba7524a?auto=format&fit=crop&q=80&w=600")
                .crossfade(true)
                .build(),
            contentDescription = "UEFA Champions League banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Gradient Overlay matching Tailwind's `bg-gradient-to-t from-black via-black/40 to-transparent`
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Live Badge + Category Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(NeonRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "UEFA Champions League",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Caze TV: Real Madrid vs Man City",
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onWatchLive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("featured_watch_now")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Watch Live",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ChannelRow(
    title: String,
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onFavoriteClick: (Channel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(channels) { channel ->
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .clickable { onChannelClick(channel) }
                        .padding(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Channel Logo Frame
                        ChannelLogo(
                            logoUrl = channel.logoUrl,
                            name = channel.name,
                            modifier = Modifier.size(56.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = channel.category,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Small Ping Indicator
                        PingBadge(pingMs = channel.pingMs)
                    }

                    // Floating Favorite Icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(DeepBlack.copy(alpha = 0.6f))
                            .clickable { onFavoriteClick(channel) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (channel.isFavorite) NeonRed else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelGridCard(
    channel: Channel,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDeleteManual: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .background(CardSurface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        ChannelLogo(
            logoUrl = channel.logoUrl,
            name = channel.name,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Info Block
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = channel.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = channel.category,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(TextSecondary, CircleShape)
                )
                PingBadge(pingMs = channel.pingMs)
            }
        }

        // Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Favorite Button
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (channel.isFavorite) NeonRed else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Custom Delete Button for manual additions
            if (!channel.isSeed) {
                IconButton(
                    onClick = onDeleteManual,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete custom channel",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Play Icon Indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NeonRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = NeonRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getInitials(name: String): String {
    if (name.isBlank()) return "TV"
    val words = name.trim().split("\\s+".toRegex())
    return if (words.size >= 2) {
        val firstChar = words[0].take(1)
        val secondChar = words[1].take(1)
        (firstChar + secondChar).uppercase()
    } else {
        val word = words[0]
        if (word.length >= 2) {
            word.take(2).uppercase()
        } else {
            word.uppercase()
        }
    }
}

@Composable
fun ChannelLogo(logoUrl: String?, name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF27272A))
            .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$name logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                error = null
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(name),
                    color = NeonRed,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PingBadge(pingMs: Long?) {
    val (text, color) = when {
        pingMs == null -> "Unchecked" to PingOffline
        pingMs < 0 -> "Offline" to PingRed
        pingMs <= 80 -> "${pingMs}ms" to PingGreen
        pingMs <= 150 -> "${pingMs}ms" to PingYellow
        pingMs <= 300 -> "${pingMs}ms" to PingOrange
        else -> "${pingMs}ms" to PingRed
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
