package com.openfiles.locked

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.ui.components.CenteredProgress
import com.openfiles.core.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedFolderScreen(onBack: () -> Unit, viewModel: LockedFolderViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Locked folder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (val state = ui) {
                LockedFolderUi.Loading -> CenteredProgress()
                is LockedFolderUi.SetupPin -> PinEntry(
                    title = "Set a PIN to protect this folder",
                    onSubmit = viewModel::setupPin,
                )
                is LockedFolderUi.EnterPin -> PinEntry(
                    title = if (state.error) "Wrong PIN, try again" else "Enter your PIN",
                    onSubmit = viewModel::submitPin,
                )
                is LockedFolderUi.Unlocked -> if (state.items.isEmpty()) {
                    EmptyState("No locked files yet")
                } else {
                    LazyColumn {
                        items(state.items, key = { it.id }) { item ->
                            ListItem(
                                headlineContent = { Text(item.originalPath.substringAfterLast('/')) },
                                supportingContent = { Text(item.originalPath) },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.restore(item) }) {
                                        Icon(Icons.Filled.LockOpen, contentDescription = "Restore")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinEntry(title: String, onSubmit: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title)
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
        )
        Button(onClick = { onSubmit(pin) }, enabled = pin.length >= 4) { Text("Continue") }
    }
}
