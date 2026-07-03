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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.openfiles.appmanager.AppManagerScreen
import com.openfiles.bookmarks.BookmarksScreen
import com.openfiles.cloud.CloudBrowserScreen
import com.openfiles.cloud.CloudConnectionsScreen
import com.openfiles.core.common.Route
import com.openfiles.duplicates.DuplicatesScreen
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
 * Backstack-as-state-list navigation (Navigation 3 style): each bottom tab keeps its own mutable
 * route stack, and viewer/detail screens push on top of the currently selected tab.
 * On screens >= 600dp wide, the Browser destination shows two independent panes side by side
 * (each its own BrowserViewModel via a distinct hiltViewModel key); cross-pane copy/move is not
 * wired up in this version -- each pane's clipboard is independent.
 */
@Composable
fun OpenFilesNavGraph(startRoute: Route = Route.Browser()) {
    val tabStacks = remember {
        mutableStateMapOf(
            Route.Browser::class to mutableStateListOf<Route>(Route.Browser()),
            Route.Gallery::class to mutableStateListOf<Route>(Route.Gallery),
            Route.Trash::class to mutableStateListOf<Route>(Route.Trash),
            Route.Settings::class to mutableStateListOf<Route>(Route.Settings),
        )
    }
    var selectedTab by remember { mutableStateOf(tabKey(startRoute) ?: Route.Browser::class) }
    val initialTab = tabKey(startRoute)
    if (initialTab != null && tabStacks[initialTab]?.firstOrNull() != startRoute) {
        tabStacks[initialTab]?.apply {
            clear()
            add(startRoute)
        }
        selectedTab = initialTab
    }
    val backstack = tabStacks.getValue(selectedTab)
    val current = backstack.last()
    val showBottomBar = current is Route.Browser || current is Route.Gallery ||
        current is Route.Settings || current is Route.Trash

    fun pop() {
        if (backstack.size > 1) backstack.removeAt(backstack.lastIndex)
    }

    fun push(route: Route) {
        backstack.add(route)
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    current = current,
                    onSelect = { route ->
                        selectedTab = tabKey(route) ?: selectedTab
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
                            initialPath = route.path,
                            viewModelKey = "browserPaneLeft",
                            onOpenRoute = ::push,
                        )
                    }
                    VerticalDivider()
                    Box(modifier = Modifier.fillMaxWidth(0.5f)) {
                        BrowserScreen(
                            initialPath = route.path,
                            viewModelKey = "browserPaneRight",
                            onOpenRoute = ::push,
                        )
                    }
                }
            } else {
                BrowserScreen(
                    modifier = Modifier.padding(padding),
                    initialPath = route.path,
                    onOpenRoute = ::push,
                )
            }
            Route.Gallery -> GalleryScreen(
                modifier = Modifier.padding(padding),
                onOpenRoute = ::push,
            )
            Route.Settings -> SettingsScreen(modifier = Modifier.padding(padding))
            Route.Trash -> TrashScreen(modifier = Modifier.padding(padding))
            Route.Bookmarks -> BookmarksScreen(onBack = ::pop, onOpenRoute = ::push)
            Route.CloudConnections -> CloudConnectionsScreen(onBack = ::pop, onOpenRoute = ::push)
            is Route.CloudBrowser -> CloudBrowserScreen(route = route, onBack = ::pop)
            Route.Locked -> LockedFolderScreen(onBack = ::pop)
            is Route.Pdf -> PdfViewerScreen(route = route, onBack = ::pop)
            is Route.Image -> ImageViewerScreen(route = route, onBack = ::pop)
            is Route.Media -> MediaPlayerScreen(route = route, onBack = ::pop)
            is Route.Office -> OfficeViewerScreen(route = route, onBack = ::pop)
            is Route.Text -> TextViewerScreen(route = route, onBack = ::pop)
            is Route.Archive -> ArchiveViewerScreen(route = route, onBack = ::pop)
            Route.Storage -> StorageDashboardScreen(onBack = ::pop, onOpenRoute = ::push)
            Route.Duplicates -> DuplicatesScreen(onBack = ::pop)
            Route.AppManager -> AppManagerScreen(onBack = ::pop)
        }
    }
}

private fun tabKey(route: Route) = when (route) {
    is Route.Browser -> Route.Browser::class
    Route.Gallery -> Route.Gallery::class
    Route.Trash -> Route.Trash::class
    Route.Settings -> Route.Settings::class
    else -> null
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
