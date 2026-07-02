package com.openfiles.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.openfiles.bookmarks.BookmarksScreen
import com.openfiles.core.common.Route
import com.openfiles.feature.browser.BrowserScreen
import com.openfiles.feature.gallery.GalleryScreen
import com.openfiles.feature.viewer.archive.ArchiveViewerScreen
import com.openfiles.feature.viewer.image.ImageViewerScreen
import com.openfiles.feature.viewer.media.MediaPlayerScreen
import com.openfiles.feature.viewer.office.OfficeViewerScreen
import com.openfiles.feature.viewer.pdf.PdfViewerScreen
import com.openfiles.feature.viewer.text.TextViewerScreen
import com.openfiles.locked.LockedFolderScreen
import com.openfiles.settings.SettingsScreen
import com.openfiles.storage.StorageDashboardScreen
import com.openfiles.trash.TrashScreen

/**
 * Backstack-as-state-list navigation (Navigation 3 style): a simple mutable list of [Route]s is
 * the single source of truth. Bottom tabs reset to their tab root; viewer screens push on top.
 * On screens >= 600dp wide, the Browser destination shows two independent panes side by side
 * (each its own BrowserViewModel via a distinct hiltViewModel key); cross-pane copy/move is not
 * wired up in this version -- each pane's clipboard is independent.
 */
@Composable
fun OpenFilesNavGraph() {
    val backstack = remember { mutableStateListOf<Route>(Route.Browser()) }

    val current = backstack.last()
    val showBottomBar = current is Route.Browser || current is Route.Gallery ||
        current is Route.Settings || current is Route.Trash

    fun pop() {
        if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    current = current,
                    onSelect = { route ->
                        backstack.clear()
                        backstack.add(route)
                    },
                )
            }
        },
    ) { padding ->
        when (val route = current) {
            is Route.Browser -> if (screenWidthDp >= 600) {
                Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth(0.5f)) {
                        BrowserScreen(
                            viewModelKey = "browserPaneLeft",
                            onOpenRoute = { backstack.add(it) },
                        )
                    }
                    VerticalDivider()
                    Box(modifier = Modifier.fillMaxWidth(0.5f)) {
                        BrowserScreen(
                            viewModelKey = "browserPaneRight",
                            onOpenRoute = { backstack.add(it) },
                        )
                    }
                }
            } else {
                BrowserScreen(
                    modifier = Modifier.padding(padding),
                    initialPath = route.path,
                    onOpenRoute = { backstack.add(it) },
                )
            }
            Route.Gallery -> GalleryScreen(
                modifier = Modifier.padding(padding),
                onOpenRoute = { backstack.add(it) },
            )
            Route.Settings -> SettingsScreen(modifier = Modifier.padding(padding))
            Route.Trash -> TrashScreen(modifier = Modifier.padding(padding))
            Route.Bookmarks -> BookmarksScreen(onBack = ::pop, onOpenRoute = { backstack.add(it) })
            Route.Locked -> LockedFolderScreen(onBack = ::pop)
            is Route.Pdf -> PdfViewerScreen(route = route, onBack = ::pop)
            is Route.Image -> ImageViewerScreen(route = route, onBack = ::pop)
            is Route.Media -> MediaPlayerScreen(route = route, onBack = ::pop)
            is Route.Office -> OfficeViewerScreen(route = route, onBack = ::pop)
            is Route.Text -> TextViewerScreen(route = route, onBack = ::pop)
            is Route.Archive -> ArchiveViewerScreen(route = route, onBack = ::pop)
            Route.Storage -> StorageDashboardScreen(onBack = ::pop)
        }
    }
}

@Composable
private fun BottomBar(current: Route, onSelect: (Route) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = current is Route.Browser,
            onClick = { onSelect(Route.Browser()) },
            icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
            label = { Text("Files") },
        )
        NavigationBarItem(
            selected = current is Route.Gallery,
            onClick = { onSelect(Route.Gallery) },
            icon = { Icon(Icons.Filled.Photo, contentDescription = null) },
            label = { Text("Gallery") },
        )
        NavigationBarItem(
            selected = current is Route.Trash,
            onClick = { onSelect(Route.Trash) },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            label = { Text("Trash") },
        )
        NavigationBarItem(
            selected = current is Route.Settings,
            onClick = { onSelect(Route.Settings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Settings") },
        )
    }
}
