package com.openfiles.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.openfiles.MainActivity
import com.openfiles.core.data.db.Bookmark
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

const val ACTION_OPEN_BOOKMARK = "com.openfiles.action.OPEN_BOOKMARK"
const val EXTRA_BOOKMARK_PATH = "bookmark_path"

/**
 * Keeps Android's home-screen dynamic shortcuts in sync with the user's bookmarks. Capped at 4 --
 * Android's own shortcuts guidance recommends publishing only 4 distinct shortcuts even though the
 * OS technically allows up to 5, for visual consistency in the launcher's long-press menu.
 */
@Singleton
class ShortcutSyncer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun sync(bookmarks: List<Bookmark>) {
        val shortcuts = bookmarks
            .sortedByDescending { it.createdAt }
            .take(4)
            .mapIndexed { index, bookmark ->
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_OPEN_BOOKMARK
                    putExtra(EXTRA_BOOKMARK_PATH, bookmark.path)
                }
                ShortcutInfoCompat.Builder(context, "bookmark_${bookmark.path.hashCode()}")
                    .setShortLabel(bookmark.label.take(10))
                    .setLongLabel(bookmark.label)
                    .setIcon(IconCompat.createWithResource(context, com.openfiles.R.drawable.ic_launcher))
                    .setIntent(intent)
                    .setRank(index)
                    .build()
            }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
