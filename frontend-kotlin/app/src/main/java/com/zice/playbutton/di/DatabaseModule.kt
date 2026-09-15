package com.zice.playbutton.di

import android.content.Context
import androidx.room.Room
import com.zice.playbutton.data.local.db.PlayButtonDatabase
import com.zice.playbutton.data.local.db.DownloadedPlaylistDao
import com.zice.playbutton.data.local.db.SongCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlayButtonDatabase =
        Room.databaseBuilder(context, PlayButtonDatabase::class.java, "playbutton.db")
            // Es solo caché: ante un cambio de esquema se puede rehacer desde la red.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideSongCacheDao(db: PlayButtonDatabase): SongCacheDao = db.songCacheDao()

    @Provides
    @Singleton
    fun provideDownloadedPlaylistDao(db: PlayButtonDatabase): DownloadedPlaylistDao =
        db.downloadedPlaylistDao()
}
