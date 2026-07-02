package com.openfiles.core.data.di

import android.content.Context
import androidx.room.Room
import com.openfiles.core.data.db.FileDao
import com.openfiles.core.data.db.OpenFilesDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OpenFilesDb =
        Room.databaseBuilder(context, OpenFilesDb::class.java, "openfiles.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFileDao(db: OpenFilesDb): FileDao = db.fileDao()
}
