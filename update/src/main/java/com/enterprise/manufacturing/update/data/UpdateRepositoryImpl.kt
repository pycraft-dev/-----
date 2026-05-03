package com.enterprise.manufacturing.update.data

import com.enterprise.manufacturing.core.utils.DispatchersProvider
import com.enterprise.manufacturing.update.BuildConfig
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val json: Json,
    private val okHttpClient: OkHttpClient,
    private val dispatchers: DispatchersProvider,
) : UpdateRepository {

    override suspend fun fetchManifest(): Result<RemoteUpdateManifest> =
        withContext(dispatchers.io) {
            runCatching {
                val url = BuildConfig.UPDATE_MANIFEST_URL.trim()
                val dto =
                    if (url.isEmpty()) {
                        json.decodeFromString<AppUpdateDto>(STUB_MANIFEST_JSON)
                    } else {
                        val req = Request.Builder().url(url).get().build()
                        okHttpClient.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) error("http_${resp.code}")
                            val body = resp.body?.string().orEmpty()
                            if (body.isBlank()) error("empty_body")
                            json.decodeFromString<AppUpdateDto>(body)
                        }
                    }
                RemoteUpdateManifest(
                    latestVersionCode = dto.versionCode,
                    apkUrl = dto.apkUrl.trim(),
                    releaseNotes = dto.releaseNotes,
                )
            }
        }

    override suspend fun downloadApk(url: String, targetFile: File): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val req = Request.Builder().url(url).build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("http_${resp.code}")
                    val body = resp.body ?: error("empty_body")
                    targetFile.parentFile?.mkdirs()
                    body.byteStream().use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Unit
                }
            }
        }

    private companion object {
        /** Локальная заглушка, если `UPDATE_MANIFEST_URL` не задан (режим разработки). */
        private const val STUB_MANIFEST_JSON =
            """{"versionCode":1,"apkUrl":"","releaseNotes":"Задайте UPDATE_MANIFEST_URL в local.properties (HTTPS-ссылка на update.json)."}"""
    }
}
