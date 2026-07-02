package com.openfiles.feature.viewer.pdf

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.Route
import com.openfiles.core.common.UiState
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState

/** Lazy, page-by-page PDF viewer built on the native android.graphics.pdf.PdfRenderer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    route: Route.Pdf,
    viewModel: PdfViewerViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }

    LaunchedEffect(route.uriString) { viewModel.open(route.uriString) }

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
            UiState.Empty -> EmptyState("This PDF has no pages", Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, onRetry = { viewModel.open(route.uriString) }, modifier = Modifier.padding(padding))
            is UiState.Content -> LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(s.data.pageCount) { index ->
                    PdfPage(viewModel = viewModel, index = index, widthPx = widthPx)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(viewModel: PdfViewerViewModel, index: Int, widthPx: Int) {
    val bitmapState = produceState(initialValue = null as android.graphics.Bitmap?, index) {
        value = viewModel.pageBitmap(index, widthPx)
    }
    bitmapState.value?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Page ${index + 1}",
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
    }
}
