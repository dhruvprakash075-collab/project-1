package com.openfiles.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.openfiles.core.data.db.FileDao
import com.openfiles.core.data.db.OpenFilesDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `doc_annotations` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `documentUri` TEXT NOT NULL,
                `anchorIndex` INTEGER NOT NULL,
                `sheetName` TEXT,
                `note` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OpenFilesDb =
        Room.databaseBuilder(context, OpenFilesDb::class.java, "openfiles.db")
            .addMigrations(MIGRATION_3_4)
            .build()

    @Provides
    fun provideFileDao(db: OpenFilesDb): FileDao = db.fileDao()
}
