package com.zice.playbutton.data.remote

/**
 * Normaliza la URL que teclea el usuario, replicando las reglas de la app
 * React Native: en release se exige HTTPS (el backend se publica tras un tunel
 * de Cloudflare), en debug se admite HTTP para apuntar a un servidor local, y
 * siempre se garantiza el sufijo `/api`.
 */
object ServerUrl {

    fun normalize(input: String, allowCleartext: Boolean): String? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isEmpty()) return null

        val withoutScheme = trimmed.removePrefix("https://").removePrefix("http://")
        if (withoutScheme.isEmpty()) return null

        // En release siempre HTTPS: el backend se publica tras un túnel y la
        // app nunca debe mandar el token en claro. En debug, si no se indica
        // esquema se asume HTTP, que es lo que hace falta para apuntar a un
        // backend local; escribir "https://" explícitamente sigue respetándose.
        val scheme = when {
            !allowCleartext -> "https://"
            trimmed.startsWith("https://") -> "https://"
            else -> "http://"
        }
        val withScheme = scheme + withoutScheme

        return if (withScheme.endsWith("/api")) withScheme else "$withScheme/api"
    }

    /**
     * Fuerza HTTPS en release sobre una URL absoluta que llega del servidor.
     *
     * El backend construye los enlaces firmados con `request.host_url`, y tras
     * el túnel ve la petición como HTTP, así que devuelve `http://`. En release
     * el tráfico en claro está prohibido y ExoPlayer falla con "Cleartext HTTP
     * traffic not permitted". El esquema lo decide la app, no la respuesta.
     */
    fun enforceScheme(url: String, allowCleartext: Boolean): String =
        if (allowCleartext || !url.startsWith("http://")) {
            url
        } else {
            "https://" + url.removePrefix("http://")
        }

    /** Versión legible para mostrar en Configuración. */
    fun display(url: String?): String =
        url?.removePrefix("https://")?.removePrefix("http://")?.removeSuffix("/api") ?: ""
}
