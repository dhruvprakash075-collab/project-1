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

@Entity(tableName = "smb_connections")
data class SmbConnection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val host: String,
    val port: Int = 445,
    val shareName: String,
    val username: String,
    val domain: String? = null,
    val encryptedPassword: String,
)

/** Ring 3, F3: a lightweight text note anchored to a paragraph (Word), slide (PPTX), or row+sheet (Excel). */
@Entity(tableName = "doc_annotations")
data class DocAnnotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentUri: String,
    val anchorIndex: Int,
    val sheetName: String? = null,
    val note: String,
    val createdAt: Long,
)
