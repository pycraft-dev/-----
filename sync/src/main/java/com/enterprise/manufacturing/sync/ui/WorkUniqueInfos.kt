package com.enterprise.manufacturing.sync.ui

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun uniqueWorkInfosFlow(
    context: Context,
    uniqueWorkName: String,
): Flow<List<WorkInfo>> = callbackFlow {
    val wm = WorkManager.getInstance(context.applicationContext)
    val liveData = wm.getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
    val observer = Observer<List<WorkInfo>> { list ->
        trySend(list ?: emptyList())
    }
    liveData.observeForever(observer)
    liveData.value?.let { trySend(it) }
    awaitClose { liveData.removeObserver(observer) }
}
