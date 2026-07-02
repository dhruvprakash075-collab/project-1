package com.openfiles.feature.viewer.image

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.openfiles.core.common.Route
import com.openfiles.core.data.ImageEditOp

/**
 * Single-image viewer with a v1 edit toolbar: rotate left/right and a grayscale filter, written
 * back to the original file in place. Pinch-to-zoom is left to Coil/system defaults. Interactive
 * freeform cropping is a bigger UI effort and is deliberately out of scope here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(route: Route.Image, onBack: () -> Unit, viewModel: ImageViewerViewModel = hiltViewModel()) {
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val reloadKey by viewModel.reloadKey.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ImageEditEvent.Saved -> snackbarHostState.showSnackbar("Saved")
                is ImageEditEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(route.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = { viewModel.edit(route.uriString, ImageEditOp.ROTATE_LEFT) }, enabled = !isSaving) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate left")
                }
                IconButton(onClick = { viewModel.edit(route.uriString, ImageEditOp.ROTATE_RIGHT) }, enabled = !isSaving) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate right")
                }
                IconButton(onClick = { viewModel.edit(route.uriString, ImageEditOp.GRAYSCALE) }, enabled = !isSaving) {
                    Icon(Icons.Filled.Contrast, contentDescription = "Grayscale filter")
                }
                if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }
        },
    ) { padding ->
        val imageRequest = remember(route.uriString, reloadKey) {
            ImageRequest.Builder(context)
                .data(route.uriString)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCacheKey("${route.uriString}_$reloadKey")
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = route.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(padding).fillMaxSize(),
        )
    }
}
