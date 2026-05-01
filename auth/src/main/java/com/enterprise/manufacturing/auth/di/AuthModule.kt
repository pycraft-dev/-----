package com.enterprise.manufacturing.auth.di

import com.enterprise.manufacturing.auth.security.PasswordHasher
import com.enterprise.manufacturing.auth.security.Pbkdf2PasswordHasher
import com.enterprise.manufacturing.auth.session.AuthSessionRepositoryImpl
import com.enterprise.manufacturing.core.session.AuthSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthSessionRepository(
        impl: AuthSessionRepositoryImpl,
    ): AuthSessionRepository

    @Binds
    @Singleton
    abstract fun bindPasswordHasher(
        impl: Pbkdf2PasswordHasher,
    ): PasswordHasher
}
