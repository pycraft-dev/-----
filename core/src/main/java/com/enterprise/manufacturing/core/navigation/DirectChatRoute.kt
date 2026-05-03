package com.enterprise.manufacturing.core.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.enterprise.manufacturing.core.R
import com.enterprise.manufacturing.core.chat.DirectChatViewModel
import com.enterprise.manufacturing.core.db.entity.GeneralChatMessageEntity
import com.enterprise.manufacturing.core.di.ChatRemoteEntryPoint
import com.enterprise.manufacturing.core.model.TeamChatMessageType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private val IncomingBubbleColor = Color(0xFFE8EEF5)
private val OutgoingGradientStart = Color(0xFF7B68EE)
private val OutgoingGradientEnd = Color(0xFF3498DB)

private sealed interface TimelineEntry {
    data class DayChip(val text: String) : TimelineEntry

    data class MessageRow(val msg: GeneralChatMessageEntity, val timeText: String) : TimelineEntry
}

private fun buildTimeline(
    messages: List<GeneralChatMessageEntity>,
    todayLabel: String,
    yesterdayLabel: String,
): List<TimelineEntry> {
    if (messages.isEmpty()) return emptyList()
    val zone = ZoneId.systemDefault()
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
    val dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    val result = mutableListOf<TimelineEntry>()
    var lastDay: LocalDate? = null
    val today = LocalDate.now(zone)
    for (m in messages) {
        val day = Instant.ofEpochMilli(m.createdAtEpochMs).atZone(zone).toLocalDate()
        if (day != lastDay) {
            lastDay = day
            val chip =
                when (day) {
                    today -> todayLabel
                    today.minusDays(1) -> yesterdayLabel
                    else -> dateFmt.format(day)
                }
            result += TimelineEntry.DayChip(chip)
        }
        val timeText =
            timeFmt.format(
                Instant.ofEpochMilli(m.createdAtEpochMs).atZone(zone).toLocalTime(),
            )
        result += TimelineEntry.MessageRow(m, timeText)
    }
    return result
}

private fun isImageAttachmentMime(mime: String?): Boolean =
    (mime?.lowercase(Locale.US) ?: "").startsWith("image/")

private fun decodeSampledChatBitmap(path: String, maxSidePx: Int): android.graphics.Bitmap? {
    val file = File(path)
    if (!file.exists() || !file.isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxSidePx || bounds.outHeight / sampleSize > maxSidePx) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
}

@Composable
private fun ChatImageThumbnailContent(path: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { decodeSampledChatBitmap(path, maxSidePx = 1440) }
    }
    Box(
        modifier = modifier.heightIn(max = 240.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null ->
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.chat_image_attachment_cd),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
            else ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                    )
                    Text(
                        stringResource(R.string.chat_image_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
        }
    }
}

@Composable
fun DirectChatRoute(navController: NavHostController) {
    val viewModel: DirectChatViewModel = hiltViewModel()
    val messages by viewModel.messages.collectAsStateWithLifecycle(initialValue = emptyList())

    val peerUser by viewModel.peerUser.collectAsStateWithLifecycle()
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
    val chatRepo =
        remember(context.applicationContext) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ChatRemoteEntryPoint::class.java,
            ).generalChatRepository()
        }
    val peerUid = viewModel.conversationPeerUserId
    LaunchedEffect(peerUid, currentUserId) {
        val uid = currentUserId ?: return@LaunchedEffect
        chatRepo.attachRemoteDirectConversation(peerUid, uid)
        try {
            awaitCancellation()
        } finally {
            chatRepo.detachRemoteDirectConversation(peerUid)
        }
    }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val transcriptFailedText = stringResource(R.string.chat_transcript_failed)

    LaunchedEffect(snackbarHostState) {
        viewModel.voiceTranscriptFailed.collect {
            snackbarHostState.showSnackbar(transcriptFailedText)
        }
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

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { success ->
            val uri = pendingCameraUri
            pendingCameraUri = null
            if (success && uri != null) {
                viewModel.sendAttachment(uri, draft.trim())
                draft = ""
            }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (!granted) return@rememberLauncherForActivityResult
            val dir = File(context.cacheDir, "camera_capture").apply { mkdirs() }
            val file = File(dir, "chat_${System.currentTimeMillis()}.jpg")
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }

    val onCameraClick: () -> Unit = {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                val dir = File(context.cacheDir, "camera_capture").apply { mkdirs() }
                val file = File(dir, "chat_${System.currentTimeMillis()}.jpg")
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                pendingCameraUri = uri
                takePictureLauncher.launch(uri)
            }

            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DirectChatScreen(
        drawerState = drawerState,
        navController = navController,
        title = peerUser?.fullName ?: stringResource(R.string.chat_direct_fallback_title),
        messages = messages,
        currentUserId = currentUserId,
        draft = draft,
        onDraftChange = { draft = it },
        recording = recording,
        playingMessageId = playingMessageId,
        onSendText = {
            viewModel.sendText(draft)
            draft = ""
        },
        onAttachClick = { pickFileLauncher.launch(arrayOf("*/*")) },
        onCameraClick = onCameraClick,
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
        onTranscribeVoice = { msgId, path ->
            viewModel.transcribeVoiceMessage(msgId, path)
        },
        onOpenFile = { path, mime ->
            val file = File(path)
            if (file.exists()) {
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                val viewIntent =
                    Intent(Intent.ACTION_VIEW).apply {
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
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectChatScreen(
    drawerState: androidx.compose.material3.DrawerState,
    navController: NavHostController,
    title: String,
    messages: List<GeneralChatMessageEntity>,
    currentUserId: Long?,
    draft: String,
    onDraftChange: (String) -> Unit,
    recording: Boolean,
    playingMessageId: Long?,
    onSendText: () -> Unit,
    onAttachClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
    onCancelRecording: () -> Unit,
    onPlayVoice: (Long, String) -> Unit,
    onTranscribeVoice: (Long, String) -> Unit,
    onOpenFile: (String, String) -> Unit,
    onNavigateDefects: () -> Unit,
    onNavigateDrawings: () -> Unit,
    onNavigateTimesheet: () -> Unit,
    onNavigateDesktop: () -> Unit,
    onNavigateAdmin: () -> Unit,
    onNavigateSync: () -> Unit,
    showAdminEntry: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    val todayStr = stringResource(R.string.chat_day_today)
    val yesterdayStr = stringResource(R.string.chat_day_yesterday)
    val timeline =
        remember(messages, todayStr, yesterdayStr) {
            buildTimeline(messages, todayStr, yesterdayStr)
        }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 320.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    },
                )
            },
            bottomBar = {
                val darkMessengerBar = isSystemInDarkTheme()
                val barColor =
                    if (darkMessengerBar) Color(0xFF141414) else MaterialTheme.colorScheme.surfaceContainerHigh
                val onBar = MaterialTheme.colorScheme.onSurface
                val fieldBg =
                    if (darkMessengerBar) Color(0xFF2A2A2A) else MaterialTheme.colorScheme.surfaceVariant

                Surface(color = barColor, tonalElevation = 2.dp) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = 6.dp),
                    ) {
                        if (recording) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
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
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(
                                onClick = { /* emoji picker — позже */ },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Mood,
                                    contentDescription = null,
                                    tint = onBar,
                                )
                            }
                            OutlinedTextField(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .heightIn(min = 42.dp, max = 120.dp),
                                value = draft,
                                onValueChange = onDraftChange,
                                placeholder = {
                                    Text(
                                        stringResource(R.string.general_chat_message_hint),
                                        color = onBar.copy(alpha = 0.55f),
                                    )
                                },
                                singleLine = false,
                                maxLines = 4,
                                shape = RoundedCornerShape(22.dp),
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        disabledBorderColor = Color.Transparent,
                                        errorBorderColor = Color.Transparent,
                                        focusedContainerColor = fieldBg,
                                        unfocusedContainerColor = fieldBg,
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                        focusedTextColor = onBar,
                                        unfocusedTextColor = onBar,
                                    ),
                            )
                            IconButton(onClick = onAttachClick, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    Icons.Outlined.AttachFile,
                                    contentDescription = null,
                                    tint = onBar,
                                )
                            }
                            IconButton(onClick = onCameraClick, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    Icons.Outlined.PhotoCamera,
                                    contentDescription = stringResource(R.string.chat_camera_cd),
                                    tint = onBar.copy(alpha = 0.85f),
                                )
                            }
                            when {
                                recording ->
                                    IconButton(onClick = onMicClick, modifier = Modifier.size(44.dp)) {
                                        Icon(
                                            Icons.Filled.Stop,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }

                                draft.isNotBlank() ->
                                    IconButton(
                                        onClick = onSendText,
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }

                                else ->
                                    IconButton(onClick = onMicClick, modifier = Modifier.size(44.dp)) {
                                        Icon(
                                            Icons.Filled.Mic,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                            }
                        }
                    }
                }
            },
        ) { padding ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(
                    timeline,
                    key = { index, entry ->
                        when (entry) {
                            is TimelineEntry.DayChip -> "day_${entry.text}_$index"
                            is TimelineEntry.MessageRow -> "msg_${entry.msg.id}"
                        }
                    },
                ) { _, entry ->
                    when (entry) {
                        is TimelineEntry.DayChip ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        text = entry.text,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }

                        is TimelineEntry.MessageRow ->
                            MessengerMessageBubble(
                                msg = entry.msg,
                                timeText = entry.timeText,
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
}

@Composable
private fun MessengerMessageBubble(
    msg: GeneralChatMessageEntity,
    timeText: String,
    currentUserId: Long?,
    playingMessageId: Long?,
    onPlayVoice: (Long, String) -> Unit,
    onTranscribeVoice: (Long, String) -> Unit,
    onOpenFile: (String, String) -> Unit,
) {
    val fromMe = msg.senderUserId == currentUserId
    val type =
        runCatching { TeamChatMessageType.valueOf(msg.messageType) }.getOrNull()
            ?: TeamChatMessageType.TEXT

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start,
    ) {
        val shape = RoundedCornerShape(18.dp)
        when (type) {
            TeamChatMessageType.TEXT ->
                Box(
                    modifier =
                        Modifier
                            .widthIn(max = 320.dp)
                            .clip(shape)
                            .then(
                                if (fromMe) {
                                    Modifier.background(
                                        Brush.horizontalGradient(
                                            listOf(OutgoingGradientStart, OutgoingGradientEnd),
                                        ),
                                    )
                                } else {
                                    Modifier.background(IncomingBubbleColor)
                                },
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(
                            msg.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (fromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            modifier = Modifier.align(Alignment.End),
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (fromMe) Color.White.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

            TeamChatMessageType.VOICE -> {
                val path = msg.attachmentLocalPath
                Box(
                    modifier =
                        Modifier
                            .widthIn(max = 320.dp)
                            .clip(shape)
                            .then(
                                if (fromMe) {
                                    Modifier.background(
                                        Brush.horizontalGradient(
                                            listOf(OutgoingGradientStart, OutgoingGradientEnd),
                                        ),
                                    )
                                } else {
                                    Modifier.background(IncomingBubbleColor)
                                },
                            )
                            .padding(12.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (path != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier =
                                        Modifier
                                            .size(46.dp)
                                            .clickable {
                                                onPlayVoice(msg.id, path)
                                            },
                                    shadowElevation = 2.dp,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector =
                                                if (playingMessageId == msg.id) Icons.Filled.Stop
                                                else Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = OutgoingGradientEnd,
                                        )
                                    }
                                }
                                VoiceWaveformBars(
                                modifier = Modifier.weight(1f),
                                seed = msg.id,
                                outgoing = fromMe,
                            )
                                Surface(
                                    onClick = {
                                        onTranscribeVoice(msg.id, path)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.22f),
                                    shadowElevation = 0.dp,
                                ) {
                                    Text(
                                        "→T",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color =
                                            if (fromMe) Color.White
                                            else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            } else {
                                Text(
                                    stringResource(R.string.chat_attachment_missing),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Text(
                            formatDuration(msg.voiceDurationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (fromMe) Color.White.copy(alpha = 0.9f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (msg.transcript.isNotBlank()) {
                            Text(
                                modifier = Modifier.padding(top = 8.dp),
                                text = msg.transcript,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (fromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            modifier = Modifier.align(Alignment.End),
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (fromMe) Color.White.copy(alpha = 0.75f)
                                else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            TeamChatMessageType.FILE -> {
                val path = msg.attachmentLocalPath
                Box(
                    modifier =
                        Modifier
                            .widthIn(max = 320.dp)
                            .clip(shape)
                            .then(
                                if (fromMe) {
                                    Modifier.background(
                                        Brush.horizontalGradient(
                                            listOf(OutgoingGradientStart, OutgoingGradientEnd),
                                        ),
                                    )
                                } else {
                                    Modifier.background(IncomingBubbleColor)
                                },
                            )
                            .padding(14.dp),
                ) {
                    Column {
                        if (path != null) {
                            val mime = msg.attachmentMime ?: "*/*"
                            if (isImageAttachmentMime(mime)) {
                                Column(
                                    modifier = Modifier.clickable { onOpenFile(path, mime) },
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp)),
                                    ) {
                                        ChatImageThumbnailContent(path = path)
                                    }
                                    val fileLabel = msg.attachmentDisplayName ?: path.substringAfterLast('/')
                                    if (fileLabel.isNotBlank()) {
                                        Text(
                                            text = fileLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color =
                                                if (fromMe) Color.White.copy(alpha = 0.85f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 6.dp),
                                            maxLines = 1,
                                        )
                                    }
                                    if (msg.body.isNotBlank()) {
                                        Text(
                                            text = msg.body,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (fromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                    }
                                }
                            } else {
                                val name = msg.attachmentDisplayName ?: path.substringAfterLast('/')
                                Text(
                                    modifier = Modifier.clickable { onOpenFile(path, mime) },
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (fromMe) Color.White else MaterialTheme.colorScheme.primary,
                                )
                                if (msg.body.isNotBlank()) {
                                    Text(
                                        msg.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (fromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        } else {
                            Text(
                                stringResource(R.string.chat_attachment_missing),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            modifier = Modifier.align(Alignment.End),
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (fromMe) Color.White.copy(alpha = 0.75f)
                                else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveformBars(
    modifier: Modifier = Modifier,
    seed: Long,
    outgoing: Boolean,
) {
    val barColor =
        if (outgoing) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val rng = Random(seed)
    Row(
        modifier =
            modifier
                .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(26) {
            val h = (8 + rng.nextInt(22)).dp
            Box(
                modifier =
                    Modifier
                        .size(width = 3.dp, height = h)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor),
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val s = TimeUnit.MILLISECONDS.toSeconds(ms).coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return String.format(Locale.US, "%d:%02d", m, r)
}
