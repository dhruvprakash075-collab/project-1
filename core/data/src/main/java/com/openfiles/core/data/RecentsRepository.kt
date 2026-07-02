package com.openfiles.core.data

import com.openfiles.core.common.FileItem
import com.openfiles.core.data.db.FileDao
import com.openfiles.core.data.db.RecentFile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentsRepository @Inject constructor(private val dao: FileDao) {
    val recents: Flow<List<RecentFile>> get() = dao.recents()

    suspend fun recordOpened(file: FileItem) {
        dao.addRecent(
            RecentFile(
                uri = file.uri.toString(),
                name = file.name,
                mimeType = file.mimeType,
                lastOpened = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun clear() = dao.clearRecents()
}
