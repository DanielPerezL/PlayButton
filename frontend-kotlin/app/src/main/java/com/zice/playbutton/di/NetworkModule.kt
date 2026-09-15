package com.zice.playbutton.di

import com.zice.playbutton.BuildConfig
import com.zice.playbutton.data.remote.ApiService
import com.zice.playbutton.data.remote.AuthInterceptor
import com.zice.playbutton.data.remote.BaseUrlInterceptor
import com.zice.playbutton.data.remote.ReachabilityInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

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
}
