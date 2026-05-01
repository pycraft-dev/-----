package com.enterprise.manufacturing.core.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.enterprise.manufacturing.core.R
import com.enterprise.manufacturing.core.chat.ChatHubViewModel
import com.enterprise.manufacturing.core.db.entity.GeneralChatMessageEntity
import com.enterprise.manufacturing.core.db.entity.UserEntity
import com.enterprise.manufacturing.core.model.TeamChatMessageType
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ChatHubRoute(navController: NavHostController) {
    val viewModel: ChatHubViewModel = hiltViewModel()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    val playingMessageId by viewModel.playingMessageId.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setChatScreenVisible(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setChatScreenVisible(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    var draft by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var pendingSpeechMessageId by remember { mutableLongStateOf(-1L) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val msgId = pendingSpeechMessageId
        pendingSpeechMessageId = -1L
        if (msgId <= 0L) return@rememberLauncherForActivityResult
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = matches?.firstOrNull().orEmpty()
        viewModel.mergeTranscriptFromSpeech(msgId, text)
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.sendAttachment(uri, draft.trim())
        draft = ""
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startRecording()
    }

    ChatHubScreen(
        drawerState = drawerState,
        messages = messages,
        users = users,
        currentUserId = currentUserId,
        draft = draft,
        onDraftChange = { draft = it },
        recording = recording,
        playingMessageId = playingMessageId,
        userOnline = { uid -> viewModel.isUserOnline(uid) },
        onSendText = {
            viewModel.sendText(draft)
            draft = ""
        },
        onAttachClick = { pickFileLauncher.launch(arrayOf("*/*")) },
        onMicClick = {
            if (recording) {
                viewModel.stopRecordingAndSend()
            } else {
                when {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED -> viewModel.startRecording()

                    else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        },
        onCancelRecording = { viewModel.cancelRecording() },
        onPlayVoice = { msgId, path -> viewModel.togglePlayVoice(msgId, path) },
        onTranscribeVoice = { msgId ->
            pendingSpeechMessageId = msgId
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag("ru-RU"))
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.chat_speech_prompt))
            }
            runCatching { speechLauncher.launch(intent) }
        },
        onOpenFile = { path, mime ->
            val file = File(path)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(viewIntent, null))
            }
        },
        onNavigateDefects = {
            scope.launch { drawerState.close() }
            navController.navigate(AppRoute.DefectList.route)
        },
        onNavigateDrawings = {
            scope.launch { drawerState.close() }
            navController.navigate(AppRoute.DrawingList.route)
        },
        onNavigateTimesheet = {
            scope.launch { drawerState.close() }
            navController.navigate(AppRoute.Timesheet.route)
        },
        onNavigateDesktop = {
            scope.launch { drawerState.close() }
            navController.navigate(AppRoute.Home.route)
        },
        onNavigateAdmin = {
            scope.launch { drawerState.close() }
            navController.navigate(AppRoute.Admin.route)
        },
        onNavigateSync = {
            scope.launch { drawerState.close() }
            navController.navigate(AppRoute.Sync.route)
        },
        showAdminEntry = isAdmin,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHubScreen(
    drawerState: androidx.compose.material3.DrawerState,
    messages: List<GeneralChatMessageEntity>,
    users: List<UserEntity>,
    currentUserId: Long?,
    draft: String,
    onDraftChange: (String) -> Unit,
    recording: Boolean,
    playingMessageId: Long?,
    userOnline: (Long) -> Boolean,
    onSendText: () -> Unit,
    onAttachClick: () -> Unit,
    onMicClick: () -> Unit,
    onCancelRecording: () -> Unit,
    onPlayVoice: (Long, String) -> Unit,
    onTranscribeVoice: (Long) -> Unit,
    onOpenFile: (String, String) -> Unit,
    onNavigateDefects: () -> Unit,
    onNavigateDrawings: () -> Unit,
    onNavigateTimesheet: () -> Unit,
    onNavigateDesktop: () -> Unit,
    onNavigateAdmin: () -> Unit,
    onNavigateSync: () -> Unit,
    showAdminEntry: Boolean,
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.chat_drawer_channels),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.chat_channel_general)) },
                            selected = true,
                            onClick = { scope.launch { drawerState.close() } },
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
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
                            onClick = onNavigateDefects,
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.core_open_drawings)) },
                            selected = false,
                            onClick = onNavigateDrawings,
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.core_open_timesheet)) },
                            selected = false,
                            onClick = onNavigateTimesheet,
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.core_open_sync)) },
                            selected = false,
                            onClick = onNavigateSync,
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.chat_drawer_desktop)) },
                            selected = false,
                            onClick = onNavigateDesktop,
                        )
                    }
                    if (showAdminEntry) {
                        item {
                            NavigationDrawerItem(
                                label = { Text(stringResource(R.string.core_open_admin)) },
                                selected = false,
                                onClick = onNavigateAdmin,
                            )
                        }
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    item {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.chat_drawer_people),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    items(users, key = { it.id }) { u ->
                        UserPresenceRow(
                            fullName = u.fullName,
                            login = u.login,
                            online = userOnline(u.id),
                        )
                    }
                    item {
                        Text(
                            modifier = Modifier.padding(16.dp),
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
                    title = { Text(stringResource(R.string.chat_hub_title)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    },
                )
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (recording) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.chat_recording),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            TextButton(onClick = onCancelRecording) {
                                Text(stringResource(R.string.chat_cancel_record))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(onClick = onAttachClick) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null)
                        }
                        IconButton(onClick = onMicClick) {
                            Icon(
                                imageVector = if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = null,
                                tint = if (recording) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            )
                        }
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = draft,
                            onValueChange = onDraftChange,
                            label = { Text(stringResource(R.string.general_chat_message_hint)) },
                            singleLine = false,
                            maxLines = 4,
                        )
                        Button(onClick = onSendText, enabled = draft.isNotBlank()) {
                            Text(stringResource(R.string.general_chat_send))
                        }
                    }
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        msg = msg,
                        currentUserId = currentUserId,
                        playingMessageId = playingMessageId,
                        onPlayVoice = onPlayVoice,
                        onTranscribeVoice = onTranscribeVoice,
                        onOpenFile = onOpenFile,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserPresenceRow(
    fullName: String,
    login: String,
    online: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (online) "●" else "○",
            color = if (online) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 8.dp),
        )
        Column {
            Text(fullName, style = MaterialTheme.typography.bodyMedium)
            Text(
                login,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(
    msg: GeneralChatMessageEntity,
    currentUserId: Long?,
    playingMessageId: Long?,
    onPlayVoice: (Long, String) -> Unit,
    onTranscribeVoice: (Long) -> Unit,
    onOpenFile: (String, String) -> Unit,
) {
    val type = runCatching { TeamChatMessageType.valueOf(msg.messageType) }.getOrNull()
        ?: TeamChatMessageType.TEXT

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "#${msg.senderUserId}" +
                    if (msg.senderUserId == currentUserId) " · вы" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (type) {
                TeamChatMessageType.TEXT ->
                    Text(msg.body, style = MaterialTheme.typography.bodyMedium)

                TeamChatMessageType.VOICE -> {
                    val path = msg.attachmentLocalPath
                    if (path != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onPlayVoice(msg.id, path) }) {
                                Icon(
                                    if (playingMessageId == msg.id) Icons.Filled.Stop
                                    else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                )
                            }
                            Text(
                                text = formatDuration(msg.voiceDurationMs),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (msg.transcript.isNotBlank()) {
                            Text(
                                modifier = Modifier.padding(top = 6.dp),
                                text = msg.transcript,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        TextButton(onClick = { onTranscribeVoice(msg.id) }) {
                            Icon(Icons.Filled.Translate, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.chat_add_transcript))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.chat_attachment_missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                TeamChatMessageType.FILE -> {
                    val path = msg.attachmentLocalPath
                    if (path != null) {
                        val name = msg.attachmentDisplayName ?: path.substringAfterLast('/')
                        val mime = msg.attachmentMime ?: "*/*"
                        Text(
                            modifier = Modifier.clickable { onOpenFile(path, mime) },
                            text = name,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (msg.body.isNotBlank()) {
                            Text(msg.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.chat_attachment_missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val s = TimeUnit.MILLISECONDS.toSeconds(ms).coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return String.format(Locale.US, "%d:%02d", m, r)
}
