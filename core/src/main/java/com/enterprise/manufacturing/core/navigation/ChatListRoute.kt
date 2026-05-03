package com.enterprise.manufacturing.core.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.enterprise.manufacturing.core.R
import com.enterprise.manufacturing.core.chat.ChatListViewModel
import com.enterprise.manufacturing.core.db.entity.UserEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListRoute(navController: NavHostController) {
    val viewModel: ChatListViewModel = hiltViewModel()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val listPresence by viewModel.screenVisible.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setScreenVisible(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setScreenVisible(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.chat_drawer_sections),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.core_open_defects)) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(AppRoute.DefectList.route)
                            },
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.core_open_drawings)) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(AppRoute.DrawingList.route)
                            },
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.core_open_timesheet)) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(AppRoute.Timesheet.route)
                            },
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.core_open_sync)) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(AppRoute.Sync.route)
                            },
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.chat_drawer_desktop)) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(AppRoute.Home.route)
                            },
                        )
                    }
                    if (isAdmin) {
                        item {
                            NavigationDrawerItem(
                                label = { Text(stringResource(R.string.core_open_admin)) },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(AppRoute.Admin.route)
                                },
                            )
                        }
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.chat_presence_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.chat_list_title)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(users, key = { "${it.id}_$listPresence" }) { user ->
                    ChatListUserRow(
                        user = user,
                        online = viewModel.isUserOnline(user.id),
                        onClick = {
                            navController.navigate(directChatPeerRoute(user.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatListUserRow(
    user: UserEntity,
    online: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (online) "●" else "○",
            color = if (online) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 12.dp),
        )
        Column {
            Text(user.fullName, style = MaterialTheme.typography.titleMedium)
            Text(
                user.login,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
