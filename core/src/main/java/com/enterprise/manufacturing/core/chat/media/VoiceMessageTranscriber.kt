package com.enterprise.manufacturing.core.chat.media

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.max

private data class PcmParams(val sampleRate: Int, val channelCount: Int, val encoding: Int)

/** Частота, с которой документация Google обычно сопоставляет [RecognizerIntent.EXTRA_AUDIO_SOURCE]. */
private const val STT_PREFERRED_SAMPLE_RATE_HZ = 16_000

/**
 * Расшифровка голосового из локального файла (AAC/M4A): декод → PCM → системный [SpeechRecognizer]
 * ([RecognizerIntent.EXTRA_AUDIO_SOURCE], Android 13+).
 */
object VoiceMessageTranscriber {

    suspend fun transcribe(
        context: Context,
        audioFile: File,
        locale: Locale = Locale.forLanguageTag("ru-RU"),
    ): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return null

        val pcmRaw = File(context.cacheDir, "voice_stt_${UUID.randomUUID()}.pcm")
        val decoded =
            decodeCompressedAudioToPcmFile(audioFile, pcmRaw)
                ?: run {
                    pcmRaw.delete()
                    return null
                }

        val attempts = mutableListOf<Pair<File, Int>>()

        if (decoded.sampleRate != STT_PREFERRED_SAMPLE_RATE_HZ) {
            val resampled =
                File(context.cacheDir, "voice_stt_16k_${UUID.randomUUID()}.pcm")
            if (resamplePcm16MonoFile(pcmRaw, decoded.sampleRate, STT_PREFERRED_SAMPLE_RATE_HZ, resampled)) {
                attempts.add(resampled to STT_PREFERRED_SAMPLE_RATE_HZ)
            }
        }
        attempts.add(pcmRaw to decoded.sampleRate)

        return try {
            var last: String? = null
            for ((file, rate) in attempts) {
                last = recognizePcmWithSpeechRecognizer(context, file, rate, locale)
                if (!last.isNullOrBlank()) return last.trim()
            }
            null
        } finally {
            val deleteSet = attempts.map { it.first }.toMutableSet()
            deleteSet.add(pcmRaw)
            deleteSet.forEach { runCatching { it.delete() } }
        }
    }

    private suspend fun recognizePcmWithSpeechRecognizer(
        context: Context,
        pcmFile: File,
        sampleRateHz: Int,
        locale: Locale,
    ): String? =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { cont ->
                val resumed = AtomicBoolean(false)
                val partialTexts = mutableListOf<String>()
                var pfd: ParcelFileDescriptor? = null
                var sr: SpeechRecognizer? = null

                fun finish(value: String?) {
                    if (!resumed.compareAndSet(false, true)) return
                    runCatching { sr?.destroy() }
                    sr = null
                    runCatching { pfd?.close() }
                    pfd = null
                    cont.resume(value?.trim()?.takeIf { it.isNotEmpty() })
                }

                try {
                    pfd = ParcelFileDescriptor.open(pcmFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } catch (_: Exception) {
                    finish(null)
                    return@suspendCancellableCoroutine
                }

                cont.invokeOnCancellation {
                    runCatching { sr?.destroy() }
                    runCatching { pfd?.close() }
                }

                val intent =
                    android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pfd)
                        putExtra(
                            RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT,
                            1,
                        )
                        putExtra(
                            RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING,
                            AudioFormat.ENCODING_PCM_16BIT,
                        )
                        putExtra(
                            RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE,
                            sampleRateHz,
                        )
                    }

                sr = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                val listener =
                    object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            val fallback =
                                partialTexts.lastOrNull { it.isNotBlank() }?.trim()?.takeIf { it.isNotEmpty() }
                            finish(fallback)
                        }

                        override fun onResults(results: Bundle?) {
                            val text =
                                results
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull()
                                    .orEmpty()
                                    .trim()
                            val chosen =
                                text.takeIf { it.isNotEmpty() }
                                    ?: partialTexts.lastOrNull { it.isNotBlank() }?.trim()?.takeIf { it.isNotEmpty() }
                            finish(chosen)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val text =
                                partialResults
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull()
                                    ?.trim()
                                    .orEmpty()
                            if (text.isNotEmpty()) partialTexts.add(text)
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    }

                sr?.setRecognitionListener(listener)
                sr?.startListening(intent)
            }
        }

    /**
     * Декодирует первый аудиотрек в little-endian PCM16 моно.
     */
    private fun decodeCompressedAudioToPcmFile(input: File, output: File): PcmParams? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(input.absolutePath)
        } catch (_: Exception) {
            extractor.release()
            return null
        }

        var audioTrackIndex = -1
        var trackFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                trackFormat = f
                break
            }
        }
        if (audioTrackIndex < 0 || trackFormat == null) {
            extractor.release()
            return null
        }

        val mime = trackFormat.getString(MediaFormat.KEY_MIME)!!
        extractor.selectTrack(audioTrackIndex)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(trackFormat, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var inputEos = false
        var outputEos = false

        var sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        FileOutputStream(output).use { fos ->
            try {
                while (!outputEos) {
                    if (!inputEos) {
                        val inIx = codec.dequeueInputBuffer(20_000)
                        if (inIx >= 0) {
                            val buf = codec.getInputBuffer(inIx)!!
                            val size = extractor.readSampleData(buf, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(
                                    inIx,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputEos = true
                            } else {
                                val pts = extractor.sampleTime
                                codec.queueInputBuffer(inIx, 0, size, pts, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outIx = codec.dequeueOutputBuffer(bufferInfo, 20_000)
                    when {
                        outIx == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        outIx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val nf = codec.outputFormat
                            sampleRate = nf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channelCount = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }

                        outIx >= 0 -> {
                            val ob = codec.getOutputBuffer(outIx)!!
                            val outFmt = codec.outputFormat
                            val pcmEncoding =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                                    outFmt.containsKey(MediaFormat.KEY_PCM_ENCODING)
                                ) {
                                    outFmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                                } else {
                                    AudioFormat.ENCODING_PCM_16BIT
                                }

                            if (bufferInfo.size > 0) {
                                ob.position(bufferInfo.offset)
                                ob.limit(bufferInfo.offset + bufferInfo.size)
                                val monoS16 =
                                    downmixToMonoS16Le(ob, pcmEncoding, channelCount)
                                fos.write(monoS16)
                            }
                            codec.releaseOutputBuffer(outIx, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputEos = true
                            }
                        }
                    }
                }
            } finally {
                codec.stop()
                codec.release()
                extractor.release()
            }
        }

        if (!output.exists() || output.length() == 0L) return null
        return PcmParams(sampleRate = sampleRate, channelCount = 1, encoding = AudioFormat.ENCODING_PCM_16BIT)
    }

    private fun resamplePcm16MonoFile(
        src: File,
        srcRate: Int,
        dstRate: Int,
        dst: File,
    ): Boolean {
        if (srcRate <= 0 || dstRate <= 0) return false
        val bytes = src.readBytes()
        if (bytes.size < 4) return false
        val shorts =
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).run {
                val sb = asShortBuffer()
                val arr = ShortArray(sb.remaining())
                sb.get(arr)
                arr
            }
        val outShorts =
            if (srcRate == dstRate) {
                shorts
            } else {
                resampleShorts(shorts, srcRate, dstRate)
            }
        dst.writeBytes(shortsToBytesLe(outShorts))
        return dst.length() > 0
    }

    private fun resampleShorts(input: ShortArray, srcRate: Int, dstRate: Int): ShortArray {
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val outLen = max(1, (input.size / ratio).toInt())
        val out = ShortArray(outLen)
        for (i in out.indices) {
            val srcPos = i * ratio
            val idx = srcPos.toInt().coerceIn(0, input.lastIndex)
            val frac = srcPos - idx
            val s0 = input[idx].toInt()
            val s1 = input.getOrElse(idx + 1) { input[idx] }.toInt()
            out[i] =
                (s0 + (s1 - s0) * frac)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
        }
        return out
    }

    private fun downmixToMonoS16Le(
        buffer: ByteBuffer,
        pcmEncoding: Int,
        channelCount: Int,
    ): ByteArray {
        buffer.order(ByteOrder.nativeOrder())
        val monoShorts: ShortArray =
            when (pcmEncoding) {
                AudioFormat.ENCODING_PCM_FLOAT -> {
                    val fb = buffer.asFloatBuffer()
                    val floats = FloatArray(fb.remaining())
                    fb.get(floats)
                    val shorts = ShortArray(floats.size / channelCount.coerceAtLeast(1))
                    var f = 0
                    val ch = channelCount.coerceAtLeast(1)
                    for (i in shorts.indices) {
                        var sum = 0f
                        repeat(ch) {
                            if (f < floats.size) sum += floats[f++]
                        }
                        val avg = sum / ch
                        shorts[i] = (avg.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                    }
                    shorts
                }

                else -> {
                    val sb = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val shorts = ShortArray(sb.remaining())
                    sb.get(shorts)
                    val ch = channelCount.coerceAtLeast(1)
                    if (ch <= 1) {
                        shorts
                    } else {
                        val framesOut = ShortArray(shorts.size / ch)
                        var idx = 0
                        for (i in framesOut.indices) {
                            var sum = 0
                            repeat(ch) {
                                if (idx < shorts.size) sum += shorts[idx++].toInt()
                            }
                            framesOut[i] = (sum / ch).toShort()
                        }
                        framesOut
                    }
                }
            }
        return shortsToBytesLe(monoShorts)
    }

    private fun shortsToBytesLe(samples: ShortArray): ByteArray {
        val out = ByteArray(samples.size * 2)
        var o = 0
        for (s in samples) {
            val v = s.toInt()
            out[o++] = (v and 0xff).toByte()
            out[o++] = ((v shr 8) and 0xff).toByte()
        }
        return out
    }
}
