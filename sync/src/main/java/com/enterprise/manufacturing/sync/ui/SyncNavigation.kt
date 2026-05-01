package com.enterprise.manufacturing.sync.ui

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.work.WorkInfo
import com.enterprise.manufacturing.sync.R

@Composable
fun SyncRoute(navController: NavHostController) {
    val viewModel: SyncViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SyncScreen(
        state = state,
        onBack = { navController.popBackStack() },
        onSyncNow = { viewModel.requestSyncNow() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncScreen(
    state: SyncDashboardUiState,
    onBack: () -> Unit,
    onSyncNow: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync_title)) },
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
                text = stringResource(R.string.sync_pending_header),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.sync_pending_defects, state.pendingDefects),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.sync_pending_messages, state.pendingMessages),
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.sync_workmanager_header),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(
                    R.string.sync_immediate_state,
                    workStateLabel(state.immediateWorkState),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(
                    R.string.sync_periodic_state,
                    workStateLabel(state.periodicWorkState),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.sync_periodic_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.sync_conflict_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                onClick = onSyncNow,
            ) {
                Text(stringResource(R.string.sync_run_now))
            }
        }
    }
}

@Composable
private fun workStateLabel(state: WorkInfo.State?): String =
    when (state) {
        null -> stringResource(R.string.sync_wm_none)
        WorkInfo.State.ENQUEUED -> stringResource(R.string.sync_wm_enqueued)
        WorkInfo.State.RUNNING -> stringResource(R.string.sync_wm_running)
        WorkInfo.State.SUCCEEDED -> stringResource(R.string.sync_wm_succeeded)
        WorkInfo.State.FAILED -> stringResource(R.string.sync_wm_failed)
        WorkInfo.State.BLOCKED -> stringResource(R.string.sync_wm_blocked)
        WorkInfo.State.CANCELLED -> stringResource(R.string.sync_wm_cancelled)
    }
