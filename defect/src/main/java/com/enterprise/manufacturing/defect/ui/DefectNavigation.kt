package com.enterprise.manufacturing.defect.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.enterprise.manufacturing.core.db.entity.DefectEntity
import com.enterprise.manufacturing.core.db.entity.DefectMessageEntity
import com.enterprise.manufacturing.core.model.DefectMessageType
import com.enterprise.manufacturing.core.navigation.AppRoute
import com.enterprise.manufacturing.core.navigation.DefectNavArgs
import com.enterprise.manufacturing.defect.R
import java.io.File

@Composable
fun DefectListRoute(navController: NavHostController) {
    val viewModel: DefectListViewModel = hiltViewModel()
    val defects by viewModel.defects.collectAsStateWithLifecycle()

    DefectListScreen(
        defects = defects,
        onBack = { navController.popBackStack() },
        onCreateNew = { navController.navigate(AppRoute.DefectNew.route) },
        onOpenChat = { id ->
            navController.navigate(DefectNavArgs.chatRoute(id))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefectListScreen(
    defects: List<DefectEntity>,
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onOpenChat: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.defect_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    ) { padding ->
        if (defects.isEmpty()) {
            Text(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp),
                text = stringResource(R.string.defect_empty_list),
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
                items(defects, key = { it.defectId }) { defect ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = defect.notes ?: defect.defectId,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(
                                    R.string.defect_row_meta,
                                    defect.defectId.take(8),
                                    defect.authorUserId,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onOpenChat(defect.defectId) }) {
                                Text(stringResource(R.string.defect_open_chat))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefectCaptureRoute(navController: NavHostController) {
    val viewModel: DefectCaptureViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { ContextCompat.getMainExecutor(context) }

    var notes by remember { mutableStateOf("") }
    var permissionGranted by remember { mutableStateOf(false) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    LaunchedEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DefectCaptureEvent.Created ->
                    navController.navigate(DefectNavArgs.chatRoute(event.defectId)) {
                        popUpTo(AppRoute.DefectNew.route) { inclusive = true }
                    }

                DefectCaptureEvent.Failure,
                DefectCaptureEvent.MissingSession,
                -> Unit
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DefectCaptureScreen(
        notes = notes,
        onNotesChange = { notes = it },
        permissionGranted = permissionGranted,
        controller = controller,
        executor = executor,
        onBack = { navController.popBackStack() },
        onCapture = { file ->
            viewModel.submitCapturedPhoto(file, notes.takeIf { it.isNotBlank() })
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefectCaptureScreen(
    notes: String,
    onNotesChange: (String) -> Unit,
    permissionGranted: Boolean,
    controller: LifecycleCameraController,
    executor: java.util.concurrent.Executor,
    onBack: () -> Unit,
    onCapture: (File) -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.defect_new_title)) },
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
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        this.controller = controller
                    }
                },
                update = { previewView ->
                    previewView.controller = controller
                },
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                value = notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.defect_notes_hint)) },
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                enabled = permissionGranted,
                onClick = {
                    val output = File(
                        context.cacheDir,
                        "defect_capture_${System.currentTimeMillis()}.jpg",
                    )
                    val options = ImageCapture.OutputFileOptions.Builder(output).build()
                    controller.takePicture(
                        options,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onCapture(output)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                /* ошибку можно пробросить в канал ViewModel при необходимости */
                            }
                        },
                    )
                },
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.defect_save),
                )
            }

            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(R.string.defect_sync_stub_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DefectChatRoute(navController: NavHostController) {
    val viewModel: DefectChatViewModel = hiltViewModel()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }

    DefectChatScreen(
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
private fun DefectChatScreen(
    messages: List<DefectMessageEntity>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.defect_chat_title)) },
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
                    label = { Text(stringResource(R.string.defect_message_hint)) },
                    singleLine = false,
                    maxLines = 4,
                )
                Button(onClick = onSend, enabled = draft.isNotBlank()) {
                    Text(stringResource(R.string.defect_send))
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
                val prefix = when (runCatching { DefectMessageType.valueOf(msg.messageType) }.getOrNull()) {
                    DefectMessageType.STATUS -> "[STATUS] "
                    DefectMessageType.MEDIA -> "[MEDIA] "
                    else -> ""
                }
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = prefix + msg.body,
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
