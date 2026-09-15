package com.zice.playbutton.data.remote

import com.zice.playbutton.data.local.SessionStore
import com.zice.playbutton.data.local.SettingsStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Se lanza cuando aún no se ha configurado ningún servidor. */
class NoServerConfiguredException : IOException("No hay servidor configurado")

/**
 * El servidor es autoalojado: cada usuario apunta la app al suyo, así que la
 * URL base no se conoce en tiempo de compilación. Retrofit se construye con
 * una base ficticia y este interceptor reescribe esquema, host, puerto y
 * prefijo de ruta en cada petición.
 */
@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val settingsStore: SettingsStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val configured = runBlocking { settingsStore.currentServerUrl() }
            ?: throw NoServerConfiguredException()
        val base = configured.toHttpUrlOrNull() ?: throw NoServerConfiguredException()

        val request = chain.request()
        // La base ya incluye el sufijo "/api" (y cualquier subdirectorio en el
        // que esté montado el backend); la ruta de Retrofit se cuelga de ella.
        val basePath = base.encodedPath.trimEnd('/')
        val newUrl = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .encodedPath(basePath + request.url.encodedPath)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}

/**
 * Inyecta el JWT y centraliza el cierre de sesión. La app React Native
 * intentaba esto en fetchingService.js, pero llamaba a `logout()` sin
 * importarlo: el ReferenceError se tragaba en el catch y el usuario se
 * quedaba con una sesión caducada dando vueltas entre reintentos.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
    private val authEvents: AuthEvents,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = sessionStore.currentBlocking()?.accessToken

        val authorized = if (token != null && request.header("Authorization") == null) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            request
        }

        val response = chain.proceed(authorized)

        // 422 lo devuelve Flask-JWT-Extended ante un token malformado; para la
        // app significa lo mismo que un 401.
        if ((response.code == 401 || response.code == 422) && token != null) {
            sessionStore.clearBlocking()
            authEvents.notifySessionExpired()
        }
        return response
    }
}

/** Bus para avisar a la UI de que la sesión dejó de ser válida. */
@Singleton
class AuthEvents @Inject constructor() {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired = _sessionExpired

    /**
     * Se mantiene hasta que la pantalla de acceso lo consume, para poder
     * explicar al usuario por qué se le pide la contraseña otra vez.
     */
    @Volatile
    var expiredPending: Boolean = false
        private set

    fun notifySessionExpired() {
        expiredPending = true
        _sessionExpired.tryEmit(Unit)
    }

    fun consumeExpired(): Boolean {
        val pending = expiredPending
        expiredPending = false
        return pending
    }
}

/**
 * Si el servidor está al alcance. No se pregunta al sistema por la
 * conectividad: se mira lo que pasa de verdad en las peticiones, que es lo
 * único que importa aquí. Un móvil con wifi y el servidor de casa apagado está
 * tan «sin conexión» como uno en modo avión, y al contrario: durante el
 * desarrollo se llega al backend por un túnel de adb sin que el dispositivo
 * tenga internet.
 *
 * Se empieza suponiendo que sí: hasta que algo falla no hay motivo para
 * esconderle nada al usuario.
 */
@Singleton
class ServerReachability @Inject constructor() {
    private val _isReachable = MutableStateFlow(true)
    val isReachable: StateFlow<Boolean> = _isReachable.asStateFlow()

    fun report(reachable: Boolean) {
        _isReachable.value = reachable
    }
}

/**
 * Alimenta [ServerReachability]. Va el primero de la cadena para ver también
 * los fallos de los interceptores que tiene debajo.
 */
@Singleton
class ReachabilityInterceptor @Inject constructor(
    private val reachability: ServerReachability,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = try {
        // Que responda basta, aunque sea un 404: el servidor está ahí.
        chain.proceed(chain.request()).also { reachability.report(true) }
    } catch (error: IOException) {
        // Que no haya servidor configurado no es quedarse sin conexión, y
        // marcarlo como tal esconderría el Modo Zen a quien solo tiene que
        // pasar por ajustes a poner la URL.
        if (error !is NoServerConfiguredException) reachability.report(false)
        throw error
    }
}
