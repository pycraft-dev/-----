package com.enterprise.manufacturing.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.enterprise.manufacturing.sync.work.EnterpriseSyncNames
import com.enterprise.manufacturing.sync.work.EnterpriseSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnterpriseSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun enqueueImmediateSync() {
        val request = OneTimeWorkRequestBuilder<EnterpriseSyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            EnterpriseSyncNames.IMMEDIATE,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** Минимальный интервал WorkManager — 15 минут. */
    fun schedulePeriodicSync() {
        val periodic = PeriodicWorkRequestBuilder<EnterpriseSyncWorker>(
            15,
            TimeUnit.MINUTES,
        )
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EnterpriseSyncNames.PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic,
        )
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}
