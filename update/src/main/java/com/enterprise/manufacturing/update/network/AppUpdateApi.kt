package com.enterprise.manufacturing.update.network

import com.enterprise.manufacturing.update.data.AppUpdateDto
import retrofit2.http.GET

interface AppUpdateApi {
    /** Заглушка: ответ подставляется [StubUpdateInterceptor] без сети. */
    @GET("update.json")
    suspend fun fetchManifest(): AppUpdateDto
}
