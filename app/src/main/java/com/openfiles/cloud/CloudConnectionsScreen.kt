package com.openfiles.cloud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.common.Route
import com.openfiles.core.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudConnectionsScreen(onBack: () -> Unit, onOpenRoute: (Route) -> Unit, viewModel: CloudConnectionsViewModel = hiltViewModel()) {
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud & network") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, contentDescription = "Add SMB connection") } },
    ) { padding ->
        if (connections.isEmpty()) {
            EmptyState("No saved connections yet.", Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(connections, key = { it.id }) { connection ->
                    ListItem(
                        headlineContent = { Text(connection.label) },
                        supportingContent = { Text("smb://${connection.host}:${connection.port}/${connection.shareName}") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = { IconButton(onClick = { viewModel.removeConnection(connection.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Remove") } },
                    )
                    TextButton(onClick = { onOpenRoute(Route.CloudBrowser(connection.id)) }) { Text("Browse") }
                }
            }
        }
    }
    if (showAddDialog) {
        AddSmbConnectionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { label, host, share, username, password, domain, port ->
                viewModel.addConnection(label, host, share, username, password, domain, port)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddSmbConnectionDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String, String, String?, Int) -> Unit) {
    var label by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("445") }
    var share by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add SMB connection") },
        text = {
            Column {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, singleLine = true)
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host / IP") }, singleLine = true)
                OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit).takeIf { it.isNotBlank() } ?: "445" }, label = { Text("Port") }, singleLine = true)
                OutlinedTextField(value = share, onValueChange = { share = it }, label = { Text("Share name") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(value = domain, onValueChange = { domain = it }, label = { Text("Domain (optional)") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(label, host, share, username, password, domain.ifBlank { null }, port.toIntOrNull() ?: 445) }, enabled = label.isNotBlank() && host.isNotBlank() && share.isNotBlank() && username.isNotBlank() && password.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
