package com.openfiles.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.FileItem
import com.openfiles.core.common.Route
import com.openfiles.core.common.UiState
import com.openfiles.core.common.toDisplayDate
import com.openfiles.core.common.toHumanReadableSize
import com.openfiles.core.ui.FileOpener
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    initialPath: String? = null,
    viewModelKey: String? = null,
    viewModel: BrowserViewModel = hiltViewModel(key = viewModelKey),
    onOpenRoute: (Route) -> Unit,
) {
    LaunchedEffect(initialPath) {
        initialPath?.let { viewModel.open(Paths.get(it)) }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
    val opProgress by viewModel.opProgress.collectAsStateWithLifecycle()
    val renameTarget by viewModel.renameTarget.collectAsStateWithLifecycle()
    val showNewFolderDialog by viewModel.showNewFolderDialog.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isCurrentPathBookmarked.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowserEvent.ShowUndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Moved to trash",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.item)
                }
                is BrowserEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when {
                selected.isNotEmpty() -> SelectionTopBar(
                    count = selected.size,
                    onClose = viewModel::clearSelection,
                    onCopy = viewModel::copySelectionToClipboard,
                    onMove = viewModel::moveSelectionToClipboard,
                    onDelete = viewModel::deleteSelection,
                    onCompress = viewModel::compressSelection,
                    onLock = viewModel::lockSelection,
                    onRename = {
                        (state as? UiState.Content)?.data?.items
                            ?.firstOrNull { it.uri.toString() in selected }
                            ?.let(viewModel::requestRename)
                    },
                )
                isSearchActive -> SearchTopBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onClose = viewModel::deactivateSearch,
                )
                else -> TopAppBar(
                    title = { Text("Files") },
                    actions = {
                        IconButton(onClick = viewModel::toggleBookmark) {
                            Icon(
                                if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark this folder",
                            )
                        }
                        IconButton(onClick = { onOpenRoute(Route.Bookmarks) }) {
                            Icon(Icons.Filled.Folder, contentDescription = "Bookmarks")
                        }
                        IconButton(onClick = { onOpenRoute(Route.Locked) }) {
                            Icon(Icons.Filled.Lock, contentDescription = "Locked folder")
                        }
                        IconButton(onClick = { onOpenRoute(Route.Storage) }) {
                            Icon(Icons.Filled.Storage, contentDescription = "Storage dashboard")
                        }
                        IconButton(onClick = { onOpenRoute(Route.CloudConnections) }) {
                            Icon(Icons.Filled.Storage, contentDescription = "Cloud and network")
                        }
                        IconButton(onClick = viewModel::activateSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search this folder")
                        }
                        SortMenuButton(current = sortOption, onSelect = viewModel::setSortOption)
                        OverflowMenuButton(onOpenRoute = onOpenRoute)
                    },
                )
            }
        },
        floatingActionButton = {
            if (selected.isEmpty() && clipboard == null && !isSearchActive) {
                FloatingActionButton(onClick = viewModel::requestNewFolder) {
                    Icon(Icons.Filled.Add, contentDescription = "New folder")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selected.isEmpty() && !isSearchActive) {
                TabStrip(tabs = tabs, activeTabId = activeTabId, onSelect = viewModel::switchTab, onClose = viewModel::closeTab, onAdd = viewModel::addTab)
                Breadcrumbs(path = currentPath, onNavigate = viewModel::open)
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val s = state) {
                    UiState.Loading -> CenteredProgress()
                    UiState.Empty -> EmptyState(
                        if (searchQuery.isNotBlank()) "No files match \"$searchQuery\"" else "This folder is empty",
                    )
                    is UiState.Error -> ErrorState(s.message, onRetry = viewModel::refresh)
                    is UiState.Content -> LazyColumn {
                        items(s.data.items, key = { it.uri.toString() }) { file ->
                            FileRow(
                                file = file,
                                selected = selected.contains(file.uri.toString()),
                                onClick = {
                                    when {
                                        selected.isNotEmpty() -> viewModel.toggleSelected(file)
                                        file.isDirectory -> file.path?.let { viewModel.open(Paths.get(it)) }
                                        else -> {
                                            viewModel.recordOpened(file)
                                            onOpenRoute(FileOpener.resolve(context, file))
                                        }
                                    }
                                },
                                onLongClick = { viewModel.toggleSelected(file) },
                            )
                        }
                    }
                }
            }

            clipboard?.let { op ->
                PasteBar(
                    op = op,
                    progress = opProgress,
                    onPaste = viewModel::pasteIntoCurrentFolder,
                    onCancel = viewModel::cancelClipboard,
                )
            }
        }
    }

    if (showNewFolderDialog) {
        NameDialog(
            title = "New folder",
            confirmLabel = "Create",
            onDismiss = viewModel::dismissNewFolder,
            onConfirm = viewModel::confirmNewFolder,
        )
    }

    renameTarget?.let { item ->
        NameDialog(
            title = "Rename",
            confirmLabel = "Rename",
            initialValue = item.name,
            onDismiss = viewModel::dismissRename,
            onConfirm = viewModel::confirmRename,
        )
    }
}

@Composable
private fun Breadcrumbs(path: Path, onNavigate: (Path) -> Unit) {
    val segments = remember(path) {
        val parts = path.toString().trim('/').split('/').filter { it.isNotEmpty() }
        var acc: Path = Paths.get("/")
        val list = mutableListOf("Storage" to acc)
        parts.forEach { part ->
            acc = acc.resolve(part)
            list += part to acc
        }
        list
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(segments) { index, (label, segmentPath) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { onNavigate(segmentPath) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
                if (index != segments.lastIndex) {
                    Text("\u203A", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Search this folder") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close search")
            }
        },
    )
}

@Composable
private fun SortMenuButton(current: SortOption, onSelect: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    leadingIcon = {
                        if (option == current) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun SortOption.label(): String = when (this) {
    SortOption.NAME -> "Name"
    SortOption.DATE -> "Date modified"
    SortOption.SIZE -> "Size"
}

@Composable
private fun OverflowMenuButton(onOpenRoute: (Route) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Bookmarks") }, onClick = { expanded = false; onOpenRoute(Route.Bookmarks) })
            DropdownMenuItem(text = { Text("Locked folder") }, onClick = { expanded = false; onOpenRoute(Route.Locked) })
            DropdownMenuItem(text = { Text("Storage dashboard") }, onClick = { expanded = false; onOpenRoute(Route.Storage) })
            DropdownMenuItem(text = { Text("App manager") }, onClick = { expanded = false; onOpenRoute(Route.AppManager) })
        }
    }
}

@Composable
private fun TabStrip(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onAdd: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(tabs, key = { it.id }) { tab ->
            val label = tab.path.fileName?.toString() ?: "Storage"
            AssistChip(
                onClick = { onSelect(tab.id) },
                label = { Text(label) },
                colors = if (tab.id == activeTabId) {
                    AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    AssistChipDefaults.assistChipColors()
                },
                trailingIcon = if (tabs.size > 1) {
                    {
                        IconButton(onClick = { onClose(tab.id) }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close tab", modifier = Modifier.size(14.dp))
                        }
                    }
                } else null,
            )
        }
        item {
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New tab")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onLock: () -> Unit,
    onRename: () -> Unit,
) {
    TopAppBar(
        title = { Text("$count selected") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Clear selection")
            }
        },
        actions = {
            if (count == 1) {
                IconButton(onClick = onRename) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename")
                }
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = onMove) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move")
            }
            IconButton(onClick = onCompress) {
                Icon(Icons.Filled.FolderZip, contentDescription = "Compress to zip")
            }
            IconButton(onClick = onLock) {
                Icon(Icons.Filled.Lock, contentDescription = "Move to locked folder")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        },
    )
}

@Composable
private fun PasteBar(
    op: ClipboardOp,
    progress: OpProgress?,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            if (progress != null) {
                val label = if (progress.mode == ClipboardMode.COPY) "Copying" else "Moving"
                Text("$label ${progress.done}/${progress.total}...", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(
                    progress = { if (progress.total == 0) 0f else progress.done.toFloat() / progress.total },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            } else {
                val verb = if (op.mode == ClipboardMode.COPY) "Copy" else "Move"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$verb ${op.items.size} item(s) here?", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        TextButton(onClick = onCancel) { Text("Cancel") }
                        TextButton(onClick = onPaste) { Text("Paste") }
                    }
                }
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    confirmLabel: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    file: FileItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (selected) {
            Checkbox(checked = true, onCheckedChange = { onLongClick() })
        } else {
            Icon(
                imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                contentDescription = null,
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(file.name, style = MaterialTheme.typography.bodyLarge)
            val subtitle = if (file.isDirectory) file.lastModified.toDisplayDate()
            else "${file.sizeBytes.toHumanReadableSize()} \u00B7 ${file.lastModified.toDisplayDate()}"
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
