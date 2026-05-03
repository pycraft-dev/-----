package com.enterprise.manufacturing.core.di

import com.enterprise.manufacturing.core.settings.UpdateManifestUrlSettings
import com.enterprise.manufacturing.core.settings.UpdateManifestUrlSettingsImpl
import com.enterprise.manufacturing.core.utils.DefaultDispatchersProvider
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindingsModule {
    @Binds
    @Singleton
    abstract fun bindDispatchersProvider(
        impl: DefaultDispatchersProvider,
    ): DispatchersProvider

    @Binds
    @Singleton
    abstract fun bindUpdateManifestUrlSettings(
        impl: UpdateManifestUrlSettingsImpl,
    ): UpdateManifestUrlSettings
}
