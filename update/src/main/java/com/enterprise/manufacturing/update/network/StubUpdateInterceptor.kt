package com.enterprise.manufacturing.update.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Локальная имитация ответа сервера обновлений (без DNS/HTTP).
 * Для боя замените клиент в [com.enterprise.manufacturing.update.di.UpdateNetworkModule].
 */
class StubUpdateInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        if (path.endsWith("update.json")) {
            val json =
                """
                {"versionCode":2,"apkUrl":"","releaseNotes":"Заглушка Retrofit + OkHttp: задайте реальный endpoint и уберите interceptor. Пока apkUrl пуст — кнопка загрузки недоступна."}
                """.trimIndent()
            val body = json.toResponseBody(JSON_MEDIA)
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(HTTP_OK)
                .message("OK")
                .body(body)
                .build()
        }
        return chain.proceed(chain.request())
    }

    private companion object {
        private const val HTTP_OK = 200
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
