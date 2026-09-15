package com.zice.playbutton.di

import android.content.Context
import androidx.room.Room
import com.zice.playbutton.data.local.db.PlayButtonDatabase
import com.zice.playbutton.data.local.db.DownloadedPlaylistDao
import com.zice.playbutton.data.local.db.MIGRATION_2_3
import com.zice.playbutton.data.local.db.MIGRATION_3_4
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
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            // Último recurso para un cambio de esquema sin migración escrita. No
            // es gratis: `downloaded_playlists` no se puede rehacer desde la red,
            // es lo único que dice qué audio hay guardado y a qué playlist
            // pertenece. Los cambios que la toquen llevan su migración.
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
