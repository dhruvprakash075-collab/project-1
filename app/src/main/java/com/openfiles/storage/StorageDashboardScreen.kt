package com.openfiles.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.UiState
import com.openfiles.core.common.toHumanReadableSize
import com.openfiles.core.data.CategoryUsage
import com.openfiles.core.data.StorageCategory
import com.openfiles.core.data.StorageSummary
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDashboardScreen(
    onBack: () -> Unit,
    viewModel: StorageDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                UiState.Loading -> CenteredProgress()
                UiState.Empty -> CenteredProgress()
                is UiState.Error -> ErrorState(s.message, onRetry = viewModel::refresh)
                is UiState.Content -> StorageContent(s.data)
            }
        }
    }
}

@Composable
private fun StorageContent(summary: StorageSummary) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val usedFraction = if (summary.totalBytes == 0L) 0f else summary.usedBytes.toFloat() / summary.totalBytes
            Text(
                "${summary.usedBytes.toHumanReadableSize()} used of ${summary.totalBytes.toHumanReadableSize()}",
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { usedFraction },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Text("${summary.freeBytes.toHumanReadableSize()} free", style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(summary.categories.filter { it.fileCount > 0 }, key = { it.category }) { usage ->
                CategoryCard(usage)
            }
        }
    }
}

@Composable
private fun CategoryCard(usage: CategoryUsage) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        ListItem(
            leadingContent = { Icon(usage.category.icon(), contentDescription = null) },
            headlineContent = { Text(usage.category.label()) },
            supportingContent = { Text("${usage.fileCount} files") },
            trailingContent = { Text(usage.sizeBytes.toHumanReadableSize()) },
        )
    }
}

private fun StorageCategory.icon(): ImageVector = when (this) {
    StorageCategory.IMAGES -> Icons.Filled.Image
    StorageCategory.VIDEO -> Icons.Filled.Videocam
    StorageCategory.AUDIO -> Icons.Filled.MusicNote
    StorageCategory.DOCUMENTS -> Icons.Filled.Description
    StorageCategory.APPS -> Icons.Filled.Apps
    StorageCategory.DOWNLOADS -> Icons.Filled.Download
    StorageCategory.OTHER -> Icons.Filled.InsertDriveFile
}

private fun StorageCategory.label(): String = when (this) {
    StorageCategory.IMAGES -> "Images"
    StorageCategory.VIDEO -> "Video"
    StorageCategory.AUDIO -> "Audio"
    StorageCategory.DOCUMENTS -> "Documents"
    StorageCategory.APPS -> "Apps"
    StorageCategory.DOWNLOADS -> "Downloads"
    StorageCategory.OTHER -> "Other"
}
