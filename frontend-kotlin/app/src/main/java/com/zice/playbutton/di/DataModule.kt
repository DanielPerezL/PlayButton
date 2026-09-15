package com.zice.playbutton.di

import com.zice.playbutton.data.local.SessionProvider
import com.zice.playbutton.data.local.SessionStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSessionProvider(sessionStore: SessionStore): SessionProvider
}
