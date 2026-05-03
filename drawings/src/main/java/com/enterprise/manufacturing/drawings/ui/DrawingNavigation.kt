package com.enterprise.manufacturing.drawings.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.enterprise.manufacturing.core.db.entity.DrawingMessageEntity
import com.enterprise.manufacturing.core.db.entity.DrawingRevisionEntity
import com.enterprise.manufacturing.core.model.DrawingStatus
import com.enterprise.manufacturing.core.model.UserRole
import com.enterprise.manufacturing.core.navigation.AppRoute
import com.enterprise.manufacturing.core.navigation.DrawingNavArgs
import com.enterprise.manufacturing.drawings.R
import com.enterprise.manufacturing.drawings.media.PdfFirstPageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DrawingListRoute(navController: NavHostController) {
    val viewModel: DrawingListViewModel = hiltViewModel()
    val revisions by viewModel.revisions.collectAsStateWithLifecycle()
    val canUpload by viewModel.canUpload.collectAsStateWithLifecycle()

    DrawingListScreen(
        revisions = revisions,
        canUpload = canUpload,
        onBack = { navController.popBackStack() },
        onOpen = { id ->
            navController.navigate(DrawingNavArgs.detailRoute(id))
        },
        onFab = {
            navController.navigate(DrawingNavArgs.uploadRoute(null))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawingListScreen(
    revisions: List<DrawingRevisionEntity>,
    canUpload: Boolean,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onFab: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawing_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            if (canUpload) {
                FloatingActionButton(onClick = onFab) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        if (revisions.isEmpty()) {
            Text(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp),
                text = stringResource(R.string.drawing_empty_list),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(revisions, key = { it.id }) { rev ->
                    DrawingRevisionCard(revision = rev, onClick = { onOpen(rev.id) })
                }
            }
        }
    }
}

@Composable
private fun DrawingRevisionCard(
    revision: DrawingRevisionEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = revision.seriesTitle,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    R.string.drawing_row_meta,
                    formatInstant(revision.createdAtEpochMs),
                    revision.version,
                    drawingStatusLabel(statusName = revision.status),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (revision.changeDescription.isNotBlank()) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = revision.changeDescription,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onClick) {
                Text(stringResource(R.string.drawing_open_detail))
            }
        }
    }
}

@Composable
fun DrawingUploadRoute(navController: NavHostController) {
    val viewModel: DrawingUploadViewModel = hiltViewModel()
    val existingSeriesId = viewModel.existingSeriesId

    var pickedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var seriesTitle by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(DrawingStatus.DRAFT) }
    var existingSeriesTitle by remember { mutableStateOf("") }
    var uploadErrorRes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(existingSeriesId) {
        existingSeriesTitle =
            if (existingSeriesId != null) viewModel.seriesTitleForExisting(existingSeriesId) else ""
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is DrawingUploadEvent.Saved ->
                    navController.navigate(DrawingNavArgs.detailRoute(ev.revisionId)) {
                        popUpTo(AppRoute.DrawingList.route) { inclusive = false }
                    }

                DrawingUploadEvent.NoFile ->
                    uploadErrorRes = R.string.drawing_err_no_file

                DrawingUploadEvent.NoSession ->
                    uploadErrorRes = R.string.drawing_err_no_session

                DrawingUploadEvent.BadFile ->
                    uploadErrorRes = R.string.drawing_err_bad_file

                DrawingUploadEvent.EmptyTitle ->
                    uploadErrorRes = R.string.drawing_err_empty_title

                DrawingUploadEvent.Error ->
                    uploadErrorRes = R.string.drawing_err_generic
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pickedUri = uri
        uploadErrorRes = null
    }

    DrawingUploadScreen(
        existingSeriesTitle = existingSeriesTitle,
        pickedUri = pickedUri,
        seriesTitle = seriesTitle,
        onSeriesTitleChange = { seriesTitle = it },
        description = description,
        onDescriptionChange = { description = it },
        status = status,
        onStatusChange = {
            status = it
            uploadErrorRes = null
        },
        uploadErrorRes = uploadErrorRes,
        onPickFile = {
            pickLauncher.launch(arrayOf("application/pdf", "*/*"))
        },
        onSave = {
            uploadErrorRes = null
            val titleForSubmit =
                if (existingSeriesId != null) existingSeriesTitle else seriesTitle
            viewModel.submit(
                pickedUri = pickedUri,
                seriesTitleInput = titleForSubmit,
                description = description,
                status = status,
            )
        },
        onBack = { navController.popBackStack() },
        showSeriesTitleField = existingSeriesId == null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawingUploadScreen(
    existingSeriesTitle: String,
    pickedUri: android.net.Uri?,
    seriesTitle: String,
    onSeriesTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    status: DrawingStatus,
    onStatusChange: (DrawingStatus) -> Unit,
    uploadErrorRes: Int?,
    onPickFile: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    showSeriesTitleField: Boolean,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawing_upload_title)) },
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
                .padding(16.dp),
        ) {
            if (showSeriesTitleField) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = seriesTitle,
                    onValueChange = onSeriesTitleChange,
                    label = { Text(stringResource(R.string.drawing_series_title_hint)) },
                    singleLine = true,
                )
            } else if (existingSeriesTitle.isNotBlank()) {
                Text(
                    text = stringResource(R.string.drawing_series_existing, existingSeriesTitle),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.drawing_pick_file),
                )
            }

            pickedUri?.let { uri ->
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.drawing_picked_uri, uri.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DrawingStatus.entries.forEach { candidate ->
                    FilterChip(
                        selected = status == candidate,
                        onClick = { onStatusChange(candidate) },
                        label = { Text(drawingStatusLabel(candidate)) },
                    )
                }
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.drawing_change_description)) },
                minLines = 3,
            )

            uploadErrorRes?.let { res ->
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = stringResource(res),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                onClick = onSave,
            ) {
                Text(stringResource(R.string.drawing_save))
            }
        }
    }
}

@Composable
fun DrawingDetailRoute(navController: NavHostController) {
    val viewModel: DrawingDetailViewModel = hiltViewModel()
    val revision by viewModel.revision.collectAsStateWithLifecycle()
    val seriesRevisions by viewModel.seriesRevisions.collectAsStateWithLifecycle()
    val role by viewModel.role.collectAsStateWithLifecycle()

    val canManage =
        role == UserRole.ADMIN || role == UserRole.CONSTRUCTOR

    DrawingDetailScreen(
        revision = revision,
        seriesRevisions = seriesRevisions,
        canManage = canManage,
        onBack = { navController.popBackStack() },
        onOpenChat = {
            revision?.id?.let { id ->
                navController.navigate(DrawingNavArgs.chatRoute(id))
            }
        },
        onOpenRevision = { id ->
            val fromId = revision?.id
            if (fromId != null && id != fromId) {
                navController.navigate(DrawingNavArgs.detailRoute(id)) {
                    popUpTo(DrawingNavArgs.detailRoute(fromId)) { inclusive = true }
                }
            }
        },
        onNewVersion = { sid ->
            navController.navigate(DrawingNavArgs.uploadRoute(sid))
        },
        onStatusSelected = { viewModel.setStatus(it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawingDetailScreen(
    revision: DrawingRevisionEntity?,
    seriesRevisions: List<DrawingRevisionEntity>,
    canManage: Boolean,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenRevision: (Long) -> Unit,
    onNewVersion: (String) -> Unit,
    onStatusSelected: (DrawingStatus) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawing_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (revision == null) {
            BoxCentered(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(revision.seriesTitle, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(
                        R.string.drawing_detail_meta,
                        revision.version,
                        formatInstant(revision.createdAtEpochMs),
                        revision.authorUserId,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.drawing_detail_change_note),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = revision.changeDescription.ifBlank {
                        stringResource(R.string.drawing_detail_no_description)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.drawing_open_chat))
                }
            }

            item {
                Text(
                    text = stringResource(R.string.drawing_versions_in_series),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    seriesRevisions.forEach { rev ->
                        FilterChip(
                            selected = rev.id == revision.id,
                            onClick = { onOpenRevision(rev.id) },
                            label = { Text("v${rev.version}") },
                        )
                    }
                }
                if (canManage) {
                    Button(
                        modifier = Modifier.padding(top = 12.dp),
                        onClick = { onNewVersion(revision.seriesId) },
                    ) {
                        Text(stringResource(R.string.drawing_new_version))
                    }
                }
            }

            item {
                val path = revision.localFilePath
                val ext = revision.extensionLower
                when (ext) {
                    "pdf" -> DrawingPdfPreview(path = path)
                    else -> Text(
                        text = stringResource(R.string.drawing_preview_unsupported, ext.uppercase()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (canManage) {
                item {
                    Text(
                        text = stringResource(R.string.drawing_status_block_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DrawingStatus.entries.forEach { candidate ->
                            FilterChip(
                                selected = runCatching {
                                    DrawingStatus.valueOf(revision.status)
                                }.getOrNull() == candidate,
                                onClick = { onStatusSelected(candidate) },
                                label = { Text(drawingStatusLabel(candidate)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxCentered(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun DrawingPdfPreview(path: String) {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(path) { mutableStateOf(true) }

    LaunchedEffect(path) {
        loading = true
        bitmap = withContext(Dispatchers.IO) {
            PdfFirstPageRenderer.renderFirstPage(File(path))
        }
        loading = false
    }

    when {
        loading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        bitmap != null -> Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
        )

        else -> Text(stringResource(R.string.drawing_pdf_render_failed))
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())

private fun formatInstant(epochMs: Long): String =
    timeFormatter.format(Instant.ofEpochMilli(epochMs))

@Composable
private fun drawingStatusLabel(statusName: String): String {
    val parsed = runCatching { DrawingStatus.valueOf(statusName) }.getOrNull()
        ?: return statusName
    return drawingStatusLabel(parsed)
}

@Composable
private fun drawingStatusLabel(status: DrawingStatus): String = when (status) {
    DrawingStatus.DRAFT -> stringResource(R.string.drawing_status_draft)
    DrawingStatus.ON_APPROVAL -> stringResource(R.string.drawing_status_on_approval)
    DrawingStatus.APPROVED -> stringResource(R.string.drawing_status_approved)
}

@Composable
fun DrawingChatRoute(navController: NavHostController) {
    val viewModel: DrawingChatViewModel = hiltViewModel()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }

    DrawingChatScreen(
        messages = messages,
        draft = draft,
        onDraftChange = { draft = it },
        onSend = {
            viewModel.send(draft)
            draft = ""
        },
        onBack = { navController.popBackStack() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawingChatScreen(
    messages: List<DrawingMessageEntity>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawing_chat_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = draft,
                    onValueChange = onDraftChange,
                    label = { Text(stringResource(R.string.drawing_message_hint)) },
                    singleLine = false,
                    maxLines = 4,
                )
                Button(onClick = onSend, enabled = draft.isNotBlank()) {
                    Text(stringResource(R.string.drawing_send))
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
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = msg.body,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "#${msg.senderUserId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
