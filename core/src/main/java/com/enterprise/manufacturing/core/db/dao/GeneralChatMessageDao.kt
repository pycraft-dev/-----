package com.enterprise.manufacturing.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enterprise.manufacturing.core.db.entity.GeneralChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneralChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: GeneralChatMessageEntity): Long

    @Query("SELECT * FROM general_chat_messages ORDER BY createdAtEpochMs ASC")
    fun observeThread(): Flow<List<GeneralChatMessageEntity>>

    @Query(
        """
        SELECT * FROM general_chat_messages
        WHERE (senderUserId = :currentUserId AND recipientUserId = :peerUserId)
           OR (senderUserId = :peerUserId AND recipientUserId = :currentUserId)
        ORDER BY createdAtEpochMs ASC
        """,
    )
    fun observeDirectThread(peerUserId: Long, currentUserId: Long): Flow<List<GeneralChatMessageEntity>>

    @Query("SELECT * FROM general_chat_messages WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: String): List<GeneralChatMessageEntity>

    @Query("UPDATE general_chat_messages SET syncStatus = :status WHERE id = :messageId")
    suspend fun updateSyncStatus(messageId: Long, status: String)

    @Query("SELECT COUNT(*) FROM general_chat_messages WHERE syncStatus = 'PENDING'")
    fun observePendingSyncCount(): Flow<Int>

    @Query("SELECT * FROM general_chat_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GeneralChatMessageEntity?

    @Query("UPDATE general_chat_messages SET transcript = :text WHERE id = :messageId")
    suspend fun updateTranscript(messageId: Long, text: String)
}
