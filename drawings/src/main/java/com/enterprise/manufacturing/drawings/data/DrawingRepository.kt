package com.enterprise.manufacturing.drawings.data



import android.net.Uri

import com.enterprise.manufacturing.core.db.entity.DrawingRevisionEntity
import com.enterprise.manufacturing.core.db.entity.DrawingMessageEntity

import com.enterprise.manufacturing.core.model.DrawingStatus

import kotlinx.coroutines.flow.Flow



interface DrawingRepository {

    fun observeAllRevisions(): Flow<List<DrawingRevisionEntity>>



    fun observeRevision(id: Long): Flow<DrawingRevisionEntity?>



    fun observeSeriesRevisions(seriesId: String): Flow<List<DrawingRevisionEntity>>



    suspend fun getLatestInSeries(seriesId: String): DrawingRevisionEntity?



    suspend fun addRevision(

        sourceUri: Uri,

        existingSeriesId: String?,

        seriesTitle: String,

        changeDescription: String,

        status: DrawingStatus,

        authorUserId: Long,

    ): Result<Long>



    suspend fun updateStatus(revisionId: Long, status: DrawingStatus): Boolean

    fun observeDrawingMessages(revisionId: Long): Flow<List<DrawingMessageEntity>>

    suspend fun sendDrawingMessage(revisionId: Long, senderUserId: Long, text: String)
}

