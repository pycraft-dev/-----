package com.enterprise.manufacturing.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "drawing_messages",
    foreignKeys = [
        ForeignKey(
            entity = DrawingRevisionEntity::class,
            parentColumns = ["id"],
            childColumns = ["revisionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["revisionId"]),
        Index(value = ["createdAtEpochMs"]),
    ],
)
data class DrawingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val revisionId: Long,
    val senderUserId: Long,
    /** [com.enterprise.manufacturing.core.model.TeamChatMessageType] name */
    val messageType: String,
    val body: String,
    val createdAtEpochMs: Long,
    /** [com.enterprise.manufacturing.core.model.SyncStatus] name */
    val syncStatus: String,
)
