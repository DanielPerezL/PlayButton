package com.zice.playbutton

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PlayButtonApp : Application(), SingletonImageLoader.Factory {

    /**
     * El que construye NetworkModule, con su propio cliente HTTP. Coil lo pide
     * la primera vez que hay que pintar una portada, mucho despues de que Hilt
     * haya inyectado en onCreate().
     */
    @Inject
    lateinit var imageLoader: ImageLoader

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
