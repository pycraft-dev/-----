package com.enterprise.manufacturing.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.enterprise.manufacturing.core.db.dao.DefectDao
import com.enterprise.manufacturing.core.db.dao.DefectMessageDao
import com.enterprise.manufacturing.core.db.dao.DrawingDao
import com.enterprise.manufacturing.core.db.dao.DrawingMessageDao
import com.enterprise.manufacturing.core.db.dao.GeneralChatMessageDao
import com.enterprise.manufacturing.core.db.dao.TimeCategoryDao
import com.enterprise.manufacturing.core.db.dao.TimeEntryDao
import com.enterprise.manufacturing.core.db.dao.UserDao
import com.enterprise.manufacturing.core.db.entity.DefectEntity
import com.enterprise.manufacturing.core.db.entity.DefectMessageEntity
import com.enterprise.manufacturing.core.db.entity.DrawingMessageEntity
import com.enterprise.manufacturing.core.db.entity.DrawingRevisionEntity
import com.enterprise.manufacturing.core.db.entity.GeneralChatMessageEntity
import com.enterprise.manufacturing.core.db.entity.TimeCategoryEntity
import com.enterprise.manufacturing.core.db.entity.TimeEntryEntity
import com.enterprise.manufacturing.core.db.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        DefectEntity::class,
        DefectMessageEntity::class,
        DrawingRevisionEntity::class,
        DrawingMessageEntity::class,
        GeneralChatMessageEntity::class,
        TimeCategoryEntity::class,
        TimeEntryEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun defectDao(): DefectDao

    abstract fun defectMessageDao(): DefectMessageDao

    abstract fun drawingDao(): DrawingDao

    abstract fun drawingMessageDao(): DrawingMessageDao

    abstract fun generalChatMessageDao(): GeneralChatMessageDao

    abstract fun timeCategoryDao(): TimeCategoryDao

    abstract fun timeEntryDao(): TimeEntryDao
}
