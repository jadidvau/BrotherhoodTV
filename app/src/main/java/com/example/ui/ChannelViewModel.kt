package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChannelViewModel(
    application: Application,
    private val repository: ChannelRepository
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _isRefreshingPings = MutableStateFlow(false)
    val isRefreshingPings = _isRefreshingPings.asStateFlow()

    private val _playlistAddingStatus = MutableStateFlow<PlaylistStatus>(PlaylistStatus.Idle)
    val playlistAddingStatus = _playlistAddingStatus.asStateFlow()

    sealed interface PlaylistStatus {
        object Idle : PlaylistStatus
        object Loading : PlaylistStatus
        data class Success(val channelCount: Int) : PlaylistStatus
        data class Error(val message: String) : PlaylistStatus
    }

    // Combined filtered channel flow based on search query and category
    val filteredChannels: StateFlow<List<Channel>> = combine(
        repository.allChannels,
        _searchQuery,
        _selectedCategory
    ) { channels, query, category ->
        var list = channels
        if (category != "All") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isNotEmpty()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Extracted categories list dynamically from all channels
    val categories: StateFlow<List<String>> = repository.allChannels.map { channels ->
        val cats = channels.map { it.category }.distinct().sorted()
        listOf("All") + cats
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("All")
    )

    val favoriteChannels: StateFlow<List<Channel>> = repository.favoriteChannels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyWatchedChannels: StateFlow<List<Channel>> = repository.recentlyWatchedChannels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Safe check to verify we populate built-in streams
        viewModelScope.launch {
            repository.ensureSeedsExist()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.updateChannel(channel.copy(isFavorite = !channel.isFavorite))
        }
    }

    fun markAsWatched(channel: Channel) {
        viewModelScope.launch {
            repository.updateChannel(channel.copy(lastWatchedTimestamp = System.currentTimeMillis()))
        }
    }

    fun deleteChannel(channelId: Int) {
        viewModelScope.launch {
            repository.deleteChannelById(channelId)
        }
    }

    fun refreshAllPings() {
        if (_isRefreshingPings.value) return
        _isRefreshingPings.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentChannels = repository.allChannels.first()
                if (currentChannels.isNotEmpty()) {
                    // Perform pings in parallel using async coroutines
                    currentChannels.map { channel ->
                        async {
                            val latency = PingUtility.pingUrl(channel.url)
                            repository.updateChannel(channel.copy(pingMs = latency))
                        }
                    }.awaitAll()
                }
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "Error refreshing pings", e)
            } finally {
                _isRefreshingPings.value = false
            }
        }
    }

    fun addPlaylist(name: String, url: String) {
        if (name.isBlank() || url.isBlank()) {
            _playlistAddingStatus.value = PlaylistStatus.Error("Playlist name and M3U URL cannot be empty.")
            return
        }

        _playlistAddingStatus.value = PlaylistStatus.Loading

        viewModelScope.launch {
            try {
                // Insert playlist record to get the auto-generated ID
                val playlist = Playlist(name = name, url = url)
                val playlistId = repository.insertPlaylist(playlist)

                // Parse the M3U from the network
                val parsedChannels = M3uParser.parseFromUrl(url, playlistId.toInt())

                if (parsedChannels.isNotEmpty()) {
                    repository.insertChannels(parsedChannels)
                    _playlistAddingStatus.value = PlaylistStatus.Success(parsedChannels.size)
                } else {
                    // Rollback playlist if no channels were found
                    repository.deletePlaylist(Playlist(id = playlistId.toInt(), name = name, url = url))
                    _playlistAddingStatus.value = PlaylistStatus.Error("Failed to parse streams. Ensure the URL is a valid M3U file.")
                }
            } catch (e: Exception) {
                _playlistAddingStatus.value = PlaylistStatus.Error("Error adding playlist: ${e.message}")
            }
        }
    }

    fun addManualChannel(name: String, url: String, category: String, logoUrl: String?) {
        viewModelScope.launch {
            val channel = Channel(
                name = name,
                url = url,
                category = if (category.isBlank()) "Live TV" else category,
                logoUrl = if (logoUrl?.isBlank() == true) null else logoUrl
            )
            repository.insertChannel(channel)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun clearPlaylistStatus() {
        _playlistAddingStatus.value = PlaylistStatus.Idle
    }
}

class ChannelViewModelFactory(
    private val application: Application,
    private val repository: ChannelRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChannelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChannelViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
