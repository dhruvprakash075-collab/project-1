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
import javax.inject.Inject

@HiltAndroidApp
class OpenFilesApp : Application() {

    @Inject lateinit var bookmarkRepository: BookmarkRepository
    @Inject lateinit var shortcutSyncer: ShortcutSyncer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        bookmarkRepository.bookmarks
            .onEach(shortcutSyncer::sync)
            .launchIn(appScope)
    }
}
