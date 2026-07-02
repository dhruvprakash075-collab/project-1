package com.openfiles.feature.viewer.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.Closeable

/**
 * Native PdfRenderer wrapper (zero third-party deps, API 21+). Renders ONE page at a time so
 * memory use never scales with document length — the core lesson from the plan's PDF section.
 */
class PdfPageRenderer(context: Context, uri: Uri) : Closeable {
    private val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")!!
    private val renderer = PdfRenderer(pfd)

    val pageCount: Int get() = renderer.pageCount

    fun renderPage(index: Int, targetWidth: Int): Bitmap {
        renderer.openPage(index).use { page ->
            val scale = targetWidth.toFloat() / page.width
            val bmp = Bitmap.createBitmap(
                targetWidth,
                (page.height * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            ).apply { eraseColor(Color.WHITE) }
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bmp
        }
    }

    override fun close() {
        renderer.close()
        pfd.close()
    }
}
