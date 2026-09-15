package com.zice.playbutton.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zice.playbutton.R
import com.zice.playbutton.data.remote.NoServerConfiguredException
import java.io.IOException

/**
 * Traduce los fallos a algo que el usuario pueda entender. El mensaje crudo de
 * una excepción de red no le dice nada a nadie.
 */
enum class UiError {
    NoServer,
    Connection,
    Generic,
    ;

    /**
     * Si volver a intentarlo puede arreglarlo. Que no haya servidor
     * configurado no se resuelve repitiendo la petición: hay que ir a ajustes.
     */
    val isRetryable: Boolean get() = this != NoServer
}

fun Throwable.toUiError(): UiError = when (this) {
    is NoServerConfiguredException -> UiError.NoServer
    is IOException -> UiError.Connection
    else -> UiError.Generic
}

@Composable
fun UiError.text(): String = stringResource(
    when (this) {
        UiError.NoServer -> R.string.server_url_empty
        UiError.Connection -> R.string.error_connection
        UiError.Generic -> R.string.error_generic
    },
)
