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
}
