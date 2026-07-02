package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Channel::class, Playlist::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "brotherhood_tv_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            scope.launch(Dispatchers.IO) {
                                seedInitialChannels(database.channelDao())
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialChannels(channelDao: ChannelDao) {
            val seeds = listOf(
                Channel(
                    name = "Caze TV",
                    url = "https://dfr80qz435crc.cloudfront.net/MNOP/Amagi/Caze/Caze_TV_BR/1080p-vtt/index.m3u8",
                    logoUrl = "https://yt3.googleusercontent.com/ytc/AIdro_loH37Ggsh4o1t7_4vNof5bWw7Z1XWjE2m_bZ1Cyg601Yg=s176-c-k-c0x00ffffff-no-rj",
                    category = "Sports",
                    isSeed = true
                ),
                Channel(
                    name = "T Sports",
                    url = "http://rgkkw.live:80/live/1Aoen7elp5/IgMJ60tmAa/130714.ts",
                    logoUrl = "https://raw.githubusercontent.com/nobel-mollik/BrotherHoodTV-Assets/main/tsports.png", // fallback or nice assets
                    category = "Sports",
                    isSeed = true
                ),
                Channel(
                    name = "PTV Sports",
                    url = "https://tvsen5.aynaott.com/PtvSports/index.m3u8",
                    logoUrl = "https://ptvsports.pk/wp-content/uploads/2021/10/ptv-sports-live-logo.png",
                    category = "Sports",
                    isSeed = true
                ),
                Channel(
                    name = "T Sports 2",
                    url = "http://103.59.176.72:8083/live1/tracks-v1a1/mono.m3u8?token=123",
                    logoUrl = "https://raw.githubusercontent.com/nobel-mollik/BrotherHoodTV-Assets/main/tsports2.png",
                    category = "Sports",
                    isSeed = true
                ),
                Channel(
                    name = "Bein Sports 1",
                    url = "https://1nyaler.streamhostingcdn.top/stream/23/index.m3u8",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/BeIn_Sports_logo.svg/1200px-BeIn_Sports_logo.svg.png",
                    category = "Sports",
                    isSeed = true
                ),
                Channel(
                    name = "WIN Sports",
                    url = "https://1nyaler.streamhostingcdn.top/stream/32/index.m3u8",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e0/Win_Sports_Logo.png",
                    category = "Sports",
                    isSeed = true
                )
            )
            channelDao.insertChannels(seeds)
        }
    }
}
