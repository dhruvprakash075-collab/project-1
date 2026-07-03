package com.openfiles.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

import com.openfiles.core.data.db.DocAnnotation

@Database(
    entities = [RecentFile::class, TrashItem::class, Bookmark::class, LockedFileEntity::class, SmbConnection::class, DocAnnotation::class],
    version = 4,
    exportSchema = true,
)
abstract class OpenFilesDb : RoomDatabase() {
    abstract fun fileDao(): FileDao
}
