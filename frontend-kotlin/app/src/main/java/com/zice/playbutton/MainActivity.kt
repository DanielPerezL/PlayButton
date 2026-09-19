package com.zice.playbutton

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zice.playbutton.player.PlayerConnection
import com.zice.playbutton.player.PlayerVisibility
import com.zice.playbutton.ui.nav.MainViewModel
import com.zice.playbutton.ui.nav.PlayButtonApp
import com.zice.playbutton.ui.screens.access.AccessScreen
import com.zice.playbutton.ui.theme.PlayButtonTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerConnection: PlayerConnection
    @Inject lateinit var playerVisibility: PlayerVisibility

    // El lint da por hecho que atar la orientacion es cosa de no haberse
    // preparado para girar. Aqui es al reves: la app se reparte sola el hueco
    // que tenga, y en un movil se deja de pie porque es como se usa.
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Aqui y no en el manifest: alli la restriccion vale para todos los
        // aparatos por igual, y lo que se quiere es que dependa del sitio que
        // haya. El recurso lo resuelve el sistema por el ancho menor de la
        // pantalla, asi que en tablet y en plegable abierto sale false y la
        // app gira.
        if (resources.getBoolean(R.bool.lock_portrait)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        var sessionResolved = false
        // La pantalla de arranque se mantiene hasta saber si hay sesión, para no
        // enseñar el login un instante a quien ya había entrado.
        splashScreen.setKeepOnScreenCondition { !sessionResolved }

        setContent {
            PlayButtonTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn != null) sessionResolved = true
                }

                RequestNotificationPermission()

                when (isLoggedIn) {
                    true -> PlayButtonApp(viewModel = viewModel)
                    false -> AccessScreen()
                    null -> Unit // Aún se muestra la pantalla de arranque.
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playerConnection.connect()
    }

    /**
     * Soltar el servicio en cuanto la pantalla deja de verse. Mantenerlo
     * vinculado es lo que impedía que la app terminase al cerrarla: un servicio
     * con clientes atados sobrevive a su propio `stopSelf`. La música no se
     * entera —suena en el servicio—, y al volver se reconecta y se resincroniza.
     *
     * El reproductor grande se recoge aquí, no al volver: el proceso puede
     * sobrevivir a que se cierre la app, y quien la reabría al poco se
     * encontraba la capa tal cual la dejó, tapando el sitio en el que estaba.
     * Hacerlo ahora es lo mismo que darle a la flecha de bajar antes de irse,
     * y así al volver no se ve el reproductor caerse solo. Lo que suena sigue
     * a mano en el mini-reproductor, que es de donde se vuelve a abrir.
     */
    override fun onStop() {
        playerVisibility.collapse()
        playerConnection.release()
        super.onStop()
    }
}

/**
 * Desde Android 13 la notificación del reproductor no aparece sin permiso
 * explícito. La app React Native nunca lo pedía.
 */
@androidx.compose.runtime.Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Si se deniega, la reproducción sigue funcionando sin notificación. */ }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
