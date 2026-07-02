package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ChannelRepository(private val channelDao: ChannelDao) {

    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels()
    val favoriteChannels: Flow<List<Channel>> = channelDao.getFavoriteChannels()
    val recentlyWatchedChannels: Flow<List<Channel>> = channelDao.getRecentlyWatchedChannels()
    val allPlaylists: Flow<List<Playlist>> = channelDao.getAllPlaylists()

    suspend fun getChannelById(id: Int): Channel? {
        return channelDao.getChannelById(id)
    }

    suspend fun insertChannel(channel: Channel): Long {
        return channelDao.insertChannel(channel)
    }

    suspend fun insertChannels(channels: List<Channel>) {
        channelDao.insertChannels(channels)
    }

    suspend fun updateChannel(channel: Channel) {
        channelDao.updateChannel(channel)
    }

    suspend fun deleteChannelById(id: Int) {
        channelDao.deleteChannelById(id)
    }

    suspend fun insertPlaylist(playlist: Playlist): Long {
        return channelDao.insertPlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        // Delete all channels loaded from this playlist
        channelDao.deleteChannelsByPlaylistId(playlist.id)
        // Delete the playlist itself
        channelDao.deletePlaylist(playlist)
    }

    suspend fun ensureSeedsExist() {
        val channels = channelDao.getAllChannels().firstOrNull()
        if (channels.isNullOrEmpty()) {
            AppDatabase.seedInitialChannels(channelDao)
        }

        // Also check if our default playlists exist, if not, pre-load them directly!
        val playlists = channelDao.getAllPlaylists().firstOrNull()
        if (playlists.isNullOrEmpty()) {
            try {
                // Preload Samsung TV Plus Playlist
                val samsungPlaylist = Playlist(
                    name = "Samsung TV Plus",
                    url = "https://raw.githubusercontent.com/FazzR/samsung-tv-plus-m3u/main/samsung-tv-plus.m3u"
                )
                val samsungId = channelDao.insertPlaylist(samsungPlaylist)
                val samsungChannels = M3uParser.parseFromUrl(samsungPlaylist.url, samsungId.toInt())
                if (samsungChannels.isNotEmpty()) {
                    channelDao.insertChannels(samsungChannels)
                }

                // Preload Pluto TV Playlist
                val plutoPlaylist = Playlist(
                    name = "Pluto TV",
                    url = "https://raw.githubusercontent.com/FazzR/iptv-pluto-tv/main/pluto-tv.m3u"
                )
                val plutoId = channelDao.insertPlaylist(plutoPlaylist)
                val plutoChannels = M3uParser.parseFromUrl(plutoPlaylist.url, plutoId.toInt())
                if (plutoChannels.isNotEmpty()) {
                    channelDao.insertChannels(plutoChannels)
                }
            } catch (e: Exception) {
                Log.e("ChannelRepository", "Error seeding default playlists", e)
            }
        }
    }
}
