package com.openfiles.core.ui

import android.content.Context
import android.content.Intent
import com.openfiles.core.common.FileItem
import com.openfiles.core.common.OfficeKind
import com.openfiles.core.common.Route

/**
 * MIME router for "open a file". The final `else` branch always uses a neutral system chooser —
 * it never names or promotes a specific proprietary app (keeps the app F-Droid/anti-feature safe).
 */
object FileOpener {
    private val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    private val PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    private val ARCHIVE_EXT = setOf("zip", "tar", "gz", "tgz", "7z", "bz2", "xz")

    fun resolve(context: Context, file: FileItem): Route {
        val mime = file.mimeType ?: context.contentResolver.getType(file.uri) ?: "*/*"
        val ext = file.name.substringAfterLast('.', "").lowercase()
        return when {
            mime == "application/pdf" -> Route.Pdf(file.uri.toString(), file.name)
            mime.startsWith("image/") -> Route.Image(file.uri.toString(), file.name)
            mime.startsWith("video/") || mime.startsWith("audio/") -> Route.Media(file.uri.toString(), file.name)
            mime == XLSX -> Route.Office(file.uri.toString(), file.name, OfficeKind.XLSX)
            mime == DOCX -> Route.Office(file.uri.toString(), file.name, OfficeKind.DOCX)
            mime == PPTX -> Route.Office(file.uri.toString(), file.name, OfficeKind.PPTX)
            ext in ARCHIVE_EXT -> Route.Archive(file.uri.toString(), file.name)
            mime.startsWith("text/") -> Route.Text(file.uri.toString(), file.name)
            else -> {
                openExternally(context, file, mime)
                Route.Browser()
            }
        }
    }

    private fun openExternally(context: Context, file: FileItem, mime: String) {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(view, null))
    }
}
