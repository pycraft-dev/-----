package com.enterprise.manufacturing.update.data

import com.enterprise.manufacturing.core.utils.DispatchersProvider
import com.enterprise.manufacturing.update.network.AppUpdateApi
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteUpdateManifest(
    val latestVersionCode: Int,
    val apkUrl: String,
    val releaseNotes: String,
)

interface UpdateRepository {
    suspend fun fetchManifest(): Result<RemoteUpdateManifest>

    suspend fun downloadApk(url: String, targetFile: File): Result<Unit>
}

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val api: AppUpdateApi,
    private val okHttpClient: OkHttpClient,
    private val dispatchers: DispatchersProvider,
) : UpdateRepository {

    override suspend fun fetchManifest(): Result<RemoteUpdateManifest> =
        withContext(dispatchers.io) {
            runCatching {
                val dto = api.fetchManifest()
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
}
