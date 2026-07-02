package com.openfiles.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.FileItem
import com.openfiles.core.common.UiState
import com.openfiles.core.data.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<FileItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<FileItem>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val media = mediaRepository.loadMedia()
                if (media.isEmpty()) UiState.Empty else UiState.Content(media)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not load media", e)
            }
        }
    }
}
