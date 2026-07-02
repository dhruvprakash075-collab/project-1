package com.openfiles.feature.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.openfiles.core.common.Route
import com.openfiles.core.common.UiState
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState
import com.openfiles.core.ui.components.ErrorState

/** Photos + videos grid backed by MediaStore. Thumbnails load lazily via Coil (memory-safe). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    onOpenRoute: (Route) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Gallery") }) },
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> CenteredProgress(Modifier.padding(padding))
            UiState.Empty -> EmptyState("No photos or videos yet", Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            is UiState.Content -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                items(s.data, key = { it.uri.toString() }) { media ->
                    AsyncImage(
                        model = media.uri,
                        contentDescription = media.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val mime = media.mimeType.orEmpty()
                                onOpenRoute(
                                    if (mime.startsWith("video/")) {
                                        Route.Media(media.uri.toString(), media.name)
                                    } else {
                                        Route.Image(media.uri.toString(), media.name)
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}
