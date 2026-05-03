package com.enterprise.manufacturing.sync.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.enterprise.manufacturing.core.sync.UserDirectorySync;
import com.enterprise.manufacturing.defect.sync.DefectSyncStub;
import com.enterprise.manufacturing.update.background.UpdateBackgroundCheck;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

/**
 * Единая точка фоновой синхронизации: брак, чаты, каталог пользователей (Supabase), проверка обновления APK.
 * Дальше: чертежи, табели, конфликты last-write-wins и очереди Retrofit.
 * <p>
 * Worker написан на Java из-за ошибки Kotlin 2.2 × kapt × androidx.hilt.worker: «Unable to read Kotlin metadata»
 * на заглушках для любого Kotlin-класса с {@code @HiltWorker}.
 */
@HiltWorker
public final class EnterpriseSyncWorker extends Worker {

    @NonNull
    private final DefectSyncStub defectSyncStub;

    @NonNull
    private final ChatMessagesSyncStub chatMessagesSyncStub;

    @NonNull
    private final UserDirectorySync userDirectorySync;

    @NonNull
    private final UpdateBackgroundCheck updateBackgroundCheck;

    @AssistedInject
    EnterpriseSyncWorker(
            @NonNull @Assisted Context context,
            @NonNull @Assisted WorkerParameters params,
            @NonNull DefectSyncStub defectSyncStub,
            @NonNull ChatMessagesSyncStub chatMessagesSyncStub,
            @NonNull UserDirectorySync userDirectorySync,
            @NonNull UpdateBackgroundCheck updateBackgroundCheck
    ) {
        super(context, params);
        this.defectSyncStub = defectSyncStub;
        this.chatMessagesSyncStub = chatMessagesSyncStub;
        this.userDirectorySync = userDirectorySync;
        this.updateBackgroundCheck = updateBackgroundCheck;
    }

    @NonNull
    @Override
    public Result doWork() {
        CoroutineContext io = Dispatchers.getIO();
        try {
            BuildersKt.runBlocking(
                    io,
                    (scope, continuation) ->
                            EnterpriseSyncLogic.INSTANCE.flushAll(
                                    defectSyncStub,
                                    chatMessagesSyncStub,
                                    userDirectorySync,
                                    updateBackgroundCheck,
                                    continuation
                            )
            );
            return Result.success();
        } catch (@NonNull Throwable ignored) {
            return Result.retry();
        }
    }
}
