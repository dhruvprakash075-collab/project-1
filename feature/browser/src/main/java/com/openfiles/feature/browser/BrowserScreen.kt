package com.openfiles.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.FileItem
import com.openfiles.core.common.Route
import com.openfiles.core.common.toDisplayDate
import com.openfiles.core.common.toHumanReadableSize
import com.openfiles.core.common.UiState
import com.openfiles.core.ui.FileOpener
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState

/** The main file browser screen: lists the current directory and routes taps through [FileOpener]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    viewModel: BrowserViewModel = hiltViewModel(),
    onOpenRoute: (Route) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Files") })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
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
                                if (file.isDirectory) {
                                    file.path?.let { viewModel.open(java.nio.file.Paths.get(it)) }
                                } else {
                                    viewModel.recordOpened(file)
                                    onOpenRoute(FileOpener.resolve(context, file))
                                }
                            },
                            onLongClick = { viewModel.toggleSelected(file) },
                        )
                    }
                }
            }
        }
    }
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
