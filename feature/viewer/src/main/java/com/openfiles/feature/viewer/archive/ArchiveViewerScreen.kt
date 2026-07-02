package com.openfiles.feature.viewer.archive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.Route
import com.openfiles.core.common.UiState
import com.openfiles.core.common.toHumanReadableSize
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState

/** Lists zip/tar/tar.gz entries read-only; full extraction is a single explicit action (Phase 5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerScreen(
    route: Route.Archive,
    viewModel: ArchiveViewerViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(route.uriString) { viewModel.open(route.uriString, route.title) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> CenteredProgress(Modifier.padding(padding))
            UiState.Empty -> EmptyState("This archive is empty", Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, onRetry = { viewModel.open(route.uriString, route.title) }, modifier = Modifier.padding(padding))
            is UiState.Content -> LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(s.data) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.name) },
                        supportingContent = { if (!entry.isDirectory) Text(entry.sizeBytes.toHumanReadableSize()) },
                        leadingContent = {
                            Icon(if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.Description, contentDescription = null)
                        },
                    )
                }
            }
        }
    }
}
