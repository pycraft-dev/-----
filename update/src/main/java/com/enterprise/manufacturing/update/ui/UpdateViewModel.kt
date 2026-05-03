package com.enterprise.manufacturing.update.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.manufacturing.update.data.UpdateRepository
import com.enterprise.manufacturing.update.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class UpdateUiState(
    val currentVersionCode: Int,
    val currentVersionName: String,
    val phase: UpdatePhase = UpdatePhase.Idle,
)

sealed interface UpdatePhase {
    data object Idle : UpdatePhase
    data object Checking : UpdatePhase
    data object UpToDate : UpdatePhase
    data class Offer(
        val latestVersionCode: Int,
        val apkUrl: String,
        val releaseNotes: String,
    ) : UpdatePhase

    data object Downloading : UpdatePhase
    data class ReadyInstall(val file: File) : UpdatePhase
    data class Error(val messageRes: Int) : UpdatePhase
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    private val installed = readInstalledVersion(context)

    private val _state = MutableStateFlow(
        UpdateUiState(
            currentVersionCode = installed.first,
            currentVersionName = installed.second,
        ),
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    /** Ручная проверка (кнопка на экране обновлений). */
    fun checkForUpdates() {
        refreshForUpdate(autoDownloadApk = false)
    }

    /**
     * Проверка манифеста; при [autoDownloadApk] и наличии ссылки сразу качает APK (рабочий стол → «Обновить»).
     */
    fun refreshForUpdate(autoDownloadApk: Boolean) {
        val phase = _state.value.phase
        if (phase is UpdatePhase.Checking || phase is UpdatePhase.Downloading) return
        viewModelScope.launch {
            _state.update { it.copy(phase = UpdatePhase.Checking) }
            updateRepository.fetchManifest().fold(
                onSuccess = { manifest ->
                    if (manifest.latestVersionCode <= installed.first) {
                        _state.update { it.copy(phase = UpdatePhase.UpToDate) }
                    } else {
                        _state.update {
                            it.copy(
                                phase = UpdatePhase.Offer(
                                    latestVersionCode = manifest.latestVersionCode,
                                    apkUrl = manifest.apkUrl,
                                    releaseNotes = manifest.releaseNotes,
                                ),
                            )
                        }
                        if (autoDownloadApk && manifest.apkUrl.isNotBlank()) {
                            performDownload(manifest.apkUrl)
                        }
                    }
                },
                onFailure = {
                    _state.update {
                        it.copy(phase = UpdatePhase.Error(R.string.update_err_check))
                    }
                },
            )
        }
    }

    fun downloadOffer() {
        val offer = (_state.value.phase as? UpdatePhase.Offer) ?: return
        if (offer.apkUrl.isBlank()) return
        viewModelScope.launch {
            performDownload(offer.apkUrl)
        }
    }

    private suspend fun performDownload(url: String) {
        _state.update { it.copy(phase = UpdatePhase.Downloading) }
        val target = File(context.cacheDir, "updates/manufacturing_update.apk")
        updateRepository.downloadApk(url, target).fold(
            onSuccess = {
                _state.update {
                    it.copy(phase = UpdatePhase.ReadyInstall(target))
                }
            },
            onFailure = {
                _state.update {
                    it.copy(phase = UpdatePhase.Error(R.string.update_err_download))
                }
            },
        )
    }

    fun clearError() {
        _state.update {
            if (it.phase is UpdatePhase.Error) it.copy(phase = UpdatePhase.Idle) else it
        }
    }

    fun dismissOffer() {
        _state.update {
            when (it.phase) {
                is UpdatePhase.Offer,
                is UpdatePhase.UpToDate,
                is UpdatePhase.ReadyInstall,
                -> it.copy(phase = UpdatePhase.Idle)

                else -> it
            }
        }
    }

    private companion object {
        private fun readInstalledVersion(context: Context): Pair<Int, String> {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            val longVc =
                if (Build.VERSION.SDK_INT >= 28) {
                    pi.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pi.versionCode.toLong()
                }
            val vc = longVc.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            return vc to (pi.versionName.orEmpty())
        }
    }
}
