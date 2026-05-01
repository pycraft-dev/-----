package com.enterprise.manufacturing.core.chat.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * Запись голоса в AAC/M4A под каталог приложения ([Context.filesDir]/chat_voice).
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    fun isRecording(): Boolean = recorder != null

    fun start(): File {
        stopInternal()
        val dir = File(context.filesDir, CHAT_VOICE_DIR).apply { mkdirs() }
        outputFile = File(dir, "voice_${UUID.randomUUID()}.m4a")
        val file = outputFile!!
        val mr =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setOutputFile(file.absolutePath)
        mr.prepare()
        mr.start()
        recorder = mr
        startedAtMs = android.os.SystemClock.elapsedRealtime()
        return file
    }

    /**
     * @return файл и длительность записи в мс (или null если записи не было).
     */
    fun stop(): Pair<File, Long>? {
        val file = outputFile ?: return null
        val dur =
            if (startedAtMs > 0L) {
                (android.os.SystemClock.elapsedRealtime() - startedAtMs).coerceAtLeast(0L)
            } else {
                0L
            }
        stopInternal()
        return file to dur
    }

    fun discard() {
        val f = outputFile
        stopInternal()
        f?.delete()
    }

    private fun stopInternal() {
        outputFile = null
        startedAtMs = 0L
        val mr = recorder ?: return
        recorder = null
        runCatching {
            mr.stop()
        }
        runCatching {
            mr.reset()
        }
        runCatching {
            mr.release()
        }
    }

    companion object {
        const val CHAT_VOICE_DIR = "chat_voice"
    }
}
