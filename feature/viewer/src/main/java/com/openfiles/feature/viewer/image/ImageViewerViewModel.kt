package com.openfiles.feature.viewer.image

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.data.ImageEditOp
import com.openfiles.core.data.ImageEditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ImageEditEvent {
    data object Saved : ImageEditEvent
    data class ShowError(val message: String) : ImageEditEvent
}

@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    private val repository: ImageEditRepository,
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Bumped after every successful edit so the image reloads instead of showing a stale cached bitmap. */
    private val _reloadKey = MutableStateFlow(0)
    val reloadKey: StateFlow<Int> = _reloadKey.asStateFlow()

    private val _events = MutableSharedFlow<ImageEditEvent>()
    val events: SharedFlow<ImageEditEvent> = _events.asSharedFlow()

    fun edit(uriString: String, op: ImageEditOp) = viewModelScope.launch {
        _isSaving.value = true
        val success = repository.apply(Uri.parse(uriString), op)
        _isSaving.value = false
        if (success) {
            _reloadKey.value += 1
            _events.emit(ImageEditEvent.Saved)
        } else {
            _events.emit(ImageEditEvent.ShowError("Could not save changes"))
        }
    }
}
