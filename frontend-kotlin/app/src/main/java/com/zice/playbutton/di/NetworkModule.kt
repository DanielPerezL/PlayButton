package com.zice.playbutton.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.zice.playbutton.BuildConfig
import com.zice.playbutton.data.remote.ApiService
import com.zice.playbutton.data.remote.AuthInterceptor
import com.zice.playbutton.data.remote.BaseUrlInterceptor
import com.zice.playbutton.data.remote.ReachabilityInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** El cliente de las portadas, que no es el de la API. Ver [NetworkModule]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // El backend puede añadir campos (por ejemplo el DTO detallado de
        // canción) sin que eso deba romper al cliente.
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        authInterceptor: AuthInterceptor,
        reachabilityInterceptor: ReachabilityInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        // El primero, para ver también lo que falla en los de debajo.
        .addInterceptor(reachabilityInterceptor)
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(authInterceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        // BASIC, no BODY: el cuerpo incluiría el token de acceso.
                        level = HttpLoggingInterceptor.Level.BASIC
                    },
                )
            }
        }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        // Marcador: BaseUrlInterceptor sustituye host y prefijo en cada petición
        // con el servidor que haya configurado el usuario.
        .baseUrl("http://playbutton.invalid/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    /**
     * Cliente aparte para las portadas, sin los interceptores de la API.
     *
     * No puede compartir el de Retrofit: [BaseUrlInterceptor] antepone el
     * prefijo de la URL base a la ruta de cada peticion, y convertiria
     * `/uploads/images/1` en `/api/uploads/images/1`. Las URL de las portadas
     * ya vienen absolutas y completas del servidor, asi que no hay nada que
     * reescribir. Tampoco interesa que [AuthInterceptor] lea un 401 de una
     * imagen como sesion caducada: el endpoint no pide token.
     */
    @Provides
    @Singleton
    @ImageHttpClient
    fun provideImageOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        @ImageHttpClient client: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
        .diskCache {
            DiskCache.Builder()
                // En filesDir y no en cacheDir: el sistema vacia la cache
                // cuando le hace falta espacio, y aqui viven tambien las
                // portadas de lo que se ha descargado para oir sin conexion.
                .directory(context.filesDir.resolve("image_cache"))
                .maxSizeBytes(IMAGE_CACHE_BYTES)
                .build()
        }
        .crossfade(true)
        .build()

    /** Unas 1.600 portadas al tamano en que las sirve el backend. */
    private const val IMAGE_CACHE_BYTES = 64L * 1024 * 1024
}
