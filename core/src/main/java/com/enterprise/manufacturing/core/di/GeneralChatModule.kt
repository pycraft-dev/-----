package com.enterprise.manufacturing.core.di

import com.enterprise.manufacturing.core.chat.data.GeneralChatRepository
import com.enterprise.manufacturing.core.chat.data.GeneralChatRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GeneralChatModule {
    @Binds
    @Singleton
    abstract fun bindGeneralChatRepository(impl: GeneralChatRepositoryImpl): GeneralChatRepository
}
