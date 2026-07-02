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
                    context.contentResolver.openInputStream(android.net.Uri.parse(uriString))!!.use { input ->
                        val bytes = input.readBytes().take(maxBytes).toByteArray()
                        String(bytes, Charsets.UTF_8)
                    }
                }
                if (text.isEmpty()) UiState.Empty else UiState.Content(text)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not open file", e)
            }
        }
    }
}
