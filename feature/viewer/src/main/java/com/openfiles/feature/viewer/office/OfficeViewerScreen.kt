package com.openfiles.feature.viewer.office

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.Route
import com.openfiles.core.common.UiState
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState

/** Read-only viewer for .xlsx (grid), .docx (paragraphs), .pptx (per-slide text) via Apache POI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeViewerScreen(
    route: Route.Office,
    viewModel: OfficeViewerViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(route.uriString) { viewModel.open(route.uriString, route.kind) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is OfficeEvent.RequestExternalViewer -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(
                            Uri.parse(event.uriString),
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> CenteredProgress(Modifier.padding(padding))
            UiState.Empty -> EmptyState("This document has no content", Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, onRetry = { viewModel.open(route.uriString, route.kind) }, modifier = Modifier.padding(padding))
            is UiState.Content -> OfficeContentView(s.data, Modifier.padding(padding))
        }
    }
}

@Composable
private fun OfficeContentView(content: OfficeContent, modifier: Modifier) {
    when (content) {
        is OfficeContent.Word -> LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
            items(content.paragraphs) { para ->
                Text(para, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        is OfficeContent.Slides -> LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
            items(content.slides) { slide ->
                Text("Slide ${slide.index + 1}", style = MaterialTheme.typography.titleMedium)
                slide.textBlocks.forEach { Text(it, modifier = Modifier.padding(vertical = 2.dp)) }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }
        }
        is OfficeContent.Excel -> LazyColumn(modifier = modifier.fillMaxSize()) {
            content.sheets.forEach { sheet ->
                item {
                    Text(sheet.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                }
                items(sheet.rows) { row ->
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        row.forEach { cell ->
                            Text(cell, modifier = Modifier.padding(end = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
