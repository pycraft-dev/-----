package com.enterprise.manufacturing.defect.data

import android.content.Context
import com.enterprise.manufacturing.core.db.dao.DefectDao
import com.enterprise.manufacturing.core.db.dao.DefectMessageDao
import com.enterprise.manufacturing.core.db.entity.DefectEntity
import com.enterprise.manufacturing.core.db.entity.DefectMessageEntity
import com.enterprise.manufacturing.core.model.DefectMessageType
import com.enterprise.manufacturing.core.model.SyncStatus
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import com.enterprise.manufacturing.defect.media.DefectPhotoCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefectRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val defectDao: DefectDao,
    private val defectMessageDao: DefectMessageDao,
    private val photoCompressor: DefectPhotoCompressor,
    private val dispatchers: DispatchersProvider,
) : DefectRepository {

    override fun observeDefects(): Flow<List<DefectEntity>> = defectDao.observeAll()

    override fun observeMessages(defectId: String): Flow<List<DefectMessageEntity>> =
        defectMessageDao.observeThread(defectId)

    override suspend fun createDefectWithPhoto(
        photoFile: File,
        notes: String?,
        authorUserId: Long,
        deviceId: String,
    ): Result<String> = withContext(dispatchers.io) {
        runCatching {
            val defectId = UUID.randomUUID().toString()
            val dir = File(context.filesDir, "defects/$defectId").apply { mkdirs() }
            val dest = File(dir, "photo.jpg")
            runCatching {
                photoCompressor.compressToFile(photoFile, dest)
            }.onFailure {
                photoFile.copyTo(dest, overwrite = true)
            }
            val now = System.currentTimeMillis()
            val pending = SyncStatus.PENDING.name

            defectDao.upsert(
                DefectEntity(
                    defectId = defectId,
                    authorUserId = authorUserId,
                    assignedUserId = null,
                    deviceId = deviceId,
                    createdAtEpochMs = now,
                    photoPath = dest.absolutePath,
                    videoPath = null,
                    notes = notes?.takeIf { it.isNotBlank() },
                    syncStatus = pending,
                ),
            )

            defectMessageDao.insert(
                DefectMessageEntity(
                    defectId = defectId,
                    senderUserId = authorUserId,
                    messageType = DefectMessageType.STATUS.name,
                    body = "Заявка создана",
                    mediaPath = null,
                    createdAtEpochMs = now,
                    syncStatus = pending,
                ),
            )
            defectId
        }
    }

    override suspend fun sendTextMessage(defectId: String, senderUserId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        withContext(dispatchers.io) {
            defectMessageDao.insert(
                DefectMessageEntity(
                    defectId = defectId,
                    senderUserId = senderUserId,
                    messageType = DefectMessageType.TEXT.name,
                    body = trimmed,
                    mediaPath = null,
                    createdAtEpochMs = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING.name,
                ),
            )
        }
    }
}
