package com.enterprise.manufacturing.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "general_chat_messages",
    indices = [
        Index(value = ["createdAtEpochMs"]),
    ],
)
data class GeneralChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderUserId: Long,
    /** [com.enterprise.manufacturing.core.model.TeamChatMessageType] name */
    val messageType: String,
    val body: String,
    val createdAtEpochMs: Long,
    /** [com.enterprise.manufacturing.core.model.SyncStatus] name */
    val syncStatus: String,
    val attachmentLocalPath: String? = null,
    val attachmentMime: String? = null,
    val attachmentDisplayName: String? = null,
    val voiceDurationMs: Long = 0L,
    val transcript: String = "",
)
