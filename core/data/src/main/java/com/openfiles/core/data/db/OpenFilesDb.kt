package com.openfiles.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecentFile::class, TrashItem::class, Bookmark::class, LockedFileEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class OpenFilesDb : RoomDatabase() {
    abstract fun fileDao(): FileDao
}
