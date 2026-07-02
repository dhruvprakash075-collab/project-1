package com.openfiles.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey val uri: String,
    val name: String,
    val mimeType: String?,
    val lastOpened: Long,
)

@Entity(tableName = "trash_items")
data class TrashItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val trashPath: String,
    val deletedAt: Long,
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val path: String,
    val label: String,
    val createdAt: Long,
)

@Entity(tableName = "locked_items")
data class LockedFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val encryptedFileName: String,
)
