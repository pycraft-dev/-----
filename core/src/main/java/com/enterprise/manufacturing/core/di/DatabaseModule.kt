package com.enterprise.manufacturing.core.di

import android.content.Context
import androidx.room.Room
import com.enterprise.manufacturing.core.db.AppDatabase
import com.enterprise.manufacturing.core.db.dao.DefectDao
import com.enterprise.manufacturing.core.db.dao.DefectMessageDao
import com.enterprise.manufacturing.core.db.dao.DrawingDao
import com.enterprise.manufacturing.core.db.dao.DrawingMessageDao
import com.enterprise.manufacturing.core.db.dao.GeneralChatMessageDao
import com.enterprise.manufacturing.core.db.dao.TimeCategoryDao
import com.enterprise.manufacturing.core.db.dao.TimeEntryDao
import com.enterprise.manufacturing.core.db.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "enterprise.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideDefectDao(db: AppDatabase): DefectDao = db.defectDao()

    @Provides
    fun provideDefectMessageDao(db: AppDatabase): DefectMessageDao = db.defectMessageDao()

    @Provides
    fun provideDrawingDao(db: AppDatabase): DrawingDao = db.drawingDao()

    @Provides
    fun provideDrawingMessageDao(db: AppDatabase): DrawingMessageDao = db.drawingMessageDao()

    @Provides
    fun provideGeneralChatMessageDao(db: AppDatabase): GeneralChatMessageDao =
        db.generalChatMessageDao()

    @Provides
    fun provideTimeCategoryDao(db: AppDatabase): TimeCategoryDao = db.timeCategoryDao()

    @Provides
    fun provideTimeEntryDao(db: AppDatabase): TimeEntryDao = db.timeEntryDao()
}
