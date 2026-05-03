package com.enterprise.manufacturing.core.di

import com.enterprise.manufacturing.core.data.RolesRepository
import com.enterprise.manufacturing.core.data.RolesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RolesBinderModule {
    @Binds
    @Singleton
    abstract fun bindRolesRepository(impl: RolesRepositoryImpl): RolesRepository
}
