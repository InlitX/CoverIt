package com.coverit.app.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.coverit.app.doc.DocLayout
import com.coverit.app.doc.DocType
import com.coverit.app.doc.Family
import com.coverit.app.office.OfficeDoc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

object Thumbs {

    private val cache = LruCache<String, Bitmap>(48)
    private val lock = Semaphore(3)

    private fun key(file: File, widthPx: Int, page: Int) = "${file.path}:${file.lastModified()}:$widthPx:$page"

    fun peek(file: File, widthPx: Int, page: Int = 0): Bitmap? = cache.get(key(file, widthPx, page))

    suspend fun load(file: File, type: DocType, widthPx: Int, page: Int = 0): Bitmap? = withContext(Dispatchers.IO) {
        val key = key(file, widthPx, page)
        cache.get(key)?.let { return@withContext it }
        val bitmap = lock.withPermit { runCatching { decode(file, type, widthPx, page) }.getOrNull() }
        bitmap?.also { cache.put(key, it) }
    }

    fun pageCount(file: File): Int = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { it.pageCount } }
    }.getOrDefault(1)

    private fun decode(file: File, type: DocType, widthPx: Int, page: Int): Bitmap? {
        if (type.family == Family.OFFICE) {
            val layout = DocLayout(OfficeDoc.open(file, type.office!!).text, monospace = type.monospace)
            return layout.render(page.coerceIn(0, layout.pages.size - 1), widthPx)
        }
        if (type == DocType.PDF) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    renderer.openPage(page.coerceIn(0, renderer.pageCount - 1)).use {
                        val height = (widthPx.toLong() * it.height / it.width).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        return bitmap
                    }
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= widthPx) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
