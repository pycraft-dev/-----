package com.enterprise.manufacturing.defect.data

import com.enterprise.manufacturing.core.db.entity.DefectEntity
import com.enterprise.manufacturing.core.db.entity.DefectMessageEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

interface DefectRepository {
    fun observeDefects(): Flow<List<DefectEntity>>

    fun observeMessages(defectId: String): Flow<List<DefectMessageEntity>>

    /**
     * Сохраняет заявку: копирует/сжимает [photoFile] в каталог приложения, создаёт статус-сообщение в чате.
     * @return defectId
     */
    suspend fun createDefectWithPhoto(
        photoFile: File,
        notes: String?,
        authorUserId: Long,
        deviceId: String,
    ): Result<String>

    suspend fun sendTextMessage(defectId: String, senderUserId: Long, text: String)
}
