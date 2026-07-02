package com.openfiles.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import java.nio.file.Paths

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    viewModel: BrowserViewModel = hiltViewModel(),
    onOpenRoute: (Route) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
    val opProgress by viewModel.opProgress.collectAsStateWithLifecycle()
    val renameTarget by viewModel.renameTarget.collectAsStateWithLifecycle()
    val showNewFolderDialog by viewModel.showNewFolderDialog.collectAsStateWithLifecycle()
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
            if (selected.isNotEmpty()) {
                SelectionTopBar(
                    count = selected.size,
                    onClose = viewModel::clearSelection,
                    onCopy = viewModel::copySelectionToClipboard,
                    onMove = viewModel::moveSelectionToClipboard,
                    onDelete = viewModel::deleteSelection,
                    onRename = {
                        (state as? UiState.Content)?.data?.items
                            ?.firstOrNull { it.uri.toString() in selected }
                            ?.let(viewModel::requestRename)
                    },
                )
            } else {
                TopAppBar(title = { Text("Files") })
            }
        },
        floatingActionButton = {
            if (selected.isEmpty() && clipboard == null) {
                FloatingActionButton(onClick = viewModel::requestNewFolder) {
                    Icon(Icons.Filled.Add, contentDescription = "New folder")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (val s = state) {
                    UiState.Loading -> CenteredProgress()
                    UiState.Empty -> EmptyState("This folder is empty")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
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
                Icon(Icons.Filled.DriveFileMove, contentDescription = "Move")
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
                Text("$label ${progress.done}/${progress.total}…", style = MaterialTheme.typography.bodyMedium)
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
            else "${file.sizeBytes.toHumanReadableSize()} · ${file.lastModified.toDisplayDate()}"
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
