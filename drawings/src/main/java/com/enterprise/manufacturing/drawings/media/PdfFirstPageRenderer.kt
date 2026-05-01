package com.enterprise.manufacturing.drawings.media



import android.graphics.Bitmap

import android.graphics.pdf.PdfRenderer

import android.os.ParcelFileDescriptor

import java.io.File



object PdfFirstPageRenderer {



    fun renderFirstPage(file: File, maxSidePx: Int = 1400): Bitmap? {

        if (!file.exists() || !file.canRead()) return null

        return try {

            val descriptor =

                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

                    ?: return null

            PdfRenderer(descriptor).use { renderer ->

                if (renderer.pageCount <= 0) return null

                renderer.openPage(0).use { page ->

                    val scale =

                        maxSidePx.toFloat() / maxOf(page.width, page.height).coerceAtLeast(1)

                    val w = (page.width * scale).toInt().coerceAtLeast(1)

                    val h = (page.height * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    bitmap

                }

            }

        } catch (_: Exception) {

            null

        }

    }

}

