package com.openfiles

import android.app.Application
import com.openfiles.core.data.BookmarkRepository
import com.openfiles.shortcuts.ShortcutSyncer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class OpenFilesApp : Application() {

    @Inject lateinit var bookmarkRepository: BookmarkRepository
    @Inject lateinit var shortcutSyncer: ShortcutSyncer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { purgeStaleCacheFiles() }
        bookmarkRepository.bookmarks
            .onEach(shortcutSyncer::sync)
            .launchIn(appScope)
    }

    private fun purgeStaleCacheFiles() {
        val cutoff = System.currentTimeMillis() - CACHE_MAX_AGE_MS
        cacheDir.listFiles()
            ?.filter { file -> file.isStaleTempFile(cutoff) }
            ?.forEach { it.delete() }
    }

    private fun File.isStaleTempFile(cutoff: Long): Boolean =
        (name.startsWith("archive_") || name.startsWith("smb_")) && lastModified() < cutoff

    private companion object {
        const val CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L
    }
}
