package com.enterprise.manufacturing.timesheet.data

import android.content.Context
import android.net.Uri
import com.enterprise.manufacturing.core.db.dao.TimeCategoryDao
import com.enterprise.manufacturing.core.db.dao.TimeEntryDao
import com.enterprise.manufacturing.core.db.entity.TimeCategoryEntity
import com.enterprise.manufacturing.core.db.entity.TimeEntryEntity
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

interface TimesheetRepository {
    fun observeCategories(): Flow<List<TimeCategoryEntity>>

    suspend fun ensureDefaultCategories()

    fun observeActiveEntry(userId: Long): Flow<TimeEntryEntity?>

    fun observeCompletedDisplay(userId: Long): Flow<List<TimesheetHistoryRow>>

    suspend fun startTimer(
        userId: Long,
        categoryId: Long?,
        taskTitle: String,
        note: String?,
    ): Result<Long>

    suspend fun stopTimer(entryId: Long, userId: Long): Boolean

    suspend fun buildCsvUtf8(userId: Long): ByteArray

    suspend fun writeCsvToUri(userId: Long, uri: Uri): Boolean
}

@Singleton
class TimesheetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeCategoryDao: TimeCategoryDao,
    private val timeEntryDao: TimeEntryDao,
    private val dispatchers: DispatchersProvider,
) : TimesheetRepository {

    override fun observeCategories(): Flow<List<TimeCategoryEntity>> =
        timeCategoryDao.observeAll().onStart { ensureDefaultCategories() }

    override suspend fun ensureDefaultCategories() {
        withContext(dispatchers.io) {
            if (timeCategoryDao.count() > 0) return@withContext
            timeCategoryDao.insertAll(
                listOf(
                    TimeCategoryEntity(name = "Производство", sortOrder = 0),
                    TimeCategoryEntity(name = "Простой / ожидание", sortOrder = 1),
                    TimeCategoryEntity(name = "Обслуживание оборудования", sortOrder = 2),
                    TimeCategoryEntity(name = "Прочее", sortOrder = 3),
                ),
            )
        }
    }

    override fun observeActiveEntry(userId: Long): Flow<TimeEntryEntity?> =
        timeEntryDao.observeActiveForUser(userId).onStart { ensureDefaultCategories() }

    override fun observeCompletedDisplay(userId: Long): Flow<List<TimesheetHistoryRow>> =
        combine(
            timeCategoryDao.observeAll(),
            timeEntryDao.observeCompletedForUser(userId),
        ) { cats, entries ->
            val map = cats.associateBy { it.id }
            entries.map { e ->
                val end = e.endEpochMs ?: e.startEpochMs
                TimesheetHistoryRow(
                    id = e.id,
                    taskTitle = e.taskTitle,
                    note = e.note,
                    categoryName = e.categoryId?.let { cid -> map[cid]?.name },
                    startEpochMs = e.startEpochMs,
                    endEpochMs = end,
                )
            }
        }.onStart { ensureDefaultCategories() }

    override suspend fun startTimer(
        userId: Long,
        categoryId: Long?,
        taskTitle: String,
        note: String?,
    ): Result<Long> = withContext(dispatchers.io) {
        runCatching {
            ensureDefaultCategories()
            val title = taskTitle.trim()
            if (title.isEmpty()) throw IllegalArgumentException("empty_title")
            if (timeEntryDao.getActiveForUser(userId) != null) {
                throw IllegalStateException("timer_already_running")
            }
            val row = TimeEntryEntity(
                userId = userId,
                categoryId = categoryId,
                taskTitle = title,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                startEpochMs = System.currentTimeMillis(),
                endEpochMs = null,
            )
            timeEntryDao.insert(row)
        }
    }

    override suspend fun stopTimer(entryId: Long, userId: Long): Boolean =
        withContext(dispatchers.io) {
            val now = System.currentTimeMillis()
            timeEntryDao.completeEntry(entryId, userId, now) > 0
        }

    override suspend fun buildCsvUtf8(userId: Long): ByteArray = withContext(dispatchers.io) {
        ensureDefaultCategories()
        val entries = timeEntryDao.loadCompletedForUserCsv(userId)
        val cats = timeCategoryDao.loadAll().associateBy { it.id }
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("start_iso;end_iso;duration_sec;category;task;note")
        val formatter = DateTimeFormatter.ISO_INSTANT
        for (e in entries) {
            val end = e.endEpochMs ?: continue
            val durSec = max(0L, (end - e.startEpochMs) / 1000)
            val cat = e.categoryId?.let { cats[it]?.name }.orEmpty()
            sb.appendLine(
                listOf(
                    formatter.format(Instant.ofEpochMilli(e.startEpochMs)),
                    formatter.format(Instant.ofEpochMilli(end)),
                    durSec.toString(),
                    escapeCsvSemicolon(cat),
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
