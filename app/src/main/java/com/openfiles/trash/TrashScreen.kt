package com.openfiles.trash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.ui.components.EmptyState

/** Soft-deleted files: restore to original location or purge permanently (no auto-expiry in v1). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(modifier: Modifier = Modifier, viewModel: TrashViewModel = viewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Trash") }) },
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState("Trash is empty", Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(items, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.originalPath.substringAfterLast('/')) },
                        supportingContent = { Text(item.originalPath) },
                        trailingContent = {
                            androidx.compose.foundation.layout.Row {
                                IconButton(onClick = { viewModel.restore(item) }) {
                                    Icon(Icons.Filled.Restore, contentDescription = "Restore")
                                }
                                IconButton(onClick = { viewModel.purge(item) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete permanently")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
