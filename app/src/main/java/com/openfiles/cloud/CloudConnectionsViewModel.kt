package com.openfiles.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.data.SmbRepository
import com.openfiles.core.data.db.SmbConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudConnectionsViewModel @Inject constructor(
    private val repository: SmbRepository,
) : ViewModel() {
    val connections: StateFlow<List<SmbConnection>> = repository.connections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addConnection(label: String, host: String, shareName: String, username: String, password: String, domain: String? = null, port: Int = 445) {
        viewModelScope.launch { repository.addConnection(label, host, shareName, username, password, domain, port) }
    }
    fun removeConnection(id: Long) = viewModelScope.launch { repository.removeConnection(id) }
}
