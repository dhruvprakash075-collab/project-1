package com.openfiles.feature.viewer.text

import android.content.Context
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
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class TextViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<String>>(UiState.Loading)
    val state: StateFlow<UiState<String>> = _state.asStateFlow()

    /** Cap at 2MB read for v1 — large text files get a truncation notice rather than an OOM. */
    private val maxBytes = 2 * 1024 * 1024

    fun open(uriString: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val text = withContext(Dispatchers.IO) {
                    val uri = android.net.Uri.parse(uriString)
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("Can't open file: $uri")
                    input.use { stream ->
                        val buffer = ByteArray(maxBytes)
                        var read = 0
                        while (read < maxBytes) {
                            val n = stream.read(buffer, read, maxBytes - read)
                            if (n < 0) break
                            read += n
                        }
                        String(buffer, 0, read, Charsets.UTF_8)
                    }
                }
                if (text.isEmpty()) UiState.Empty else UiState.Content(text)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not open file", e)
            }
        }
    }
}
