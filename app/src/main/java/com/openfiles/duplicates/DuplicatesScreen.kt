package com.openfiles.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.UiState
import com.openfiles.core.common.toHumanReadableSize
import com.openfiles.core.data.DuplicateGroup
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(onBack: () -> Unit, viewModel: DuplicatesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicate files") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                UiState.Loading -> CenteredProgress()
                UiState.Empty -> EmptyState("No duplicate files found")
                is UiState.Error -> ErrorState(s.message, onRetry = viewModel::refresh)
                is UiState.Content -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(s.data, key = { it.hash }) { group ->
                        DuplicateGroupCard(group = group, onCleanUp = { viewModel.trashDuplicatesKeepingFirst(group) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(group: DuplicateGroup, onCleanUp: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            ListItem(
                headlineContent = { Text("${group.files.size} copies \u00B7 ${group.sizeBytes.toHumanReadableSize()} each") },
                supportingContent = { Text(group.files.first().name) },
            )
            TextButton(onClick = onCleanUp) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Keep first, trash the other ${group.files.size - 1}")
            }
        }
    }
}
