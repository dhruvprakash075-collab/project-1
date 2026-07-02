package com.openfiles.locked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.data.SecurityRepository
import com.openfiles.core.data.db.LockedFileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LockedFolderUi {
    data object Loading : LockedFolderUi
    data object SetupPin : LockedFolderUi
    data class EnterPin(val error: Boolean = false) : LockedFolderUi
    data class Unlocked(val items: List<LockedFileEntity>) : LockedFolderUi
}

@HiltViewModel
class LockedFolderViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow<LockedFolderUi>(LockedFolderUi.Loading)
    val ui: StateFlow<LockedFolderUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            _ui.value = if (securityRepository.hasPin()) LockedFolderUi.EnterPin() else LockedFolderUi.SetupPin
        }
    }

    fun setupPin(pin: String) = viewModelScope.launch {
        securityRepository.setPin(pin)
        unlock()
    }

    fun submitPin(pin: String) = viewModelScope.launch {
        if (securityRepository.verifyPin(pin)) {
            unlock()
        } else {
            _ui.value = LockedFolderUi.EnterPin(error = true)
        }
    }

    private fun unlock() = viewModelScope.launch {
        securityRepository.lockedItems.collect { items ->
            _ui.value = LockedFolderUi.Unlocked(items)
        }
    }

    fun restore(item: LockedFileEntity) = viewModelScope.launch {
        securityRepository.unlockFile(item)
    }
}
