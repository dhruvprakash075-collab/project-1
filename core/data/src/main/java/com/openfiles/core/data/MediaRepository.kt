package com.openfiles.core.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.openfiles.core.common.FileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Queries MediaStore for the Gallery grid (images + videos), scoped-storage safe, no filesystem walk. */
@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun loadMedia(): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        items += queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false)
        items += queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
        items.sortedByDescending { it.lastModified }
    }

    private fun queryCollection(collection: Uri, isVideo: Boolean): List<FileItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val results = mutableListOf<FileItem>()
        val sort = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        context.contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(collection, id.toString())
                results += FileItem(
                    name = cursor.getString(nameCol) ?: "",
                    uri = uri,
                    isDirectory = false,
                    sizeBytes = cursor.getLong(sizeCol),
                    lastModified = cursor.getLong(dateCol) * 1000L,
                    mimeType = cursor.getString(mimeCol),
                )
            }
        }
        return results
    }
}
