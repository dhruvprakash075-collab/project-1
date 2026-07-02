package com.openfiles.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.data.TrashRepository
import com.openfiles.core.data.db.TrashItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TrashEvent {
    data class ShowError(val message: String) : TrashEvent
}

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository,
) : ViewModel() {

    val items = trashRepository.trash.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<TrashEvent>()
    val events: SharedFlow<TrashEvent> = _events.asSharedFlow()

    fun restore(item: TrashItem) = viewModelScope.launch {
        val restored = trashRepository.restore(item)
        if (!restored) {
            _events.emit(TrashEvent.ShowError("Could not restore ${item.originalPath.substringAfterLast('/')}"))
        }
    }

    fun purge(item: TrashItem) = viewModelScope.launch { trashRepository.purge(item) }
}
