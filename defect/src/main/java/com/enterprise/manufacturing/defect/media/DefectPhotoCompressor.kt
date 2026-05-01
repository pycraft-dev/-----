package com.enterprise.manufacturing.defect.media

import android.content.Context
import android.graphics.Bitmap
import com.enterprise.manufacturing.core.utils.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сжатие фото перед сохранением (ориентир ≤ 2 МБ для синка).
 */
@Singleton
class DefectPhotoCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun compressToFile(source: File, destination: File): File =
        withContext(dispatchers.io) {
            destination.parentFile?.mkdirs()
            val compressed = Compressor.compress(context, source, dispatchers.io) {
                default(format = Bitmap.CompressFormat.JPEG)
            }
            compressed.copyTo(destination, overwrite = true)
            destination
        }
}
