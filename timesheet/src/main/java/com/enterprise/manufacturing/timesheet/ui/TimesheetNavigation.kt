package com.enterprise.manufacturing.timesheet.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.enterprise.manufacturing.core.navigation.AppRoute
import com.enterprise.manufacturing.timesheet.R
import com.enterprise.manufacturing.timesheet.data.TimesheetHistoryRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val historyDateFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yy HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun TimesheetTimerRoute(navController: NavHostController) {
    val viewModel: TimesheetTimerViewModel = hiltViewModel()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val activeEntry by viewModel.activeEntry.collectAsStateWithLifecycle()

    var taskTitle by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                TimesheetTimerEvent.Started -> {
                    taskTitle = ""
                    note = ""
                    selectedCategoryId = null
                }

                TimesheetTimerEvent.Stopped -> Unit

                TimesheetTimerEvent.NoSession ->
                    snackbarHostState.showSnackbar(context.getString(R.string.timesheet_err_no_session))

                TimesheetTimerEvent.EmptyTitle ->
                    snackbarHostState.showSnackbar(context.getString(R.string.timesheet_err_empty_title))

                TimesheetTimerEvent.AlreadyRunning ->
                    snackbarHostState.showSnackbar(context.getString(R.string.timesheet_err_already_running))

                TimesheetTimerEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(R.string.timesheet_err_generic))

                TimesheetTimerEvent.StopFailed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.timesheet_err_stop))
            }
        }
    }

    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeEntry?.id) {
        if (activeEntry == null) return@LaunchedEffect
        while (true) {
            delay(1_000)
            tick = System.currentTimeMillis()
        }
    }

    TimesheetTimerScreen(
        snackbarHostState = snackbarHostState,
        categories = categories,
        activeEntry = activeEntry,
        tickNow = tick,
        taskTitle = taskTitle,
        onTaskTitleChange = { taskTitle = it },
        note = note,
        onNoteChange = { note = it },
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = { selectedCategoryId = it },
        onStart = {
            viewModel.start(
                taskTitle = taskTitle,
                note = note.takeIf { it.isNotBlank() },
                categoryId = selectedCategoryId,
            )
        },
        onStop = { viewModel.stop() },
        onOpenHistory = { navController.navigate(AppRoute.TimesheetHistory.route) },
        onBack = { navController.popBackStack() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimesheetTimerScreen(
    snackbarHostState: SnackbarHostState,
    categories: List<com.enterprise.manufacturing.core.db.entity.TimeCategoryEntity>,
    activeEntry: com.enterprise.manufacturing.core.db.entity.TimeEntryEntity?,
    tickNow: Long,
    taskTitle: String,
    onTaskTitleChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timesheet_timer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = stringResource(R.string.timesheet_open_history))
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
            if (activeEntry != null) {
                val elapsed = (tickNow - activeEntry.startEpochMs).coerceAtLeast(0L)
                Text(
                    text = stringResource(R.string.timesheet_elapsed, formatDurationMs(elapsed)),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.timesheet_active_task),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = activeEntry.taskTitle,
                    style = MaterialTheme.typography.titleMedium,
                )
                activeEntry.note?.takeIf { it.isNotBlank() }?.let { n ->
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = n,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    onClick = onStop,
                ) {
                    Text(stringResource(R.string.timesheet_stop))
                }
            } else {
                Text(
                    text = stringResource(R.string.timesheet_category_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text(stringResource(R.string.timesheet_cat_none)) },
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { onCategorySelected(cat.id) },
                            label = { Text(cat.name) },
                        )
                    }
                }

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    value = taskTitle,
                    onValueChange = onTaskTitleChange,
                    label = { Text(stringResource(R.string.timesheet_task_hint)) },
                    singleLine = false,
                    minLines = 2,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.timesheet_note_hint)) },
                    minLines = 2,
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    onClick = onStart,
                ) {
                    Text(stringResource(R.string.timesheet_start))
                }
            }
        }
    }
}

@Composable
fun TimesheetHistoryRoute(navController: NavHostController) {
    val viewModel: TimesheetHistoryViewModel = hiltViewModel()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (viewModel.exportTo(uri)) {
                TimesheetExportEvent.NoSession ->
                    snackbarHostState.showSnackbar(context.getString(R.string.timesheet_err_no_session))

                TimesheetExportEvent.Success ->
                    snackbarHostState.showSnackbar(context.getString(R.string.timesheet_export_ok))

                TimesheetExportEvent.Failed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.timesheet_export_fail))
            }
        }
    }

    TimesheetHistoryScreen(
        snackbarHostState = snackbarHostState,
        rows = rows,
        onBack = { navController.popBackStack() },
        onExport = {
            val name = "timesheet_${java.time.LocalDate.now()}.csv"
            createDocLauncher.launch(name)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimesheetHistoryScreen(
    snackbarHostState: SnackbarHostState,
    rows: List<TimesheetHistoryRow>,
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timesheet_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.SaveAlt, contentDescription = stringResource(R.string.timesheet_export_csv))
                    }
                },
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            Text(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp),
                text = stringResource(R.string.timesheet_history_empty),
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
                items(rows, key = { it.id }) { row ->
                    HistoryRowCard(row = row)
                }
            }
        }
    }
}

@Composable
private fun HistoryRowCard(row: TimesheetHistoryRow) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = row.taskTitle,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(
                    R.string.timesheet_row_meta,
                    historyDateFmt.format(Instant.ofEpochMilli(row.startEpochMs)),
                    historyDateFmt.format(Instant.ofEpochMilli(row.endEpochMs)),
                    formatDurationMs(row.endEpochMs - row.startEpochMs),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            row.categoryName?.let { cat ->
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = cat,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            row.note?.takeIf { it.isNotBlank() }?.let { n ->
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = n,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = ms.coerceAtLeast(0L) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
}
