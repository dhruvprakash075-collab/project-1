package com.openfiles.core.common

import android.net.Uri

/** Immutable, source-agnostic representation of a filesystem entry (NIO2 path or SAF document). */
data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String?,
    /** Absolute filesystem path when known (NIO2 source); null for pure SAF/document tree items. */
    val path: String? = null,
)

/** Navigation destinations shared across feature modules. Kept here so :core:common has no upward deps. */
sealed interface Route {
    data class Browser(val path: String? = null) : Route
    data object Gallery : Route
    data class Pdf(val uriString: String, val title: String) : Route
    data class Image(val uriString: String, val title: String) : Route
    data class Media(val uriString: String, val title: String) : Route
    data class Office(val uriString: String, val title: String, val kind: OfficeKind) : Route
    data class Text(val uriString: String, val title: String) : Route
    data class Archive(val uriString: String, val title: String) : Route
    data object Bookmarks : Route
    data object Locked : Route
    data object Settings : Route
    data object Trash : Route
    data object Storage : Route
    data object AppManager : Route
}

enum class OfficeKind { XLSX, DOCX, PPTX }
