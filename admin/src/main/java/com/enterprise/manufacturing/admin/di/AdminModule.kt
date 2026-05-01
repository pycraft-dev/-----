package com.enterprise.manufacturing.admin.di

import com.enterprise.manufacturing.admin.data.AdminUsersRepository
import com.enterprise.manufacturing.admin.data.AdminUsersRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdminModule {
    @Binds
    @Singleton
    abstract fun bindAdminUsersRepository(
        impl: AdminUsersRepositoryImpl,
    ): AdminUsersRepository
}
