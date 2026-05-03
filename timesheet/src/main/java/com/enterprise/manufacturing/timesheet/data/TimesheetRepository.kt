package com.enterprise.manufacturing.timesheet.data

import android.content.Context
import android.net.Uri
import com.enterprise.manufacturing.core.db.dao.TimeEntryDao
import com.enterprise.manufacturing.core.db.entity.TimeEntryEntity
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

interface TimesheetRepository {
    fun observeCompletedDisplay(userId: Long): Flow<List<TimesheetHistoryRow>>

    suspend fun addManualEntry(userId: Long, taskTitle: String, hours: Int, minutes: Int): Result<Long>

    suspend fun buildCsvUtf8(userId: Long): ByteArray

    suspend fun writeCsvToUri(userId: Long, uri: Uri): Boolean
}

@Singleton
class TimesheetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeEntryDao: TimeEntryDao,
    private val dispatchers: DispatchersProvider,
) : TimesheetRepository {

    override fun observeCompletedDisplay(userId: Long): Flow<List<TimesheetHistoryRow>> =
        timeEntryDao.observeCompletedForUser(userId).map { entries ->
            entries.mapNotNull { e ->
                val end = e.endEpochMs ?: return@mapNotNull null
                TimesheetHistoryRow(
                    id = e.id,
                    taskTitle = e.taskTitle,
                    note = e.note,
                    startEpochMs = e.startEpochMs,
                    endEpochMs = end,
                )
            }
        }

    override suspend fun addManualEntry(
        userId: Long,
        taskTitle: String,
        hours: Int,
        minutes: Int,
    ): Result<Long> =
        withContext(dispatchers.io) {
            runCatching {
                val title = taskTitle.trim()
                if (title.isEmpty()) throw IllegalArgumentException("empty_title")
                if (hours < 0 || minutes !in 0..59) throw IllegalArgumentException("invalid_time")
                val totalMinutes = hours * 60 + minutes
                if (totalMinutes <= 0) throw IllegalArgumentException("zero_duration")

                val durationMs = totalMinutes * 60_000L
                val endMs = System.currentTimeMillis()
                val startMs = endMs - durationMs

                val row =
                    TimeEntryEntity(
                        userId = userId,
                        categoryId = null,
                        taskTitle = title,
                        note = null,
                        startEpochMs = startMs,
                        endEpochMs = endMs,
                    )
                timeEntryDao.insert(row)
            }
        }

    override suspend fun buildCsvUtf8(userId: Long): ByteArray =
        withContext(dispatchers.io) {
            val entries = timeEntryDao.loadCompletedForUserCsv(userId)
            val sb = StringBuilder()
            sb.append('\uFEFF')
            sb.appendLine("start_iso;end_iso;duration_sec;task;note")
            val formatter = DateTimeFormatter.ISO_INSTANT
            for (e in entries) {
                val end = e.endEpochMs ?: continue
                val durSec = max(0L, (end - e.startEpochMs) / 1000)
                sb.appendLine(
                    listOf(
                        formatter.format(Instant.ofEpochMilli(e.startEpochMs)),
                        formatter.format(Instant.ofEpochMilli(end)),
                        durSec.toString(),
                        escapeCsvSemicolon(e.taskTitle),
                        escapeCsvSemicolon(e.note.orEmpty()),
                    ).joinToString(";"),
                )
            }
            sb.toString().encodeToByteArray()
        }

    override suspend fun writeCsvToUri(userId: Long, uri: Uri): Boolean =
        withContext(dispatchers.io) {
            runCatching {
                val bytes = buildCsvUtf8(userId)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(bytes)
                } ?: return@runCatching false
                true
            }.getOrDefault(false)
        }
}

private fun escapeCsvSemicolon(field: String): String =
    field.replace("\r\n", "\n").replace("\"", "\"\"").let { v ->
        if (v.contains(';') || v.contains('\n') || v.contains('"')) "\"$v\"" else v
    }
