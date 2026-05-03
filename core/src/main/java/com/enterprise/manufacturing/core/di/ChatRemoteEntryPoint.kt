package com.enterprise.manufacturing.core.di

import com.enterprise.manufacturing.core.chat.data.GeneralChatRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatRemoteEntryPoint {
    fun generalChatRepository(): GeneralChatRepository
}
