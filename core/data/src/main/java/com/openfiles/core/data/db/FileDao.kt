package com.openfiles.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC LIMIT 50")
    fun recents(): Flow<List<RecentFile>>

    @Upsert
    suspend fun addRecent(file: RecentFile)

    @Query("DELETE FROM recent_files")
    suspend fun clearRecents()

    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun trash(): Flow<List<TrashItem>>

    @Insert
    suspend fun addTrash(item: TrashItem): Long

    @Delete
    suspend fun removeTrash(item: TrashItem)

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun bookmarks(): Flow<List<Bookmark>>

    @Upsert
    suspend fun addBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE path = :path")
    suspend fun removeBookmark(path: String)

    @Query("SELECT * FROM locked_items ORDER BY id DESC")
    fun lockedItems(): Flow<List<LockedFileEntity>>

    @Insert
    suspend fun addLockedItem(item: LockedFileEntity): Long

    @Delete
    suspend fun removeLockedItem(item: LockedFileEntity)

    @Query("SELECT * FROM smb_connections ORDER BY id DESC")
    fun smbConnections(): Flow<List<SmbConnection>>

    @Insert
    suspend fun addSmbConnection(connection: SmbConnection): Long

    @Query("DELETE FROM smb_connections WHERE id = :id")
    suspend fun removeSmbConnection(id: Long)

    @Query("SELECT * FROM doc_annotations WHERE documentUri = :documentUri ORDER BY anchorIndex ASC")
    fun annotationsForDocument(documentUri: String): Flow<List<DocAnnotation>>

    @Insert
    suspend fun addAnnotation(annotation: DocAnnotation): Long

    @Delete
    suspend fun removeAnnotation(annotation: DocAnnotation)
}
