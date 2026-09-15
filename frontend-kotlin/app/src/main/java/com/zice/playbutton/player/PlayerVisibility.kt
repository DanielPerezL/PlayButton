package com.zice.playbutton.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Si el reproductor a pantalla completa está desplegado.
 *
 * Vive fuera de los ViewModels porque quien lo abre y quien lo pinta no son la
 * misma pantalla: la capa la dibuja la raíz de la app, pero abrirla es
 * consecuencia de darle a reproducir en cualquier sitio —el Modo Zen, una
 * playlist entera, una canción suelta de la lista—. Pasarlo por callbacks
 * desde la navegación obligaba a repetir la regla en cada pantalla, con lo
 * fácil que es que a la siguiente se le olvide.
 *
 * Desplegado no es lo mismo que visible: la raíz de la app solo enseña la capa
 * si además hay algo que escuchar. Aquí se guarda lo que ha pedido el usuario,
 * no lo que acaba pintándose.
 */
@Singleton
class PlayerVisibility @Inject constructor() {

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    /** Poner algo a sonar es pedir ver lo que suena. */
    fun expand() {
        _isExpanded.value = true
    }

    fun collapse() {
        _isExpanded.value = false
    }
}
