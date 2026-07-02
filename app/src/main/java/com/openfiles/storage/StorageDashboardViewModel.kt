package com.openfiles.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.UiState
import com.openfiles.core.data.StorageRepository
import com.openfiles.core.data.StorageSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageDashboardViewModel @Inject constructor(
    private val repository: StorageRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<StorageSummary>>(UiState.Loading)
    val state: StateFlow<UiState<StorageSummary>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(repository.summarize())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not read storage", e)
            }
        }
    }
}
