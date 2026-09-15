package com.zice.playbutton.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore("session")

data class Session(
    val accessToken: String,
    val userId: Int,
    val isAdmin: Boolean,
)

/** Lo unico que el repositorio necesita saber de la sesion. */
interface SessionProvider {
    suspend fun currentUserId(): Int?
}

/**
 * Sesion del usuario. El backend emite un JWT de 90 dias y no expone
 * endpoint de refresco, asi que la unica salida ante un 401 es limpiar la
 * sesion y volver a pedir credenciales.
 */
@Singleton
class SessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SessionProvider {
    private object Keys {
        val Token = stringPreferencesKey("access_token")
        val UserId = intPreferencesKey("user_id")
        val IsAdmin = booleanPreferencesKey("is_admin")
    }

    val session: Flow<Session?> = context.sessionDataStore.data.map { prefs ->
        val token = prefs[Keys.Token]
        val userId = prefs[Keys.UserId]
        if (token.isNullOrBlank() || userId == null) {
            null
        } else {
            Session(token, userId, prefs[Keys.IsAdmin] ?: false)
        }
    }

    val isLoggedIn: Flow<Boolean> = session.map { it != null }

    suspend fun current(): Session? = session.first()

    override suspend fun currentUserId(): Int? = current()?.userId

    /**
     * Lectura sincrona para el interceptor de OkHttp, que corre en un hilo de
     * red donde no podemos suspender.
     */
    fun currentBlocking(): Session? = runBlocking { current() }

    suspend fun save(token: String, userId: Int, isAdmin: Boolean) {
        context.sessionDataStore.edit {
            it[Keys.Token] = token
            it[Keys.UserId] = userId
            it[Keys.IsAdmin] = isAdmin
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }

    fun clearBlocking() = runBlocking { clear() }
}
