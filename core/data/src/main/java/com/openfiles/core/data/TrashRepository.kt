package com.openfiles.core.data

import android.content.Context
import com.openfiles.core.data.db.FileDao
import com.openfiles.core.data.db.TrashItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Soft-delete: moves a file into the app's private trash directory and records the original path
 * so it can be restored. Trash is purged permanently on explicit "Empty trash" only (never a timer
 * in v1, to avoid silently deleting user data).
 *
 * File.renameTo() only works within a single filesystem/mount point -- it reliably fails (returns
 * false, no side effects) when moving between external storage and the app's internal filesDir,
 * which is exactly what every real move here does. Falls back to copy-then-delete when rename
 * fails, the standard Google-confirmed fix for this exact situation (see ADR-024).
 */
@Singleton
class TrashRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: FileDao,
) {
    private val trashDir: File get() = File(context.filesDir, "trash").apply { mkdirs() }

    val trash: Flow<List<TrashItem>> get() = dao.trash()

    suspend fun moveToTrash(file: File): TrashItem? = withContext(Dispatchers.IO) {
        val target = File(trashDir, "${System.currentTimeMillis()}_${file.name}")
        if (!moveFile(file, target)) return@withContext null
        val item = TrashItem(
            originalPath = file.absolutePath,
            trashPath = target.absolutePath,
            deletedAt = System.currentTimeMillis(),
        )
        val id = dao.addTrash(item)
        item.copy(id = id)
    }

    suspend fun restore(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        val trashFile = File(item.trashPath)
        val original = File(item.originalPath)
        original.parentFile?.mkdirs()
        val moved = moveFile(trashFile, original)
        if (moved) dao.removeTrash(item)
        moved
    }

    suspend fun purge(item: TrashItem) = withContext(Dispatchers.IO) {
        File(item.trashPath).deleteRecursively()
        dao.removeTrash(item)
    }

    /**
     * Moves [source] to [destination]: tries the fast same-filesystem rename first, falls back to
     * copy-then-delete for cross-volume moves (external storage <-> app-internal storage).
     */
    private fun moveFile(source: File, destination: File): Boolean {
        if (source.renameTo(destination)) return true
        return try {
            source.copyTo(destination, overwrite = true)
            val deleted = source.delete()
            if (!deleted) destination.delete()
            deleted
        } catch (e: Exception) {
            destination.delete()
            false
        }
    }
}
