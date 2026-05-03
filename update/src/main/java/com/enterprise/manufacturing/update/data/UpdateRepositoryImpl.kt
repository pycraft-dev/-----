package com.enterprise.manufacturing.update.data

import com.enterprise.manufacturing.core.settings.UpdateManifestUrlSettings
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
    private val manifestUrlSettings: UpdateManifestUrlSettings,
) : UpdateRepository {

    override suspend fun fetchManifest(): Result<RemoteUpdateManifest> =
        withContext(dispatchers.io) {
            runCatching {
                val url =
                    manifestUrlSettings.resolveEffectiveUrl(BuildConfig.UPDATE_MANIFEST_URL)
                val dto =
                    if (url.isEmpty()) {
                        json.decodeFromString<AppUpdateDto>(STUB_MANIFEST_JSON)
                    } else {
                        val direct = resolvePublicDownloadUrl(url)
                        val req =
                            Request.Builder()
                                .url(direct)
                                .get()
                                .header("User-Agent", "ManufacturingEnterprise/1.0 (update-manifest)")
                                .build()
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
                val direct = resolvePublicDownloadUrl(url)
                val req =
                    Request.Builder()
                        .url(direct)
                        .header("User-Agent", "ManufacturingEnterprise/1.0 (update-apk)")
                        .build()
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

    /**
     * Обычный HTTPS — как есть. Публичный Яндекс.Диск — сначала `…/public/resources/download`, затем `href`.
     */
    private fun resolvePublicDownloadUrl(originalUrl: String): String {
        val trimmed = originalUrl.trim()
        if (!YandexDiskPublicResolver.shouldResolve(trimmed)) {
            return trimmed
        }
        val metaUrl = YandexDiskPublicResolver.buildPublicDownloadMetaRequestUrl(trimmed)
        val metaReq =
            Request.Builder()
                .url(metaUrl)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "ManufacturingEnterprise/1.0 (yandex-disk-meta)")
                .build()
        okHttpClient.newCall(metaReq).execute().use { resp ->
            if (!resp.isSuccessful) error("yandex_disk_meta_http_${resp.code}")
            val metaBody = resp.body?.string().orEmpty()
            if (metaBody.isBlank()) error("yandex_disk_meta_empty")
            val link = json.decodeFromString<YandexDiskDownloadLinkDto>(metaBody)
            if (link.href.isBlank()) error("yandex_disk_meta_no_href")
            return link.href.trim()
        }
    }

    private companion object {
        /** Локальная заглушка, если `UPDATE_MANIFEST_URL` не задан (режим разработки). */
        private const val STUB_MANIFEST_JSON =
            """{"versionCode":1,"apkUrl":"","releaseNotes":"Задайте UPDATE_MANIFEST_URL в local.properties или HTTPS-ссылку на update.json в админ-панели."}"""
    }
}
