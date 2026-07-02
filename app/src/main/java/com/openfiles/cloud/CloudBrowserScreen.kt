package com.openfiles.cloud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.Route
import com.openfiles.core.common.UiState
import com.openfiles.core.common.toHumanReadableSize
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBrowserScreen(route: Route.CloudBrowser, onBack: () -> Unit, viewModel: CloudBrowserViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val path by viewModel.path.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(route.connectionId) { viewModel.start(route.connectionId, route.path) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CloudBrowserEvent.OpenFile -> context.startActivity(event.intent)
                is CloudBrowserEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(path.ifBlank { "Root" }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> CenteredProgress()
            UiState.Empty -> EmptyState("This folder is empty", Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, onRetry = { viewModel.open(path) })
            is UiState.Content -> LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(s.data, key = { it.name }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { if (!item.isDirectory) Text(item.sizeBytes.toHumanReadableSize()) },
                        leadingContent = { Icon(if (item.isDirectory) Icons.Filled.Folder else Icons.Filled.Description, contentDescription = null) },
                        modifier = Modifier.clickable {
                            if (item.isDirectory) {
                                val newPath = if (path.isBlank()) item.name else "$path\\${item.name}"
                                viewModel.open(newPath)
                            } else {
                                viewModel.openFile(item)
                            }
                        },
                    )
                }
            }
        }
    }
}
