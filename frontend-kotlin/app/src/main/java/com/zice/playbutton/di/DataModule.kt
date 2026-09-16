package com.zice.playbutton.di

import com.zice.playbutton.data.local.CoilImageCache
import com.zice.playbutton.data.local.ContentResolverPickedImages
import com.zice.playbutton.data.local.ImageCache
import com.zice.playbutton.data.local.PickedImages
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

    @Binds
    @Singleton
    abstract fun bindImageCache(imageCache: CoilImageCache): ImageCache

    @Binds
    @Singleton
    abstract fun bindPickedImages(pickedImages: ContentResolverPickedImages): PickedImages
}
