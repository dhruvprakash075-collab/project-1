package com.openfiles.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.data.TrashRepository
import com.openfiles.core.data.db.TrashItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository,
) : ViewModel() {

    val items = trashRepository.trash.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(item: TrashItem) = viewModelScope.launch { trashRepository.restore(item) }
    fun purge(item: TrashItem) = viewModelScope.launch { trashRepository.purge(item) }
}
