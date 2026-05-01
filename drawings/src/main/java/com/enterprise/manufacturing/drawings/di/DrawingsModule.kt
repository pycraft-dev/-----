package com.enterprise.manufacturing.drawings.di



import com.enterprise.manufacturing.drawings.data.DrawingRepository

import com.enterprise.manufacturing.drawings.data.DrawingRepositoryImpl

import dagger.Binds

import dagger.Module

import dagger.hilt.InstallIn

import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton



@Module

@InstallIn(SingletonComponent::class)

abstract class DrawingsModule {

    @Binds

    @Singleton

    abstract fun bindDrawingRepository(impl: DrawingRepositoryImpl): DrawingRepository

}

