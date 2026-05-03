package com.enterprise.manufacturing.core.di

import com.enterprise.manufacturing.core.sync.UserDirectorySync
import com.enterprise.manufacturing.core.sync.UserDirectorySyncImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserSyncModule {
    @Binds
    @Singleton
    abstract fun bindUserDirectorySync(impl: UserDirectorySyncImpl): UserDirectorySync
}
