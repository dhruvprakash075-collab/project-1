package com.openfiles.duplicates

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.UiState
import com.openfiles.core.data.DuplicateFinderRepository
import com.openfiles.core.data.DuplicateGroup
import com.openfiles.core.data.TrashRepository
import com.openfiles.core.ui.permissions.StoragePermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val repository: DuplicateFinderRepository,
    private val trashRepository: TrashRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<DuplicateGroup>>>(UiState.Loading)
    val state: StateFlow<UiState<List<DuplicateGroup>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            if (!StoragePermissions.hasStorageAccess(context)) {
                _state.value = UiState.Error("Storage permission required")
                return@launch
            }
            _state.value = try {
                val groups = repository.findDuplicates()
                if (groups.isEmpty()) UiState.Empty else UiState.Content(groups)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not scan for duplicates", e)
            }
        }
    }

    fun trashDuplicatesKeepingFirst(group: DuplicateGroup) = viewModelScope.launch {
        group.files.drop(1).forEach { file -> trashRepository.moveToTrash(file) }
        refresh()
    }
}
