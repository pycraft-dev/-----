package com.enterprise.manufacturing.update.ui

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.enterprise.manufacturing.update.R
import com.enterprise.manufacturing.update.install.AppUpdateInstaller
import kotlinx.coroutines.launch

@Composable
fun UpdateRoute(navController: NavHostController) {
    val viewModel: UpdateViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(state.phase) {
        val err = state.phase as? UpdatePhase.Error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(err.messageRes))
        viewModel.clearError()
    }

    UpdateScreen(
        snackbarHostState = snackbarHostState,
        state = state,
        onBack = { navController.popBackStack() },
        onCheck = { viewModel.checkForUpdates() },
        onDownload = { viewModel.downloadOffer() },
        onDismissResult = { viewModel.dismissOffer() },
        onInstall = {
            val activity = context as? Activity ?: return@UpdateScreen
            val file = (state.phase as? UpdatePhase.ReadyInstall)?.file ?: return@UpdateScreen
            if (!AppUpdateInstaller.canRequestInstallPackages(activity)) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.update_need_install_permission),
                    )
                }
                AppUpdateInstaller.openInstallPermissionSettings(activity)
            } else {
                val ok = AppUpdateInstaller.launchInstall(activity, file)
                if (!ok) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.update_err_install))
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateScreen(
    snackbarHostState: SnackbarHostState,
    state: UpdateUiState,
    onBack: () -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onDismissResult: () -> Unit,
    onInstall: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.update_current_version,
                    state.currentVersionName,
                    state.currentVersionCode,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = state.phase !is UpdatePhase.Checking &&
                    state.phase !is UpdatePhase.Downloading,
                onClick = onCheck,
            ) {
                Text(stringResource(R.string.update_check))
            }

            when (val phase = state.phase) {
                UpdatePhase.Checking -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(R.string.update_checking),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                UpdatePhase.UpToDate -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.update_up_to_date),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onDismissResult) {
                        Text(stringResource(R.string.update_close_result))
                    }
                }

                is UpdatePhase.Offer -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(
                            R.string.update_available_line,
                            phase.latestVersionCode,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (phase.releaseNotes.isNotBlank()) {
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = phase.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (phase.apkUrl.isNotBlank()) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onDownload,
                        ) {
                            Text(stringResource(R.string.update_download))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.update_apk_url_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onDismissResult) {
                        Text(stringResource(R.string.update_close_result))
                    }
                }

                UpdatePhase.Downloading -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(R.string.update_downloading),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                is UpdatePhase.ReadyInstall -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.update_ready_install),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onInstall,
                    ) {
                        Text(stringResource(R.string.update_install))
                    }
                }

                UpdatePhase.Idle,
                is UpdatePhase.Error,
                -> Unit
            }
        }
    }
}
