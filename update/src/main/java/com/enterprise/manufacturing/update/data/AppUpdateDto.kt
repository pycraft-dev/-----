package com.enterprise.manufacturing.update.data

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateDto(
    val versionCode: Int,
    val apkUrl: String = "",
    val releaseNotes: String = "",
)
