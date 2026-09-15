package com.zice.playbutton.ui.screens.access

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import android.content.Intent
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zice.playbutton.R
import com.zice.playbutton.data.remote.ServerUrl
import com.zice.playbutton.ui.components.PasswordField
import com.zice.playbutton.ui.theme.Brand500

/** Repositorio publico del proyecto. */
private const val PLAYBUTTON_REPOSITORY = "https://github.com/DanielPerezL/PlayButton"

/**
 * Puerta de entrada. PlayButton es un servidor privado: antes de poder
 * iniciar sesión hay que decirle a la app a qué servidor conectarse.
 */
@Composable
fun AccessScreen(
    modifier: Modifier = Modifier,
    viewModel: AccessViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val noServerMessage = stringResource(R.string.server_url_empty)
    val failedMessage = stringResource(R.string.login_failed)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_wordmark),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(0.8f),
            )

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.shapes.large,
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Sin la pista de autorrelleno el sistema adivina por
                // heurísticas y acaba metiendo la contraseña también aquí.
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = viewModel::onNicknameChange,
                    label = { Text(stringResource(R.string.login_nickname)) },
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Username },
                )

                PasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(R.string.login_password),
                    enabled = !state.isSubmitting,
                    imeAction = ImeAction.Go,
                    onImeAction = { viewModel.login(noServerMessage, failedMessage) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (viewModel.arrivedByExpiry && state.error == null) {
                    Text(
                        text = stringResource(R.string.error_session_expired),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Button(
                    onClick = { viewModel.login(noServerMessage, failedMessage) },
                    enabled = !state.isSubmitting &&
                        state.nickname.isNotBlank() &&
                        state.password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSubmitting) {
                        // Cuadrado y no solo con altura: en un rectángulo el
                        // arco se dibuja deformado y gira fuera de su centro.
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.login_submit))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.login_no_account),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            TextButton(onClick = viewModel::openServerDialog) {
                Text(
                    text = serverUrl?.let { ServerUrl.display(it) }
                        ?: stringResource(R.string.login_configure_server),
                    color = Brand500,
                )
            }

            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, PLAYBUTTON_REPOSITORY.toUri()),
                    )
                },
            ) {
                Text(
                    text = stringResource(R.string.login_view_github),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (state.serverDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::closeServerDialog,
            title = { Text(stringResource(R.string.server_url)) },
            text = {
                OutlinedTextField(
                    value = state.serverDraft,
                    onValueChange = viewModel::onServerDraftChange,
                    placeholder = { Text(stringResource(R.string.server_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.saveServer() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveServer) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeServerDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}
