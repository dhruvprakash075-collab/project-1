package com.openfiles.appmanager

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.UiState
import com.openfiles.core.common.toHumanReadableSize
import com.openfiles.core.data.InstalledApp
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState
import com.openfiles.shizuku.ShizukuChecker
import com.openfiles.shizuku.ShizukuState
import com.openfiles.shizuku.rememberShizukuState

/** Lists installed apps; uninstall via the system dialog (default) or, once Shizuku is granted, bulk-uninstall a selection with no per-app confirmation dialog. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppManagerScreen(onBack: () -> Unit, viewModel: AppManagerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showSystemApps by viewModel.showSystemApps.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val shizukuState = rememberShizukuState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AppManagerEvent.BackupComplete -> snackbarHostState.showSnackbar("Saved to ${event.path}")
                is AppManagerEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is AppManagerEvent.BulkUninstallComplete ->
                    snackbarHostState.showSnackbar("Uninstalled ${event.succeeded}, failed ${event.failed}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (selected.isEmpty()) "App manager" else "${selected.size} selected") },
                navigationIcon = {
                    IconButton(onClick = { if (selected.isEmpty()) onBack() else viewModel.clearSelection() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selected.isNotEmpty() && shizukuState == ShizukuState.READY) {
                        IconButton(onClick = viewModel::bulkUninstallSelected) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Uninstall selected (Shizuku)")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Show system apps")
                Checkbox(checked = showSystemApps, onCheckedChange = { viewModel.toggleShowSystemApps() })
            }
            if (shizukuState == ShizukuState.NEEDS_PERMISSION) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Shizuku detected \u2014 grant access for bulk uninstall")
                    TextButton(onClick = { ShizukuChecker.requestPermission() }) { Text("Grant") }
                }
            }
            when (val s = state) {
                UiState.Loading -> CenteredProgress()
                UiState.Empty -> EmptyState("No apps found")
                is UiState.Error -> ErrorState(s.message, onRetry = {})
                is UiState.Content -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(s.data, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            isSelected = app.packageName in selected,
                            selectionModeActive = selected.isNotEmpty(),
                            onBackup = { viewModel.backup(app) },
                            onToggleSelected = { viewModel.toggleSelected(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: InstalledApp,
    isSelected: Boolean,
    selectionModeActive: Boolean,
    onBackup: () -> Unit,
    onToggleSelected: () -> Unit,
) {
    val context = LocalContext.current
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { if (selectionModeActive) onToggleSelected() },
            onLongClick = onToggleSelected,
        ),
        leadingContent = if (selectionModeActive) {
            { Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() }) }
        } else {
            null
        },
        headlineContent = { Text(app.label) },
        supportingContent = {
            Text("${app.packageName} \u00B7 ${app.versionName ?: "?"} \u00B7 ${app.sizeBytes.toHumanReadableSize()}")
        },
        trailingContent = {
            Row {
                IconButton(onClick = onBackup) {
                    Icon(Icons.Filled.Save, contentDescription = "Backup APK")
                }
                IconButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Uninstall")
                }
            }
        },
    )
}
