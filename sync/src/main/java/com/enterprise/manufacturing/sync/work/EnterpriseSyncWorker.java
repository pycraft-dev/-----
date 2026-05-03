package com.enterprise.manufacturing.sync.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.enterprise.manufacturing.defect.sync.DefectSyncStub;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

/**
 * Единая точка фоновой синхронизации. Сейчас: брак ({@link DefectSyncStub}), сообщения чатов ({@link ChatMessagesSyncStub}).
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

    @AssistedInject
    EnterpriseSyncWorker(
            @NonNull @Assisted Context context,
            @NonNull @Assisted WorkerParameters params,
            @NonNull DefectSyncStub defectSyncStub,
            @NonNull ChatMessagesSyncStub chatMessagesSyncStub
    ) {
        super(context, params);
        this.defectSyncStub = defectSyncStub;
        this.chatMessagesSyncStub = chatMessagesSyncStub;
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
                                    continuation
                            )
            );
            return Result.success();
        } catch (@NonNull Throwable ignored) {
            return Result.retry();
        }
    }
}
