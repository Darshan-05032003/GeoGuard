package com.example.geoguard.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.geoguard.data.model.Group
import com.example.geoguard.util.InviteCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsScreen(
    uiState: GroupsUiState,
    onGroupClick: (groupId: String) -> Unit,
    onCreateClick: () -> Unit,
    onJoin: (code: String) -> Unit,
    onClearMessages: () -> Unit,
    onLogout: () -> Unit
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.joinedGroupName) {
        val name = uiState.joinedGroupName ?: return@LaunchedEffect
        showJoinDialog = false
        snackbarHostState.showSnackbar("You joined $name")
        onClearMessages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Groups") },
                actions = { TextButton(onClick = onLogout) { Text("Logout") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreateClick) { Text("Create group") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onClearMessages()
                    showJoinDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join with invite code")
            }
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.loadError != null -> {
                    Text(text = uiState.loadError, color = MaterialTheme.colorScheme.error)
                }
                uiState.groups.isEmpty() -> {
                    Text(
                        text = "You're not in any group yet. Create one or join with a code.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.groups, key = { group -> group.id }) { group ->
                            GroupCard(group = group, onClick = { onGroupClick(group.id) })
                        }
                    }
                }
            }
        }
    }

    if (showJoinDialog) {
        JoinGroupDialog(
            isJoining = uiState.isJoining,
            errorMessage = uiState.joinError,
            onJoin = onJoin,
            onDismiss = { showJoinDialog = false }
        )
    }
}

@Composable
private fun GroupCard(group: Group, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = group.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${group.memberIds.size} member(s) · ${group.zone.radiusMeters.toInt()} m zone",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Invite code: ${group.inviteCode}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun JoinGroupDialog(
    isJoining: Boolean,
    errorMessage: String?,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join a group") },
        text = {
            Column {
                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        code = input.uppercase().take(InviteCodeGenerator.CODE_LENGTH)
                    },
                    label = { Text("6-character invite code") },
                    singleLine = true
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(code) },
                enabled = !isJoining && code.length == InviteCodeGenerator.CODE_LENGTH
            ) {
                Text(if (isJoining) "Joining..." else "Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}