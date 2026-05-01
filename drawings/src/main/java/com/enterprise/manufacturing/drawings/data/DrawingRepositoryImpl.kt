package com.enterprise.manufacturing.drawings.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.enterprise.manufacturing.core.db.dao.DrawingDao
import com.enterprise.manufacturing.core.db.dao.DrawingMessageDao
import com.enterprise.manufacturing.core.db.entity.DrawingMessageEntity
import com.enterprise.manufacturing.core.db.entity.DrawingRevisionEntity
import com.enterprise.manufacturing.core.model.DrawingStatus
import com.enterprise.manufacturing.core.model.SyncStatus
import com.enterprise.manufacturing.core.model.TeamChatMessageType
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val drawingDao: DrawingDao,
    private val drawingMessageDao: DrawingMessageDao,
    private val dispatchers: DispatchersProvider,
) : DrawingRepository {

    override fun observeAllRevisions(): Flow<List<DrawingRevisionEntity>> =
        drawingDao.observeAll()

    override fun observeRevision(id: Long): Flow<DrawingRevisionEntity?> =
        drawingDao.observeById(id)

    override fun observeSeriesRevisions(seriesId: String): Flow<List<DrawingRevisionEntity>> =
        drawingDao.observeSeries(seriesId)

    override suspend fun getLatestInSeries(seriesId: String): DrawingRevisionEntity? =
        drawingDao.getLatestInSeries(seriesId)

    override suspend fun addRevision(
        sourceUri: Uri,
        existingSeriesId: String?,
        seriesTitle: String,
        changeDescription: String,
        status: DrawingStatus,
        authorUserId: Long,
    ): Result<Long> = withContext(dispatchers.io) {
        runCatching {
            val ext = resolveExtension(sourceUri)
                ?: throw IllegalArgumentException("unsupported_file")

            if (ext !in ALLOWED_EXT) {
                throw IllegalArgumentException("unsupported_file")
            }

            val seriesId = existingSeriesId ?: UUID.randomUUID().toString()
            val version = if (existingSeriesId == null) {
                1
            } else {
                drawingDao.maxVersion(seriesId) + 1
            }

            val resolvedTitle = if (existingSeriesId != null) {
                drawingDao.getLatestInSeries(seriesId)?.seriesTitle
                    ?: seriesTitle.trim().ifBlank { throw IllegalArgumentException("empty_series_title") }
            } else {
                seriesTitle.trim().ifBlank { throw IllegalArgumentException("empty_series_title") }
            }

            val dir = File(context.filesDir, "drawings/$seriesId").apply { mkdirs() }
            val dest = File(dir, "v$version.$ext")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("open_stream_failed")

            val entity = DrawingRevisionEntity(
                seriesId = seriesId,
                version = version,
                seriesTitle = resolvedTitle,
                localFilePath = dest.absolutePath,
                extensionLower = ext,
                changeDescription = changeDescription.trim(),
                status = status.name,
                authorUserId = authorUserId,
                createdAtEpochMs = System.currentTimeMillis(),
            )
            drawingDao.insert(entity)
        }
    }

    override suspend fun updateStatus(revisionId: Long, status: DrawingStatus): Boolean =
        withContext(dispatchers.io) {
            drawingDao.updateStatus(revisionId, status.name) > 0
        }

    override fun observeDrawingMessages(revisionId: Long): Flow<List<DrawingMessageEntity>> =
        drawingMessageDao.observeThread(revisionId)

    override suspend fun sendDrawingMessage(revisionId: Long, senderUserId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        withContext(dispatchers.io) {
            drawingMessageDao.insert(
                DrawingMessageEntity(
                    revisionId = revisionId,
                    senderUserId = senderUserId,
                    messageType = TeamChatMessageType.TEXT.name,
                    body = trimmed,
                    createdAtEpochMs = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING.name,
                ),
            )
        }
    }

    private fun resolveExtension(uri: Uri): String? {
        val mime = context.contentResolver.getType(uri)?.lowercase(Locale.US)
        when {
            mime?.contains("pdf") == true -> return "pdf"
            mime?.contains("dwg") == true -> return "dwg"
            mime?.contains("cad") == true -> return "dwg"
            else -> Unit
        }

        displayName(uri)?.substringAfterLast('.', "")?.lowercase(Locale.US)?.takeIf { it.isNotBlank() }
            ?.let { candidate ->
                when (candidate) {
                    "pdf" -> return "pdf"
                    "dwg" -> return "dwg"
                    else -> Unit
                }
            }

        uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase(Locale.US)?.takeIf { it.isNotBlank() }
            ?.let { candidate ->
                when (candidate) {
                    "pdf" -> return "pdf"
                    "dwg" -> return "dwg"
                    else -> Unit
                }
            }

        return null
    }

    private fun displayName(uri: Uri): String? {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor.use { c ->
            if (c == null || !c.moveToFirst()) return null
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx < 0) return null
            return c.getString(idx)
        }
    }

    private companion object {
        val ALLOWED_EXT = setOf("pdf", "dwg")
    }
}
