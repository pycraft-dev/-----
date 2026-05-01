package com.enterprise.manufacturing

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.enterprise.manufacturing.sync.EnterpriseSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ManufacturingApp : Application() {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var enterpriseSyncScheduler: EnterpriseSyncScheduler

    override fun onCreate() {
        super.onCreate()
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(
                this,
                Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .build(),
            )
        }
        enterpriseSyncScheduler.enqueueImmediateSync()
        enterpriseSyncScheduler.schedulePeriodicSync()
    }
}
