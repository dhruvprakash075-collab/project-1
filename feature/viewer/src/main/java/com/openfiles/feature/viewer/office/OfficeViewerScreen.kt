package com.openfiles.feature.viewer.office

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.OfficeKind
import com.openfiles.core.common.Route
import com.openfiles.core.common.UiState
import com.openfiles.core.data.DocSearchMatch
import com.openfiles.core.data.ExcelRepository
import com.openfiles.core.data.db.DocAnnotation
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState
import java.io.File
import kotlinx.coroutines.launch

/** Read-only viewer for .xlsx (grid), .docx (paragraphs), .pptx (per-slide text) via Apache POI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeViewerScreen(
    route: Route.Office,
    viewModel: OfficeViewerViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchActive by viewModel.searchActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val annotations by remember(route.uriString) { viewModel.annotationsFlow(route.uriString) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(route.uriString) { viewModel.open(route.uriString, route.kind) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is OfficeEvent.RequestExternalViewer -> {
                    val uri = externalViewerUri(context, event.uriString)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(
                            uri,
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                }
                is OfficeEvent.JumpToMatch -> {
                    coroutineScope.launch { listState.animateScrollToItem(event.anchorIndex) }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(route.title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    },
                    actions = {
                        IconButton(onClick = viewModel::toggleSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search in document")
                        }
                    },
                )
                if (searchActive) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        results = searchResults,
                        onResultClick = { match ->
                            val target = if (route.kind == OfficeKind.XLSX) {
                                val content = (state as? UiState.Content)?.data as? OfficeContent.Excel
                                content?.let { excelFlatIndex(it.sheets, match) } ?: match.anchorIndex
                            } else {
                                match.anchorIndex
                            }
                            viewModel.jumpTo(match.copy(anchorIndex = target))
                        },
                    )
                }
            }
        },
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> CenteredProgress(Modifier.padding(padding))
            UiState.Empty -> EmptyState("This document has no content", Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, onRetry = { viewModel.open(route.uriString, route.kind) }, modifier = Modifier.padding(padding))
            is UiState.Content -> OfficeContentView(
                content = s.data,
                modifier = Modifier.padding(padding),
                listState = listState,
                annotations = annotations,
                onAddAnnotation = { anchorIndex, note, sheetName -> viewModel.addAnnotation(anchorIndex, note, sheetName) },
                onRemoveAnnotation = viewModel::removeAnnotation,
            )
        }
    }
}

private fun externalViewerUri(context: android.content.Context, uriString: String): Uri {
    val uri = Uri.parse(uriString)
    return if (uri.scheme == "file") {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(requireNotNull(uri.path)),
        )
    } else {
        uri
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<DocSearchMatch>,
    onResultClick: (DocSearchMatch) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search in document") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (query.isNotBlank()) {
            Text(
                "${results.size} match${if (results.size == 1) "" else "es"}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            results.take(20).forEach { match ->
                ListItem(
                    headlineContent = { Text(match.snippet, maxLines = 1) },
                    supportingContent = match.sheetName?.let { name -> { Text(name) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun excelFlatIndex(sheets: List<ExcelRepository.Sheet>, match: DocSearchMatch): Int {
    var index = 0
    for (sheet in sheets) {
        index += 1
        if (sheet.name == match.sheetName) {
            return index + match.anchorIndex
        }
        index += sheet.rows.size
    }
    return 0
}

@Composable
private fun OfficeContentView(
    content: OfficeContent,
    modifier: Modifier,
    listState: LazyListState,
    annotations: List<DocAnnotation>,
    onAddAnnotation: (anchorIndex: Int, note: String, sheetName: String?) -> Unit,
    onRemoveAnnotation: (DocAnnotation) -> Unit,
) {
    when (content) {
        is OfficeContent.Word -> LazyColumn(state = listState, modifier = modifier.fillMaxSize().padding(16.dp)) {
            items(content.paragraphs.size) { index ->
                val paragraph = content.paragraphs[index]
                val noteForParagraph = annotations.firstOrNull { it.anchorIndex == index }
                WordParagraphRow(
                    text = paragraph,
                    note = noteForParagraph,
                    onAddNote = { note -> onAddAnnotation(index, note, null) },
                    onRemoveNote = { noteForParagraph?.let(onRemoveAnnotation) },
                )
            }
        }
        is OfficeContent.Slides -> LazyColumn(state = listState, modifier = modifier.fillMaxSize().padding(16.dp)) {
            items(content.slides, key = { it.index }) { slide ->
                val sheetName = "Slide ${slide.index + 1}"
                val noteForSlide = annotations.firstOrNull { it.anchorIndex == slide.index && it.sheetName == sheetName }
                Text("Slide ${slide.index + 1}", style = MaterialTheme.typography.titleMedium)
                slide.textBlocks.forEach { Text(it, modifier = Modifier.padding(vertical = 2.dp)) }
                NoteButtonRow(
                    note = noteForSlide,
                    onAddNote = { note -> onAddAnnotation(slide.index, note, sheetName) },
                    onRemoveNote = { noteForSlide?.let(onRemoveAnnotation) },
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }
        }
        is OfficeContent.Excel -> LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
            content.sheets.forEach { sheet ->
                item {
                    Text(sheet.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                }
                itemsIndexed(sheet.rows) { rowIndex, row ->
                    val noteForRow = annotations.firstOrNull { it.anchorIndex == rowIndex && it.sheetName == sheet.name }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        row.forEach { cell ->
                            Text(cell, modifier = Modifier.padding(end = 16.dp))
                        }
                        NoteButtonRow(
                            note = noteForRow,
                            onAddNote = { note -> onAddAnnotation(rowIndex, note, sheet.name) },
                            onRemoveNote = { noteForRow?.let(onRemoveAnnotation) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteButtonRow(
    note: DocAnnotation?,
    onAddNote: (String) -> Unit,
    onRemoveNote: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        if (note != null) {
            Card(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(note.note, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRemoveNote) { Text("Remove") }
                }
            }
        }
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Filled.NoteAdd, contentDescription = "Add note")
        }
    }
    if (showDialog) {
        NoteDialog(
            initialValue = note?.note.orEmpty(),
            onDismiss = { showDialog = false },
            onSave = { draft ->
                onAddNote(draft)
                showDialog = false
            },
        )
    }
}

@Composable
private fun WordParagraphRow(
    text: String,
    note: DocAnnotation?,
    onAddNote: (String) -> Unit,
    onRemoveNote: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text)
            if (note != null) {
                Card(modifier = Modifier.padding(top = 4.dp)) {
                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(note.note, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = onRemoveNote) { Text("Remove") }
                    }
                }
            }
        }
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Filled.NoteAdd, contentDescription = "Add note")
        }
    }
    if (showDialog) {
        NoteDialog(
            initialValue = note?.note.orEmpty(),
            onDismiss = { showDialog = false },
            onSave = { draft ->
                onAddNote(draft)
                showDialog = false
            },
        )
    }
}

@Composable
private fun NoteDialog(initialValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var draft by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note") },
        text = {
            OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
