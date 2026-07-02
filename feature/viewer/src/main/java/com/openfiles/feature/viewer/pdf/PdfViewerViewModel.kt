package com.openfiles.feature.viewer.pdf

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PdfUiData(val pageCount: Int, val renderer: PdfPageRenderer)

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<PdfUiData>>(UiState.Loading)
    val state: StateFlow<UiState<PdfUiData>> = _state.asStateFlow()

    private val pageCache = mutableMapOf<Int, Bitmap>()
    private var renderer: PdfPageRenderer? = null

    fun open(uriString: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val r = withContext(Dispatchers.IO) {
                    PdfPageRenderer(context, android.net.Uri.parse(uriString))
                }
                renderer = r
                if (r.pageCount == 0) UiState.Empty else UiState.Content(PdfUiData(r.pageCount, r))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not open PDF", e)
            }
        }
    }

    suspend fun pageBitmap(index: Int, targetWidth: Int): Bitmap? = withContext(Dispatchers.Default) {
        pageCache.getOrPut(index) {
            renderer?.renderPage(index, targetWidth) ?: return@withContext null
        }
    }

    override fun onCleared() {
        renderer?.close()
        pageCache.values.forEach { it.recycle() }
        pageCache.clear()
    }
}
