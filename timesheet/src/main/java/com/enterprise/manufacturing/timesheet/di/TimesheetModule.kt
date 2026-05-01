package com.enterprise.manufacturing.timesheet.di

import com.enterprise.manufacturing.timesheet.data.TimesheetRepository
import com.enterprise.manufacturing.timesheet.data.TimesheetRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimesheetModule {
    @Binds
    @Singleton
    abstract fun bindTimesheetRepository(impl: TimesheetRepositoryImpl): TimesheetRepository
}
