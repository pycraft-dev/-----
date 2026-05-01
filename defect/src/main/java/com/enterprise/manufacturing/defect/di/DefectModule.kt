package com.enterprise.manufacturing.defect.di

import com.enterprise.manufacturing.defect.data.DefectRepository
import com.enterprise.manufacturing.defect.data.DefectRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DefectModule {
    @Binds
    @Singleton
    abstract fun bindDefectRepository(impl: DefectRepositoryImpl): DefectRepository
}
