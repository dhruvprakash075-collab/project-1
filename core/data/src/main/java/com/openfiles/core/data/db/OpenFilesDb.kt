package com.openfiles.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RecentFile::class, TrashItem::class], version = 1, exportSchema = true)
abstract class OpenFilesDb : RoomDatabase() {
    abstract fun fileDao(): FileDao
}
