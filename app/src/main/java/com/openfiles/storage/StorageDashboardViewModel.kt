package com.openfiles.storage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.UiState
import com.openfiles.core.data.StorageRepository
import com.openfiles.core.data.StorageSummary
import com.openfiles.core.ui.permissions.StoragePermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageDashboardViewModel @Inject constructor(
    private val repository: StorageRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<StorageSummary>>(UiState.Loading)
    val state: StateFlow<UiState<StorageSummary>> = _state.asStateFlow()

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
                UiState.Content(repository.summarize())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not read storage", e)
            }
        }
    }
}
