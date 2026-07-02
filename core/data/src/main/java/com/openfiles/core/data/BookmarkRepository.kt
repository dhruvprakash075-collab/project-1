package com.openfiles.core.data

import com.openfiles.core.data.db.Bookmark
import com.openfiles.core.data.db.FileDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val dao: FileDao,
) {
    val bookmarks: Flow<List<Bookmark>> = dao.bookmarks()

    suspend fun addBookmark(path: String, label: String) {
        dao.addBookmark(Bookmark(path = path, label = label, createdAt = System.currentTimeMillis()))
    }

    suspend fun removeBookmark(path: String) {
        dao.removeBookmark(path)
    }
}
