package com.enterprise.manufacturing.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enterprise.manufacturing.core.R

@Composable
fun LoginPlaceholderScreen(modifier: Modifier = Modifier) {
    StubScreen(
        modifier = modifier,
        titleRes = R.string.core_stub_login_title,
        hintRes = R.string.core_stub_stage_hint,
    )
}

@Composable
fun HomePlaceholderScreen(
    onSignOut: () -> Unit,
    showAdminEntry: Boolean,
    onOpenAdmin: () -> Unit,
    showDefectEntry: Boolean,
    onOpenDefects: () -> Unit,
    showDrawingsEntry: Boolean,
    onOpenDrawings: () -> Unit,
    showTimesheetEntry: Boolean,
    onOpenTimesheet: () -> Unit,
    showUpdateEntry: Boolean,
    onOpenUpdate: () -> Unit,
    showSyncEntry: Boolean,
    onOpenSync: () -> Unit,
    showChatHubEntry: Boolean,
    onOpenChatHub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.core_stub_home_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(R.string.core_stub_stage_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showChatHubEntry) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenChatHub) {
                    Text(text = stringResource(R.string.core_open_general_chat))
                }
            }
            if (showDefectEntry) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenDefects) {
                    Text(text = stringResource(R.string.core_open_defects))
                }
            }
            if (showDrawingsEntry) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenDrawings) {
                    Text(text = stringResource(R.string.core_open_drawings))
                }
            }
            if (showTimesheetEntry) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenTimesheet) {
                    Text(text = stringResource(R.string.core_open_timesheet))
                }
            }
            if (showUpdateEntry) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenUpdate) {
                    Text(text = stringResource(R.string.core_open_updates))
                }
            }
            if (showSyncEntry) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenSync) {
                    Text(text = stringResource(R.string.core_open_sync))
                }
            }
            if (showAdminEntry) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenAdmin) {
                    Text(text = stringResource(R.string.core_open_admin))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSignOut) {
                Text(text = stringResource(R.string.core_sign_out))
            }
        }
    }
}

@Composable
fun AdminPlaceholderScreen(modifier: Modifier = Modifier) {
    StubScreen(
        modifier = modifier,
        titleRes = R.string.core_stub_admin_title,
        hintRes = R.string.core_stub_stage_hint,
    )
}

@Composable
private fun StubScreen(
    titleRes: Int,
    hintRes: Int,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(hintRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
