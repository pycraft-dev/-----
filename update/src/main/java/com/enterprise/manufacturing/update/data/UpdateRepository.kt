package com.enterprise.manufacturing.update.data

import java.io.File

data class RemoteUpdateManifest(
    val latestVersionCode: Int,
    val apkUrl: String,
    val releaseNotes: String,
)

interface UpdateRepository {
    suspend fun fetchManifest(): Result<RemoteUpdateManifest>

    suspend fun downloadApk(url: String, targetFile: File): Result<Unit>
}
