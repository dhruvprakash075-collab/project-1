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

@Singleton
class TrashRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: FileDao,
) {
    private val trashDir: File get() = File(context.filesDir, "trash").apply { mkdirs() }

    val trash: Flow<List<TrashItem>> get() = dao.trash()

    suspend fun moveToTrash(file: File): TrashItem? = withContext(Dispatchers.IO) {
        val target = File(trashDir, "${System.currentTimeMillis()}_${file.name}")
        if (file.renameTo(target)) {
            val item = TrashItem(
                originalPath = file.absolutePath,
                trashPath = target.absolutePath,
                deletedAt = System.currentTimeMillis(),
            )
            val id = dao.addTrash(item)
            item.copy(id = id)
        } else {
            null
        }
    }

    suspend fun restore(item: TrashItem) = withContext(Dispatchers.IO) {
        val trashFile = File(item.trashPath)
        val original = File(item.originalPath)
        original.parentFile?.mkdirs()
        if (trashFile.renameTo(original)) {
            dao.removeTrash(item)
        }
    }

    suspend fun purge(item: TrashItem) = withContext(Dispatchers.IO) {
        File(item.trashPath).deleteRecursively()
        dao.removeTrash(item)
    }
}
